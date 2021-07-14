package org.xyz.server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.xyz.api.config.MqConfig;
import org.xyz.server.netty.NettyServer;

@Configuration
public class KafkaTopicConfig {
    @Autowired
    private Config config;

    @Bean
    public KafkaAdmin admin(KafkaProperties properties){
        KafkaAdmin admin = new KafkaAdmin(properties.buildAdminProperties());
        admin.setFatalIfBrokerNotAvailable(true);
        return admin;
    }

    // 创建topic
    @Bean
    public NewTopic nettyServerTopic() {
        // 创建topic，需要指定创建的topic的"名称"、"分区数"、"副本数量(副本数数目的值要小于等于Broker数量)"
        return new NewTopic(MqConfig.getNettyServerTopic(config.getInstanceId()), config.getPartitions(), (short) config.getReplicates());
    }

    @Bean
    public NewTopic cmdResultTopic() {
        // 创建topic，需要指定创建的topic的"名称"、"分区数"、"副本数量(副本数数目的值要小于等于Broker数量)"
        return new NewTopic(config.getCmdResultTopic(), config.getPartitions(), (short) config.getReplicates());
    }
}
