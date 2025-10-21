package com.yupi.yurpc.loadbalancer;

import com.yupi.yurpc.model.ServiceMetaInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadBalancerTest {

    final LoadBalancer loadBalancer=new ConsistentHashLoadBalancer();

    @Test
    public void select(){
        //请求参数
        Map<String,Object> requestParams=new HashMap<>();
        requestParams.put("methodName","add");
        //服务列表
        ServiceMetaInfo serviceMetaInfo1=new ServiceMetaInfo();
        serviceMetaInfo1.setServiceName("myService");
        serviceMetaInfo1.setServiceVersion("1.0");
        serviceMetaInfo1.setServiceHost("localhost");
        serviceMetaInfo1.setServicePort(1234);
        ServiceMetaInfo serviceMetaInfo2=new ServiceMetaInfo();
        serviceMetaInfo2.setServiceName("myService");
        serviceMetaInfo2.setServiceVersion("1.0");
        serviceMetaInfo2.setServiceHost("yupi.icu");
        serviceMetaInfo2.setServicePort(80);
        List<ServiceMetaInfo> serviceMetaInfoList = Arrays.asList(serviceMetaInfo1, serviceMetaInfo2);
        //调用三次
        for(int i=0;i<3;i++){
            ServiceMetaInfo selectedServiceMetaInfo=loadBalancer.select(requestParams,serviceMetaInfoList);
            System.out.println(selectedServiceMetaInfo);
            Assert.assertNotNull(selectedServiceMetaInfo);
        }
    }
}
