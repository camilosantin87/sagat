package com.execise.sagat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SagatApplication {

	public static void main(String[] args) {
		SpringApplication.run(SagatApplication.class, args);
	}

}
