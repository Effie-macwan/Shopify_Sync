package com.example.ShopifyLearn.models.dto.response;

import java.util.List;

public record CustomerResponse(String shopifyId, Long storeId, String firstName, String lastname, List<CustomerAddressResponse> customerAddressList) {
}
