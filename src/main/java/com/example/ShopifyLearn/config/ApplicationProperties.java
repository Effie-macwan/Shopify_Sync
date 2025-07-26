package com.example.ShopifyLearn.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("app")
@Getter
@Setter
public class ApplicationProperties {

    private String url;

    private String webhookUrl;

}
