package com.example.ShopifyLearn.service.impl.sync;

import com.example.ShopifyLearn.exception.ShopifyException;
import com.example.ShopifyLearn.exception.ShopifyParsingException;
import com.example.ShopifyLearn.models.dto.shopify.bulk.ProductParseBulk;
import com.example.ShopifyLearn.models.dto.shopify.bulk.VariantParseBulk;
import com.example.ShopifyLearn.models.entity.Product;
import com.example.ShopifyLearn.models.entity.Store;
import com.example.ShopifyLearn.models.entity.SyncLog;
import com.example.ShopifyLearn.models.helper.OffsetDateTimeAdapter;
import com.example.ShopifyLearn.repository.SyncLogRepo;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSyncProcessor {

    @Autowired
    private SyncLogRepo syncLogRepo;

    @Autowired
    private ProductSyncService productSyncService;

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

    private final JdbcTemplate jdbcTemplate;

    public ProductSyncProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Async(value = "syncProductAsyncExecutor")
    public void readProductFile(String url, Store store, SyncLog syncLog) {

        syncLog.setAppStatus(SyncLog.appStatus.PROCESSING);
        syncLogRepo.save(syncLog);

        DataSource dataSource = jdbcTemplate.getDataSource();
        long startTime = System.currentTimeMillis();

        final long storeId = store.getId();
        int batchCounter = 0;

        try (Connection con = dataSource.getConnection();
             PreparedStatement productPs = con.prepareStatement(productSyncService.saveProductQuery);
             PreparedStatement variantPs = con.prepareStatement(productSyncService.saveVariantQuery);) {
            try (InputStream inputStream = new URL(url).openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));) {
                int count = 0;
                String line;

                Map<String, List<VariantParseBulk>> parentToVariantMap = new HashMap<>();

                //READ NEXT LINE
                while ((line = reader.readLine()) != null) {
                    count++;

                    if (!line.contains("__parentId")) {
                        //product
                        ProductParseBulk productParseBulk = gson.fromJson(line, ProductParseBulk.class);
                        if (productParseBulk == null) {
                            log.error("Error in parsing Product from JSONL file");
                            throw new ShopifyParsingException("Error in parsing Product from JSONL file");
                        }

                        JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                        String featuredImageUrl = null;
                        if (json.has("featuredMedia") && json.get("featuredMedia").isJsonObject()) {
                            JsonObject preview = json.getAsJsonObject("featuredMedia").getAsJsonObject("preview");
                            if (preview != null && preview.has("image") && preview.get("image").isJsonObject()) {
                                JsonObject image = preview.getAsJsonObject("image");
                                if (image.has("url") && !image.get("url").isJsonNull()) {
                                    featuredImageUrl = image.get("url").getAsString();
                                }
                            }
                        }

                        JsonObject categoryObj = json.has("category") && json.get("category").isJsonObject()
                                ? json.getAsJsonObject("category") : null;
                        String categoryName = categoryObj != null && categoryObj.has("name") && !categoryObj.get("name").isJsonNull()
                                ? categoryObj.get("name").getAsString() : null;

                        JsonArray tagsElement = json.has("tags") ? json.get("tags").getAsJsonArray() : null;
                        String cleanedTags = "";
                        if (tagsElement != null && tagsElement.isJsonArray()) {
                            JsonArray tagsArray = tagsElement.getAsJsonArray();
                            List<String> tags = new ArrayList<>();
                            for (JsonElement tagElement : tagsArray) {
                                tags.add(tagElement.getAsString());
                            }
                            cleanedTags = String.join(",", tags);
                        }

                        Product product = new Product();
                        product.setStoreId(storeId);
                        String prodStatus = productParseBulk.status();
                        Product.ProductStatus status;
                        if (prodStatus.equalsIgnoreCase(Product.ProductStatus.DRAFT.toString())){
                            status = Product.ProductStatus.DRAFT;
                        } else if (prodStatus.equalsIgnoreCase(Product.ProductStatus.ACTIVE.toString())) {
                            status = Product.ProductStatus.ACTIVE;
                        } else if (prodStatus.equalsIgnoreCase(Product.ProductStatus.ARCHIVED.toString())) {
                            status = Product.ProductStatus.ARCHIVED;
                        } else {
                            log.error("Invalid product status");
                            throw new ShopifyException("Invalid product status");
                        }
                        product.setStatus(status);
                        productSyncService.setProductParameters(productPs,productParseBulk,product,featuredImageUrl,categoryName,cleanedTags);
                        productPs.addBatch();
                    }
                    else {
                        //variant
                        VariantParseBulk variantParseBulk = gson.fromJson(line, VariantParseBulk.class);
                        if (variantParseBulk == null) {
                            log.error("Error in parsing Variant from JSONL file");
                            throw new ShopifyParsingException("Error in parsing Variant from JSONL file");
                        }

                        JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                        String imageUrl = null;
                        if (json.has("image") && json.get("image").isJsonObject()) {
                            JsonObject imageObj = json.getAsJsonObject("image");
                            if (imageObj.has("url") && !imageObj.get("url").isJsonNull()) {
                                imageUrl = imageObj.get("url").getAsString();
                            }
                        }

                        String inventoryItemId = null;
                        if (json.has("inventoryItem") && json.get("inventoryItem").isJsonObject()) {
                            JsonObject inventoryObj = json.getAsJsonObject("inventoryItem");
                            if (inventoryObj.has("id") && !inventoryObj.get("id").isJsonNull()) {
                                inventoryItemId = inventoryObj.get("id").getAsString();
                            }
                        }

                        String selectedOptionsJson = null;
                        if (json.has("selectedOptions") && json.get("selectedOptions").isJsonArray()) {
                            JsonArray selectedOptions = json.getAsJsonArray("selectedOptions");
                            selectedOptionsJson = selectedOptions.toString();
                        }

                        variantParseBulk.setImageUrl(imageUrl);
                        variantParseBulk.setInventoryItemId(inventoryItemId);
                        variantParseBulk.setRawSelectedOptions(selectedOptionsJson);
                        List<VariantParseBulk> variants;
                        boolean containsKey = parentToVariantMap.containsKey(variantParseBulk.getParentId());
                        if (containsKey) {
                            variants = parentToVariantMap.get(variantParseBulk.getParentId());
                            variants.add(variantParseBulk);
                        }
                        else {
                            variants = new ArrayList<>();
                            variants.add(variantParseBulk);
                            parentToVariantMap.put(variantParseBulk.getParentId(), variants);
                        }

                    }

                    batchCounter++;
                    if (batchCounter % 100 == 0) {
                        log.info("Executing batch");
                        con.setAutoCommit(false);
                        productPs.executeBatch();
                        con.commit();
                        batchCounter = 0;
                        productPs.clearBatch();
                    }

                }

                //executing remaining products
                if (batchCounter % 500 != 0) {
                    log.info("Executing for remaining records in batch");
                    con.setAutoCommit(false);
                    productPs.executeBatch();
                    con.commit();
                    productPs.clearBatch();
                }

                //GET PRODUCT DETAILS
                Map<String, Long> shopifyToDbIdMap = new HashMap<>();

                List<String> shopifyIds = new ArrayList<>(parentToVariantMap.keySet());
                String sql = String.format(
                        "SELECT id, shopify_product_id FROM product WHERE shopify_product_id IN (%s)",
                        shopifyIds.stream().map(id -> "?").collect(Collectors.joining(","))
                );
                List<Object> params = new ArrayList<>(shopifyIds);
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

                for (Map<String, Object> row : rows) {
                    String shopifyId = (String) row.get("shopify_product_id");
                    Long dbId = ((Number) row.get("id")).longValue();
                    shopifyToDbIdMap.put(shopifyId, dbId);
                }

                for (Map.Entry<String, List<VariantParseBulk>> entry : parentToVariantMap.entrySet()) {
                    String productShopifyId = entry.getKey();
                    Long productDbId = shopifyToDbIdMap.get(productShopifyId);
                    if (productDbId == null) {
                        log.warn("Skipping product with id {} as it is not found.", productDbId);
                        continue;
                    }

                    for (VariantParseBulk variantParse: entry.getValue()) {
                        productSyncService.setVariantParameters(variantPs, variantParse, productDbId, storeId);
                        variantPs.addBatch();
                    }
                }

                long totalVariants = parentToVariantMap.values().stream().flatMap(List::stream).count();
                log.info("Total unique variants to insert: {}", totalVariants);

                Set<String> uniqueIds = new HashSet<>();
                for (List<VariantParseBulk> vlist : parentToVariantMap.values()) {
                    for (VariantParseBulk variant : vlist) {
                        if (!uniqueIds.add(variant.getShopifyVariantId())) {
                            log.warn("DUPLICATE VARIANT ID FOUND: {}", variant.getShopifyVariantId());
                        }
                    }
                }
                log.info("Unique variants to be inserted: {}", uniqueIds.size());

                //save address
                log.info("START SAVING VARIANTS");
                con.setAutoCommit(false);
                variantPs.executeBatch();
                con.commit();
                variantPs.clearBatch();
                log.info("SAVED VARIANTS");


                log.info("Product Sync Completed, time took = {}ms", (System.currentTimeMillis() - startTime));
                syncLog.setAppStatus(SyncLog.appStatus.SUCCESS);
                syncLogRepo.save(syncLog);
            }
        } catch (MalformedURLException e) {
            syncLog.setAppStatus(SyncLog.appStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepo.save(syncLog);
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            log.error("IO error while fetching bulk operation URL: {}", e.getMessage());
            syncLog.setAppStatus(SyncLog.appStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepo.save(syncLog);
            throw new ShopifyException("Error while fetching bulk operation data", e);
        }
        catch (SQLException e) {
            log.error("SQL error while processing bulk operation: {}", e.getMessage());
            syncLog.setAppStatus(SyncLog.appStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepo.save(syncLog);
            throw new ShopifyException("Database error during bulk sync", e);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            syncLog.setAppStatus(SyncLog.appStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepo.save(syncLog);
            throw new ShopifyException("There is something wrong while starting the Product sync process");
        }

    }

}
