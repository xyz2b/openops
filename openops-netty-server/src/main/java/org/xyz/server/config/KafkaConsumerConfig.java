package org.xyz.server.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.xyz.api.config.MqConfig;
import org.xyz.server.service.message.MqConsumerService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {
    @Autowired
    private Config config;

    @Autowired
    private KafkaProperties properties;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Bean("consumerKafkaContainerFactory")
//    @ConditionalOnBean(ConcurrentKafkaListenerContainerFactoryConfigurer.class)
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory());
        return factory;
    }

    // 获得创建消费者工厂
    public ConsumerFactory<Object, Object> consumerFactory() {
        try {
            KafkaProperties properties = objectMapper.readValue(objectMapper.writeValueAsBytes(this.properties), KafkaProperties.class);
            // 对模板 properties 进行定制化
            properties.setBootstrapServers(config.getMyServers());
            properties.getTemplate().setDefaultTopic( MqConfig.getNettyServerTopic(config.getInstanceId()));
            properties.getListener().setConcurrency(config.getKafkaConsumerListenerConcurrency());

            Map<String, Object> consumerProperties = properties.buildConsumerProperties();
            consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, MqConfig.getNettyServerGroup(config.getInstanceId()));
            consumerProperties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true);


            return new DefaultKafkaConsumerFactory<>(consumerProperties);
        } catch (IOException e) {
            log.error("get consumerFactory error: " + e);
        }
        return null;
    }


//    @Bean("consumerListenerContainer")
//    public KafkaMessageListenerContainer consumerListenerContainer() {
//        ContainerProperties properties = new ContainerProperties(MqConfig.getNettyServerTopic(config.getInstanceId()));
//
//        properties.setGroupId(MqConfig.getNettyServerGroup(config.getInstanceId()));
//
//        properties.setMessageListener(new MessageListener<Integer,String>() {
//            @Override
//            public void onMessage(ConsumerRecord<Integer, String> record) {
//                log.info("topic.quick.bean receive : " + record.toString());
//            }
//        });
//
//        return new KafkaMessageListenerContainer(consumerFactory(), properties);
//    }

}
