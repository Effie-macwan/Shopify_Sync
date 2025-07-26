package com.example.ShopifyLearn.models.dto.shopify;

import lombok.Data;

@Data
public class GraphqlNode<T> {

    private T node;

}