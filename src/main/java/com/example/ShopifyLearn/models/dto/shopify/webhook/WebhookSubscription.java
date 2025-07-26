package com.example.ShopifyLearn.models.dto.shopify.webhook;

import lombok.Data;

@Data
public class WebhookSubscription {

    private String id;
    private String topic;
    private String format;
    private GraphqlEndpoint endpoint;
}
