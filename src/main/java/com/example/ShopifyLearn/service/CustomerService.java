package com.example.ShopifyLearn.service;

import com.example.ShopifyLearn.models.dto.request.CustomerDeleteRequest;
import com.example.ShopifyLearn.models.dto.request.CustomerRequest;
import com.example.ShopifyLearn.models.entity.Customer;
import com.example.ShopifyLearn.models.entity.Store;

import java.util.Optional;

public interface CustomerService {

    public void upsertCustomer(CustomerRequest customerDto, Store store);

    public void deleteCustomer(CustomerDeleteRequest customerDto, Store store);
}
