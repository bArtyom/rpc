package com.yupi.example.provider;

import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.registry.LocalRegistry;
import com.yupi.yurpc.server.HttpServer;
import com.yupi.yurpc.server.VertxHttpServer;

/**
 * 简易服务提供者示例
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class EasyProviderExample {

    public static void main(String[] args) {
        // RPC 框架初始化
        RpcApplication.init();
        System.out.println("序列化器: " + RpcApplication.getRpcConfig().getSerializer());
        
        // 使用工具类获取端口号（优先级：VM参数 > 程序参数 > 配置文件）
        int serverPort = ProviderConfigUtil.getServerPort(args, RpcApplication.getRpcConfig().getServerPort());
        
        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动 web 服务
        HttpServer httpServer = new VertxHttpServer();
        httpServer.doStart(serverPort);
        System.out.println("✅ 服务启动成功，地址为: http://localhost:" + serverPort);
    }
}
