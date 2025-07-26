package com.example.ShopifyLearn.models.dto.shopify.bulk;

import com.example.ShopifyLearn.models.dto.shopify.GraphqlUserErrors;
import lombok.Data;

import java.util.List;

@Data
public class BulkOperationRunQuery {

    private BulkOperation bulkOperation;
    private List<GraphqlUserErrors> userErrors;

}
