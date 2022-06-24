package com.ashishnitw.eventsproducer.producer;

import com.ashishnitw.eventsproducer.model.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class EventProducer {

    String topic = "library-events";

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    // APPROACH 1
    public void sendLibraryEvent(Event event) throws JsonProcessingException {

        Integer key = event.getId();
        String value = objectMapper.writeValueAsString(event);

        // Asynchronous Call that returns future object
        ListenableFuture<SendResult<Integer, String>> listenableFuture = kafkaTemplate.sendDefault(key, value); // sends message to default topic that we mention in application.yml file
        listenableFuture.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                handleFailure(key, value, ex);
            }

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                handleSuccess(key, value, result);
            }
        });
    }

    // APPROACH 2 => to particular topic
    public ListenableFuture<SendResult<Integer, String>> sendLibraryEvent_Approach2(Event event) throws JsonProcessingException {

        Integer key = event.getId();
        String value = objectMapper.writeValueAsString(event);

        ProducerRecord<Integer, String> producerRecord = buildProducerRecord(key, value, topic);

        ListenableFuture<SendResult<Integer, String>> listenableFuture = kafkaTemplate.send(producerRecord);

        listenableFuture.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                handleFailure(key, value, ex);
            }

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                handleSuccess(key, value, result);
            }
        });

        return listenableFuture;
    }

    private ProducerRecord<Integer, String> buildProducerRecord(Integer key, String value, String topic) {

        // we can add additional info in headers
        List<Header> recordHeaders = Arrays.asList(new RecordHeader("event-source", "scanner".getBytes()));

        return new ProducerRecord<>(topic, null, key, value, recordHeaders);
    }


    // APPROACH 3 => ASYNCHRONOUS
    public SendResult<Integer, String> sendLibraryEventSynchronous(Event event) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {

        Integer key = event.getId();
        String value = objectMapper.writeValueAsString(event);
        SendResult<Integer, String> sendResult = null;
        try {
            // Asynchronous call => .get() waits for the result
            //sendResult = kafkaTemplate.sendDefault(key, value).get();
            sendResult = kafkaTemplate.sendDefault(key, value).get(1, TimeUnit.SECONDS); // with timeout
        } catch (ExecutionException | InterruptedException e) {
            log.error("ExecutionException/InterruptedException Sending the Message and the exception is {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Exception Sending the Message and the exception is {}", e.getMessage());
            throw e;
        }
        System.out.println("Message Sent SuccessFully: " + sendResult);
        return sendResult;
    }

    private void handleFailure(Integer key, String value, Throwable ex) {
        log.error("Error Sending the Message and the exception is {}", ex.getMessage());
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in OnFailure: {}", throwable.getMessage());
        }
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> result) {
        log.info("Message Sent SuccessFully for the key : {} and the value is {} , partition is {}", key, value, result.getRecordMetadata().partition());
    }
}
