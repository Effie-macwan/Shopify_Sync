package com.example.ShopifyLearn.models.dto.request;

import com.google.gson.annotations.SerializedName;

public record CustomerAddressRequest(
        String id,
        @SerializedName("customer_id")
        String customerId,
        String address1,
        String address2,
        String city,
        String zip,
        String country,
        String province,
        String phone,
        @SerializedName("default")
        boolean isDefault
) {
}
