package com.example.swp391.aistudenthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AistudenthubApplication {

	public static void main(String[] args) {
		SpringApplication.run(AistudenthubApplication.class, args);
	}

}
