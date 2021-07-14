package org.xyz.netty.codec;

import io.netty.handler.codec.marshalling.DefaultMarshallerProvider;
import io.netty.handler.codec.marshalling.DefaultUnmarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallerProvider;
import io.netty.handler.codec.marshalling.UnmarshallerProvider;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;

public final class MarshallingCodecFactory {
    // 创建Jboss Marshalling 解码器 MarshallingDecoder
    public static MarshallingDecoder buildMarshallingDecoder() {
        final MarshallerFactory marshallingFactory = Marshalling.getProvidedMarshallerFactory("serial");
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(5);
        UnmarshallerProvider provider = new DefaultUnmarshallerProvider(marshallingFactory, configuration);
        return new MarshallingDecoder(provider);
    }

    // 创建Jboss Marshalling 编码器 MarshallingEncoder
    public static MarshallingEncoder buildMarshallingEncoder() {
        final MarshallerFactory marshallingFactory = Marshalling.getProvidedMarshallerFactory("serial");
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(5);
        MarshallerProvider provider = new DefaultMarshallerProvider(marshallingFactory, configuration);
        return new MarshallingEncoder(provider);
    }
}
