package com.dpd.uk.returns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.statemachine.config.EnableStateMachine;

@SpringBootApplication
@EnableFeignClients
@EnableMongoAuditing
@EnableStateMachine
public class ReturnsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReturnsServiceApplication.class, args);
	}
}
