package com.example.ShopifyLearn.repository;

import com.example.ShopifyLearn.models.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreRepo extends JpaRepository<Store, Long> {
    public Optional<Store> findByShopifyStoreId(String shopifyStoreId);

    public Optional<Store> findByShopifyDomain(String shopDomain);
}
