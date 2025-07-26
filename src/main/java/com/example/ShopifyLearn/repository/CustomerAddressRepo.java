package com.example.ShopifyLearn.repository;

import com.example.ShopifyLearn.models.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerAddressRepo extends JpaRepository<CustomerAddress, Long> {

    public Optional<CustomerAddress> findByShopifyAddressId(String shopifyId);

    @Query("SELECT ca FROM CustomerAddress ca WHERE ca.customer.id = :customerId AND ca.isDefault = :isDefault")
    public Optional<CustomerAddress> findDefaultAddress(@Param("customerId") Long customerId,
                                                        @Param("isDefault") boolean isDefault);

}
