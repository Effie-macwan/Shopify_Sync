package com.example.ShopifyLearn.models.dto.shopify.bulk;

public record CustomerAddressDto(
        String id,
        String customerId,
        String address1,
        String address2,
        String city,
        String country,
        String zip,
        String province,
        String phone
) {
}
