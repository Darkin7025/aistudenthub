package com.example.swp391.aistudenthub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${thread-pool.core-size:2}")
    private int corePoolSize;

    @Value("${thread-pool.max-size:4}")
    private int maxPoolSize;

    @Value("${thread-pool.queue-capacity:200}")
    private int queueCapacity;

    @Bean(name = "documentTaskExecutor")
    public Executor documentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Cấu hình tăng luồng xử lý đồng thời để tận dụng tài nguyên sau khi nâng cấp cấu hình Render
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("DocExtract-");
        executor.initialize();
        return executor;
    }
}
