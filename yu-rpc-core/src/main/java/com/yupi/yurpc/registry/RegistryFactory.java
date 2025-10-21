package com.yupi.yurpc.registry;

import com.yupi.yurpc.spi.SpiLoader;

/**
 * 注册中心工厂（工厂模式，用于获取注册中心对象）
 */
public class RegistryFactory {
    
    static{
        SpiLoader.load(Registry.class);
    }

    /**
     * 获取实例
     * @param key
     * @return
     */
    public static Registry getInstance(String key){
        return SpiLoader.getInstance(Registry.class, key);
    }
}

