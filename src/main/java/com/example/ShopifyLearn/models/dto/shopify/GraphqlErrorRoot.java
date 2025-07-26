package com.example.ShopifyLearn.models.dto.shopify;

import lombok.Data;

@Data
public class GraphqlErrorRoot {
    private String message;
    private Extensions extensions;
}
