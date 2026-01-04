package com.votzz.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// ADICIONE ESTA LINHA DENTRO DOS PARÊNTESES:
@SpringBootApplication(exclude = {
    io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration.class,
    io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration.class
})
@EnableAsync // <--- Adicione esta linha
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}