package com.example.ShopifyLearn.models.dto.shopify;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GraphqlRequest {

    private String query;

    private Object variables;

}
