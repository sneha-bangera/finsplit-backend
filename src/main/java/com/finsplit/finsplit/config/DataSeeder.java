package com.finsplit.finsplit.config;

import com.finsplit.finsplit.entity.Category;
import com.finsplit.finsplit.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner{

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            List<Category> defaults = List.of(
                    Category.builder().name("Food & Dining").icon("food").build(),
                    Category.builder().name("Transportation").icon("transport").build(),
                    Category.builder().name("Shopping").icon("shopping").build(),
                    Category.builder().name("Entertainment").icon("entertainment").build(),
                    Category.builder().name("Rent & Utilities").icon("home").build(),
                    Category.builder().name("Health").icon("health").build(),
                    Category.builder().name("Travel").icon("travel").build(),
                    Category.builder().name("Education").icon("education").build(),
                    Category.builder().name("Other").icon("other").build()
            );
            categoryRepository.saveAll(defaults);
            System.out.println("✅ Default categories seeded.");
        }
    }
}
