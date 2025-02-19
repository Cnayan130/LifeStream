package top.principlecreativity.lifestream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class LifeStreamApplication {
	public static void main(String[] args) {
		SpringApplication.run(LifeStreamApplication.class, args);
	}
}
