package com.example.ShopifyLearn.service.impl.sync;

import com.example.ShopifyLearn.models.dto.shopify.bulk.ProductParseBulk;
import com.example.ShopifyLearn.models.dto.shopify.bulk.VariantParseBulk;
import com.example.ShopifyLearn.models.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
public class ProductSyncService {

    public final String saveProductQuery = "INSERT INTO product ( " +
            "    shopify_product_id, " +
            "    store_id, " +
            "    title, " +
            "    description, " +
            "    description_html, " +
            "    status, " +
            "    tags, " +
            "    vendor, " +
            "    image_url, " +
            "    category_name, " +
            "    created_at, " +
            "    updated_at " +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "store_id         = VALUES(store_id), " +
            "title            = VALUES(title), " +
            "description      = VALUES(description), " +
            "description_html = VALUES(description_html), " +
            "status           = VALUES(status), " +
            "tags           = VALUES(tags), " +
            "vendor           = VALUES(vendor), " +
            "image_url        = VALUES(image_url), " +
            "category_name    = VALUES(category_name), " +
            "updated_at       = VALUES(updated_at)";

    public final String saveVariantQuery = "INSERT INTO variant ( " +
            "    shopify_variant_id, " +
            "    store_id, " +
            "    title, " +
            "    sku, " +
            "    price, " +
            "    compare_at_price, " +
            "    selected_options, " +
            "    image_url, " +
            "    inventory_item_id, " +
            "    product_id, " +
            "    created_at, " +
            "    updated_at " +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "    store_id           = VALUES(store_id), " +
            "    title              = VALUES(title), " +
            "    sku                = VALUES(sku), " +
            "    price              = VALUES(price), " +
            "    compare_at_price   = VALUES(compare_at_price), " +
            "    selected_options   = VALUES(selected_options), " +
            "    image_url          = VALUES(image_url), " +
            "    inventory_item_id  = VALUES(inventory_item_id), " +
            "    product_id          = VALUES(product_id), " +
            "    updated_at         = VALUES(updated_at)";



    public void setProductParameters(PreparedStatement ps, ProductParseBulk productParseBulk, Product product, String url, String name, String cleanedTags) throws SQLException {
        ps.setString(1,productParseBulk.shopifyProductId());
        ps.setLong(2,product.getStoreId());
        ps.setString(3, productParseBulk.title());
        ps.setString(4, productParseBulk.description());
        ps.setString(5, productParseBulk.descriptionHtml());
        ps.setString(6, productParseBulk.status());
        ps.setString(7, cleanedTags);
        ps.setString(8, productParseBulk.vendor());
        ps.setString(9, url);
        ps.setString(10, name);
        ps.setTimestamp(11, convertToTimestamp(productParseBulk.createdAt()));
        ps.setTimestamp(12, convertToTimestamp(productParseBulk.updatedAt()));

    }

    public void setVariantParameters(PreparedStatement ps, VariantParseBulk variantParseBulk, Long productId, Long storeId) throws SQLException {
        ps.setString(1, variantParseBulk.getShopifyVariantId().trim());
        ps.setLong(2, storeId);
        ps.setString(3, variantParseBulk.getTitle());
        ps.setString(4, variantParseBulk.getSku());
        ps.setBigDecimal(5, variantParseBulk.getPrice());
        ps.setBigDecimal(6, variantParseBulk.getCompareAtPrice());
        ps.setString(7, variantParseBulk.getRawSelectedOptions());
        ps.setString(8, variantParseBulk.getImageUrl());
        ps.setString(9, variantParseBulk.getInventoryItemId());
        ps.setLong(10, productId);

        Timestamp createdAt = convertToTimestamp(variantParseBulk.getCreatedAt());
        Timestamp updatedAt = convertToTimestamp(variantParseBulk.getUpdatedAt());
        ps.setTimestamp(11, createdAt);
        ps.setTimestamp(12, updatedAt);
    }

    private Timestamp convertToTimestamp(String time) {
        if (time == null || time.trim().isEmpty()) {
            return null;
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(time);
            return Timestamp.from(odt.toInstant());
        } catch (DateTimeParseException e) {
            log.error("Error parsing date: {} - {}", time, e.getMessage());
            return null;
        }
    }

}
