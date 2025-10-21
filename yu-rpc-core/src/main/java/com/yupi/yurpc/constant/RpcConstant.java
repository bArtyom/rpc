package com.yupi.yurpc.constant;

/**
 * RPC常量定义接口
 * 包含RPC框架使用的所有常量定义
 */
public interface RpcConstant {
    /**
     * 默认配置文件加载前缀
     * 用于指定配置文件中RPC相关配置的根节点
     */
    String DEFAULT_CONFIG_PREFIX = "rpc";

    /**
     * 默认服务版本
     */
    String DEFAULT_SERVICE_VERSION="1.0";
}
