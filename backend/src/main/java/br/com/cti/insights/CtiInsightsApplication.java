package br.com.cti.insights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude =
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
            .class)
public class CtiInsightsApplication {
  public static void main(String[] args) {
    SpringApplication.run(CtiInsightsApplication.class, args);
  }
}
