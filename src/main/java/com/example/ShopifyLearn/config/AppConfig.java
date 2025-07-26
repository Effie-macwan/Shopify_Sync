package com.example.ShopifyLearn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class AppConfig {

//    private final DataSource dataSource;
//
//    public AppConfig(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
