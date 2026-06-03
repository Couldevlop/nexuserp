package com.nexuserp.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.inventory", "com.nexuserp.core"})
@EnableCaching
public class NexusInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusInventoryApplication.class, args);
    }
}
