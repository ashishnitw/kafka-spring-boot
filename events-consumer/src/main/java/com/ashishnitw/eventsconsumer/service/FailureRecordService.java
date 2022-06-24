package com.ashishnitw.eventsconsumer.service;

import com.ashishnitw.eventsconsumer.jpa.FailureRecordRepository;
import com.ashishnitw.eventsconsumer.model.FailureRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FailureRecordService {

    private FailureRecordRepository failureRecordRepository;

    public FailureRecordService(FailureRecordRepository failureRecordRepository) {
        this.failureRecordRepository = failureRecordRepository;
    }

    public void saveFailedRecord(ConsumerRecord<Integer, String> record, Exception exception, String recordStatus) {
        FailureRecord failureRecord = new FailureRecord(null, record.topic(), record.key(), record.value(),
                record.partition(), record.offset(), exception.getCause().getMessage(), recordStatus);
        failureRecordRepository.save(failureRecord);
    }
}
