package org.xyz.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xyz.api.entity.SendRequest;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;
import org.xyz.netty.util.NettyMessageBuilder;
import org.xyz.server.service.message.MessageSendService;

import java.util.List;

@Service
@Slf4j
public class MessageHttpService {
    @Autowired
    private MessageSendService messageSendService;

    public void send(SendRequest request) {
        //根据标识进行推送
        List<String> list = request.getTo();
        if(CollectionUtils.isEmpty(list)){
            return ;
        }
        for(String client : list){
            messageSendService.sendMessage(client, NettyMessageBuilder.builder(MessageType.CMD_EXEC, request.getMsg()));
        }
    }
}
