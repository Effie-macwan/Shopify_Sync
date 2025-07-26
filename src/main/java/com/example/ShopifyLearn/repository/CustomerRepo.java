package com.example.ShopifyLearn.repository;

import com.example.ShopifyLearn.models.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {

    public Optional<Customer> findByStoreIdAndShopifyCustomerId(Long storeId, String shopifyCustomerId);

    public Optional<Customer> findByShopifyCustomerId(String shopifyId);

}
