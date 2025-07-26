package com.example.ShopifyLearn.models.dto.shopify;

import lombok.Data;

import java.util.List;

@Data
public class GraphqlEdgeContainer<T> {

    private List<GraphqlNode<T>> edges;

    private GraphqlPageInfo pageInfo;

}
