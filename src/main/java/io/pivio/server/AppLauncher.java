package io.pivio.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppLauncher {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(AppLauncher.class, args);
    }
}
