package org.xyz.server.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xyz.server.netty.handler.*;
import org.xyz.netty.codec.NettyMessageDecoder;
import org.xyz.netty.codec.NettyMessageEncoder;

@Component
public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    @Autowired
    private LoginAuthRespHandler loginAuthRespHandler;
    @Autowired
    private AuthHandler authHandler;
    @Autowired
    private HeartBeatRespHandler heartBeatRespHandler;
    @Autowired
    private ClientManagementHandler clientManagementHandler;
    @Autowired
    private ClientResponseHandler clientResponseHandler;
    @Autowired
    private ExceptionHandler exceptionHandler;

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline p = channel.pipeline();
        p.addLast("MessageDecoder", new NettyMessageDecoder(1024 * 1024, 4, 4, -8, 0));
        p.addLast("MessageEncoder", new NettyMessageEncoder());
        p.addLast("ReadTimeoutHandler", new ReadTimeoutHandler(50));
        p.addLast("LoginAuthRespHandler", loginAuthRespHandler);
        p.addLast("AuthHandler", authHandler);
        p.addLast("HeartBeatRespHandler", heartBeatRespHandler);
//        p.addLast("ClientManagementHandler", clientManagementHandler);
        p.addLast("", clientResponseHandler);
        p.addLast("ExceptionHandler", exceptionHandler);
    }
}
