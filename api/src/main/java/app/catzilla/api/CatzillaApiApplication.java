package app.catzilla.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Catzilla API",
                        version = "v1",
                        description =
                                "Track cats' health, habits, weight and feeding schedules, shared within a Home."))
public class CatzillaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatzillaApiApplication.class, args);
    }
}
