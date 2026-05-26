package br.com.realize.digitalbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DigitalBankApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalBankApiApplication.class, args);
    }
}
