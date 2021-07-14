package org.xyz.server.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.util.NettyMessageBuilder;

@Slf4j
@Component
@ChannelHandler.Sharable
public class ExceptionHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.writeAndFlush(NettyMessageBuilder.builder(MessageType.BUSINESS_RESP, cause.getMessage().getBytes()));
        log.error("netty handler error, " + cause.getMessage());
        ctx.channel().close();
    }
}
