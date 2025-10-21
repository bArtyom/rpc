package com.yupi.yurpc.proxy;


import com.yupi.yurpc.RpcApplication;
import java.lang.reflect.Proxy;

/**
 * 服务代理工厂（用于创建对象）
 */
public class ServiceProxyFactory {

    public static <T> T getProxy(Class<T> serviceClass){
        //Data注解关于boolean的类型生成的是isXXX
        if(RpcApplication.getRpcConfig().isMock())
            return getMockProxy(serviceClass);
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new ServiceProxy());
    }

    private static <T> T getMockProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(serviceClass.getClassLoader()
                ,new Class[]{serviceClass},
                new MockServiceProxy());
    }
}
