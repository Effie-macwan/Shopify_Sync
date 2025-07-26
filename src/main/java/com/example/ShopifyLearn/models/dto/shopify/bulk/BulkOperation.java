package com.example.ShopifyLearn.models.dto.shopify.bulk;

import lombok.Data;

@Data
public class BulkOperation {

    private String id;
    private String url;
    private String partialDataUrl;
    private int objectCount;
    private int rootObjectCount;
    private int fileSize;
    private String status;
    private String errorCode;

}
