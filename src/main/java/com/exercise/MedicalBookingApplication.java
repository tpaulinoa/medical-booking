package com.exercise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MedicalBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalBookingApplication.class, args);
    }
}
