package com.yupi.yurpc.bootstrap;

import com.yupi.yurpc.RpcApplication;

public class ConsumerBootstrap {

    public static void init(){
        //RPC框架初始化（配置和注册中心）
        RpcApplication.init();
    }
}
