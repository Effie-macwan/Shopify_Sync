package com.example.ShopifyLearn.config.graphql;

import lombok.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class ShopifyGraphqlConfig {

    private String shopDomain;

    private String accessToken;

    @Builder.Default
    private String version = "2025-01";

}
