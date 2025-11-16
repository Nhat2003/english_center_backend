package com.example.English.Center.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EnglishCenterDataApplication {

    private static final Logger logger = LoggerFactory.getLogger(EnglishCenterDataApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EnglishCenterDataApplication.class, args);
        logger.info("English Center Data application started successfully");
    }

}
