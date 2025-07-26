package com.example.ShopifyLearn.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties("shopify.app")
@Getter
@Setter
public class ShopifyProperties {

    private String accessKey;

    private String secretKey;

    private String callbackUrl;

    private String scope;

    private String webhookUrl;

    private List<String> webhookTopics;

}
