package com.ashishnitw.eventsconsumer.consumer;

import com.ashishnitw.eventsconsumer.service.EventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LibraryEventsRetryConsumer {

    @Autowired
    private EventService eventService;

    @KafkaListener(topics = {"${topics.retry}"},
            autoStartup = "${retryListener.startup:true}",
            groupId = "retry-listener-group")
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {

        log.info("ConsumerRecord in Retry Consumer: {} ", consumerRecord);
        // check all headers values
        //consumerRecord.headers().forEach(header -> log.info("key : {}, value : {}", header.key(), header.value()));
        eventService.processEvent(consumerRecord);
    }
}
