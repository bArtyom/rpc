package com.yupi.yurpc.server;

/**
 * http服务器接口
 */
public interface HttpServer {
    /**
     * 启动http服务器
     */
    void doStart(int port);
}
