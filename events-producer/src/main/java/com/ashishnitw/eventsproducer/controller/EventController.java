package com.ashishnitw.eventsproducer.controller;

import com.ashishnitw.eventsproducer.constant.EventType;
import com.ashishnitw.eventsproducer.model.Event;
import com.ashishnitw.eventsproducer.producer.EventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class EventController {

    @Autowired
    EventProducer eventProducer;

    @PostMapping("/event")
    public ResponseEntity<Event> postEvent(@RequestBody @Valid Event event) throws JsonProcessingException {

        // invoke kafka producer
        event.setType(EventType.NEW);
        //eventProducer.sendLibraryEvent(event);  // Async
        eventProducer.sendLibraryEvent_Approach2(event);  // Async
        //eventProducer.sendLibraryEventSynchronous(event);  // Sync

        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PutMapping("/event")
    public ResponseEntity<?> putEvent(@RequestBody @Valid Event event) throws JsonProcessingException {

        if(event.getId() == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please pass the event id");
        event.setType(EventType.UPDATE);
        eventProducer.sendLibraryEvent_Approach2(event);  // Async
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }
}
