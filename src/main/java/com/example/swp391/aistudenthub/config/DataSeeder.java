package com.example.swp391.aistudenthub.config;

import com.example.swp391.aistudenthub.feature.auth.entity.Role;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking database for initial data seeding...");
        
        // Seed Admin User
        if (userRepository.findByEmailAndDeletedAtIsNull("admin@aistudyhub.com").isEmpty()) {
            User admin = User.builder()
                    .email("admin@aistudyhub.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .role(Role.ADMIN)
                    .active(true)
                    .emailVerified(true)
                    .build();
            userRepository.save(admin);
            log.info("Seeded Admin user: admin@aistudyhub.com / admin123");
        }

        // Seed Normal User
        if (userRepository.findByEmailAndDeletedAtIsNull("user@aistudyhub.com").isEmpty()) {
            User normalUser = User.builder()
                    .email("user@aistudyhub.com")
                    .password(passwordEncoder.encode("user123"))
                    .fullName("Test User")
                    .role(Role.USER)
                    .active(true)
                    .emailVerified(true)
                    .build();
            userRepository.save(normalUser);
            log.info("Seeded Normal user: user@aistudyhub.com / user123");
        }
        
        log.info("Database seeding completed.");
    }
}
