package com.nexuserp.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.finance", "com.nexuserp.core"})
@EnableCaching
@EnableScheduling
public class NexusFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusFinanceApplication.class, args);
    }
}
