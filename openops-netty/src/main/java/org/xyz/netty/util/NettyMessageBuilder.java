package org.xyz.netty.util;

import org.xyz.netty.message.Header;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;

import java.util.Map;

public class NettyMessageBuilder {
    public static NettyMessage builder(MessageType type, Object body) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(type.value());
        message.setHeader(header);
        message.setBody(body);
        return message;
    }

    public static NettyMessage builder(MessageType type) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(type.value());
        message.setHeader(header);
        return message;
    }


    public static NettyMessage builder(MessageType type, Map<String, Object> attachment, Object body) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(type.value());
        header.setAttachment(attachment);
        message.setHeader(header);
        message.setBody(body);
        return message;
    }

    public static NettyMessage builder(MessageType type, Map<String, Object> attachment) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(type.value());
        header.setAttachment(attachment);
        message.setHeader(header);
        return message;
    }
}
