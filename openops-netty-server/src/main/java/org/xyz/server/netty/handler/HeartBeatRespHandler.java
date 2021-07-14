package org.xyz.server.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;
import org.xyz.netty.util.NettyMessageBuilder;

/*
* 服务端的心跳Handler非常简单，接收到心跳请求消息之后，构造心跳应答消息返回，并打印接收和发送心跳消息
*
* 心跳超时的实现非常简单，直接利用netty的ReadTimeoutHandler机制，当一定周期内（默认50s）没有读取到对方任何消息时，需要主动关闭链路
* 如果是客户端，重新发起连接；如果是服务端，释放资源，请求客户端登录缓存信息，等待客户端重连
* */

@Slf4j
@Component
@ChannelHandler.Sharable
public class HeartBeatRespHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        // 返回心跳应道消息
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.HEARTBEAT_REQ.value()) {
            log.debug("Receive client heartbeat message: ----> " + message);
            NettyMessage heartBeat = NettyMessageBuilder.builder(MessageType.HEARTBEAT_RESP);
            log.debug("Send heartbeat response message to client: ----> " + heartBeat);
            ctx.writeAndFlush(heartBeat);
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
