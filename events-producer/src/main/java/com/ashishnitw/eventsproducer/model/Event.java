package com.ashishnitw.eventsproducer.model;

import com.ashishnitw.eventsproducer.constant.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Event {

    private Integer id;
    private EventType type;
    @NotNull
    @Valid
    private Book book;
}