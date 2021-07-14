package org.xyz.netty.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xyz.netty.message.Header;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;
import org.xyz.netty.util.NettyMessageBuilder;

import java.util.HashMap;
import java.util.Map;

/*
* 握手的发起是在客户端和服务端TCP链路建立成功通道激活时，握手消息的接入和安全认证在服务端处理
*
* 下面是握手认证的客户端Handler，用于在通道激活时发起握手请求
*
* ChannelActive:
*   当客户端跟服务端TCP三次握手成功之后，由客户端构造握手请求消息发送给服务端，由于采用IP白名单机制，因此，不需要携带消息体，消息体为空，消息类型为3: 握手请求消息。
*   握手成功后，按照协议规范，服务端需要返回握手应答消息
*
* channelRead:
*   对握手应答消息进行处理，首先判断消息是否是握手应答消息，如果不是，直接透传给后面的ChannelHandler进行处理
*   如果是握手应答消息，则对应答结果进行判断，如果非0，说明认证失败，关闭链路，重新发起连接
* */
@Slf4j
@ChannelHandler.Sharable
@Component
public class LoginAuthReqHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("remoteAddr", "127.0.0.1");
        log.error("remoteAddr: " + attachment.get("remoteAddr"));
        ctx.writeAndFlush(NettyMessageBuilder.builder(MessageType.LOGIN_REQ, attachment));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;

        // 如果是握手应答消息，需要判断是否认证成功
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_RESP.value()) {
            byte loginResult = (byte) message.getBody();
            // 判断握手应答结果，如果非0，说明认证失败，关闭链路，重新发起连接
            if (loginResult != (byte)0) {
                log.error("验证失败，关闭连接");
                // 握手失败，关闭连接
                ctx.close();
            } else {
                log.info("Login is ok: " + message);
                // 握手验证通过，通过pipeline传给下个Handler
                ctx.fireChannelRead(msg);
            }
        } else {
            //  不是握手应答消息，直接在pipeline往下传给下个Handler
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);
    }
}
