package com.nexuserp.importservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.nexuserp.importservice", "com.nexuserp.core"})
public class NexusImportApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusImportApplication.class, args);
    }
}
