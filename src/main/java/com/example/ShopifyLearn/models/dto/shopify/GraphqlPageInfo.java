package com.example.ShopifyLearn.models.dto.shopify;

import lombok.Data;

@Data
public class GraphqlPageInfo {
    private String endCursor;
    private boolean hasNextPage;
}
