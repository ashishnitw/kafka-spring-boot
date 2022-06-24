package com.ashishnitw.eventsconsumer.intg.consumer;

import com.ashishnitw.eventsconsumer.constant.EventType;
import com.ashishnitw.eventsconsumer.consumer.EventConsumer;
import com.ashishnitw.eventsconsumer.jpa.EventRepository;
import com.ashishnitw.eventsconsumer.jpa.FailureRecordRepository;
import com.ashishnitw.eventsconsumer.model.Book;
import com.ashishnitw.eventsconsumer.model.Event;
import com.ashishnitw.eventsconsumer.service.EventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@SpringBootTest
@EmbeddedKafka(topics = {"library-events", "library-events-retry", "library-events-dlt"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "retryListener.startup=false"})
public class EventConsumerIntegrationTest {

    @Value("${topics.retry}")
    private String retryTopic;

    @Value("${topics.dlt}")
    private String deadLetterTopic;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry endpointRegistry;

    @SpyBean
    EventConsumer eventConsumer;

    @SpyBean
    EventService eventService;

    @SpyBean
    EventRepository eventRepository;

    @SpyBean
    FailureRecordRepository failureRecordRepository;

    @Autowired
    ObjectMapper objectMapper;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {
        // If we have multiple consumers in app
        MessageListenerContainer container = endpointRegistry.getListenerContainers()
                .stream()
                .filter(messageListenerContainer -> Objects.equals(messageListenerContainer.getGroupId(), "library-events-listener-group"))
                .collect(Collectors.toList())
                .get(0);
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

//        for (MessageListenerContainer container : endpointRegistry.getListenerContainers()) {
//            System.out.println("Group Id : " + container.getGroupId());
//            if (Objects.equals(container.getGroupId(), "library-events-listener-group")) {
//                System.out.println("Waiting for assignment");
//                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
//            }
//        }
    }

    @AfterEach
    void tearDown() {
        eventRepository.deleteAll();
        failureRecordRepository.deleteAll();
    }

    @Test
    void publishNewEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
        String json = "{\"id\":null,\"type\":\"NEW\",\"book\":{\"id\":123,\"name\":\"Kafka Book\"}}";
        kafkaTemplate.sendDefault(json).get(); // .get() is used to make call synchronous

        CountDownLatch latch = new CountDownLatch(1);    // blocks current execution of thread, helpful when writing tests with async calls
        latch.await(3, TimeUnit.SECONDS);

        verify(eventConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(eventService, times(1)).processEvent(isA(ConsumerRecord.class));

        List<Event> events = (List<Event>) eventRepository.findAll();
        assert events.size() == 1;
        events.forEach(event -> {
            assert event.getId() != null;
            assertEquals(123, event.getBook().getId());
        });
    }

    @Test
    void publishUpdateEvent() throws ExecutionException, InterruptedException, JsonProcessingException {

        String json = "{\"id\":null,\"type\":\"NEW\",\"book\":{\"id\":123,\"name\":\"Kafka Book\"}}";
        Event event = objectMapper.readValue(json, Event.class);
        event.getBook().setEvent(event);
        eventRepository.save(event);

        Book updatedBook = Book.builder().id(123).name("Kafka Book 2.0").build();
        event.setBook(updatedBook);
        event.setType(EventType.UPDATE);

        String updatedJson = objectMapper.writeValueAsString(event);
        kafkaTemplate.sendDefault(event.getId(), updatedJson).get(); // .get() is used to make call synchronous

        CountDownLatch latch = new CountDownLatch(1);    // blocks current execution of thread, helpful when writing tests with async calls
        latch.await(3, TimeUnit.SECONDS);

        // Not necessary here
        //verify(eventConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        //verify(eventService, times(1)).processEvent(isA(ConsumerRecord.class));

        Event savedEvent = eventRepository.findById(event.getId()).get();
        assertEquals("Kafka Book 2.0", savedEvent.getBook().getName());
    }

