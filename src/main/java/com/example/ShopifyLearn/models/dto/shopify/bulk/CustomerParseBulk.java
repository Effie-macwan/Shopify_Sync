package com.example.ShopifyLearn.models.dto.shopify.bulk;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record CustomerParseBulk(
        @SerializedName("id")
        String shopifyCustomerId,
        String firstName,
        String lastName,
        String state,
        int numberOfOrders,
        boolean verifiedEmail,
        boolean taxExempt,
        boolean dataSaleOptOut,
        String[] tags,
        String displayName,
        String email,
        String phone,
        String createdAt,
        String updatedAt,
        @SerializedName("defaultAddress")
        CustomerAddressDto defaultAddress,
        @SerializedName("addresses")
        List<CustomerAddressDto> customerAddressDtoList

) {
        public CustomerParseBulk {
                state = "enabled";
        }
}

