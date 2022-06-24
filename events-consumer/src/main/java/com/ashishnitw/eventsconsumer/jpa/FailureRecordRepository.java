package com.ashishnitw.eventsconsumer.jpa;

import com.ashishnitw.eventsconsumer.model.FailureRecord;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FailureRecordRepository extends CrudRepository<FailureRecord, Integer> {

    List<FailureRecord> findAllByStatus(String status);
}