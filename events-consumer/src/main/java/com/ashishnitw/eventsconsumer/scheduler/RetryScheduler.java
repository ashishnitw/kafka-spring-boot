package com.ashishnitw.eventsconsumer.scheduler;

import com.ashishnitw.eventsconsumer.config.EventConsumerConfig;
import com.ashishnitw.eventsconsumer.jpa.FailureRecordRepository;
import com.ashishnitw.eventsconsumer.model.FailureRecord;
import com.ashishnitw.eventsconsumer.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryScheduler {

    @Autowired
    EventService eventService;

    @Autowired
    FailureRecordRepository failureRecordRepository;

    @Scheduled(fixedRate = 10000)
    public void retryFailedRecords() {
        log.info("Running Retry Scheduler");
        String status = EventConsumerConfig.RETRY;
        failureRecordRepository.findAllByStatus(status).forEach(failureRecord -> {
            try {
                log.info("Retrying Failed Record");
                ConsumerRecord<Integer, String> consumerRecord = buildConsumerRecord(failureRecord);
                eventService.processEvent(consumerRecord);
                // libraryEventsConsumer.onMessage(consumerRecord); // This does not involve the recovery code for in the consumerConfig
                failureRecord.setStatus(EventConsumerConfig.SUCCESS);
                failureRecordRepository.save(failureRecord);
            } catch (Exception e) {
                log.error("Exception in retryFailedRecords : ", e);
            }
        });
    }

    private ConsumerRecord<Integer, String> buildConsumerRecord(FailureRecord failureRecord) {
        return new ConsumerRecord<>(
                failureRecord.getTopic(),
                failureRecord.getPartition(),
                failureRecord.getOffset_value(),
                failureRecord.getKey(),
                failureRecord.getErrorRecord()
        );
    }
}
