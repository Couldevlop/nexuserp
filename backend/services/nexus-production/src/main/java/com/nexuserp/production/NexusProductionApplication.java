package com.nexuserp.production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.production", "com.nexuserp.core"})
@EnableCaching
@EnableScheduling
public class NexusProductionApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusProductionApplication.class, args);
    }
}
