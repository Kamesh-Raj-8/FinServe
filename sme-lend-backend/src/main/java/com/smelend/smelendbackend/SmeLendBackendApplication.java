package com.smelend.smelendbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmeLendBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmeLendBackendApplication.class, args);
    }
}
