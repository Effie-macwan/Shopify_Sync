package com.example.ShopifyLearn.models.dto.shopify.webhook;

import com.example.ShopifyLearn.models.dto.shopify.GraphqlUserErrors;
import lombok.Data;

@Data
public class GraphqlWebhookRoot {

    public WebhookSubscriptionResponseRoot webhookSubscriptionCreate;

    public WebhookSubscriptionResponseRoot webhookSubscriptionUpdate;

    public WebhookSubscriptionResponseRoot webhookSubscriptionDelete;

    public GraphqlUserErrors userErrors;

}
