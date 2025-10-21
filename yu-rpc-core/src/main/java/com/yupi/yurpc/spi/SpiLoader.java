package com.yupi.yurpc.spi;

import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI 加载器（支持键值对映射）
 */
@Slf4j
public class  SpiLoader {

    /**
     * 存储已加载的类：接口名 =>（key => 实现类）
     * 示例
     * // 外层 key 是接口名，内层 key 是实现标识
     * loaderMap = {
     *     "com.yupi.yurpc.serializer.Serializer": {
     *         "jdk": JdkSerializer.class,
     *         "json": JsonSerializer.class
     *     },
     *     "com.yupi.yurpc.loadbalance.LoadBalance": {
     *         "jdk": JdkLoadBalance.class,
     *         "random": RandomLoadBalance.class
     *     }
     * }
     */
    private static Map<String, Map<String, Class<?>>> loaderMap = new ConcurrentHashMap<>();

    /**
     * 对象实例缓存（避免重复 new），类路径 => 对象实例，单例模式
     */
    private static Map<String, Object> instanceCache = new ConcurrentHashMap<>();

    /**
     * 系统 SPI 目录
     */
    private static final String RPC_SYSTEM_SPI_DIR = "META-INF/rpc/system/";

    /**
     * 用户自定义 SPI 目录
     */
    private static final String RPC_CUSTOM_SPI_DIR = "META-INF/rpc/custom/";

    /**
     * 扫描路径
     */
    private static final String[] SCAN_DIRS = new String[]{RPC_SYSTEM_SPI_DIR, RPC_CUSTOM_SPI_DIR};

    /**
     * 加载指定类型
     * @param aClass
     * @return
     */
    public static Map<String,Class <?>> load(Class<?> aClass) {
        log.info("开始加载 SPI 类：{}", aClass.getName());
        //扫描路径，用户自定义的SPI优先级高于系统SPI
        Map<String,Class <?>>keyClassMap=new HashMap<>();
        for(String scanDir:SCAN_DIRS){
            List<URL>resources=ResourceUtil.getResources(scanDir+aClass.getName());
            //读取每个资源文件
            for(URL resource:resources){
                try{
                    InputStreamReader inputStreamReader=new InputStreamReader(resource.openStream());
                    BufferedReader bufferedReader=new BufferedReader(inputStreamReader);
                    String line;
                    while((line=bufferedReader.readLine())!=null){
                        String []strArray=line.split("=");
                        if(strArray.length>1){
                            String key=strArray[0];
                            String className=strArray[1];
                            keyClassMap.put(key,Class.forName(className));
                        }
                    }
                } catch (Exception e) {
                    log.error("SPI 资源加载失败", e);
                }
            }
        }
        loaderMap.put(aClass.getName(),keyClassMap);
        return keyClassMap;
    }

    /**
     * 获取某个接口的实例
     */
    public static <T> T getInstance(Class<?> tClass,String key){
        String tClassName=tClass.getName();
        Map<String,Class<?>> keyClassMap=loaderMap.get(tClassName);
        if(keyClassMap==null){
            throw new RuntimeException(String.format("SpiLoader未加载 %s 类型",tClassName));
        }
        if(!keyClassMap.containsKey(key)){
            throw new RuntimeException(String.format("SpiLoader的 %s 不存在key=%s 的类型",tClassName,key));
        }
        //获取到要加载的实现类型
        Class<?> implClass=keyClassMap.get(key);
        //从实例缓存中获取
        String implClassName=implClass.getName();
        if(!instanceCache.containsKey(implClassName)){
            try {
                instanceCache.put(implClassName,implClass.newInstance());
            } catch (InstantiationException  | IllegalAccessException e) {
                String errorMsg=String.format("实例化 %s 类失败",implClassName);
                throw new RuntimeException(errorMsg,e);
            }
        }
        return (T) instanceCache.get(implClassName);
    }
}

