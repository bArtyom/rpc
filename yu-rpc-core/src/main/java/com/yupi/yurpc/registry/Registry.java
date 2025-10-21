package com.yupi.yurpc.registry;

import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;

import java.util.List;

/**
 * 注册中心接口
 */
public interface Registry {

    /**
     * 初始化
     * @param registryConfig
     */
    void init(RegistryConfig registryConfig);

    /**
     * 注册服务
     * @param serviceMetaInfo
     * @throws Exception
     */
    void register(ServiceMetaInfo serviceMetaInfo) throws Exception;

    /**
     * 注销服务
     * @param serviceMetaInfo
     */
    void unRegister(ServiceMetaInfo serviceMetaInfo);

    /**
     * 服务发现
     * @param serviceName
     * @return
     */
    List<ServiceMetaInfo> serviceDiscovery(String serviceName);

    /**
     * 销毁
     */
    void destroy();

    /**
     * 心跳检测（服务端)
     */
    void heartBeat();

    /**
     * 监听服务节点变化（服务端)
     * @param serviceNodeKey
     */
    void watch(String serviceNodeKey);

}

