package org.xyz.server.controller;

import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xyz.server.service.channel.ChannelService;
import org.xyz.netty.message.Header;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;

@RestController
public class HelloController {
    @Autowired
    private ChannelService channelService;

    private NettyMessage test() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.BUSINESS_RESP.value());
        message.setHeader(header);
        return message;
    }


    @GetMapping("/hello")
    public String hello() {
        Channel channel = channelService.get("127.0.0.1");
        if (channel != null) {
            channel.writeAndFlush(test());
            System.err.println(channel.isOpen());
            System.err.println(channelService.get("127.0.0.1").id().asLongText());
        } else {
            System.err.println("127.0.0.1 client not existed");
        }

        return "hello world";
    }
}
