package dev.michaelgoldman.recipebookbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {
    @Bean
    SimpleModule blankToNullStringModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new BlankToNullStringDeserializer());
        return module;
    }
}
