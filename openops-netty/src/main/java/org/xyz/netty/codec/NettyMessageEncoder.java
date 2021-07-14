package org.xyz.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.xyz.netty.message.NettyMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class NettyMessageEncoder extends MessageToMessageEncoder<NettyMessage> {
    private MarshallingEncoder marshallingEncoder;

    public NettyMessageEncoder() throws IOException {
        this.marshallingEncoder = MarshallingCodecFactory.buildMarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg, List<Object> out) throws Exception {
        if (msg == null || msg.getHeader() == null) {
            throw new Exception("The encode message is null");
        }

        ByteBuf sendBuf = Unpooled.buffer();
        sendBuf.writeInt(msg.getHeader().getCrcCode());
        sendBuf.writeInt(msg.getHeader().getLength());
        sendBuf.writeLong(msg.getHeader().getSessionID());
        sendBuf.writeByte(msg.getHeader().getType());
        sendBuf.writeByte(msg.getHeader().getPriority());
        sendBuf.writeInt(msg.getHeader().getAttachment().size());

        String key = null;
        byte[] keyArray = null;
        Object value = null;
        for (Map.Entry<String, Object> param : msg.getHeader().getAttachment().entrySet()) {
            key = param.getKey();
            keyArray = key.getBytes(StandardCharsets.UTF_8);
            sendBuf.writeInt(keyArray.length);
            sendBuf.writeBytes(keyArray);
            value = param.getValue();
            marshallingEncoder.encode(ctx, value, sendBuf);
        }
        key = null;
        keyArray = null;
        // TODO: 不能写入小于4字节的body，因为解码时判断有没有body，是判断读取完header之后剩余的字节数是不是大于4
        //  解决方案: 结尾填充可以在有body的时候也进行填充，如果有body时也进行填充，填充完之后的字节数就大于4了
        if (msg.getBody() != null) {
            marshallingEncoder.encode(ctx, msg.getBody(), sendBuf);
        }
        sendBuf.writeInt(0);
//        else {
//            // 没有Body时结尾填充一个int类型的0，占4个字节
//            sendBuf.writeInt(0);
//        }

        // setInt 可以有两个参数，第一个参数是写入ByteBuf时的开始索引(byte计数单位)，第二个参数是写入的值(int类型)
        // 写入索引为4字节处，正好是Header中length字段的位置，而ByteBuf.readableBytes()正是写入ByteBuf的字节数
        // readableBytes = writerIndex - readerIndex
        // ByteBuf初始化出来之后， writerIndex = readerIndex = 0， 由于之后只进行了写入，没有读取，所以 readerIndex = 0，writerIndex = 写入的字节数
        sendBuf.setInt(4, sendBuf.readableBytes());

        out.add(sendBuf);
    }
}
