package com.yupi.example.consumer;


import com.yupi.yurpc.serializer.Serializer;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

public class SPITest {
    @Test
    public void test1(){
// 指定序列化器
        Serializer serializer = null;
        ServiceLoader<Serializer> serviceLoader = ServiceLoader.load(Serializer.class);
        for (Serializer service : serviceLoader) {
            serializer = service;
        }
        System.out.println(serializer);
    }

}
