package com.ashishnitw.eventsconsumer.config;

import com.ashishnitw.eventsconsumer.service.FailureRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableKafka
@Slf4j
public class EventConsumerConfig {

    public static final String RETRY = "RETRY";
    public static final String SUCCESS = "SUCCESS";
    public static final String DEAD = "DEAD";

    @Value("${topics.retry:library-events-retry}")
    private String retryTopic;

    @Value("${topics.dlt:library-events-dlt}")
    private String deadLetterTopic;

    @Autowired
    FailureRecordService failureRecordService;

    @Autowired
    KafkaProperties kafkaProperties;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Bean
    @ConditionalOnMissingBean(name = {"kafkaListenerContainerFactory"})
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConcurrentKafkaListenerContainerFactoryConfigurer configurer, ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(3); // 3 threads with same instance of Kafka listener
        factory.setCommonErrorHandler(errorHandler());
        // Manual offset
        //factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    public DefaultErrorHandler errorHandler() {
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2L);

        ExponentialBackOffWithMaxRetries expBackOff = new ExponentialBackOffWithMaxRetries(2);
        expBackOff.setInitialInterval(1_000L);
        expBackOff.setMultiplier(2.0);
        expBackOff.setMaxInterval(2_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                // to handle failures : publishingRecoverer or consumerRecordRecoverer
                //publishingRecoverer()
                consumerRecordRecoverer,

                // Use expBackOff or fixedBackOff
                //fixedBackOff
                expBackOff
        );

        // we can ignore some exceptions, which are not recoverable
        List<Class> exceptionsToIgnore = Arrays.asList(IllegalArgumentException.class);
        exceptionsToIgnore.forEach(errorHandler::addNotRetryableExceptions);

        errorHandler.setRetryListeners((consumerRecord, ex, deliveryAttempt) -> {
            log.info("Failed record in Retry Listener, Exception : {}, deliveryAttempt : {}", ex.getMessage(), deliveryAttempt);
        });
        return errorHandler;
    }

    public DeadLetterPublishingRecoverer publishingRecoverer() {
        // It also adds additional headers in ConsumerRecord about exceptions.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (r, e) -> {
            log.error("Exception in publishingRecoverer : {} ", e.getMessage(), e);
            if (e.getCause() instanceof RecoverableDataAccessException) {
                return new TopicPartition(retryTopic, r.partition());
            } else {
                return new TopicPartition(deadLetterTopic, r.partition());
            }
        });
        return recoverer;
    }

    ConsumerRecordRecoverer consumerRecordRecoverer = (record, ex) -> {

        log.error("Exception is : {} Failed Record : {} ", ex, record);
        ConsumerRecord<Integer, String> consumerRecord = (ConsumerRecord<Integer, String>) record;

        if (ex.getCause() instanceof RecoverableDataAccessException) {
            log.info("Inside the recoverable logic");
            // Add any Recovery Code here
            failureRecordService.saveFailedRecord(consumerRecord, ex, RETRY);
        } else {
            log.info("Inside the non recoverable logic and skipping the record : {}", record);
            // non recovery logic
            failureRecordService.saveFailedRecord(consumerRecord, ex, DEAD);
        }
    };
}