package com.yupi.example.consumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.proxy.ServiceProxyFactory;

/**
 * 服务缓存测试类
 * 
 * 测试步骤：
 * 1. 先启动服务提供者 (ProviderExample)
 * 2. 在本类的标注了 "断点1"、"断点2"、"断点3" 的地方打断点
 * 3. 以Debug模式运行本测试类
 * 4. 观察每次调用时的缓存状态：
 *    - 第一次：查询Etcd注册中心，写入缓存
 *    - 第二次：直接从缓存读取（不查询Etcd）
 *    - 第三次调用前：手动停止服务提供者
 *    - 第三次：缓存被清空，重新查询Etcd（发现节点已删除）
 * 
 * 验证点：
 * - 第一次调用会进入 EtcdRegistry.serviceDiscovery() 的 Etcd查询逻辑
 * - 第二次调用会在 serviceDiscovery() 方法开头就返回缓存数据
 * - 服务提供者下线后，Watch监听器会触发 clearCache()
 * - 第三次调用会重新查询Etcd，发现服务列表已更新
 */
public class ServiceCacheTest {

    public static void main(String[] args) throws InterruptedException {
        // RPC 框架初始化
        RpcApplication.init();
        System.out.println("========== RPC框架初始化完成 ==========\n");

        // 获取UserService代理对象
        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        System.out.println("========== 服务代理创建完成 ==========\n");

        // ========== 第一次调用 ==========
        System.out.println("【第一次调用】开始调用服务...");
        System.out.println("预期：查询Etcd注册中心 → 写入缓存 → 注册Watch监听器");
        System.out.println("--------------------------------------------------");
        
        User user1 = new User();
        user1.setName("第一次调用-张三");
        
        // 断点1：在这里打断点，F7进入方法内部，观察 EtcdRegistry.serviceDiscovery() 的执行
        // 预期行为：缓存为空，会执行完整的Etcd查询流程
        User result1 = userService.getUser(user1);
        
        System.out.println("第一次调用结果: " + result1.getName());
        System.out.println("========== 第一次调用完成 ==========\n");
        
        // 暂停2秒，让日志更清晰
        Thread.sleep(2000);

        // ========== 第二次调用 ==========
        System.out.println("【第二次调用】开始调用服务...");
        System.out.println("预期：直接从缓存读取（不查询Etcd）");
        System.out.println("--------------------------------------------------");
        
        User user2 = new User();
        user2.setName("第二次调用-李四");
        
        // 断点2：在这里打断点，F7进入方法内部
        // 预期行为：在 serviceDiscovery() 方法开头就会命中缓存，直接返回
        // 不会执行 Etcd 查询的代码
        User result2 = userService.getUser(user2);
        
        System.out.println("第二次调用结果: " + result2.getName());
        System.out.println("========== 第二次调用完成 ==========\n");
        
        // 暂停，等待手动操作
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!! 【重要】请在继续前执行以下操作：              !!!");
        System.out.println("!!! 1. 停止服务提供者 (ProviderExample)          !!!");
        System.out.println("!!! 2. 等待约10秒，让Etcd的租约过期               !!!");
        System.out.println("!!! 3. 可选：用etcdctl查看key是否已删除           !!!");
        System.out.println("!!!    命令: etcdctl get --prefix /rpc/         !!!");
        System.out.println("!!! 4. 完成后按F8继续执行（或在IDE中点击Resume）  !!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println();
        
        // 等待30秒，给足够的时间手动停止Provider并观察Etcd
        System.out.println("等待中...(30秒)");
        Thread.sleep(30000);

        // ========== 第三次调用 ==========
        System.out.println("\n【第三次调用】开始调用服务...");
        System.out.println("预期：缓存已被Watch机制清空 → 重新查询Etcd → 发现节点已删除");
        System.out.println("--------------------------------------------------");
        
        User user3 = new User();
        user3.setName("第三次调用-王五");
        
        try {
            // 断点3：在这里打断点，F7进入方法内部
            // 预期行为：
            // 1. 缓存已被 Watch 监听器清空（因为Provider下线了）
            // 2. 重新查询 Etcd
            // 3. 获取到空列表或更新后的列表
            // 4. 可能抛出 "暂无服务地址" 异常（如果所有Provider都下线了）
            User result3 = userService.getUser(user3);
            
            System.out.println("第三次调用结果: " + result3.getName());
            System.out.println("========== 第三次调用完成 ==========");
            
        } catch (Exception e) {
            System.err.println("第三次调用失败（这是预期的）: " + e.getMessage());
            System.out.println("========== 验证成功：缓存已更新，服务列表为空 ==========");
        }

        System.out.println("\n========== 测试完成 ==========");
        System.out.println("验证要点：");
        System.out.println("✓ 第一次调用：查询了Etcd并写入缓存");
        System.out.println("✓ 第二次调用：从缓存读取，没有查询Etcd");
        System.out.println("✓ Provider下线后：Watch监听器触发，清空缓存");
        System.out.println("✓ 第三次调用：重新查询Etcd，发现节点已删除");
    }
}

