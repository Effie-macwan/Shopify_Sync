package com.example.ShopifyLearn.config.Async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AppAsyncConfig {

    @Bean(name = "syncCustomersAsyncExecutor")
    public Executor syncCustomersAsyncExecutor(){
        ThreadPoolTaskExecutor threadPoolExecutor =new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(2);
        threadPoolExecutor.setMaxPoolSize(5);
        threadPoolExecutor.setThreadNamePrefix("syncCustomerProcess-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }

    @Bean(name = "syncProductAsyncExecutor")
    public Executor syncProductAsyncExecutor(){
        ThreadPoolTaskExecutor threadPoolExecutor =new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(5);
        threadPoolExecutor.setMaxPoolSize(7);
        threadPoolExecutor.setThreadNamePrefix("syncCustomerProcess-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }

    @Bean(name = "syncOrderAsyncExecutor")
    public Executor syncOrderAsyncExecutor(){
        ThreadPoolTaskExecutor threadPoolExecutor =new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(2);
        threadPoolExecutor.setMaxPoolSize(5);
        threadPoolExecutor.setThreadNamePrefix("syncCustomerProcess-");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }

}
