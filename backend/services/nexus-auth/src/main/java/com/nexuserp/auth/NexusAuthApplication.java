package com.nexuserp.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.auth", "com.nexuserp.core"})
public class NexusAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusAuthApplication.class, args);
    }
}
