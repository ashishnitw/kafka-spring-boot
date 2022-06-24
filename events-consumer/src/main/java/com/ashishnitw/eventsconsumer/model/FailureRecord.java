package com.ashishnitw.eventsconsumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
public class FailureRecord {
    @Id
    @GeneratedValue
    private Integer bookId;
    private String topic;
    private Integer key;
    private String errorRecord;
    private Integer partition;
    private Long offset_value;
    private String exception;
    private String status;
}
