package com.example.ShopifyLearn.models.dto.response;

public record CustomerAddressResponse(String address1, String address2, String city, String country, int zip, boolean isDefault, String phone) {
}
