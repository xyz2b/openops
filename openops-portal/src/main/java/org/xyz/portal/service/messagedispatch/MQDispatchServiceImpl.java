package org.xyz.portal.service.messagedispatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.xyz.api.config.MqConfig;
import org.xyz.api.entity.SendRequest;
import org.xyz.portal.service.messagedispatch.MessageDispatchService;

@Slf4j
@Service
public class MQDispatchServiceImpl implements MessageDispatchService {
    @Autowired
    private KafkaTemplate<String, SendRequest> kafkaTemplate;

    @Override
    public void send(String instanceId, SendRequest request) {
        kafkaTemplate.send(MqConfig.getNettyServerTopic(instanceId), request);
    }
}
