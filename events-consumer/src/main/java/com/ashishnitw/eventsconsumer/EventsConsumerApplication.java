package com.ashishnitw.eventsconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventsConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventsConsumerApplication.class, args);
	}

}
