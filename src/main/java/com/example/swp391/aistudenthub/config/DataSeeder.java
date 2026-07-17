package com.example.swp391.aistudenthub.config;

import com.example.swp391.aistudenthub.feature.admin.entity.SystemConfig;
import com.example.swp391.aistudenthub.feature.admin.entity.LogLevel;
import com.example.swp391.aistudenthub.feature.admin.entity.SystemLog;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemConfigRepository;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemLogRepository;
import com.example.swp391.aistudenthub.feature.auth.entity.Role;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatMessage;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatSession;
import com.example.swp391.aistudenthub.feature.chat.enums.MessageSender;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatMessageRepository;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigRepository systemConfigRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SystemLogRepository systemLogRepository;

    @Value("${app.seed.demo-data:true}")
    private boolean demoDataEnabled;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking database for initial data seeding...");
        
        User admin = seedUser("admin@aistudyhub.com", "admin123", "System Admin", Role.ADMIN);
        User normalUser = seedUser("user@aistudyhub.com", "user123", "Test User", Role.USER);
        
        // Seed default system configs (feature flags)
        seedConfig("feature.ai_chat.enabled",   "true",  "Bật/tắt tính năng AI chat với tài liệu");
        seedConfig("feature.upload.enabled",     "true",  "Bật/tắt tính năng upload tài liệu");
        seedConfig("feature.public_docs.enabled","true",  "Bật/tắt tính năng xem tài liệu công khai");
        seedConfig("system.max_file_size_mb",    "10",    "Kích thước file tối đa cho phép upload (MB)");
        seedConfig("system.ai_model",            "gemini-2.5-flash", "Model AI đang sử dụng");

        if (demoDataEnabled) {
            seedDemoData(admin, normalUser);
        } else {
            log.info("Demo API data seeding is disabled. Set SEED_DEMO_DATA=true to enable it.");
        }

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

    private User seedUser(String email, String password, String fullName, Role role) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseGet(() -> {
                    User user = User.builder()
                            .email(email)
                            .password(passwordEncoder.encode(password))
                            .fullName(fullName)
                            .role(role)
                            .active(true)
                            .emailVerified(true)
                            .build();
                    User saved = userRepository.save(user);
                    log.info("Seeded {} user: {}", role, email);
                    return saved;
                });
    }

    private void seedDemoData(User admin, User normalUser) {
        User alice = seedUser("alice.demo@aistudyhub.com", "demo123", "Alice Demo", Role.USER);
        User minh = seedUser("minh.demo@aistudyhub.com", "demo123", "Minh Demo", Role.USER);

        seedChatSession(normalUser, "Java Collections review", List.of(
                new SeedMessage(MessageSender.USER, "Explain the difference between List and Set."),
                new SeedMessage(MessageSender.AI, "List preserves order and can contain duplicates; Set prevents duplicates."),
                new SeedMessage(MessageSender.USER, "Please give a short example."),
                new SeedMessage(MessageSender.AI, "Use ArrayList for indexed access and HashSet when you need unique values.")
        ));
        seedChatSession(normalUser, "Moderation review demo", List.of(
                new SeedMessage(MessageSender.USER, "This is a demo conversation for testing moderation."),
                new SeedMessage(MessageSender.AI, "An administrator can inspect and remove this session with a reason.")
        ));
        seedChatSession(alice, "Semester study plan", List.of(
                new SeedMessage(MessageSender.USER, "Help me make a four-week exam study plan."),
                new SeedMessage(MessageSender.AI, "Break goals into weekly milestones and reserve one day per week for review.")
        ));
        seedChatSession(minh, "Algorithm document question", List.of(
                new SeedMessage(MessageSender.USER, "What is the average complexity of HashMap lookup?"),
                new SeedMessage(MessageSender.AI, "get and put are typically O(1) on average, subject to hash distribution.")
        ));

        seedSystemLog(SystemLog.builder()
                .level(LogLevel.INFO)
                .action("DEMO_SYSTEM_CONFIG_UPDATED")
                .message("Demo system configuration update")
                .source("DataSeeder")
                .actorUserId(admin.getId())
                .actorEmail(admin.getEmail())
                .targetType("SYSTEM_CONFIG")
                .targetId("feature.ai_chat.enabled")
                .build());
        seedSystemLog(SystemLog.builder()
                .level(LogLevel.WARN)
                .action("DEMO_MODERATION_REVIEW")
                .message("A demo chat session was marked for moderation review")
                .source("DataSeeder")
                .actorUserId(admin.getId())
                .actorEmail(admin.getEmail())
                .targetType("CHAT_SESSION")
                .targetId("demo-session")
                .build());
        seedSystemLog(SystemLog.builder()
                .level(LogLevel.ERROR)
                .action("DEMO_UNHANDLED_ERROR")
                .message("Demo error for log filtering and detail view")
                .stackTrace("java.lang.IllegalStateException: Demo failure\n\tat demo.DataSeeder.seedDemoData(DataSeeder.java:1)")
                .source("DataSeeder")
                .requestMethod("POST")
                .requestPath("/api/v1/chat")
                .httpStatus(500)
                .clientIp("127.0.0.1")
                .actorUserId(alice.getId())
                .actorEmail(alice.getEmail())
                .build());
        log.info("Seeded demo data for admin chat and system log APIs.");
    }

    private void seedChatSession(User user, String title, List<SeedMessage> messages) {
        if (chatSessionRepository.existsByUserIdAndTitle(user.getId(), title)) {
            return;
        }
        ChatSession session = chatSessionRepository.save(ChatSession.builder()
                .userId(user.getId())
                .title(title)
                .build());
        List<ChatMessage> chatMessages = messages.stream()
                .map(message -> ChatMessage.builder()
                        .session(session)
                        .sender(message.sender())
                        .message(message.content())
                        .build())
                .toList();
        chatMessageRepository.saveAll(chatMessages);
        log.info("Seeded demo chat session '{}' for {}", title, user.getEmail());
    }

    private void seedSystemLog(SystemLog systemLog) {
        if (!systemLogRepository.existsByAction(systemLog.getAction())) {
            systemLogRepository.save(systemLog);
        }
    }

    private record SeedMessage(MessageSender sender, String content) {
    }
}
