package com.daewon.xeno_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication(exclude={SecurityAutoConfiguration.class})
public class XenoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(XenoBackendApplication.class, args);
	}

}
