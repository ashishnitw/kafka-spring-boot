package com.ashishnitw.eventsproducer.intg.controller;

import com.ashishnitw.eventsproducer.model.Book;
import com.ashishnitw.eventsproducer.model.Event;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
public class EventControllerIntegrationTest {

    @Autowired
    TestRestTemplate testRestTemplate;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    @Timeout(5) // or use Thread.sleep()
    void postEvent() {
        Book book = Book.builder().id(1).name("Kafka Book").build();
        Event event = Event.builder().id(null).book(book).build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", MediaType.APPLICATION_JSON.toString());

        HttpEntity<Event> request = new HttpEntity<>(event, headers);
        ResponseEntity<Event> response = testRestTemplate.exchange("/event", HttpMethod.POST, request, Event.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        ConsumerRecords<Integer, String> consumerRecords = KafkaTestUtils.getRecords(consumer);
        //Thread.sleep(3000);

        //assert consumerRecords.count() == 2;

        consumerRecords.forEach(record -> {
            if (record.key() == null) {
                String expectedValue = "{\"id\":null,\"type\":\"NEW\",\"book\":{\"id\":1,\"name\":\"Kafka Book\"}}";
                String value = record.value();
                assertEquals(expectedValue, value);
            }
        });
    }

    @Test
    @Timeout(5)
    void putEvent() {
        Book book = Book.builder().id(1).name("Kafka Book").build();
        Event event = Event.builder().id(12).book(book).build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", MediaType.APPLICATION_JSON.toString());

        HttpEntity<Event> request = new HttpEntity<>(event, headers);
        ResponseEntity<Event> response = testRestTemplate.exchange("/event", HttpMethod.PUT, request, Event.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        ConsumerRecords<Integer, String> consumerRecords = KafkaTestUtils.getRecords(consumer);
        //Thread.sleep(3000);

        //assert consumerRecords.count() == 2;

        consumerRecords.forEach(record -> {
            if (record.key() != null) {
                String expectedValue = "{\"id\":12,\"type\":\"UPDATE\",\"book\":{\"id\":1,\"name\":\"Kafka Book\"}}";
                String value = record.value();
                assertEquals(expectedValue, value);
            }
        });
    }
}
