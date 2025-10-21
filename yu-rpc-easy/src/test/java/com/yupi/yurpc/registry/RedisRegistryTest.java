package com.yupi.yurpc.registry;

import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Redis 注册中心测试
 */
public class RedisRegistryTest {

    private RedisRegistry registry;

    @Before
    public void setUp() {
        // 初始化 Redis 注册中心
        RegistryConfig config = new RegistryConfig();
        config.setRegistry("redis");
        config.setAddress("127.0.0.1:6379");  // 修改为你的 Redis 地址
        config.setTimeout(10000L);

        registry = new RedisRegistry();
        registry.init(config);
    }

    @Test
    public void testRegister() throws Exception {
        // 创建服务元信息
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("UserService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("127.0.0.1");
        serviceMetaInfo.setServicePort(8080);

        // 注册服务
        registry.register(serviceMetaInfo);

        System.out.println("服务注册成功！");

        // 保持运行一段时间，观察心跳
        Thread.sleep(35000);  // 等待 35 秒，观察心跳续期
    }

    @Test
    public void testServiceDiscovery() throws Exception {
        // 先注册几个服务
        ServiceMetaInfo service1 = new ServiceMetaInfo();
        service1.setServiceName("UserService");
        service1.setServiceVersion("1.0");
        service1.setServiceHost("192.168.1.100");
        service1.setServicePort(8080);
        registry.register(service1);

        ServiceMetaInfo service2 = new ServiceMetaInfo();
        service2.setServiceName("UserService");
        service2.setServiceVersion("1.0");
        service2.setServiceHost("192.168.1.101");
        service2.setServicePort(8080);
        registry.register(service2);

        System.out.println("已注册 2 个服务实例\n");

        // 服务发现
        List<ServiceMetaInfo> services = registry.serviceDiscovery("UserService:1.0");

        System.out.println("发现 " + services.size() + " 个服务实例：");
        for (ServiceMetaInfo service : services) {
            System.out.println("========================================");
            System.out.println("服务地址: " + service.getServiceAddress());
            System.out.println("注册时间: " + service.getRegisterTime());
            System.out.println("更新时间: " + service.getUpdateTime());

            // 计算运行时长
            long uptime = System.currentTimeMillis() - service.getRegisterTime();
            System.out.println("已运行: " + (uptime / 1000) + " 秒");

            // 最后更新时间
            long lastUpdate = System.currentTimeMillis() - service.getUpdateTime();
            System.out.println("最后更新: " + (lastUpdate / 1000) + " 秒前");
        }

        System.out.println("\n========================================");
        System.out.println("第二次查询（从缓存）：");
        List<ServiceMetaInfo> cachedServices = registry.serviceDiscovery("UserService:1.0");
        System.out.println("从缓存获取 " + cachedServices.size() + " 个实例");
    }

    @Test
    public void testUnRegister() throws Exception {
        // 注册服务
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("UserService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("127.0.0.1");
        serviceMetaInfo.setServicePort(8080);
        registry.register(serviceMetaInfo);

        System.out.println("服务已注册");

        // 等待 2 秒
        Thread.sleep(2000);

        // 注销服务
        registry.unRegister(serviceMetaInfo);
        System.out.println("服务已注销");

        // 尝试查询
        List<ServiceMetaInfo> services = registry.serviceDiscovery("UserService:1.0");
        System.out.println("剩余服务数量: " + services.size());
    }

    @Test
    public void testHeartbeat() throws Exception {
        // 注册服务
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("UserService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("127.0.0.1");
        serviceMetaInfo.setServicePort(8080);
        registry.register(serviceMetaInfo);

        System.out.println("服务已注册，开始观察心跳...");
        System.out.println("每 10 秒会自动续期一次\n");

        // 保持运行 1 分钟，观察心跳日志
        for (int i = 1; i <= 6; i++) {
            Thread.sleep(10000);
            System.out.println("已运行 " + (i * 10) + " 秒...");
        }

        System.out.println("\n心跳测试完成！");
    }

    @Test
    public void testAutoExpire() throws Exception {
        // 注册服务
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("UserService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("127.0.0.1");
        serviceMetaInfo.setServicePort(8080);
        registry.register(serviceMetaInfo);

        System.out.println("服务已注册");

        // 停止心跳（模拟服务宕机）
        registry.destroy();
        System.out.println("心跳已停止（模拟服务宕机）");

        System.out.println("等待 Redis 自动过期（30秒）...");
        Thread.sleep(35000);

        // 创建新的注册中心实例查询
        RedisRegistry newRegistry = new RedisRegistry();
        RegistryConfig config = new RegistryConfig();
        config.setRegistry("redis");
        config.setAddress("127.0.0.1:6379");
        newRegistry.init(config);

        List<ServiceMetaInfo> services = newRegistry.serviceDiscovery("UserService:1.0");
        System.out.println("自动过期后，剩余服务数量: " + services.size() + " (应该为0)");
    }
}

