package com.example.English.Center.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EnglishCenterDataApplication {

    private static final Logger logger = LoggerFactory.getLogger(EnglishCenterDataApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EnglishCenterDataApplication.class, args);
        logger.info("English Center Data application started successfully");
    }

}
