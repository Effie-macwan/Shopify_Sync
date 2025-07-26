package com.example.ShopifyLearn.models.dto.request;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record VariantRequest(
        @SerializedName("admin_graphql_api_id")
        @NotNull(message = "Variant id is required")
        String shopifyId,
        @NotNull(message = "Product Title is required")
        String title,
        String sku,
        @Digits(integer = 10, fraction = 2, message = "Price must be a valid number with up to 10 digits and 2 decimal places")
        @DecimalMin(value = "0.00", inclusive = true, message = "Price must be greater than 0 or equal to 0")
        @NotNull(message = "Price is required")
        BigDecimal price,
        @SerializedName("compare_at_price")
        @DecimalMin(value = "0.0", inclusive = true, message = "Compare-at price must be greater than or equal to 0")
        BigDecimal compareAtPrice,
        @SerializedName("image_id")
        String imageId,
        String option1,
        String option2,
        String option3,
        @SerializedName("inventory_item_id")
        String inventoryItemId,
        @SerializedName("created_at")
        String createdAt,
        @SerializedName("updated_at")
        String updatedAt
) {
}
