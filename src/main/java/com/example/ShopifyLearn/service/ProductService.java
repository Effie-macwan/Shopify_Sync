package com.example.ShopifyLearn.service;

import com.example.ShopifyLearn.models.dto.request.ProductRequest;
import com.example.ShopifyLearn.models.entity.Store;

import java.util.List;
import java.util.Map;

public interface ProductService {

    public void upsertProduct(ProductRequest productRequest, Map<String, List<String>> options, Map<String, String> imagesMap, Store store);

    public void deleteProduct(String productId, Store store);

}
