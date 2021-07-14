package org.xyz.server.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class Config {
    @Value("${hostip}")
    private String hostip;
    @Value("${eureka.instance.instance-id}")
    private String instanceId;
    @Value("${spring.kafka.listener.concurrency}")
    private int kafkaConsumerListenerConcurrency;
    @Value("${spring.kafka.bootstrap-servers}")
    private List<String> myServers;
    @Value("${spring.kafka.topic.auto-create.partitions}")
    private int partitions;
    @Value("${spring.kafka.topic.auto-create.replicates}")
    private int replicates;
    @Value("${spring.kafka.producer.topic}")
    private String cmdResultTopic;
}
