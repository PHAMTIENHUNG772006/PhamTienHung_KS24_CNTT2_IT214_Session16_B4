package com.res.session16_b4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class Session16B4Application {

    public static void main(String[] args) {
        SpringApplication.run(Session16B4Application.class, args);
    }

}
