package com.nexuserp.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.reporting", "com.nexuserp.core"})
@EnableCaching
public class NexusReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusReportingApplication.class, args);
    }
}
