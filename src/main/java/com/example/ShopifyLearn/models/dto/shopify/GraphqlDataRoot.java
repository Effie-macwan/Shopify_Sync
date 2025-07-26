package com.example.ShopifyLearn.models.dto.shopify;

import lombok.Data;

@Data
public class GraphqlDataRoot<T> {

    private T shop;

}
