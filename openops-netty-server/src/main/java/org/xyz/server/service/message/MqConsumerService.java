package org.xyz.server.service.message;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.protocol.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.xyz.api.entity.SendRequest;
import org.xyz.netty.message.MessageType;
import org.xyz.netty.message.NettyMessage;
import org.xyz.netty.util.NettyMessageBuilder;
import org.xyz.server.service.channel.ChannelService;

import java.util.HashMap;
import java.util.Map;

// 处理MQ中的消息，对客户端进行发送
@Service
@Slf4j
public class MqConsumerService {


    @Autowired
    private ChannelService channelService;

    @Autowired
    private MessageSendService messageSendService;

    @Autowired
    private KafkaTemplate<String, SendRequest> kafkaTemplate;

    @KafkaListener(topics = "${spring.kafka.listener.topic}", containerFactory = "consumerKafkaContainerFactory")
    public void consumeMsg(SendRequest message, ConsumerRecord<String, SendRequest> record) {
        log.info("消费消息: " + message);
        log.info("requestId: " + message.getRequestId());
        //根据请求标识进行推送
        for(String clientId : message.getTo()){
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("UUID", message.getRequestId());

            NettyMessage sendMessage = NettyMessageBuilder.builder(MessageType.CMD_EXEC, attachment, message.getMsg());
            messageSendService.sendMessage(clientId, sendMessage);
            // 记录发送日志
            log.info("成功发送消息给客户端[{}], 消息内容为[{}]", clientId, sendMessage.getBody());
        }
    }
}
