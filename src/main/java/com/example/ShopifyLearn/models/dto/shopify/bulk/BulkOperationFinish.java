package com.example.ShopifyLearn.models.dto.shopify.bulk;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BulkOperationFinish {

    @SerializedName(value = "admin_graphql_api_id")
    private String adminGraphqlApiId;

    @SerializedName("completed_at")
    private OffsetDateTime completedAt;

    @SerializedName("created_at")
    private OffsetDateTime createdAt;

    @SerializedName("error_code")
    private String errorCode;

    private String status;

    private String type;

}
