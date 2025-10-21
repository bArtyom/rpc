package com.yupi.yurpc.loadbalancer;

import com.yupi.yurpc.model.ServiceMetaInfo;

import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class LeastActiveLoadBalancer implements LoadBalancer {

    //存储每个服务的活跃请求数
    private final ConcurrentHashMap<String, AtomicInteger> activeCountMap = new ConcurrentHashMap<>();

    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (serviceMetaInfoList == null || serviceMetaInfoList.isEmpty()) {
            return null;
        }

        if(serviceMetaInfoList.size() == 1) {
            return serviceMetaInfoList.get(0);
        }

        //找出活跃数最少的服务
        ServiceMetaInfo leastActiveService = null;
        int leastActive=Integer.MAX_VALUE;
        List<ServiceMetaInfo> leastActiveList=new ArrayList<>();

        for(ServiceMetaInfo service:serviceMetaInfoList){
            String address=service.getServiceAddress();
            //获取或初始化活跃数
            // AtomicInteger activeCount=activeCountMap.computeIfAbsent(address,new Function<String,AtomicInteger>() {
            //     @Override
            //     public AtomicInteger apply(String k) {
            //         return new AtomicInteger(0);
            //     } 
            // });
            //函数式接口可以用lamda简化
            AtomicInteger activeCount=activeCountMap.computeIfAbsent(address,k->new AtomicInteger(0));
            
            int active=activeCount.get();

            if(active<leastActive){
                leastActive=active;
                leastActiveList.clear();
                leastActiveList.add(service);
            }else if(active==leastActive){
                leastActiveList.add(service);
            }
        }// 如果有多个最少活跃数的服务,随机选择一个
        if (leastActiveList.size() > 1) {
            Random random = new Random();
            leastActiveService = leastActiveList.get(
                random.nextInt(leastActiveList.size())
            );
        } else {
            leastActiveService = leastActiveList.get(0);
        }
        
        return leastActiveService;
    }

    // 增加活跃数的方法(在发起请求前调用)
    public void increaseActive(String serviceAddress) {
        activeCountMap.computeIfAbsent(serviceAddress, 
            k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    // 减少活跃数的方法(在请求完成后调用)
    public void decreaseActive(String serviceAddress) {
        AtomicInteger count = activeCountMap.get(serviceAddress);
        if (count != null) {
            count.decrementAndGet();
        }
    }
}
