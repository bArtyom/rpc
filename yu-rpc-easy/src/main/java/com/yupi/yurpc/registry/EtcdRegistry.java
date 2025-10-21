package com.yupi.yurpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchEvent;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class EtcdRegistry implements Registry{

    // 测试etcd
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //创建一个客户端使用节点
        Client client = Client.builder().endpoints("http://127.0.0.1:2379").build();
        KV kv = client.getKVClient();
        ByteSequence key=ByteSequence.from("test_key".getBytes());
        ByteSequence value=ByteSequence.from("test_value".getBytes());

        //put the key-value
        kv.put(key,value).get();

        //get the completablefuture
        CompletableFuture<GetResponse> getFuture=kv.get(key);

        //get the value from CompletableFuture
        GetResponse getResponse=getFuture.get();

        System.out.println(getResponse.getKvs().get(0).getValue());

        //delete the key
        kv.delete(key).get();
    }

    private Client client;

    private KV kvClient;

    /**
     * etcd根路径
     */
    private static final String ETCD_ROOT_PATH="/rpc/";

    @Override
    public void init(RegistryConfig registryConfig) {
        client=Client.builder().endpoints(registryConfig.getAddress()).connectTimeout(Duration.ofMillis(registryConfig.getTimeout()))
                .build();
        kvClient=client.getKVClient();
        heartBeat();
    }

    /**
     * 本地注册的节点集合
     */
    private final Set<String> localRegisterNodeKeySet=new HashSet<>();
    
    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        //创建Lease和KV客户端
        Lease leaseClient=client.getLeaseClient();

        //创建一个30秒的租约
        long leaseId=leaseClient.grant(30).get().getID();

        //设置注册时间和更新时间
        if (serviceMetaInfo.getRegisterTime() == null) {
            serviceMetaInfo.setRegisterTime(System.currentTimeMillis());
        }
        serviceMetaInfo.setUpdateTime(System.currentTimeMillis());

        //设置要存储的键值对
        String registerKey=ETCD_ROOT_PATH+serviceMetaInfo.getServiceNodeKey();
        ByteSequence key=ByteSequence.from(registerKey, StandardCharsets.UTF_8);
        ByteSequence value=ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        //将键值对与租约关联起来，并设置过期时间
        PutOption putOption=PutOption.builder().withLeaseId(leaseId).build();
        kvClient.put(key,value,putOption).get();
        System.out.println("服务注册成功，key = " + registerKey);

        //添加节点信息到本地缓存
        localRegisterNodeKeySet.add(registerKey);
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        String deleteKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        try {
            kvClient.delete(ByteSequence.from(deleteKey, StandardCharsets.UTF_8)).get();
            localRegisterNodeKeySet.remove(deleteKey);
            System.out.println("服务注销成功，key = " + deleteKey);
        } catch (Exception e) {
            throw new RuntimeException("服务注销失败", e);
        }
    }

    /**
     * 注册中心服务缓存
     */
    private final RegistryServiceCache registryServiceCache=new RegistryServiceCache();

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        //优先从缓存获取服务
        List<ServiceMetaInfo>cachedServiceMetaInfoList=registryServiceCache.readCache();
        if(CollUtil.isNotEmpty(cachedServiceMetaInfoList)){
            System.out.println("【服务发现】从缓存获取服务: " + serviceKey + " -> " + cachedServiceMetaInfoList.size() + "个实例");
            return cachedServiceMetaInfoList;
        }
        
        System.out.println("【服务发现】缓存未命中，查询注册中心: " + serviceKey);
        //前缀搜索，结尾一定要加/
        String searchPrefix=ETCD_ROOT_PATH+serviceKey+"/";
        GetOption getOption=GetOption.builder().isPrefix(true).build();
        try {
            List<KeyValue> keyValues=kvClient.get(ByteSequence.from(searchPrefix, StandardCharsets.UTF_8),getOption)
                    .get()
                    .getKvs();
            System.out.println("【服务发现】从Etcd查询到 " + keyValues.size() + " 个服务节点");
            
            //解析服务信息
                List<ServiceMetaInfo>serviceMetaInfoList=keyValues.stream()
                        .map(keyValue->{
                            String key=keyValue.getKey().toString(StandardCharsets.UTF_8);
                            //监听key的变化
                            watch(key);
                            String value=keyValue.getValue().toString(StandardCharsets.UTF_8);
                            ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value,ServiceMetaInfo.class);
                            System.out.println("  - 节点: " + serviceMetaInfo.getServiceHost() + ":" + serviceMetaInfo.getServicePort());
                            return serviceMetaInfo;
            }).collect(Collectors.toList());
            //写入服务缓存
            registryServiceCache.writeCache(serviceMetaInfoList);
            System.out.println("【服务发现】已写入缓存，共 " + serviceMetaInfoList.size() + " 个实例");
            return serviceMetaInfoList;
        } catch (Exception e) {
            throw new RuntimeException("获取列表服务失败",e);
        }
    }

    @Override
    public void destroy() {
        System.out.println("当前节点下线");
        //下线节点
        //遍历本节点所有的key
        for(String key:localRegisterNodeKeySet){
            try {
                kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            } catch (Exception e) {
                throw new RuntimeException(key+"节点下线失败");
            }
        }
        // 释放资源
        if (kvClient != null) {
            kvClient.close();
        }
        if (client != null) {
            client.close();
        }
    }
    @Override
    public void heartBeat() {
        //10秒续签一次
        CronUtil.schedule("*/10 * * * * *",new Task(){
            @Override
            public void execute() {
                //遍历本节点所有的key
                for(String key:localRegisterNodeKeySet){
                    try {
                        List<KeyValue> keyValues=kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8))
                                .get()
                                .getKvs();
                        //该节点已经过期（需重启节点才能重新注册)
                        if(CollUtil.isEmpty(keyValues)){
                            continue;
                        }
                        //节点未过期，重新注册（相当于续签）
                        KeyValue keyValue=keyValues.get(0);
                        String value=keyValue.getValue().toString(StandardCharsets.UTF_8);
                        ServiceMetaInfo serviceMetaInfo=JSONUtil.toBean(value,ServiceMetaInfo.class);
                        //续签时保留原来的注册时间，只更新 updateTime
                        //register 方法会自动处理：如果 registerTime 不为空则保留，只更新 updateTime
                        register(serviceMetaInfo);
                    } catch (Exception e) {
                        throw new RuntimeException(key+"续签失败",e);
                    }
                }
            }
        });
        //支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    /**
     * 正在监听的key集合
     */
    private final Set<String> watchingKeySet=new ConcurrentHashSet<>();

    @Override
    public void watch(String serviceNodeKey) {
        Watch watchClient=client.getWatchClient();
        //之前未被监听，开启监听
        boolean newWatch=watchingKeySet.add(serviceNodeKey);
        if(newWatch){
            System.out.println("【Watch监听】开始监听节点: " + serviceNodeKey);
            watchClient.watch(ByteSequence.from(serviceNodeKey, StandardCharsets.UTF_8),response->{
                    for(WatchEvent event : response.getEvents()){
                        switch(event.getEventType()){
                            //key 删除时触发
                            case DELETE:
                                System.out.println("【Watch监听】检测到节点删除事件: " + serviceNodeKey);
                                //清理注册服务缓存
                                registryServiceCache.clearCache();
                                System.out.println("【Watch监听】已清空服务缓存");
                                break;
                            case PUT:
                                System.out.println("【Watch监听】检测到节点更新事件: " + serviceNodeKey);
                                break;
                            default:
                                break;
                        }
                    }
            });
        }
    }

}
