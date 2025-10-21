package com.yupi.yurpc.model;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * 服务元信息
 */
@Data
public class ServiceMetaInfo {
    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务版本
     */
    private String serviceVersion="1.0";

    /**
     * 服务地址
     */
    private String serviceHost;

    /**
     * 服务端口
     */
    private Integer servicePort;

    /**
     * 服务分组(尚未实现)
     */
    private String serviceGroup="default";

    /**
     * 服务注册时间（毫秒时间戳）
     */
    private Long registerTime;

    /**
     * 服务最后更新时间（毫秒时间戳）
     */
    private Long updateTime;

    /**
     * 获取服务键名
     * @return
     */
    public String getServiceKey(){
        //后续可扩展服务分组
        //return String.format("%s:%s:%s:%s",serviceName,serviceVersion,serviceGroup);
        return String.format("%s:%s",serviceName,serviceVersion);
    }

    /**
     * 获取服务注册节点键名
     * @return
     */
    public String getServiceNodeKey(){
        return String.format("%s/%s:%s",getServiceKey(),serviceHost,servicePort);
    }

    public String getServiceAddress(){
        if(!StrUtil.contains(serviceHost,"http")){
            return String.format("http://%s:%s",serviceHost,servicePort);
        }
        return String.format("%s:%s",serviceHost,servicePort);
    }
}

