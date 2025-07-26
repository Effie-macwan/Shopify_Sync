package com.example.ShopifyLearn.models.dto.shopify.bulk;


import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Array;

public record ProductParseBulk(
        @SerializedName("id")
        String shopifyProductId,
        String title,
        String description,
        String descriptionHtml,
        String status,
        String vendor,
        String createdAt,
        String updatedAt
) {
}
