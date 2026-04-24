package com.umc.puppymode2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Puppymode2Application {

	public static void main(String[] args) {
		SpringApplication.run(Puppymode2Application.class, args);
	}

}
