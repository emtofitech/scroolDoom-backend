package com.scrolldoom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScrollDoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrollDoomApplication.class, args);
    }
}
