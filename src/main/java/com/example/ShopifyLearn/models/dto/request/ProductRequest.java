package com.example.ShopifyLearn.models.dto.request;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record ProductRequest(
        @SerializedName("admin_graphql_api_id")
        @NotNull(message = "Product Id is required")
        String shopifyId,
        @NotNull(message = "Product Title is required")
        String title,
        String description,
        String descriptionHtml,
        @NotNull(message = "Product status is required")
        String status,
        @NotNull(message = "Vendor is required")
        String vendor,
        String tags,
        String url,
        String categoryName,
        List<VariantRequest> variants,
        String createdAt,
        String updatedAt
) {
}
