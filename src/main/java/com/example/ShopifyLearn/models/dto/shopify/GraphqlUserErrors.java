package com.example.ShopifyLearn.models.dto.shopify;

import lombok.Data;

@Data
public class GraphqlUserErrors {

    private String code;
    private String field;
    private String message;

}
