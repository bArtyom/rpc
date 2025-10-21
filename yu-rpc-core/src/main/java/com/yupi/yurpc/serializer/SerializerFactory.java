package com.yupi.yurpc.serializer;

import com.yupi.yurpc.spi.SpiLoader;

/**
 * 序列化器工厂（工厂模式，用于获取序列化器对象）
 */
public class SerializerFactory {
    
    /**
     * 是否已初始化SPI加载器
     */
    private static volatile boolean initFlag = false;

    /**
     * 获取序列化器实例（懒加载方式）
     *
     * @param key 序列化器键名
     * @return 序列化器实例
     */
    public static Serializer getInstance(String key) {
        // 双重检查锁定确保线程安全
        if (!initFlag) {
            synchronized (SerializerFactory.class) {
                if (!initFlag) {
                    SpiLoader.load(Serializer.class);
                    initFlag = true;
                }
            }
        }
        return SpiLoader.getInstance(Serializer.class, key);
    }
}

