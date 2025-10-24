package com.yupi.yurpc.config;
import com.yupi.yurpc.fault.retry.RetryStrategyKeys;
import com.yupi.yurpc.loadbalancer.LoadBalancerKeys;
import lombok.Data;

/**
 * RPC 框架配置
 */
@Data
public class RpcConfig {

    /**
     * 名称
     */
    private String name = "yu-rpc";

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 服务器主机名
     */
    private String serverHost = "localhost";

    /**
     * 服务器端口号
     */
    private Integer serverPort = 8080;

    /**
     * 是否开启模拟
     */
    private boolean mock=false;
    
    /**
     * 序列化器类型
     */
    private String serializer = "jdk";

    /**
     * 注册中心配置
     */
    private RegistryConfig registryConfig=new RegistryConfig();

    /**
     * 负载均衡类型
     */
    private String loadBalance = LoadBalancerKeys.ROUND_ROBIN;

    /**
     * 重试策略类型
     */
    private String retryStrategy= RetryStrategyKeys.NO;

}
