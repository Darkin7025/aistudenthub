package com.example.swp391.aistudenthub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "documentTaskExecutor")
    public Executor documentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Giới hạn luồng tối đa là 1 để xử lý tuần tự nhằm bảo vệ bộ nhớ RAM dưới 512MB
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("DocExtract-");
        executor.initialize();
        return executor;
    }
}
