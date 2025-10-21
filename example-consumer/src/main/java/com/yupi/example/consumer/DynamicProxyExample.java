package com.yupi.example.consumer;

import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.proxy.ServiceProxy;

import java.lang.reflect.Proxy;

public class DynamicProxyExample {
    public static void main(String[] args) {
        // RPC 框架初始化
        RpcApplication.init();
        
        // 使用动态代理方式
        ServiceProxy serviceProxy = new ServiceProxy();
        UserService userService = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class[]{UserService.class},
                serviceProxy
        );
        
        // 创建用户对象
        com.yupi.example.common.model.User user = new com.yupi.example.common.model.User();
        user.setName("Bob");
        
        // 调用代理方法
        com.yupi.example.common.model.User resultUser = userService.getUser(user);
        
        if (resultUser != null) {
            System.out.println("通过动态代理获取到用户: " + resultUser.getName());
        } else {
            System.out.println("通过动态代理未获取到用户");
        }
    }
}