package org.xyz.server.service.message;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xyz.netty.message.NettyMessage;
import org.xyz.server.service.channel.ChannelService;

// 向客户端发送消息
@Slf4j
@Service
public class MessageSendService {
    @Autowired
    private ChannelService channelService;

    // 具体发送消息实现方法
    public void sendMessage(String client, NettyMessage message) {
        Channel channel = channelService.get(client);
        if (!checkClient(channel, client)) return;

        // 修改本地和redis中维护的客户端的最新活跃时间
        channelService.updateActiveTime(channel);

        // 将消息发送给客户端
        channel.writeAndFlush(message);

        // 记录发送日志
        log.info("成功发送消息给客户端[{}], 消息内容为[{}]", client, message.getBody());
    }

    private boolean checkClient(Channel channel, String client){
        if(channel == null){
            log.info("找不到该客户端[{}].", client);
            return false;
        }
        if(!channel.isOpen()){
            log.info("客户端不可达[{}].", client);
            return false;
        }
        return true;
    }
}
