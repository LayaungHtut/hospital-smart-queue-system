package com.hospitalqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HospitalSmartQueueSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(HospitalSmartQueueSystemApplication.class, args);
	}

}
