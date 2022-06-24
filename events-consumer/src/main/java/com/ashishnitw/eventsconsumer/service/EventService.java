package com.ashishnitw.eventsconsumer.service;

import com.ashishnitw.eventsconsumer.jpa.EventRepository;
import com.ashishnitw.eventsconsumer.model.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class EventService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    ObjectMapper objectMapper;

    public void processEvent(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        Event event = objectMapper.readValue(consumerRecord.value(), Event.class);
        log.info("event : {}", event);

        switch (event.getType()) {
            case NEW:
                save(event);
                break;
            case UPDATE:
                validate(event);
                save(event);
                break;
            default:
                log.info("Invalid Event Type");
        }
    }

    private void validate(Event event) {
        if(event != null && event.getId() != null && event.getId() == 999) {
            // hardcoded just for testing recoverable exception
            throw new RecoverableDataAccessException("Testing recoverable exception...");
        }
        if (event.getId() == null) {
            throw new IllegalArgumentException("Event Id is missing");
        }
        Optional<Event> eventOptional = eventRepository.findById(event.getId());
        if (!eventOptional.isPresent()) {
            throw new IllegalArgumentException("Not a valid Event");
        }
        log.info("Validation is successful for the library Event : {} ", eventOptional.get());
    }

    private void save(Event event) {
        event.getBook().setEvent(event);
        eventRepository.save(event);
        log.info("Successfully Persisted the Event {} ", event);
    }
}
