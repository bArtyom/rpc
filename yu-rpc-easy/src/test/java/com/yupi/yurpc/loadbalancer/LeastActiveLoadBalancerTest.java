package com.yupi.yurpc.loadbalancer;

import com.yupi.yurpc.model.ServiceMetaInfo;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 最少活跃数负载均衡器测试
 * 演示活跃数管理的完整流程
 */
public class LeastActiveLoadBalancerTest {

    @Test
    public void testLeastActiveLoadBalancer() throws InterruptedException {
        // 1. 准备测试数据 - 创建3个服务节点
        List<ServiceMetaInfo> serviceMetaInfoList = new ArrayList<>();
        
        ServiceMetaInfo service1 = new ServiceMetaInfo();
        service1.setServiceName("userService");
        service1.setServiceVersion("1.0");
        service1.setServiceHost("127.0.0.1");
        service1.setServicePort(8080);
        serviceMetaInfoList.add(service1);

        ServiceMetaInfo service2 = new ServiceMetaInfo();
        service2.setServiceName("userService");
        service2.setServiceVersion("1.0");
        service2.setServiceHost("127.0.0.1");
        service2.setServicePort(8081);
        serviceMetaInfoList.add(service2);

        ServiceMetaInfo service3 = new ServiceMetaInfo();
        service3.setServiceName("userService");
        service3.setServiceVersion("1.0");
        service3.setServiceHost("127.0.0.1");
        service3.setServicePort(8082);
        serviceMetaInfoList.add(service3);

        // 2. 创建负载均衡器实例
        LeastActiveLoadBalancer loadBalancer = new LeastActiveLoadBalancer();
        
        // 3. 模拟并发请求场景
        System.out.println("==================== 开始测试最少活跃数负载均衡器 ====================\n");
        
        // 用于统计每个服务被选择的次数
        Map<String, AtomicInteger> selectCountMap = new HashMap<>();
        selectCountMap.put(service1.getServiceAddress(), new AtomicInteger(0));
        selectCountMap.put(service2.getServiceAddress(), new AtomicInteger(0));
        selectCountMap.put(service3.getServiceAddress(), new AtomicInteger(0));
        
        // 创建线程池模拟并发请求
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(30); // 模拟30个请求
        
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", "getUserInfo");
        
        // 模拟30个并发请求
        for (int i = 0; i < 30; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    // 【步骤1】选择服务节点
                    ServiceMetaInfo selected = loadBalancer.select(requestParams, serviceMetaInfoList);
                    String serviceAddress = selected.getServiceAddress();
                    
                    System.out.println(String.format("请求 #%d: 选择了服务 %s", 
                        requestId, serviceAddress));
                    
                    // 【步骤2】在发起RPC调用前,增加活跃数
                    loadBalancer.increaseActive(serviceAddress);
                    selectCountMap.get(serviceAddress).incrementAndGet();
                    
                    System.out.println(String.format("请求 #%d: 服务 %s 活跃数 +1", 
                        requestId, serviceAddress));
                    
                    try {
                        // 【步骤3】模拟RPC调用执行
                        // 为了演示效果,不同服务设置不同的处理时间
                        if (serviceAddress.contains("8080")) {
                            Thread.sleep(100); // 服务1处理较慢
                        } else if (serviceAddress.contains("8081")) {
                            Thread.sleep(50);  // 服务2处理中等
                        } else {
                            Thread.sleep(30);  // 服务3处理较快
                        }
                        
                        System.out.println(String.format("请求 #%d: 服务 %s 处理完成", 
                            requestId, serviceAddress));
                            
                    } finally {
                        // 【步骤4】在调用完成后(无论成功失败),减少活跃数
                        loadBalancer.decreaseActive(serviceAddress);
                        System.out.println(String.format("请求 #%d: 服务 %s 活跃数 -1", 
                            requestId, serviceAddress));
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
            
            // 间隔一点时间发送请求,便于观察
            Thread.sleep(20);
        }
        
        // 等待所有请求完成
        latch.await();
        executor.shutdown();
        
        // 4. 输出统计结果
        System.out.println("\n==================== 测试结果统计 ====================");
        System.out.println("各服务被选择次数统计:");
        selectCountMap.forEach((address, count) -> {
            System.out.println(String.format("  %s: 被选择 %d 次", address, count.get()));
        });
        
        System.out.println("\n分析:");
        System.out.println("  - 处理速度快的服务(8082)应该被选择更多次");
        System.out.println("  - 处理速度慢的服务(8080)应该被选择更少次");
        System.out.println("  - 这体现了'最少活跃数'算法的自适应特性");
    }
    
    @Test
    public void testSequentialRequests() {
        // 测试顺序请求场景
        System.out.println("==================== 顺序请求测试 ====================\n");
        
        List<ServiceMetaInfo> serviceMetaInfoList = new ArrayList<>();
        
        ServiceMetaInfo service1 = new ServiceMetaInfo();
        service1.setServiceHost("192.168.1.1");
        service1.setServicePort(8080);
        serviceMetaInfoList.add(service1);

        ServiceMetaInfo service2 = new ServiceMetaInfo();
        service2.setServiceHost("192.168.1.2");
        service2.setServicePort(8080);
        serviceMetaInfoList.add(service2);

        LeastActiveLoadBalancer loadBalancer = new LeastActiveLoadBalancer();
        Map<String, Object> requestParams = new HashMap<>();
        
        // 发送5个顺序请求
        for (int i = 0; i < 5; i++) {
            System.out.println("--- 请求 " + (i + 1) + " ---");
            
            // 选择服务
            ServiceMetaInfo selected = loadBalancer.select(requestParams, serviceMetaInfoList);
            System.out.println("选择服务: " + selected.getServiceAddress());
            
            // 增加活跃数
            loadBalancer.increaseActive(selected.getServiceAddress());
            System.out.println("活跃数 +1");
            
            // 模拟调用
            System.out.println("执行RPC调用...");
            
            // 减少活跃数
            loadBalancer.decreaseActive(selected.getServiceAddress());
            System.out.println("活跃数 -1\n");
        }
        
        System.out.println("顺序请求测试完成!");
        System.out.println("由于每次请求都会立即完成,活跃数会归零,所以会在两个服务间平均分配");
    }
}
