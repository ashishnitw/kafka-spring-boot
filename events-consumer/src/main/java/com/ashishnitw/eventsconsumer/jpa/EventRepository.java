package com.ashishnitw.eventsconsumer.jpa;

import com.ashishnitw.eventsconsumer.model.Event;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<Event, Integer> {

}