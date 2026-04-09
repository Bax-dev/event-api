package com.digicore.eventapi;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EventApiApplication {

    public static void main(String[] args) {
        // Load .env into system properties before Spring initialises
        Dotenv.configure()
                .ignoreIfMissing()
                .load()
                .entries()
                .forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        SpringApplication.run(EventApiApplication.class, args);
    }
}
