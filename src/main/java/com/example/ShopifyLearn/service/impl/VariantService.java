package com.example.ShopifyLearn.service.impl;

import com.example.ShopifyLearn.exception.ResourceNotFoundException;
import com.example.ShopifyLearn.models.entity.Product;
import com.example.ShopifyLearn.models.entity.Variant;
import com.example.ShopifyLearn.repository.VariantRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VariantService {

    @Autowired
    private VariantRepo variantRepo;

    public void updateVariants(Variant variant, Product product) {

    }

    public void insertVariants(Variant variant, Product product) {

    }

    public void deleteVariants(String shopifyId) {
        Variant variant = variantRepo.findByShopifyVariantId(shopifyId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant with id "+ shopifyId + " not found"));
        Long id = variant.getId();
        variantRepo.deleteById(id);
        log.info("variant with id {} deleted", id);
    }
}
