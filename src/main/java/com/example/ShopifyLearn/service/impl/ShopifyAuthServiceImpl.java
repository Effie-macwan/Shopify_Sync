package com.example.ShopifyLearn.service.impl;

import com.example.ShopifyLearn.config.ApplicationProperties;
import com.example.ShopifyLearn.config.graphql.ShopifyGraphqlConfig;
import com.example.ShopifyLearn.config.ShopifyProperties;
import com.example.ShopifyLearn.exception.ShopifyAuthException;
import com.example.ShopifyLearn.exception.ShopifyParsingException;
import com.example.ShopifyLearn.graphql.GraphqlClient;
import com.example.ShopifyLearn.graphql.util.GraphqlSchemaReader;
import com.example.ShopifyLearn.models.dto.request.*;
import com.example.ShopifyLearn.models.dto.shopify.*;
import com.example.ShopifyLearn.models.dto.shopify.bulk.BulkOperationFinish;
import com.example.ShopifyLearn.models.dto.shopify.shop.GraphqlShopRoot;
import com.example.ShopifyLearn.models.dto.shopify.shop.ShopifyShop;
import com.example.ShopifyLearn.models.dto.shopify.webhook.WebhookSubscription;
import com.example.ShopifyLearn.models.entity.Store;
import com.example.ShopifyLearn.models.entity.SyncLog;
import com.example.ShopifyLearn.models.helper.ShopifyToken;
import com.example.ShopifyLearn.models.dto.response.AuthShopInstallResponse;
import com.example.ShopifyLearn.repository.StoreRepo;
import com.example.ShopifyLearn.repository.SyncLogRepo;
import com.example.ShopifyLearn.repository.UsersRepo;
import com.example.ShopifyLearn.service.CustomerService;
import com.example.ShopifyLearn.service.ProductService;
import com.example.ShopifyLearn.service.ShopifyAuthService;
import com.example.ShopifyLearn.service.impl.sync.BulkOperationHelper;
import com.example.ShopifyLearn.service.impl.sync.SyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.ShopifyLearn.service.impl.ShopifyWebhookHelper.*;

@Slf4j
@Service
public class ShopifyAuthServiceImpl implements ShopifyAuthService {
    @Autowired
    private ShopifyProperties shopifyProperties;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private StoreRepo storeRepo;

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private SyncLogRepo syncLogRepo;

    @Autowired
    private BulkOperationHelper bulkOperationHelper;

    @Autowired
    private ShopifyWebhookHelper shopifyWebhookHelper;

    @Autowired
    private SyncService syncService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductService productService;

    Gson gson = new Gson();
//    private final String stateToken = "shopify@123";

    public AuthShopInstallResponse shopInstall(AuthShopRequest authShopRequest) {
        AuthShopInstallResponse authShopInstallResponse = new AuthShopInstallResponse();

        String shop = authShopRequest.getShop();
        String redirectUrl = getRedirectUrl(shop);
        authShopInstallResponse.setRedirectUrl(redirectUrl);

        return authShopInstallResponse;
    }

    public AuthShopInstallResponse getToken(CallbackTokenRequest tokenRequest, HttpServletRequest request, HttpServletResponse response) {
        AuthShopInstallResponse authShopInstallResponse = new AuthShopInstallResponse();

        //prevents CSRF
        if (tokenRequest.getState() == null) {
            throw new ShopifyAuthException("State parameter is missing in callback request");
        }
        String receivedState = tokenRequest.getState();
        String state = hashedToken(tokenRequest.getShop());
        if (!state.equals(receivedState)) {
            throw new ShopifyAuthException("Invalid State Parameter");
        }

        //GET ACCESS TOKEN
        Map<String, String[]> queryMap = request.getParameterMap();
        Map<String, String[]> params = new HashMap<>(queryMap);
        String receivedHmac = params.get("hmac")[0];
        ShopifyToken shopifyToken = new ShopifyToken();
        String accessToken;
        //hmac verify
        boolean isVerified = verifyHmac(tokenRequest.getHmac(), params);
        if (isVerified) {
            accessToken = getAccessToken(shopifyProperties.getAccessKey(), shopifyProperties.getSecretKey(), tokenRequest, response, shopifyToken);
        } else {
            throw new ShopifyAuthException("Invalid Hmac Parameter");
        }

        //FETCH STORE
        Store store = saveStoreDetails(accessToken, tokenRequest.getShopDomain());

        //SAVE user as admin
//        Users user = saveUser(shopifyToken, store);

        //subscribe to webhook
        subscribeWebhook(store.getAccessToken(), store.getShopifyDomain());

        syncService.startSync(store);

        //get redirect url
        String url = applicationProperties.getUrl() + "/main/homePage?shopName=" + store.getName() + "&domain=" + store.getShopifyDomain();
        authShopInstallResponse.setShop(store.getName());
        authShopInstallResponse.setUsername(store.getEmail());
        authShopInstallResponse.setRedirectUrl(url);

        return authShopInstallResponse;
    }

