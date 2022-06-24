package com.ashishnitw.eventsconsumer.model;

import com.ashishnitw.eventsconsumer.constant.EventType;
import lombok.*;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
public class Event {

    @Id
    @GeneratedValue
    private Integer id;
    @Enumerated(EnumType.STRING)
    private EventType type;
    @OneToOne(mappedBy = "event", cascade = {CascadeType.ALL})
    @ToString.Exclude
    private Book book;
}