package org.xyz.netty.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xyz.netty.client.handler.HeartBeatReqHandler;
import org.xyz.netty.client.handler.LoginAuthReqHandler;
import org.xyz.netty.client.handler.OpenOpsRequestHandler;
import org.xyz.netty.codec.NettyMessageDecoder;
import org.xyz.netty.codec.NettyMessageEncoder;

@Component
public class ClientInitializer extends ChannelInitializer<SocketChannel> {
    @Autowired
    private LoginAuthReqHandler loginAuthReqHandler;

    @Autowired
    private HeartBeatReqHandler heartBeatReqHandler;

    @Autowired
    private OpenOpsRequestHandler openOpsRequestHandler;

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline p = channel.pipeline();
        p.addLast("MessageDecoder", new NettyMessageDecoder(1024 * 1024, 4, 4, -8, 0));
        p.addLast("MessageEncoder", new NettyMessageEncoder());
        p.addLast("ReadTimeoutHandler", new ReadTimeoutHandler(50));
        p.addLast("LoginAuthReqHandler", loginAuthReqHandler);
        p.addLast("HeartBeatReqHandler", heartBeatReqHandler);
        p.addLast("OpenOpsRequestHandler", openOpsRequestHandler);
    }
}
