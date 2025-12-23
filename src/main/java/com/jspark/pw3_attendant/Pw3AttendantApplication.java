package com.jspark.pw3_attendant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Pw3AttendantApplication {

	public static void main(String[] args) {
		SpringApplication.run(Pw3AttendantApplication.class, args);
	}

}
