package com.ashishnitw.eventsproducer.unit.producer;

import com.ashishnitw.eventsproducer.constant.EventType;
import com.ashishnitw.eventsproducer.model.Book;
import com.ashishnitw.eventsproducer.model.Event;
import com.ashishnitw.eventsproducer.producer.EventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventProducerUnitTest {
    
    @Mock
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    EventProducer eventProducer;

    @Test
    void sendLibraryEvent_Approach2_failure() {
        Book book = Book.builder().id(1).name("Kafka Book").build();
        Event event = Event.builder().id(1).type(EventType.NEW).book(book).build();

        SettableListenableFuture future = new SettableListenableFuture();
        future.setException(new RuntimeException("Exception calling Kafka"));

        Mockito.when(kafkaTemplate.send(Mockito.isA(ProducerRecord.class)))
                .thenReturn(future);

        Assertions.assertThrows(Exception.class, () -> eventProducer.sendLibraryEvent_Approach2(event).get());
    }

    @Test
    void sendLibraryEvent_Approach2_Success() throws JsonProcessingException, ExecutionException, InterruptedException {
        Book book = Book.builder().id(1).name("Kafka Book").build();
        Event event = Event.builder().id(1).type(EventType.NEW).book(book).build();

        String record = objectMapper.writeValueAsString(event);

        SettableListenableFuture future = new SettableListenableFuture();
        ProducerRecord producerRecord = new ProducerRecord("library-events", event);
        RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition("library-events", 1), 1, 1, System.currentTimeMillis(), 1, 2);
        SendResult<Integer, String> sendResult = new SendResult<>(producerRecord, recordMetadata);

        future.set(sendResult);
        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(future);

        ListenableFuture<SendResult<Integer,String>> listenableFuture = eventProducer.sendLibraryEvent_Approach2(event);

        SendResult<Integer, String> sendResult1 = listenableFuture.get();
        assert sendResult1.getRecordMetadata().partition()==1;
    }
}