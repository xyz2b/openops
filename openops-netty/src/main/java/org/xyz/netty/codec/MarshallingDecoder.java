package org.xyz.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.marshalling.UnmarshallerProvider;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Unmarshaller;

public class MarshallingDecoder {
    private final UnmarshallerProvider provider;

    public MarshallingDecoder(UnmarshallerProvider provider) {
        this.provider = provider;
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 在编码时，先写入了msg编码后的长度，所以在解码时先读出这个长度，然后再解码msg
        int objectSize = in.readInt();
        // 获取msg编码后的内容的ByteBuf的切片
        ByteBuf buf = in.slice(in.readerIndex(), objectSize);

        try (ByteInput input = new ChannelBufferByteInput(buf);
             Unmarshaller unmarshaller = this.provider.getUnmarshaller(ctx)) {
            unmarshaller.start(input);
            Object obj = unmarshaller.readObject();
            unmarshaller.finish();
            in.readerIndex(in.readerIndex() + objectSize);
            return obj;
        }
    }
}
