package com.example.ShopifyLearn.repository;

import com.example.ShopifyLearn.models.entity.Variant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VariantRepo extends JpaRepository<Variant, Long> {

    public Optional<Variant> findByShopifyVariantId(String shopifyId);

}
