package com.yupi.example.consumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.proxy.ServiceProxyFactory;

public class EasyConsumerExample {
    public static void main(String []args){
        // RPC 框架初始化
        RpcApplication.init();
        
        //静态代理
        //UserService userService = new UserServiceProxy();
        UserService userService= ServiceProxyFactory.getProxy(UserService.class);
        //UserService userService=new UserServiceProxy();

        User user=new User();
        user.setName("yupi");
        //调用
        User newUser = userService.getUser(user);
        if(newUser != null){
            System.out.println("用户名: "+ newUser.getName());
        }else {
            System.out.println("用户不存在");
        }
    }
}
