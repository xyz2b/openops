package org.xyz.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.marshalling.MarshallerProvider;
import org.jboss.marshalling.Marshaller;

public class MarshallingEncoder {
    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private final MarshallerProvider provider;

    public MarshallingEncoder(MarshallerProvider provider) {
        this.provider = provider;
    }

    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try (Marshaller marshaller = this.provider.getMarshaller(ctx)) {
            int lengthPos = out.writerIndex();
            // 编码后的msg长度的占位(未编码时并不知道编码后多长)，4个字节
            out.writeBytes(LENGTH_PLACEHOLDER);
            ChannelBufferByteOutput output = new ChannelBufferByteOutput(out);
            marshaller.start(output);
            marshaller.writeObject(msg);
            marshaller.finish();
            // 将实际编码后的msg的长度写入长度占位
            out.setInt(lengthPos, out.writerIndex() - lengthPos - 4);
        }
    }
}