    public void handleWebhook(String payload, HttpServletRequest request) {

        String hmac = request.getHeader("X-Shopify-Hmac-Sha256");
        if (verifyWebhookHmac(payload, hmac)) {
            String shopDomain = request.getHeader("X-Shopify-Shop-Domain");
            if (shopDomain == null) {
                throw new RuntimeException("Shop domain not present");
            }
            Optional<Store> oStore = storeRepo.findByShopifyDomain(shopDomain);
            if (!oStore.isPresent()) {
                log.error("No store found for domain: {}", shopDomain);
                return;
            } else {
                Store store = oStore.get();
                String requestTopic = request.getHeader("X-Shopify-Topic");
                log.info("Handling webhook event for topic: {}", requestTopic);
                String topic = TOPIC_MAP.get(requestTopic);
                if (topic == null) {
                    log.warn("Unhandled topic: {}", requestTopic);
                    return;
                }
                switch (topic) {
                    case BULK_OPERATIONS_FINISH -> handleBulkOperationFinish(payload, store);
                    case CUSTOMERS_CREATE, CUSTOMERS_UPDATE -> handleCustomer(payload, store);
                    case CUSTOMERS_DELETE -> handleCustomerDelete(payload, store);
                    case PRODUCTS_CREATE, PRODUCTS_UPDATE -> handleProduct(payload, store);
                    case PRODUCTS_DELETE -> handleProductDelete(payload, store);
                    default -> log.warn("No handler defined for topic: {}", topic);
                }
            }
        } else {
            log.error("Invalid webhook hmac");
            throw new ShopifyAuthException("Invalid webhook hmac");
        }

    }

    private void handleBulkOperationFinish(String payload, Store store) {
        BulkOperationFinish bulkOperationFinish = bulkOperationHelper.handleBulkFinish(payload, store);
        if (bulkOperationFinish != null) {
            Optional<SyncLog> oSyncLog = syncLogRepo.findByStoreIdAndBulkOperationId(store.getId(), bulkOperationFinish.getAdminGraphqlApiId());
            if (oSyncLog.isPresent()) {
                SyncLog syncLog = oSyncLog.get();
                syncLog.setShopifyStatus(bulkOperationFinish.getStatus());
                syncLog.setAppStatus(SyncLog.appStatus.STARTED);
                syncLogRepo.save(syncLog);
                bulkOperationHelper.startProcessingBulkOperation(bulkOperationFinish, store, syncLog);
            } else {
                log.error("No bulk operation exists with id {}", bulkOperationFinish.getAdminGraphqlApiId());
            }
        }
    }

    private void handleCustomer(String payload, Store store) {

        CustomerRequest request = gson.fromJson(payload, CustomerRequest.class);
        if (request != null) {
            customerService.upsertCustomer(request, store);
        } else {
            throw new ShopifyParsingException("Error in parsing Customer webhook payload.");
        }

    }

    private void handleCustomerDelete(String payload, Store store) {
        CustomerDeleteRequest request = gson.fromJson(payload, CustomerDeleteRequest.class);
        if (request.getId() != null) {
            customerService.deleteCustomer(request, store);
        } else {
            throw new ShopifyParsingException("Error in parsing Customer request payload for webhook.");
        }
    }

