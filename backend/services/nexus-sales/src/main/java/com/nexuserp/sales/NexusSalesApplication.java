package com.nexuserp.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.sales", "com.nexuserp.core"})
@EnableCaching
@EnableScheduling
public class NexusSalesApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusSalesApplication.class, args);
    }
}
