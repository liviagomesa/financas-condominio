package com.financas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancasBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancasBackendApplication.class, args);
	}

}
