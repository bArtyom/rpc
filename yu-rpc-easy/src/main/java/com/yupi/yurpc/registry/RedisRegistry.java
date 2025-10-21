package com.yupi.yurpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis 注册中心实现
 * 
 * 设计思路：
 * 1. 使用 Redis String 类型存储服务信息，key 为服务节点路径，value 为 JSON
 * 2. 使用 EXPIRE 设置过期时间（30秒），模拟临时节点
 * 3. 通过定时任务（每10秒）续期，保持服务在线
 * 4. 使用 Redis Pub/Sub 实现服务变更监听
 * 5. 使用本地缓存提升查询性能
 */
@Slf4j
public class  RedisRegistry implements Registry {

    /**
     * Redis 连接池
     */
    private JedisPool jedisPool;

    /**
     * Redis 根路径
     */
    private static final String REDIS_ROOT_PATH = "/rpc/";

    /**
     * 服务过期时间（秒）
     */
    private static final int SERVICE_EXPIRE_TIME = 30;

    /**
     * 本地注册的节点 key 集合（用于维护续期）
     */
    private final Set<String> localRegisterNodeKeySet = new ConcurrentHashSet<>();

    /**
     * 注册中心服务缓存
     */
    private final RegistryServiceCache registryServiceCache = new RegistryServiceCache();

    /**
     * 正在监听的 key 集合
     */
    private final Set<String> watchingKeySet = new ConcurrentHashSet<>();

    /**
     * 监听器线程
     */
    private Thread watchThread;

    @Override
    public void init(RegistryConfig registryConfig) {
        // 解析 Redis 地址（格式：host:port 或 host:port:password）
        String address = registryConfig.getAddress();
        String[] parts = address.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;
        String password = parts.length > 2 ? parts[2] : null;

        // 配置 Redis 连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);

        // 创建连接池
        if (password != null && !password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, 2000);
        }

        log.info("Redis 注册中心初始化成功: {}:{}", host, port);

        // 启动心跳
        heartBeat();
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        // 设置注册时间和更新时间
        if (serviceMetaInfo.getRegisterTime() == null) {
            serviceMetaInfo.setRegisterTime(System.currentTimeMillis());
        }
        serviceMetaInfo.setUpdateTime(System.currentTimeMillis());

        // 构建 Redis key
        String registerKey = REDIS_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();

        // 序列化服务信息为 JSON
        String serviceJson = JSONUtil.toJsonStr(serviceMetaInfo);

        // 存储到 Redis，并设置过期时间
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(registerKey, SERVICE_EXPIRE_TIME, serviceJson);
            
            // 发布服务注册事件（用于通知监听者）
            jedis.publish("service:register", registerKey);
        }

        // 添加到本地缓存
        localRegisterNodeKeySet.add(registerKey);

        log.info("服务注册成功: {} -> {}", registerKey, serviceMetaInfo.getServiceAddress());
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        String registerKey = REDIS_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();

        try (Jedis jedis = jedisPool.getResource()) {
            // 删除 Redis 中的服务信息
            jedis.del(registerKey);
            
            // 发布服务注销事件
            jedis.publish("service:unregister", registerKey);
        }

        // 从本地缓存移除
        localRegisterNodeKeySet.remove(registerKey);

        log.info("服务注销成功: {}", registerKey);
    }

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        // 优先从缓存获取服务
        List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceCache.readCache();
        if (CollUtil.isNotEmpty(cachedServiceMetaInfoList)) {
            log.debug("【服务发现】从缓存获取服务: {} -> {} 个实例", serviceKey, cachedServiceMetaInfoList.size());
            return cachedServiceMetaInfoList;
        }

        log.debug("【服务发现】缓存未命中，查询 Redis: {}", serviceKey);

        // 从 Redis 查询服务
        String searchPattern = REDIS_ROOT_PATH + serviceKey + "/*";
        List<ServiceMetaInfo> serviceMetaInfoList = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {
            // 使用 SCAN 命令查找匹配的 key（更安全，不会阻塞 Redis）
            Set<String> keys = jedis.keys(searchPattern);

            log.debug("【服务发现】从 Redis 查询到 {} 个服务节点", keys.size());

            // 批量获取服务信息
            for (String key : keys) {
                String serviceJson = jedis.get(key);
                if (serviceJson != null) {
                    ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(serviceJson, ServiceMetaInfo.class);
                    serviceMetaInfoList.add(serviceMetaInfo);
                    
                    // 监听这个 key 的变化
                    watch(key);
                    
                    log.debug("  - 节点: {}", serviceMetaInfo.getServiceAddress());
                }
            }

            // 写入缓存
            registryServiceCache.writeCache(serviceMetaInfoList);
            log.debug("【服务发现】已写入缓存，共 {} 个实例", serviceMetaInfoList.size());

            return serviceMetaInfoList;
        } catch (Exception e) {
            log.error("服务发现失败: {}", serviceKey, e);
            throw new RuntimeException("获取服务列表失败", e);
        }
    }

    @Override
    public void heartBeat() {
        // 每10秒续期一次
        CronUtil.schedule("*/10 * * * * *", new Task() {
            @Override
            public void execute() {
                // 遍历本节点所有的 key
                for (String key : localRegisterNodeKeySet) {
                    try (Jedis jedis = jedisPool.getResource()) {
                        // 获取当前服务信息
                        String serviceJson = jedis.get(key);

                        if (serviceJson == null) {
                            // 服务已过期，需要重新注册
                            log.warn("服务已过期，需要重启: {}", key);
                            continue;
                        }

                        // 解析服务信息
                        ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(serviceJson, ServiceMetaInfo.class);

                        // 续期：重新注册（保留 registerTime，更新 updateTime）
                        register(serviceMetaInfo);

                        log.debug("服务续期成功: {}", key);
                    } catch (Exception e) {
                        log.error("服务续期失败: {}", key, e);
                    }
                }
            }
        });

        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();

        log.info("Redis 心跳任务已启动");
    }

    @Override
    public void watch(String serviceNodeKey) {
        // 检查是否已经在监听
        boolean newWatch = watchingKeySet.add(serviceNodeKey);
        if (!newWatch) {
            return;
        }

        log.info("【Watch监听】开始监听服务变化");

        // 启动监听线程（如果还未启动）
        if (watchThread == null || !watchThread.isAlive()) {
            watchThread = new Thread(() -> {
                try (Jedis jedis = jedisPool.getResource()) {
                    // 订阅服务注册和注销事件
                    jedis.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            log.info("【Watch监听】接收到事件: {} -> {}", channel, message);

                            // 清空缓存，下次查询时会重新从 Redis 获取
                            registryServiceCache.clearCache();
                            log.info("【Watch监听】已清空服务缓存");
                        }
                    }, "service:register", "service:unregister");
                } catch (Exception e) {
                    log.error("Redis 监听失败", e);
                }
            });
            watchThread.setDaemon(true);
            watchThread.start();
        }
    }

    @Override
    public void destroy() {
        log.info("Redis 注册中心开始下线");

        // 停止心跳
        CronUtil.stop();

        // 下线所有服务节点
        for (String key : localRegisterNodeKeySet) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(key);
                log.info("删除服务节点: {}", key);
            } catch (Exception e) {
                log.error("删除服务节点失败: {}", key, e);
            }
        }

        // 关闭监听线程
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
        }

        // 关闭连接池
        if (jedisPool != null) {
            jedisPool.close();
        }

        log.info("Redis 注册中心已下线");
    }
}