    @Test
    void publishUpdateEvent_nullEventId() throws ExecutionException, InterruptedException, JsonProcessingException {

        String json = "{\"id\":null,\"type\":\"UPDATE\",\"book\":{\"id\":123,\"name\":\"Kafka Book\"}}";

        kafkaTemplate.sendDefault(json).get(); // .get() is used to make call synchronous

        CountDownLatch latch = new CountDownLatch(1);    // blocks current execution of thread, helpful when writing tests with async calls
        latch.await(5, TimeUnit.SECONDS);   // increasing wait time, because it will retry multiple times

        verify(eventConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(eventService, times(1)).processEvent(isA(ConsumerRecord.class));

        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group3", "true", embeddedKafkaBroker));
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, deadLetterTopic);

        ConsumerRecords<Integer, String> consumerRecords = KafkaTestUtils.getRecords(consumer);

        List<ConsumerRecord<Integer, String>> deadLetterList = new ArrayList<>();
        consumerRecords.forEach((record) -> {
            if (record.topic().equals(deadLetterTopic)) {
                deadLetterList.add(record);
            }
        });

        List<ConsumerRecord<Integer, String>> finalList = deadLetterList.stream()
                .filter(record -> record.value().equals(json))
                .collect(Collectors.toList());

        assert finalList.size() == 1;
    }

    @Test
    void publishUpdateEvent_999_eventId() throws ExecutionException, InterruptedException, JsonProcessingException {

        String json = "{\"id\":999,\"type\":\"UPDATE\",\"book\":{\"id\":123,\"name\":\"Kafka Book\"}}";

        kafkaTemplate.sendDefault(json).get(); // .get() is used to make call synchronous

        CountDownLatch latch = new CountDownLatch(1);    // blocks current execution of thread, helpful when writing tests with async calls
        latch.await(5, TimeUnit.SECONDS);   // increasing wait time, because it will retry multiple times

        verify(eventConsumer, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(eventService, times(3)).processEvent(isA(ConsumerRecord.class));
    }

    @Test
    void publishUpdateEvent_999_eventId_deadLetterTopic() throws ExecutionException, InterruptedException, JsonProcessingException {

        String json = "{\"id\":999,\"type\":\"UPDATE\",\"book\":{\"id\":123,\"name\":\"Kafka Book\"}}";

        kafkaTemplate.sendDefault(json).get(); // .get() is used to make call synchronous

        CountDownLatch latch = new CountDownLatch(1);    // blocks current execution of thread, helpful when writing tests with async calls
        latch.await(5, TimeUnit.SECONDS);   // increasing wait time, because it will retry multiple times

        //without Retry listener
        //verify(eventConsumer, times(3)).onMessage(isA(ConsumerRecord.class));
        //verify(eventService, times(3)).processEvent(isA(ConsumerRecord.class));

        //With Retry listener
        verify(eventConsumer, atLeast(3)).onMessage(isA(ConsumerRecord.class));
        verify(eventService, atLeast(3)).processEvent(isA(ConsumerRecord.class));

        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, retryTopic);

        ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, retryTopic);

        System.out.println("consumer Record in deadLetter topic : " + consumerRecord.value());

        assertEquals(json, consumerRecord.value());
        consumerRecord.headers().forEach(header -> System.out.println("Header Key : " + header.key() + ", Header Value : " + new String(header.value())));
    }

    @Test
    @Disabled
    void publishModifyLibraryEvent_null_eventId_failureRecord() throws JsonProcessingException, InterruptedException, ExecutionException {

        String json = "{\"id\":null,\"type\":\"UPDATE\",\"book\":{\"id\":123,\"name\":\"Kafka Book\"}}";
        kafkaTemplate.sendDefault(json).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);

        verify(eventConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(eventService, times(1)).processEvent(isA(ConsumerRecord.class));

        assertEquals(1, failureRecordRepository.count());
        failureRecordRepository.findAll().forEach(failureRecord -> System.out.println("failureRecord : " + failureRecord));

    }

    @Test
    @Disabled
    void publishModifyLibraryEvent_999_eventId_failureRecord() throws JsonProcessingException, InterruptedException, ExecutionException {

        String json = "{\"id\":999,\"type\":\"UPDATE\",\"book\":{\"id\":123,\"name\":\"Kafka Book\"}}";
        kafkaTemplate.sendDefault(999, json).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);

        verify(eventConsumer, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(eventService, times(3)).processEvent(isA(ConsumerRecord.class));

        assertEquals(1, failureRecordRepository.count());
        failureRecordRepository.findAll().forEach(failureRecord -> System.out.println("failureRecord : " + failureRecord));

    }

}
