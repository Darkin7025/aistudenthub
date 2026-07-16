package com.example.swp391.aistudenthub.config;

import com.example.swp391.aistudenthub.feature.admin.entity.SystemConfig;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemConfigRepository;
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
    private final SystemConfigRepository systemConfigRepository;

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
        
        // Seed default system configs (feature flags)
        seedConfig("feature.ai_chat.enabled",   "true",  "Bật/tắt tính năng AI chat với tài liệu");
        seedConfig("feature.upload.enabled",     "true",  "Bật/tắt tính năng upload tài liệu");
        seedConfig("feature.public_docs.enabled","true",  "Bật/tắt tính năng xem tài liệu công khai");
        seedConfig("system.max_file_size_mb",    "10",    "Kích thước file tối đa cho phép upload (MB)");
        seedConfig("system.ai_model",            "gemini-2.5-flash", "Model AI đang sử dụng");

        log.info("Database seeding completed.");
    }

    // ---- Helper: Seed một config nếu chưa tồn tại ----
    private void seedConfig(String key, String value, String description) {
        if (systemConfigRepository.findById(key).isEmpty()) {
            systemConfigRepository.save(SystemConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .description(description)
                    .build());
            log.info("Seeded system config: {} = {}", key, value);
        }
    }
}
