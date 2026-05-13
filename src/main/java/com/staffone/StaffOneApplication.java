package com.staffone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StaffOneApplication {
    public static void main(String[] args) {
        SpringApplication.run(StaffOneApplication.class, args);
    }
}
