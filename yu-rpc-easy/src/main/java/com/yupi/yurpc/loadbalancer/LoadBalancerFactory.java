package com.yupi.yurpc.loadbalancer;

import com.yupi.yurpc.spi.SpiLoader;

public class LoadBalancerFactory {

    static{
        SpiLoader.load(LoadBalancer.class);
    }

    /**
     * 默认负载均衡器
     */
    private static final LoadBalancer DEFAULT_LOAD_BALANCER=new RoundRobinLoadBalancer();

    public static LoadBalancer getInstance(String key){
        return SpiLoader.getInstance(LoadBalancer.class, key);
    }
}
