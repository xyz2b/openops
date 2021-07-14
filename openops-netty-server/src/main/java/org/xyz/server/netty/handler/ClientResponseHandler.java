package org.xyz.server.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xyz.api.entity.CmdResult;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;
import org.xyz.server.config.Config;
import org.xyz.server.service.message.messagedispatch.MessageDispatchService;

import javax.annotation.Resource;

@Slf4j
@Component
@ChannelHandler.Sharable
public class ClientResponseHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    private Config config;

    @Resource(name = "MQDispatchServiceImpl")
    private MessageDispatchService messageDispatchService;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // TODO: 结果投递到MQ，MQ输出到ES，portal去ES中检索结果（根据请求的UUID，全程唯一）
        NettyMessage message = (NettyMessage) msg;
        log.info("UUID: " + message.getHeader().getAttachment().get("UUID"));
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.CMD_EXEC_RESULT.value()) {
            log.error("std: " + message.getBody());
            messageDispatchService.send(config.getCmdResultTopic(), (CmdResult) message.getBody());
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
