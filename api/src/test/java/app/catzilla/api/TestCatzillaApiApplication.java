package app.catzilla.api;

import org.springframework.boot.SpringApplication;

public class TestCatzillaApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(CatzillaApiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
