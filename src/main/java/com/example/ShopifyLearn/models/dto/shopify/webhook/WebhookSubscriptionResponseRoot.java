package com.example.ShopifyLearn.models.dto.shopify.webhook;

import com.example.ShopifyLearn.models.dto.shopify.GraphqlEdgeContainer;
import lombok.Data;

@Data
public class WebhookSubscriptionResponseRoot {

    private GraphqlEdgeContainer<WebhookSubscription> webhookSubscriptions;

    private WebhookSubscription webhookSubscription;

    private String deletedWebhookSubscriptionId;

}
