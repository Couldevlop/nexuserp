package com.nexuserp.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.hr", "com.nexuserp.core"})
@EnableCaching
@EnableScheduling
public class NexusHrApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusHrApplication.class, args);
    }
}
