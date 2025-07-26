package com.example.ShopifyLearn.service.impl;

import com.example.ShopifyLearn.exception.ResourceNotFoundException;
import com.example.ShopifyLearn.models.dto.request.ProductRequest;
import com.example.ShopifyLearn.models.dto.request.VariantRequest;
import com.example.ShopifyLearn.models.entity.Product;
import com.example.ShopifyLearn.models.entity.Store;
import com.example.ShopifyLearn.models.entity.Variant;
import com.example.ShopifyLearn.repository.ProductRepo;
import com.example.ShopifyLearn.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private VariantService variantService;

    @Override
    public void upsertProduct(ProductRequest request, Map<String, List<String>> options, Map<String, String> imagesMap, Store store) {

        log.info("Reached the service");
        String inventoryId = "gid://shopify/InventoryItem/";

        List<Variant> newVariants = request.variants()
                .stream().map(varReq -> toEntity(varReq, store.getId(), imagesMap, inventoryId, options))
                .toList();

        Optional<Product> oProduct = productRepo.findByStoreIdAndShopifyProductId(store.getId(), request.shopifyId());
        Product product;

        if (oProduct.isPresent()) {
            // update existing
            product = oProduct.get();
        } else {
            // create new
            product = new Product();
            product.setStoreId(store.getId());
            product.setShopifyProductId(request.shopifyId());
            product.setVariants(new ArrayList<>());
        }

        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setDescriptionHtml(request.descriptionHtml());
        product.setStatus(getStatus(request.status()));
        product.setVendor(request.vendor());
        product.setTags(request.tags());
        product.setImageUrl(request.url());
        product.setCategoryName(request.categoryName());

        newVariants.forEach(variant -> variant.setProduct(product));
        if (newVariants != null) {
            product.getVariants().clear();
            productRepo.save(product);

            newVariants.forEach(v -> v.setProduct(product));
            product.getVariants().addAll(newVariants);
        }

        productRepo.save(product);
        log.info("Upserted product {} with {} variants", product.getShopifyProductId(), newVariants.size());

    }

//    public void handleVariants(List<Variant> existingVariants, List<Variant> newVariants, Product product) {
//
//        if (existingVariants == null || existingVariants.isEmpty()) {
//            for (Variant v : newVariants) {
//                variantService.insertVariants(v, product);
//            }
//            log.info("Created new variants for the product {}", product.getShopifyProductId());
//            return;
//        }
//
//        Map<String, Variant> existingMap = existingVariants.stream()
//                .collect(Collectors.toMap(Variant::getShopifyVariantId, Function.identity()));
//
//        Set<String> processedVariantIds = new HashSet<>();
//
//        for (Variant newVar : newVariants) {
//            String newId = newVar.getShopifyVariantId();
//            if (existingMap.containsKey(newId)) {
//                variantService.updateVariants(newVar, product);
//            } else {
//                variantService.insertVariants(newVar, product);
//            }
//            processedVariantIds.add(newId);
//        }
//
//        //delete existing that are not present in newList
//        for (Variant existingVar : existingVariants) {
//            if (!processedVariantIds.contains(existingVar.getShopifyVariantId())) {
//                // Delete the variant using your delete logic.
//                variantService.deleteVariants(existingVar.getShopifyVariantId());
//            }
//        }
//
//    }

    @Override
    public void deleteProduct(String shopifyProductId, Store store) {
        Product product = productRepo.findByStoreIdAndShopifyProductId(store.getId(), shopifyProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + shopifyProductId + " not found."));

        Long id = product.getId();
        productRepo.deleteById(id);

        log.info("product with {} deleted", id);
    }

    private Product.ProductStatus getStatus(String strStatus) {
        Product.ProductStatus status;
        switch (strStatus.toLowerCase()) {
            case "active" -> status = Product.ProductStatus.ACTIVE;
            case "draft" -> status = Product.ProductStatus.DRAFT;
            case "archived" -> status = Product.ProductStatus.ARCHIVED;
            default -> {
                log.warn("Unknown status found : {}", strStatus);
                status = Product.ProductStatus.DRAFT;
            }
        }
        return status;
    }

    private Variant toEntity(VariantRequest request, Long storeId, Map<String, String> imagesMap, String inventoryItem, Map<String, List<String>> options) {

        if (request == null) {
            throw new IllegalArgumentException("VariantRequest cannot be null");
        }

        if (request.inventoryItemId() == null || request.inventoryItemId().isBlank()) {
            throw new IllegalArgumentException("InventoryItemId is required for variant");
        }

        if (request.shopifyId() == null || request.shopifyId().isBlank()) {
            throw new IllegalArgumentException("Shopify Variant ID is missing for one of the variants.");
        }

        Variant variant = new Variant();

        variant.setShopifyVariantId(request.shopifyId());
        variant.setStoreId(storeId);
        variant.setTitle(request.title());
        variant.setSku(request.sku());
        variant.setPrice(request.price());
        variant.setCompareAtPrice(request.compareAtPrice());

        String selectedOptions = getOptions(options, request.option1(), request.option2(), request.option3());
        variant.setSelectedOptions(selectedOptions);

        String imageId = request.imageId();
        if (imageId != null && !imageId.isBlank()) {
            String url = imagesMap.get(imageId);
            if (url != null) {
                variant.setImageUrl(url);
            } else {
                log.warn("Image not found for imageId: {}", imageId);
                variant.setImageUrl(null);
            }
        }

        variant.setInventoryItemId(inventoryItem + request.inventoryItemId());

        return variant;
    }

    private String getOptions(Map<String, List<String>> optionsMap, String option1, String option2, String option3) {

        if (optionsMap == null || optionsMap.isEmpty()) {
            throw new IllegalArgumentException("Options map cannot be null or empty");
        }

        List<Map<String, String>> result = new ArrayList<>();
        List<String> variantOptions = Arrays.asList(option1, option2, option3);

        int index = 0;
        for (Map.Entry<String, List<String>> entry : optionsMap.entrySet()) {
            if (index >= variantOptions.size()) break;

            String optionValue = variantOptions.get(index);
            if (optionValue != null && !optionValue.isEmpty()) {
                Map<String, String> optionObj = new HashMap<>();
                optionObj.put("name", entry.getKey());
                optionObj.put("value", optionValue);
                result.add(optionObj);
            }
            index++;
        }

        try {
            return new ObjectMapper().writeValueAsString(result); // Converts list to JSON string
        } catch (JsonProcessingException e) {
            log.error("Error converting options to JSON", e);
            throw new RuntimeException("Failed to convert options to JSON", e);
        }

    }

}
