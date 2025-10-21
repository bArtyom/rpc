package com.yupi.yurpc.protocol;


import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.serializer.Serializer;
import com.yupi.yurpc.serializer.SerializerFactory;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProtocolMessageDecoder {

    /**
     * 解码单个协议消息
     * @param buffer 包含完整协议消息的buffer
     * @return 解码后的协议消息
     * @throws IOException
     */
    public static ProtocolMessage<?> decode(Buffer buffer) throws IOException {
        // 检查最小长度（消息头长度）
        if (buffer.length() < ProtocolConstant.MESSAGE_HEADER_LENGTH) {
            throw new RuntimeException("消息长度不足，无法解析消息头");
        }
        
        //分别从指定位置读出buffer
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        byte magic = buffer.getByte(0);
        //校验魔数
        if (magic != ProtocolConstant.PROTOCOL_MAGIC) {
            throw new RuntimeException("消息magic非法");
        }
        header.setMagic(magic);
        header.setVersion(buffer.getByte(1));
        header.setSerializer(buffer.getByte(2));
        header.setType(buffer.getByte(3));
        header.setStatus(buffer.getByte(4));
        header.setRequestId(buffer.getLong(5));
        header.setBodyLength(buffer.getInt(13));

        // 检查是否有足够的数据读取消息体
        int totalLength = ProtocolConstant.MESSAGE_HEADER_LENGTH + header.getBodyLength();
        if (buffer.length() < totalLength) {
            throw new RuntimeException("消息体数据不完整，期望长度: " + totalLength + ", 实际长度: " + buffer.length());
        }

        //解决粘包问题 - 只读取当前消息的数据
        byte[] bodyBytes = buffer.getBytes(17, 17 + header.getBodyLength());

        //解析消息体
        ProtocolMessageSerializerEnum serializerEnum = ProtocolMessageSerializerEnum.getEnumByKey(header.getSerializer());
        if (serializerEnum == null) {
            throw new RuntimeException("序列化消息的协议不存在");
        }
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());
        ProtocolMessageTypeEnum messageTypeEnum = ProtocolMessageTypeEnum.getEnumByKey(header.getType());
        if (messageTypeEnum == null) {
            throw new RuntimeException("序列化消息类型不存在");
        }
        switch (messageTypeEnum) {
            case REQUEST:
                RpcRequest request = serializer.deserialize(bodyBytes, RpcRequest.class);
                return new ProtocolMessage<>(header, request);
            case RESPONSE:
                RpcResponse response = serializer.deserialize(bodyBytes, RpcResponse.class);
                return new ProtocolMessage<>(header, response);
            case HEART_BEAT:
            case OTHERS:
            default:
                throw new RuntimeException("暂不支持该消息类型");
        }
    }

    /**
     * 批量解码协议消息，解决粘包问题
     * @param buffer 可能包含多个协议消息的buffer
     * @return 解码后的协议消息列表
     */
    public static List<ProtocolMessage<?>> decodeMultiple(Buffer buffer) throws IOException {
        List<ProtocolMessage<?>> messages = new ArrayList<>();
        int offset = 0;
        
        while (offset < buffer.length()) {
            // 检查是否还有足够的数据读取消息头
            if (offset + ProtocolConstant.MESSAGE_HEADER_LENGTH > buffer.length()) {
                break; // 数据不足，等待更多数据
            }
            
            // 读取消息头
            int bodyLength = buffer.getInt(offset + 13);
            int totalLength = ProtocolConstant.MESSAGE_HEADER_LENGTH + bodyLength;
            
            // 检查是否还有足够的数据读取完整消息
            if (offset + totalLength > buffer.length()) {
                break; // 数据不完整，等待更多数据
            }
            
            // 提取单个消息的buffer
            Buffer messageBuffer = buffer.getBuffer(offset, offset + totalLength);
            
            // 解码单个消息
            ProtocolMessage<?> message = decode(messageBuffer);
            messages.add(message);
            
            // 移动到下一个消息
            offset += totalLength;
        }
        
        return messages;
    }

    /**
     * 检查buffer是否包含完整的协议消息
     * @param buffer 待检查的buffer
     * @return 如果包含完整消息，返回消息长度；否则返回-1
     */
    public static int getCompleteMessageLength(Buffer buffer) {
        if (buffer.length() < ProtocolConstant.MESSAGE_HEADER_LENGTH) {
            return -1; // 消息头都不完整
        }
        
        int bodyLength = buffer.getInt(13);
        int totalLength = ProtocolConstant.MESSAGE_HEADER_LENGTH + bodyLength;
        
        if (buffer.length() >= totalLength) {
            return totalLength; // 消息完整
        } else {
            return -1; // 消息不完整
        }
    }
}