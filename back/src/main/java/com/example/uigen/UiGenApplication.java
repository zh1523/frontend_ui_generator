package com.example.uigen;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class UiGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(UiGenApplication.class, args);
    }
}
