package com.yupi.example.provider;

import cn.hutool.core.net.NetUtil;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.bootstrap.ProviderBootstrap;
import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.model.ServiceRegisterInfo;
import com.yupi.yurpc.registry.EtcdRegistry;
import com.yupi.yurpc.registry.LocalRegistry;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.server.HttpServer;
import com.yupi.yurpc.server.VertxHttpServer;
import com.yupi.yurpc.server.tcp.VertxTcpServer;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务提供者示例
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class ProviderExample {

    public static void main(String[] args) {
       //要注册的服务
        List<ServiceRegisterInfo<?>> serviceRegisterInfoList=new ArrayList<>();
        ServiceRegisterInfo<?>  serviceRegisterInfo=new ServiceRegisterInfo<>(UserService.class.getName(), UserServiceImpl.class);
        serviceRegisterInfoList.add(serviceRegisterInfo);

        //服务提供者初始化
        ProviderBootstrap.init(serviceRegisterInfoList);
    }
}
