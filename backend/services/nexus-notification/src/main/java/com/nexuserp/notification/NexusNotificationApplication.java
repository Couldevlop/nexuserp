package com.nexuserp.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.notification", "com.nexuserp.core"})
@EnableAsync
public class NexusNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusNotificationApplication.class, args);
    }
}
