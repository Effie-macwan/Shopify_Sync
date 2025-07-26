package com.example.ShopifyLearn.models.dto.shopify;

import lombok.Data;

import java.util.List;

@Data
public class GraphqlResponseRoot<T> {

    private T data;

    private List<GraphqlErrorRoot> errors;

}
