package com.newsverifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class NewsVerifierApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewsVerifierApplication.class, args);
    }
}
