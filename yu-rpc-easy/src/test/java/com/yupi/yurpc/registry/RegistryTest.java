package com.yupi.yurpc.registry;

import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * 注册中心测试
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">程序员鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class RegistryTest {

    final Registry registry = new EtcdRegistry();

    @Before
    public void init() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("http://localhost:2379");
        registry.init(registryConfig);
    }

    @Test
    public void register() throws Exception {
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(1234);
        registry.register(serviceMetaInfo);
        serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(1235);
        registry.register(serviceMetaInfo);
        serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("2.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(1234);
        registry.register(serviceMetaInfo);
    }

    @Test
    public void unRegister() {
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(1234);
        
        // 打印要删除的key，方便调试
        String deleteKey = "/rpc/" + serviceMetaInfo.getServiceNodeKey();
        System.out.println("尝试删除的key: " + deleteKey);
        
        registry.unRegister(serviceMetaInfo);
    }

    @Test
    public void serviceDiscovery() {
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("1.0");
        String serviceKey = serviceMetaInfo.getServiceKey();
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceKey);
        Assert.assertNotNull(serviceMetaInfoList);
    }

    /**
     * 测试注册后再注销
     */
    @Test
    public void testRegisterAndUnRegister() throws Exception {
        // 创建服务元信息
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("testService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(8888);
        
        // 1. 先注册服务
        System.out.println("========== 注册服务 ==========");
        String nodeKey = serviceMetaInfo.getServiceNodeKey();
        System.out.println("服务节点Key: " + nodeKey);
        registry.register(serviceMetaInfo);
        System.out.println("注册成功");
        
        // 2. 查询服务，验证注册成功
        System.out.println("\n========== 验证注册 ==========");
        String serviceKey = serviceMetaInfo.getServiceKey();
        List<ServiceMetaInfo> discoveredServices = registry.serviceDiscovery(serviceKey);
        System.out.println("查询到的服务数量: " + discoveredServices.size());
        Assert.assertTrue("注册后应该能查询到服务", discoveredServices.size() > 0);
        
        // 3. 注销服务
        System.out.println("\n========== 注销服务 ==========");
        registry.unRegister(serviceMetaInfo);
        
        // 4. 再次查询，验证注销成功
        // 注意：由于etcd有租约机制，这里可能需要等待一段时间
        System.out.println("\n========== 验证注销 ==========");
        Thread.sleep(1000); // 等待1秒
        List<ServiceMetaInfo> afterUnregister = registry.serviceDiscovery(serviceKey);
        System.out.println("注销后查询到的服务数量: " + afterUnregister.size());
        
        // 验证该特定节点已被删除
        boolean nodeExists = afterUnregister.stream()
                .anyMatch(info -> info.getServiceNodeKey().equals(nodeKey));
        Assert.assertFalse("注销后该节点应该不存在", nodeExists);
        System.out.println("注销验证成功！");
    }

    @Test
    public void heartBeat() throws Exception{
        //init 方法中已经执行心跳检测了
        register();
        //阻塞一分钟
        Thread.sleep(60*1000L);
    }
}
