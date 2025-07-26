package com.example.ShopifyLearn.service.impl;

import com.example.ShopifyLearn.config.ShopifyProperties;
import com.example.ShopifyLearn.config.graphql.ShopifyGraphqlConfig;
import com.example.ShopifyLearn.exception.ShopifyParsingException;
import com.example.ShopifyLearn.graphql.GraphqlClient;
import com.example.ShopifyLearn.graphql.util.GraphqlSchemaReader;
import com.example.ShopifyLearn.models.dto.shopify.*;
import com.example.ShopifyLearn.models.dto.shopify.webhook.GraphqlWebhookRoot;
import com.example.ShopifyLearn.models.dto.shopify.webhook.WebhookSubscription;
import com.example.ShopifyLearn.models.dto.shopify.webhook.WebhookSubscriptionResponseRoot;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ShopifyWebhookHelper {

    private Gson gson = new Gson();

    public static final String ORDERS_CREATE = "ORDERS_CREATE";
    public static final String ORDERS_UPDATED = "ORDERS_UPDATED";
    public static final String ORDERS_DELETE = "ORDERS_DELETE";

    public static final String CUSTOMERS_CREATE = "CUSTOMERS_CREATE";
    public static final String CUSTOMERS_UPDATE = "CUSTOMERS_UPDATE";
    public static final String CUSTOMERS_DELETE = "CUSTOMERS_DELETE";

    public static final String PRODUCTS_CREATE = "PRODUCTS_CREATE";
    public static final String PRODUCTS_UPDATE = "PRODUCTS_UPDATE";
    public static final String PRODUCTS_DELETE = "PRODUCTS_DELETE";

    public static final String BULK_OPERATIONS_FINISH = "BULK_OPERATIONS_FINISH";

    public static final Map<String, String> TOPIC_MAP = new HashMap<>();

    static {
        TOPIC_MAP.put("orders/create", ORDERS_CREATE);
        TOPIC_MAP.put("orders/updated", ORDERS_UPDATED);
        TOPIC_MAP.put("orders/delete", ORDERS_DELETE);

        TOPIC_MAP.put("customers/create", CUSTOMERS_CREATE);
        TOPIC_MAP.put("customers/update", CUSTOMERS_UPDATE);
        TOPIC_MAP.put("customers/delete", CUSTOMERS_DELETE);

        TOPIC_MAP.put("products/create", PRODUCTS_CREATE);
        TOPIC_MAP.put("products/update", PRODUCTS_UPDATE);
        TOPIC_MAP.put("products/delete", PRODUCTS_DELETE);

        TOPIC_MAP.put("bulk_operations/finish",BULK_OPERATIONS_FINISH);
    }

    public void createSubscription(String newTopic, ShopifyProperties shopifyProperties, ShopifyGraphqlConfig config) throws IOException {
//        String topic = TOPIC_MAP.get(newTopic);
        if (newTopic != null) {
            createWebhook(newTopic, shopifyProperties, config);
        } else {
            log.error("Unknown topic: {}", newTopic);
            throw new ShopifyParsingException("Unknown topic");
        }
    }

    public void createWebhook(String topic, ShopifyProperties shopifyProperties, ShopifyGraphqlConfig config) throws IOException {
        //SUBSCRIBE A NEW TOPIC

        GraphqlRequest request = new GraphqlRequest();
        String query = GraphqlSchemaReader.fetchQueryFromFile(GraphqlSchemaReader.CREATE_WEBHOOK);
        request.setQuery(query);

        Map<String, Object> variables = new HashMap<>();
        variables.put("topic", topic);
        Map<String, String> webhookSubscription = new HashMap<>();
        webhookSubscription.put("callbackUrl", shopifyProperties.getWebhookUrl());
        webhookSubscription.put("format", "JSON");
        variables.put("webhookSubscription", webhookSubscription);

//        JsonObject webhookJson = new JsonObject();
//        webhookJson.addProperty("format", "JSON");
//        webhookJson.addProperty("callbackUrl", shopifyProperties.getCallbackUrl());
//
//        Map<String, Object> webhookSubscriptionMap = new Gson().fromJson(webhookJson, Map.class);
        request.setVariables(variables);

        GraphqlClient graphqlClient = new GraphqlClient();
        String response = graphqlClient.execute(request, config);

        TypeToken<GraphqlResponseRoot<GraphqlWebhookRoot>> typeToken = new TypeToken<>() {
        };
        GraphqlResponseRoot<GraphqlWebhookRoot> graphqlResponseRoot = gson.fromJson(response, typeToken);

        if (graphqlResponseRoot.getData().getWebhookSubscriptionCreate() == null && graphqlResponseRoot.getData().getUserErrors() != null) {
            log.error("Bad response: {}", response);
            throw new RuntimeException("Failed to create webhook subscription: " + graphqlResponseRoot.getData().getUserErrors().getMessage());
        } else {
            log.info("Webhook created: {}", graphqlResponseRoot.getData().getWebhookSubscriptionCreate().getWebhookSubscription().getId());
        }
    }

    public void updateWebhook(WebhookSubscription webhookSubscription, ShopifyGraphqlConfig config, String callbackUrl) throws IOException {
        GraphqlRequest request = new GraphqlRequest();
        String query = GraphqlSchemaReader.fetchQueryFromFile(GraphqlSchemaReader.UPDATE_WEBHOOK);
        request.setQuery(query);

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", webhookSubscription.getId());

        JsonObject webhookJson = new JsonObject();
        webhookJson.addProperty("format", "JSON");
        webhookJson.addProperty("callbackUrl", callbackUrl);

        Map<String, Object> webhookSubscriptionMap = new Gson().fromJson(webhookJson, Map.class);

        variables.put("webhookSubscription", webhookSubscriptionMap);
        request.setVariables(variables);

        GraphqlClient graphqlClient = new GraphqlClient();
        String response = graphqlClient.execute(request, config);

        TypeToken<GraphqlResponseRoot<GraphqlWebhookRoot>> typeToken = new TypeToken<>() {
        };
        GraphqlResponseRoot<GraphqlWebhookRoot> graphqlResponseRoot = gson.fromJson(response, typeToken);

        if (graphqlResponseRoot.getData().getWebhookSubscriptionUpdate().getWebhookSubscription() == null && graphqlResponseRoot.getData().getUserErrors() != null) {
            throw new RuntimeException("Failed to create webhook subscription. Error Message: " + graphqlResponseRoot.getData().getUserErrors().getMessage());
        } else {
            log.info("Webhook updated: {}", graphqlResponseRoot.getData().getWebhookSubscriptionUpdate().getWebhookSubscription().getEndpoint().getCallbackUrl());
        }

    }

    public void deleteWebhook(WebhookSubscription webhookSubscription, ShopifyGraphqlConfig config) throws IOException {
        GraphqlRequest request = new GraphqlRequest();
        String query = GraphqlSchemaReader.fetchQueryFromFile(GraphqlSchemaReader.DELETE_WEBHOOK);
        request.setQuery(query);
        Map<String, String> variable = new HashMap<>();
        variable.put("id", webhookSubscription.getId());
        request.setVariables(variable);

        GraphqlClient graphqlClient = new GraphqlClient();
        String response = graphqlClient.execute(request, config);

        TypeToken<GraphqlResponseRoot<GraphqlWebhookRoot>> typeToken = new TypeToken<>() {
        };
        GraphqlResponseRoot<GraphqlWebhookRoot> graphqlResponseRoot = gson.fromJson(response, typeToken);

        if (graphqlResponseRoot.getData().getWebhookSubscriptionDelete() == null && graphqlResponseRoot.getData().getUserErrors() != null) {
            throw new RuntimeException("Failed to create webhook subscription. Error Message: " + graphqlResponseRoot.getData().getUserErrors().getMessage());
        } else {
            log.info("Webhook with id : {} deleted", graphqlResponseRoot.getData().getWebhookSubscriptionDelete().getDeletedWebhookSubscriptionId());
        }

    }

    public List<WebhookSubscription> getExistingWebhooks(String accessToken, String shopDomain) {
        try {
            List<WebhookSubscription> result = new ArrayList<>();

            ShopifyGraphqlConfig config = new ShopifyGraphqlConfig();
            config.setAccessToken(accessToken);
            config.setShopDomain(shopDomain);
            String query = GraphqlSchemaReader.fetchQueryFromFile(GraphqlSchemaReader.FETCH_WEBHOOKS);
            GraphqlRequest request = new GraphqlRequest();
            request.setQuery(query);
            log.info("Sending request to fetch all subscribed webhooks");
            GraphqlClient client = new GraphqlClient();
            String response = client.execute(request, config);


            GraphqlResponseRoot<WebhookSubscriptionResponseRoot> graphqlResponseRoot;
            TypeToken<GraphqlResponseRoot<WebhookSubscriptionResponseRoot>> typeToken = new TypeToken<>() {
            };
            graphqlResponseRoot = gson.fromJson(response, typeToken);

            if (graphqlResponseRoot.getData().getWebhookSubscriptions().getEdges() != null) {
                List<GraphqlNode<WebhookSubscription>> webhookSubscriptionList = graphqlResponseRoot.getData().getWebhookSubscriptions().getEdges();
                for (GraphqlNode<WebhookSubscription> subscriptionGraphqlNode : webhookSubscriptionList) {
                    WebhookSubscription subscription = subscriptionGraphqlNode.getNode();
                    result.add(subscription);
                }
                return result;
            } else {
                throw new ShopifyParsingException(response);
            }
        } catch (ShopifyParsingException e) {
            log.error("Response: {}", e.getMessage());
            throw new ShopifyParsingException("Null response object while parsing webhook");
        } catch (IOException e) {
            log.error("Error fetching existing webhooks for shop {}: {}", shopDomain, e.getMessage());
            throw new RuntimeException("Failed to fetch webhooks", e);
        }

    }


}
