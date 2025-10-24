package com.yupi.yurpc.server.tcp;

import com.yupi.yurpc.protocol.ProtocolConstant;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

public class TcpBufferHandlerWrapper implements Handler<Buffer> {
    
    private final RecordParser recordParser;
    
    public TcpBufferHandlerWrapper(Handler <Buffer> bufferHandler) {
        recordParser=initRecordParser(bufferHandler);
    }
    
    @Override
    public void handle(Buffer buffer) {
        recordParser.handle(buffer);
    }
    
    private RecordParser initRecordParser(Handler <Buffer> bufferHandler) {
       //构造 parser
        RecordParser parser=RecordParser.newFixed(ProtocolConstant.MESSAGE_HEADER_LENGTH);
        
        parser.setOutput(new Handler<Buffer>() {
            int size=-1;
            Buffer resultBuffer=Buffer.buffer();
            @Override
            public void handle(Buffer buffer) {
                if(-1==size){
                    //读取消息体长度（从偏移量13开始的4个字节）
                    size=buffer.getInt(13);
                    parser.fixedSizeMode(size);
                    //写入头信息到结果
                    resultBuffer.appendBuffer(buffer);
                }else{
                    //写入体信息到结果
                    resultBuffer.appendBuffer(buffer);
                    
                    //调用传入的 bufferHandler 处理完整消息
                    bufferHandler.handle(resultBuffer);
                    
                    //重置状态
                    parser.fixedSizeMode(ProtocolConstant.MESSAGE_HEADER_LENGTH);
                    size=-1;
                    resultBuffer=Buffer.buffer();
                }
            }
        });
        return parser;
    }
}