    private void handleProduct(String payload, Store store) {

        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
        if (json == null) {
            throw new ShopifyParsingException("Error in parsing as json object ");
        }

        Map<String, List<String>> options = new HashMap<>();
        Map<String, String> imagesMap = new HashMap<>();

        ProductRequest request = parseProductRequest(json, options, imagesMap);

        if (request != null) {
            productService.upsertProduct(request, options, imagesMap, store);
        } else {
            throw new ShopifyParsingException("Error in parsing Product webhook payload.");
        }

    }

    private ProductRequest parseProductRequest(JsonObject jsonObject, Map<String, List<String>> options, Map<String, String> imagesMap) {

        String description, descriptionHtml, url = null, categoryName = null, tags = null, cleanedTags=null;
        List<VariantRequest> variants = new ArrayList<>();

        descriptionHtml = jsonObject.has("body_html") ? jsonObject.get("body_html").getAsString() : null;

        if (jsonObject.has("image") && jsonObject.get("image").isJsonObject()) {
            JsonObject imageObj = jsonObject.getAsJsonObject("image");
            if (imageObj.has("src") && !imageObj.get("src").isJsonNull()) {
                url = imageObj.get("src").getAsString();
            }
        }

        if (jsonObject.has("category") && jsonObject.get("category").isJsonObject()) {
            JsonObject categoryObj = jsonObject.getAsJsonObject("category");
            if (categoryObj.has("name") && !categoryObj.get("name").isJsonNull()) {
                categoryName = categoryObj.get("name").getAsString();
            }
        }

        if (jsonObject.has("tags") && !jsonObject.get("tags").isJsonNull()) {
            tags = jsonObject.get("tags").getAsString();
            cleanedTags = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.joining(","));
        }

        description = descriptionHtml != null ? Jsoup.parse(descriptionHtml).text() : null;

        //options map
        JsonArray optionsArray = jsonObject.has("options") ? jsonObject.getAsJsonArray("options") : null;
        if(optionsArray != null){
            for (JsonElement e : optionsArray) {
                JsonObject option = e.getAsJsonObject();
                String name = option.has("name") ? option.get("name").getAsString() : null;
                List<String> values = new ArrayList<>();

                JsonArray val = option.has("values") ? option.getAsJsonArray("values") : null;
                for (JsonElement v : val) {
                    String value = v.getAsString();
                    values.add(value);
                }

                options.put(name,values);
            }
        }

        JsonArray variantArray = jsonObject.has("variants") ? jsonObject.getAsJsonArray("variants") : null;
        if (!variantArray.isEmpty()) {
            for (JsonElement v : variantArray) {
                VariantRequest variantRequest = gson.fromJson(v, VariantRequest.class);
                variants.add(variantRequest);
            }
        } else {
            log.warn("No variants present for product {}",jsonObject.get("admin_graphql_api_id").getAsString());
        }

        JsonArray images = jsonObject.has("images") ? jsonObject.getAsJsonArray("images") : null;
        for (JsonElement imageObj : images) {
            JsonObject variantImage = imageObj.getAsJsonObject();
            String imageId = variantImage.has("id") ? variantImage.get("id").getAsString() :null;
            String imageUrl = variantImage.has("src") ? variantImage.get("src").getAsString() :null;

            imagesMap.put(imageId, imageUrl);
        }

