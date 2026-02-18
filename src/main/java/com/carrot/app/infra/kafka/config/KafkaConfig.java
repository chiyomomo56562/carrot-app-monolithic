package com.carrot.app.infra.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        // 메시지 발송자
        @Bean
        public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
                Map<String, Object> configProps = new HashMap<>();
                configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

                return new DefaultKafkaProducerFactory<>(configProps,
                                new StringSerializer(),
                                new JsonSerializer<>(objectMapper));
        }

        // 메시지 발송 도구
        @Bean
        public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
                return new KafkaTemplate<>(producerFactory);
        }

        // 리스너 엔진
        // @KafkaListener를 사용할 수 있도록 설정
        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
                        ConsumerFactory<String, Object> consumerFactory,
                        KafkaTemplate<String, Object> kafkaTemplate) {
                ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(consumerFactory);

                // DLQ 설정: 재시도 3번 후 실패 시 DLQ로 전송
                factory.setCommonErrorHandler(kafkaErrorHandler(kafkaTemplate));

                return factory;
        }

        /**
         * Kafka Error Handler with DLQ
         * - 3번 재시도 (exponential backoff)
         * - 실패 시 {original-topic}.dlq로 전송
         */
        @Bean
        public DefaultErrorHandler kafkaErrorHandler(
                        KafkaTemplate<String, Object> kafkaTemplate) {

                // DLQ로 실패한 메시지 전송
                DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                                kafkaTemplate,
                                (record, ex) -> {
                                        // DLQ 토픽 이름: {original-topic}.dlq
                                        String dlqTopic = record.topic() + ".dlq";
                                        return new TopicPartition(dlqTopic, record.partition());
                                });

                // 재시도 정책: 3번 시도, exponential backoff (1초, 2초, 4초)
                DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                                recoverer,
                                new FixedBackOff(1000L, 3L)); // 1초 간격, 3번 재시도

                // 재시도하지 않을 예외 타입 (필요시 추가)
                // errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

                return errorHandler;
        }

        // 메시지 수신자
        @Bean
        public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
                Map<String, Object> configProps = new HashMap<>();
                configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

                // Json 역직렬화
                JsonDeserializer<Object> deserializer = new JsonDeserializer<>(objectMapper);
                // Json 역직렬화를 위한 허용 패키지
                // *이 아닌 패키지 지정 필요
                deserializer.addTrustedPackages("*");

                return new DefaultKafkaConsumerFactory<>(configProps,
                                new StringDeserializer(),
                                deserializer);
        }
}
