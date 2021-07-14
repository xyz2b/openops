package org.xyz.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.CharsetUtil;
import org.xyz.netty.message.Header;
import org.xyz.netty.message.NettyMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/*
LengthFieldBasedFrameDecoder 参数选择

BEFORE DECODE (26 bytes)
+--------------+---------------+-----------------+--------------+----------------+-----------------------+--------------------------------------+------------------+------------------+
| CrcCode(int) | Length(int)   | SessionID(long) |  Type(byte)  | Priority(byte) | AttachmentSize(int)   |  Attachment(HashMap<String, Object>) |  Body(Object)    |    Padding(int)  |
|  0xabef0101  | 26            |   0             |  3           |    0           |      0                |  NULL                                |  NULL            |     0            |
+--------------+---------------+-----------------+--------------+----------------+-----------------------+--------------------------------------+------------------+------------------+

AFTER DECODE (26 bytes)
+--------------+---------------+-----------------+--------------+----------------+-----------------------+--------------------------------------+------------------+------------------+
| CrcCode(int) | Length(int)   | SessionID(long) |  Type(byte)  | Priority(byte) | AttachmentSize(int)   |  Attachment(HashMap<String, Object>) |  Body(Object)    |    Padding(int)  |
|  0xabef0101  | 26            |   0             |  3           |    0           |      0                |  NULL                                |  NULL            |     0            |
+--------------+---------------+-----------------+--------------+----------------+-----------------------+--------------------------------------+------------------+------------------+

header + body的结构。CrcCode为魔数，为4个字节。参数如下：
    - lengthFieldOffset=4：开始的4个字节是CrcCode，然后才是长度域，所以长度域偏移为4。
    - lengthFieldLength=4：长度域4个字节。
    - lengthAdjustment=-8：长度域为总长度，所以我们需要修正数据长度，也就是减去8(lengthFieldOffset+lengthFieldLength)
        因为Length字段以及它之前的字段在 LengthFieldBasedFrameDecoder 中认为是非数据的长度，会在计算整个报文长度的时候 使用Length字段的值 + Length字段以及它之前所有字段的长度
        即frameLength += (long)(this.lengthAdjustment + this.lengthFieldEndOffset)
        而我们定义的消息长度是包含所有字段在内的长度，其中包括了Length字段以及它之前所有字段的长度，所以需要在计算整个报文长度的时候把这部分减去
    - initialBytesToStrip=0：发送和接收数据相同，不需要跳过数据。

* */
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {
    private MarshallingDecoder marshallingDecoder;

    public NettyMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) throws IOException {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
        this.marshallingDecoder = MarshallingCodecFactory.buildMarshallingDecoder();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 继承 netty 的 LengthFieldBasedFrameDecoder 解码器，它支持自动的TCP的粘包和半包处理，只需要给出标识长度的字段偏移量和消息长度自身所占的字节数(构造函数中给出的)，netty就能自动实现对半包的处理
        // 对于业务解码器来说，调用父类 LengthFieldBasedFrameDecoder 解码之后，返回的就是整包消息或者为空，如果为空说明是个半包消息，直接返回继续由I/O线程读取后续的码流
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setCrcCode(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionID(frame.readLong());
        header.setType(frame.readByte());
        header.setPriority(frame.readByte());

        int size = frame.readInt();
        if (size > 0) {
            Map<String, Object> attach = new HashMap<String, Object>(size);
            int keySize = 0;
            byte[] keyArray = null;
            String key = null;
            for (int i = 0; i < size; i++) {
                keySize = frame.readInt();
                keyArray = new byte[keySize];
                frame.readBytes(keyArray);
                key = new String(keyArray, CharsetUtil.UTF_8);
                attach.put(key, marshallingDecoder.decode(ctx, frame));
            }
            keyArray = null;
            key = null;
            header.setAttachment(attach);
        }

        if (frame.readableBytes() > 4) {
            message.setBody(marshallingDecoder.decode(ctx, frame));
        }

//        // 判断结尾填充是否为0，不是就不是合法报文
//        if (frame.readInt() != 0) {
//            return null;
//        }

        message.setHeader(header);
        return message;
    }
}
