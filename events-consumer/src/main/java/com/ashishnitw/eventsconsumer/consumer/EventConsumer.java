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
public class EventConsumer {

    @Autowired
    EventService eventService;

    @KafkaListener(topics = {"library-events"},
            autoStartup = "${libraryListener.startup:true}",
            groupId = "library-events-listener-group")
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        
        log.info("ConsumerRecord : {}", consumerRecord);
        eventService.processEvent(consumerRecord);
    }
}
