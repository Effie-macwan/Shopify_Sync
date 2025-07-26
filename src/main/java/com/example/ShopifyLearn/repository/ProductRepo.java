package com.example.ShopifyLearn.repository;

import com.example.ShopifyLearn.models.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

    Optional<Product> findByStoreIdAndShopifyProductId(Long storeId, String shopifyProductId);
}
