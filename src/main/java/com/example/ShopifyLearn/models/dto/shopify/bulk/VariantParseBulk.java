package com.example.ShopifyLearn.models.dto.shopify.bulk;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantParseBulk {
        @SerializedName("id")
        @NotNull
        String shopifyVariantId;
        String title;
        String sku;
        BigDecimal price;
        BigDecimal compareAtPrice;
        String createdAt;
        String updatedAt;
        String rawSelectedOptions;
        @NotNull(message = "Parent id must not be null in variant")
        @SerializedName("__parentId")
        String parentId;
        String imageUrl;
        String inventoryItemId;
}
