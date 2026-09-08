package com.aicoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiInterviewCoachApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiInterviewCoachApplication.class, args);
    }
}
