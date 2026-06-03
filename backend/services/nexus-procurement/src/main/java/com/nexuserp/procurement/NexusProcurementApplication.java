package com.nexuserp.procurement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.procurement", "com.nexuserp.core"})
@EnableCaching
public class NexusProcurementApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusProcurementApplication.class, args);
    }
}
