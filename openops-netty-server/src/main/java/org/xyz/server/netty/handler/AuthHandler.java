package org.xyz.server.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.util.NettyMessageBuilder;
import org.xyz.server.constants.AttrConstants;
import org.xyz.server.util.LogUtil;

@Slf4j
@Component
@ChannelHandler.Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String remoteAddr = ctx.channel().attr(AttrConstants.remoteAddr).get();
        log.info("remoteAddr: " + remoteAddr);


        if (!LogUtil.hasLogin(ctx.channel())) {
            ctx.writeAndFlush(NettyMessageBuilder.builder(MessageType.BUSINESS_RESP,
                    ("Non login auth, close channel!" + ctx.channel().remoteAddress().toString()).getBytes()));
            ctx.channel().close();
        } else {
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (LogUtil.hasLogin(ctx.channel())) {
            log.info("当前连接登录验证完毕，无需再次验证, AuthHandler 被移除");
        } else {
            log.error("无登录验证，强制关闭连接!");
        }
    }
}
