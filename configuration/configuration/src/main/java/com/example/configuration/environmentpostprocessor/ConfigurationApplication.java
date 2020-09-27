package com.example.configuration.environmentpostprocessor;


import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

@Log4j2
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(Environment environment) {
        return args -> log.info("message from " + BootifulEnvironmentProcessor.class.getName() + ": " + environment.getProperty("bootiful-message"));
    }
}


@Log4j2
class BootifulEnvironmentProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        log.info("contributing " + this.getClass().getName() + '.');
        environment.getPropertySources().addFirst(new BootifulPropertySource());
    }
}

class BootifulPropertySource extends PropertySource<String> {


    BootifulPropertySource() {
        super("bootiful");
    }

    @Override
    public Object getProperty(String name) {

        if (name.equalsIgnoreCase("bootiful-message")) {
            return "Hello from " + this.getClass().getSimpleName() + "!";
        }

        return null;
    }

}
