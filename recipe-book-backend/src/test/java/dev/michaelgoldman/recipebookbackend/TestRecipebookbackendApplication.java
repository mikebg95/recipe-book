package dev.michaelgoldman.recipebookbackend;

import org.springframework.boot.SpringApplication;

public class TestRecipebookbackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(RecipebookbackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
