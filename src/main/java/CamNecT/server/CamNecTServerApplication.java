package CamNecT.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class CamNecTServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CamNecTServerApplication.class, args);
	}

}
