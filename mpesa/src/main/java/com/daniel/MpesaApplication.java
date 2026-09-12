package com.daniel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MpesaApplication {
    public static void main(String[] args) {
        SpringApplication.run(MpesaApplication.class, args);
    }
}