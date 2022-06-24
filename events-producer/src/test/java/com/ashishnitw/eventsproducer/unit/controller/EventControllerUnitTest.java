package com.ashishnitw.eventsproducer.unit.controller;

import com.ashishnitw.eventsproducer.constant.EventType;
import com.ashishnitw.eventsproducer.controller.EventController;
import com.ashishnitw.eventsproducer.model.Book;
import com.ashishnitw.eventsproducer.model.Event;
import com.ashishnitw.eventsproducer.producer.EventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc
public class EventControllerUnitTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    EventProducer eventProducer;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postEvent() throws Exception {
        Book book = Book.builder().id(1).name("Kafka Book").build();
        Event event = Event.builder().id(1).type(EventType.NEW).book(book).build();

        String json = objectMapper.writeValueAsString(event);
        when(eventProducer.sendLibraryEvent_Approach2(isA(Event.class))).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/event")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    void postEvent_4xx() throws Exception {
        Book book = Book.builder().id(null).name("").build();
        Event event = Event.builder().id(1).type(EventType.NEW).book(book).build();

        String json = objectMapper.writeValueAsString(event);
        when(eventProducer.sendLibraryEvent_Approach2(isA(Event.class))).thenReturn(null);

        String expectedErrorMessage = "book.id - must not be null, book.name - must not be blank";

        mockMvc.perform(MockMvcRequestBuilders.post("/event")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andExpect(MockMvcResultMatchers.content().string(expectedErrorMessage));
    }

    @Test
    void putEvent() throws Exception {
        Book book = Book.builder().id(1).name("Kafka Book").build();
        Event event = Event.builder().id(12).book(book).build();

        String json = objectMapper.writeValueAsString(event);
        when(eventProducer.sendLibraryEvent_Approach2(isA(Event.class))).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.put("/event")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void putEvent_nullEventId() throws Exception {
        Book book = Book.builder().id(1).name("Kafka Book").build();
        Event event = Event.builder().id(null).book(book).build();

        String json = objectMapper.writeValueAsString(event);
        when(eventProducer.sendLibraryEvent_Approach2(isA(Event.class))).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.put("/event")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andExpect(MockMvcResultMatchers.content().string("Please pass the event id"));
    }

}
