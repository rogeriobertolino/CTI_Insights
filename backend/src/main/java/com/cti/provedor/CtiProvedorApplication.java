package com.cti.provedor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude =
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
            .class)
public class CtiProvedorApplication {
  public static void main(String[] args) {
    SpringApplication.run(CtiProvedorApplication.class, args);
  }
}