        return ProductRequest.builder()
                .shopifyId(jsonObject.has("admin_graphql_api_id") ? jsonObject.get("admin_graphql_api_id").getAsString() : null)
                .title(jsonObject.has("title") ? jsonObject.get("title").getAsString() : null)
                .description(description)
                .descriptionHtml(descriptionHtml)
                .status(jsonObject.has("status") ? jsonObject.get("status").getAsString() : null)
                .vendor(jsonObject.has("vendor") ? jsonObject.get("vendor").getAsString() : null)
                .tags(tags)
                .url(url)
                .categoryName(categoryName)
                .variants(variants)
                .createdAt(jsonObject.has("created_at") ? jsonObject.get("created_at").getAsString() : null)
                .updatedAt(jsonObject.has("updated_at") ? jsonObject.get("updated_at").getAsString() : null)
                .build();

    }

    private void handleProductDelete(String payload, Store store) {
        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

        String id = json.get("id").getAsString();
        String gid = "gid://shopify/Product/"+id;

        productService.deleteProduct(gid,store);

    }

    private String getRedirectUrl(String shop) {
        String receivedShop = shop.substring(0, shop.indexOf(".myshopify.com"));
        return "https://" + shop + "/admin/oauth/authorize?client_id="
                + shopifyProperties.getAccessKey()
                + "&scope=" + shopifyProperties.getScope()
                + "&redirect_uri=" + shopifyProperties.getCallbackUrl()
                + "&state=" + hashedToken(receivedShop);
    }

    private String hashedToken(String input) {
        try {
            //encode
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing algorithm not found", e);
            throw new ShopifyAuthException("Hashing algorithm not found", e);
        }
    }

    private boolean verifyHmac(String hmac, Map<String, String[]> params) {

        String receivedHmac = params.get("hmac")[0];
        params.remove("hmac");

        SortedMap<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            sortedParams.put(entry.getKey(), entry.getValue()[0]); // Only taking first value
        }

        // Create query string (key=value&key=value)
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (data.length() > 0) {
                data.append("&");
            }
            data.append(entry.getKey()).append("=").append(entry.getValue());
        }

        String generatedHmac = generateHmac(shopifyProperties.getSecretKey(), data);
        return generatedHmac.equals(receivedHmac);
    }

    private String generateHmac(String secret, StringBuilder remainingQuery) {
        try {
            // Generate HMAC using SHA-256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(remainingQuery.toString().getBytes());

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new ShopifyAuthException("Hashing algorithm not found", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private String getAccessToken(String clientApi, String secretKey, CallbackTokenRequest tokenRequest, HttpServletResponse response, ShopifyToken shopifyToken) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("client_id", clientApi);
            requestBody.put("client_secret", secretKey);
            requestBody.put("code", tokenRequest.getCode());
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            WebClient webClient = WebClient.create("https://" + tokenRequest.getShopDomain());

            String responseEntity = webClient.post()
                    .uri("/admin/oauth/access_token")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            shopifyToken = objectMapper.readValue(responseEntity, ShopifyToken.class);
            String access_token = shopifyToken.getAccessToken();

            log.info("Access token: {}", access_token);

            return access_token;

        } catch (IOException e) {
            log.error("Error reading response from Shopify API", e);
            throw new ShopifyAuthException("Failed to retrieve access token", e);
        }
    }

    private Store saveStoreDetails(String accessToken, String shopDomain) {

        try {
            ShopifyGraphqlConfig config = new ShopifyGraphqlConfig();
            config.setAccessToken(accessToken);
            config.setShopDomain(shopDomain);
            GraphqlRequest graphqlRequest = new GraphqlRequest();
            String query = GraphqlSchemaReader.fetchQueryFromFile(GraphqlSchemaReader.FETCH_STORE_DETAILS);
            graphqlRequest.setQuery(query);

            GraphqlClient graphqlClient = new GraphqlClient();
            String response = graphqlClient.execute(graphqlRequest, config);

            TypeToken<GraphqlResponseRoot<GraphqlShopRoot>> typeToken = new TypeToken<>() {
            };
            GraphqlResponseRoot<GraphqlShopRoot> graphqlResponseRoot = gson.fromJson(response, typeToken);
            List<GraphqlErrorRoot> errors = graphqlResponseRoot.getErrors();
            if (errors != null) {
                if (errors.get(0).getMessage() != null) {
                    log.error("Error occurred");
                    throw new RuntimeException("Error in query");
                }
            }
            ShopifyShop shop = graphqlResponseRoot.getData().getShop();

            Store store = saveShop(shop, accessToken);

            return store;

        } catch (IOException e) {
            throw new RuntimeException("Error retrieving store details", e);
        }

    }

    private Store saveShop(ShopifyShop shop, String accessToken) {

        String id = shop.getId();
        String sId = id.substring(id.lastIndexOf("/") + 1);
        Optional<Store> oStore = storeRepo.findByShopifyStoreId(sId);
        Store store;
        if (oStore.isPresent()) {
            store = oStore.get();
        } else {
            store = new Store();
        }
        store.setName(shop.getName());
        store.setEmail(shop.getEmail());
        store.setShopOwner(shop.getShopOwnerName());
        store.setShopifyStoreId(sId);
        store.setAccessToken(accessToken);
        store.setIanaTimezone(shop.getIanaTimezone());
        store.setCurrencyCode(shop.getCurrencyCode());
        store.setShopifyDomain(shop.getMyshopifyDomain());
        store.setPlan(shop.getPlan().getDisplayName());
        store.setCreatedAt(shop.getCreatedAt());
        store.setUrl(shop.getUrl());
        store.setWeightUnit(shop.getWeightUnit());
        store.setShipsToCountries(String.join(",", shop.getShipsToCountries()));
        return storeRepo.save(store);
    }

//    private Users saveUser(ShopifyToken shopifyToken, Store store) {
//
//        Optional<Users> optionalUser = usersRepo.findByUsernameAndStoreId(shopifyToken.getAssociatedUser().getFirstName(),store.getShopifyStoreId());
//        Users user;
//        if (optionalUser.isPresent()) {
//            user = optionalUser.get();
//        } else {
//            user = new Users();
//            user.setRole(Roles.ROLE_ADMIN);
//            user.setFirstName(shopifyToken.getAssociatedUser().getFirstName());
//            user.setLastName(shopifyToken.getAssociatedUser().getLastName());
//            user.setEmail(shopifyToken.getAssociatedUser().getEmail());
//            user.setEmailVerified(shopifyToken.getAssociatedUser().getEmailVerified());
//            user.setAdmin(true);
//            user.setLocale(shopifyToken.getAssociatedUser().getLocale());
//            user.setCollaborator(shopifyToken.getAssociatedUser().getCollaborator());
//            usersRepo.save(user);
//        }
//        return user;
//    }

    public void subscribeWebhook(String accessToken, String shopDomain) {
        try {
            ShopifyGraphqlConfig config = new ShopifyGraphqlConfig();
            config.setShopDomain(shopDomain);
            config.setAccessToken(accessToken);

            List<WebhookSubscription> existingSubscriptions = shopifyWebhookHelper.getExistingWebhooks(accessToken, shopDomain);
            Set<String> newTopics = getProperFormat();

            List<String> topicsToRemove = new ArrayList<>();
            for (WebhookSubscription subscription : existingSubscriptions) {
                if (newTopics.contains(subscription.getTopic())) {
                    String callback = subscription.getEndpoint().getCallbackUrl();
                    if (!callback.equals(shopifyProperties.getWebhookUrl()) || subscription.getFormat() == null) {
//                        System.out.println("First: " + !callback.equals(shopifyProperties.getWebhookUrl()));
//                        System.out.println("Second: " + (subscription.getFormat() == null));
                        log.info("Updating webhook with topic: {}", subscription.getTopic());
                        shopifyWebhookHelper.updateWebhook(subscription, config, shopifyProperties.getWebhookUrl());
                    }
                    topicsToRemove.add(subscription.getTopic());
                } else {
                    log.info("Deleting webhook with topic: {}", subscription.getTopic());
                    shopifyWebhookHelper.deleteWebhook(subscription, config);
                }
            }

            newTopics.removeAll(topicsToRemove);
            for (String newTopic : newTopics) {
                shopifyWebhookHelper.createSubscription(newTopic, shopifyProperties, config);
            }

        } catch (IOException e) {
            log.error("Error handling webhooks for shop: {}", shopDomain, e);
            throw new RuntimeException(e);
        }
    }

    private Set<String> getProperFormat() {
        Set<String> mappedNewTopics = new HashSet<>();
        for (String topic : shopifyProperties.getWebhookTopics()) {
            String mappedTopic = ShopifyWebhookHelper.TOPIC_MAP.get(topic);
            if (mappedTopic != null) {
                mappedNewTopics.add(mappedTopic);
            } else {
                log.warn("No mapping found for topic: {}", topic);
            }
        }
        return mappedNewTopics;
    }

    public boolean verifyWebhookHmac(String payload, String receivedHmac) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(shopifyProperties.getSecretKey().getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] calculatedHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedHmac = Base64.getEncoder().encodeToString(calculatedHmac);

            return expectedHmac.equals(receivedHmac);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key in webhook hmac", e);
        }

    }

}
