package org.xyz.server.service.message.messagedispatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.xyz.api.config.MqConfig;
import org.xyz.api.entity.CmdResult;
import org.xyz.api.entity.SendRequest;

@Slf4j
@Service
public class MQDispatchServiceImpl implements MessageDispatchService {
    @Autowired
    private KafkaTemplate<String, CmdResult> kafkaTemplate;

    @Override
    public void send(String topic, CmdResult request) {
        kafkaTemplate.send(topic, request);
    }
}
