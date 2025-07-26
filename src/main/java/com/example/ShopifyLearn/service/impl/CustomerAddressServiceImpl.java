package com.example.ShopifyLearn.service.impl;

import com.example.ShopifyLearn.exception.ResourceNotFoundException;
import com.example.ShopifyLearn.models.entity.Customer;
import com.example.ShopifyLearn.models.entity.CustomerAddress;
import com.example.ShopifyLearn.repository.CustomerAddressRepo;
import com.example.ShopifyLearn.service.CustomerAddressService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerAddressServiceImpl implements CustomerAddressService {

    @Autowired
    private CustomerAddressRepo customerAddressRepo;

    public CustomerAddress upsertAddress(CustomerAddress address, Customer customer) {

        CustomerAddress customerAddress;
        Optional<CustomerAddress> existingAddressOpt = customerAddressRepo.findByShopifyAddressId(address.getShopifyAddressId());
        customerAddress = existingAddressOpt.orElseGet(CustomerAddress::new);

        customerAddress.setShopifyAddressId(address.getShopifyAddressId());
        customerAddress.setAddress1(address.getAddress1());
        customerAddress.setAddress2(address.getAddress2());
        customerAddress.setCity(address.getCity());
        customerAddress.setCountry(address.getCountry());
        customerAddress.setZip(address.getZip());
        customerAddress.setProvince(address.getProvince());
        customerAddress.setPhone(address.getPhone());
        customerAddress.setDefault(address.isDefault());
        customerAddress.setCustomer(customer);

        return customerAddressRepo.save(customerAddress);

    }

    public CustomerAddress findByShopifyId(String shopifyId) {
        CustomerAddress address = customerAddressRepo.findByShopifyAddressId(shopifyId)
                .orElseThrow(() -> new ResourceNotFoundException("Failed to fetch customer address with id "+shopifyId));
        return address;
    }

    public void deleteByShopifyId(String shopifyId) {
        CustomerAddress address = customerAddressRepo.findByShopifyAddressId(shopifyId)
                .orElseThrow(() -> new ResourceNotFoundException("Failed to fetch customer address with id "+shopifyId));
        Long id = address.getId();
        customerAddressRepo.deleteById(id);
    }

}
