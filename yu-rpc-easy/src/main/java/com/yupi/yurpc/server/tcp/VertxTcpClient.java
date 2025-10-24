package com.yupi.yurpc.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.protocol.*;
import io.vertx.core.Vertx;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Vertx tcp 客户端
 */
public class VertxTcpClient {


    /**
     * 发送请求
     */
    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException {
        //发送tcp请求
        Vertx vertx=Vertx.vertx();
        NetClient netClient=vertx.createNetClient();
        CompletableFuture<RpcResponse> responseFuture=new CompletableFuture<>();
        netClient.connect(serviceMetaInfo.getServicePort(),serviceMetaInfo.getServiceHost(),result->{
           if(!result.succeeded()){
               System.err.println("❌ 连接服务器失败: " + result.cause().getMessage());
               responseFuture.completeExceptionally(new RuntimeException("连接失败", result.cause()));
               return;
           }
           System.out.println("✅ 成功连接到服务器: " + serviceMetaInfo.getServiceHost() + ":" + serviceMetaInfo.getServicePort());
           
           //连接成功返回一个socket
            NetSocket socket=result.result();
           //发送数据
           //构造消息
            ProtocolMessage<RpcRequest> protocolMessage=new ProtocolMessage<>();
            ProtocolMessage.Header header=new ProtocolMessage.Header();
            header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
            header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
            header.setSerializer((byte)ProtocolMessageSerializerEnum.getEnumByValue(RpcApplication.getRpcConfig().getSerializer()).getKey());
            header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
            header.setStatus((byte) ProtocolMessageStatusEnum.OK.getValue());
            //生成全局id
            header.setRequestId(IdUtil.getSnowflakeNextId());
            protocolMessage.setHeader( header);
            protocolMessage.setBody(rpcRequest);

            //编码请求
            try {
                Buffer encodeBuffer= ProtocolMessageEncoder.encode(protocolMessage);
                System.out.println("📤 发送请求，长度: " + encodeBuffer.length() + " 字节");
                socket.write(encodeBuffer);
            } catch (IOException e) {
                System.err.println("❌ 协议消息编码错误: " + e.getMessage());
                responseFuture.completeExceptionally(new RuntimeException("协议消息编码错误", e));
                return;
            }

            //接收响应
            TcpBufferHandlerWrapper bufferHandlerWrapper=new TcpBufferHandlerWrapper(buffer->{
                try {
                    System.out.println("📥 收到响应，长度: " + buffer.length() + " 字节");
                    ProtocolMessage<RpcResponse> rpcResponseProtocolMessage= (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
                    responseFuture.complete(rpcResponseProtocolMessage.getBody());
                } catch (IOException e) {
                    System.err.println("❌ 响应解码错误: " + e.getMessage());
                    responseFuture.completeExceptionally(new RuntimeException("响应解码错误", e));
                }
            });
            socket.handler(bufferHandlerWrapper);
        });
        RpcResponse rpcResponse=responseFuture.get();
        //关闭连接
        netClient.close();
        return rpcResponse;
    }
    public void start() {
        // 创建 Vert.x 实例
        Vertx vertx = Vertx.vertx();

        vertx.createNetClient().connect(8081, "localhost", result -> {
            if (result.succeeded()) {
                System.out.println("Connected to TCP server");
                io.vertx.core.net.NetSocket socket = result.result();
                for (int i = 0; i < 1000; i++) {
                    // 发送数据
                    Buffer buffer=Buffer.buffer();
                    String str="Hello, server!Hello, server!Hello, server!Hello, server!";
                    buffer.appendInt(0);
                    buffer.appendInt(str.getBytes().length);
                    buffer.appendBytes(str.getBytes());
                    socket.write( buffer);
                }
                // 接收响应
                socket.handler(buffer -> {
                    System.out.println("Received response from server: " + buffer.toString());
                });
            } else {
                System.err.println("Failed to connect to TCP server");
            }
        });
    }

    public static void main(String[] args) {
        new VertxTcpClient().start();
    }
}
