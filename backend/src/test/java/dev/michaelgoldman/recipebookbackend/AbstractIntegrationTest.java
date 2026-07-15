package dev.michaelgoldman.recipebookbackend;

import dev.michaelgoldman.recipebookbackend.config.WebConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, WebConfig.class})
public abstract class AbstractIntegrationTest {
}
