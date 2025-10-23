package com.yupi.yurpc.server.tcp;

import com.yupi.yurpc.server.HttpServer;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.parsetools.RecordParser;

public class VertxTcpServer implements HttpServer {

    @Override
    public void doStart(int port) {
        //创建vert.x 实例
        Vertx vertx = Vertx.vertx();
        //创建TCP服务器
        NetServer server = vertx.createNetServer();

        //使用正确的TCP服务器处理器
        server.connectHandler(new TcpServerHandler());

        //处理请求
    //     server.connectHandler(socket->{
    //             //构造parser
    //             RecordParser parser=RecordParser.newFixed(8);
    //             parser.setOutput(new Handler<Buffer>() {
    //                 int size=-1;
    //                 Buffer resultBuffer=Buffer.buffer();
    //                 @Override
    //                 public void handle(Buffer buffer) {
    //                     if(-1==size){
    //                         //读取消息长度
    //                         size=buffer.getInt(4);
    //                         parser.fixedSizeMode(size);
    //                         //写入头信息到结果
    //                         resultBuffer.appendBuffer(buffer);
    //                     }else{
    //                         //写入体信息到结果
    //                         resultBuffer.appendBuffer(buffer);
    //                         //打印结果
    //                         System.out.println("收到消息："+resultBuffer.toString());
    //                         //重置状态
    //                         parser.fixedSizeMode(8);
    //                         size=-1;
    //                         resultBuffer=Buffer.buffer(); 
    //                     }
    //                 }
    //             });
    //             //将socket的数据流交给parser处理
    //             socket.handler(parser);
    // });
       

        //启动TCP服务器并监听指定端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("TCP服务器启动成功，端口：" + port);
            } else {
                System.err.println("TCP服务器启动失败：" + result.cause());
            }
        });
    }
    public static void main(String[] args) {
        VertxTcpServer vertxTcpServer = new VertxTcpServer();
        vertxTcpServer.doStart(8081);
    }
}
