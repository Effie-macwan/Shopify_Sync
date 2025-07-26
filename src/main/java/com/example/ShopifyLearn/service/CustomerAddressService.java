package com.example.ShopifyLearn.service;

import com.example.ShopifyLearn.models.entity.Customer;
import com.example.ShopifyLearn.models.entity.CustomerAddress;

public interface CustomerAddressService {

    public CustomerAddress upsertAddress(CustomerAddress address, Customer customer);

    public CustomerAddress findByShopifyId(String shopifyId);

    public void deleteByShopifyId(String shopifyId);

}
