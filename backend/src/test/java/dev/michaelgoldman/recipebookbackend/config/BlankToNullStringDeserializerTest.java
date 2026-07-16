package dev.michaelgoldman.recipebookbackend.config;

import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JsonTest
@Import(JacksonConfig.class)
public class BlankToNullStringDeserializerTest {
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void whenLeadingAndTrailingWhitespace_shouldTrim() {
        RecipeRequest request = objectMapper.readValue(
                "{\"name\":\"  Pasta  \"}", RecipeRequest.class);
        assertThat(request.getName()).isEqualTo("Pasta");
    }

    @Test
    void whenBlank_shouldBecomeNull() {
        RecipeRequest request = objectMapper.readValue(
                "{\"name\":\"    \"}", RecipeRequest.class);
        assertThat(request.getName()).isNull();
    }
}
