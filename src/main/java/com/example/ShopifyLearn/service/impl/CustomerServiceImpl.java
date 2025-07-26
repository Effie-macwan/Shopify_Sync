package com.example.ShopifyLearn.service.impl;

import com.example.ShopifyLearn.exception.ResourceNotFoundException;
import com.example.ShopifyLearn.models.dto.request.CustomerAddressRequest;
import com.example.ShopifyLearn.models.dto.request.CustomerDeleteRequest;
import com.example.ShopifyLearn.models.dto.request.CustomerRequest;
import com.example.ShopifyLearn.models.entity.Customer;
import com.example.ShopifyLearn.models.entity.CustomerAddress;
import com.example.ShopifyLearn.models.entity.Store;
import com.example.ShopifyLearn.repository.CustomerRepo;
import com.example.ShopifyLearn.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private CustomerAddressServiceImpl customerAddressServiceImpl;


    @Override
    public void upsertCustomer(CustomerRequest customerDto, Store store) {

        String sId = customerDto.id();
        Customer customer;
        List<CustomerAddress> existingAddress = null;

        Optional<Customer> oCustomer = customerRepo.findByStoreIdAndShopifyCustomerId(store.getId(), sId);
        if (oCustomer.isPresent()) {
            //UPDATE
            customer = oCustomer.get();
            existingAddress = customer.getCustomerAddress();
        } else {
            //CREATE
            customer = new Customer();
            customer.setShopifyCustomerId(sId);
            customer.setStoreId(store.getId());
        }

        customer.setFirstName(customerDto.firstName());
        customer.setLastName(customerDto.lastName());
        customer.setDisplayName(customerDto.firstName() + " " + customerDto.lastName());
        customer.setDefaultEmailAddress(customerDto.email());
        customer.setDefaultPhone(customerDto.phone());
        Customer.CustomerState state = null;
        try {
            state = Customer.CustomerState.valueOf(customerDto.state().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown customer state: {}", customerDto.state());
            state = Customer.CustomerState.DISABLED;
        }
        customer.setState(state);
        customer.setVerifiedEmail(customerDto.verifiedEmail());
        customer.setTaxExempt(customerDto.taxExempt());
        if (customerDto.createdAt() == null) {
            customer.setCreatedAt(Timestamp.from(OffsetDateTime.now().toInstant()));
        }
        else {
            customer.setCreatedAt(getTimestamp(customerDto.createdAt()));
        }
        if (customerDto.updatedAt() == null) {
            customer.setUpdatedAt(Timestamp.from(OffsetDateTime.now().toInstant()));
        }
        else {
            customer.setUpdatedAt(getTimestamp(customerDto.updatedAt()));
        }
        customerRepo.save(customer);

        //handle address
        handleAddress(customerDto, existingAddress, customer);


        log.info("Successfully handled customer create/update");


    }

    public void deleteCustomer(CustomerDeleteRequest customerDeleteRequest, Store store) {
        try {
            Customer customer = customerRepo.findByStoreIdAndShopifyCustomerId(store.getId(), customerDeleteRequest.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("customer with id " + customerDeleteRequest.getId() + " not found"));

            Long id = customer.getId();
            customerRepo.deleteById(id);
            log.info("Successfully deleted customer with id: {}", id);

        } catch (ResourceNotFoundException resourceNotFoundException) {
            log.error(resourceNotFoundException.getMessage());
        }

    }

    private void handleAddress(CustomerRequest customerDto, List<CustomerAddress> existingAddress, Customer customer) {
        List<CustomerAddress> newAddressList = customerDto.addresses().stream()
                .map(dto -> toEntity(dto,customer))
                .toList();

        Set<String> existingIds = new HashSet<>();
        if (existingAddress != null) {
            existingIds = existingAddress.stream()
                    .map(CustomerAddress::getShopifyAddressId)
                    .collect(Collectors.toSet());
        }

        for (CustomerAddress newAddress : newAddressList) {
            customerAddressServiceImpl.upsertAddress(newAddress, customer);
            existingIds.remove(newAddress.getShopifyAddressId());
        }

        if (!existingIds.isEmpty()) {
            for (String deleteId : existingIds) {
                customerAddressServiceImpl.deleteByShopifyId(deleteId);
            }
        }
        log.info("Handled addresses for customer {}", customer.getShopifyCustomerId());
    }

    private CustomerAddress toEntity(CustomerAddressRequest addressDto, Customer customer) {
        CustomerAddress customerAddress = new CustomerAddress();

//        Customer customer = customerRepo.findByShopifyCustomerId(addressDto.customerId())
//                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + addressDto.customerId() + " not found"));

        customerAddress.setShopifyAddressId(addressDto.id());
        customerAddress.setAddress1(addressDto.address1());
        customerAddress.setAddress2(addressDto.address2());
        customerAddress.setCity(addressDto.city());
        customerAddress.setCountry(addressDto.country());
        customerAddress.setZip(addressDto.zip());
        customerAddress.setProvince(addressDto.province());
        customerAddress.setPhone(addressDto.phone());
        customerAddress.setDefault(addressDto.isDefault());
        customerAddress.setCustomer(customer);

        return customerAddress;
    }

    private Timestamp getTimestamp(String time) {
        return Timestamp.from(OffsetDateTime.parse(time).toInstant());
    }

    private String extractShopifyId(String gid) {
        String sId = gid.substring(gid.lastIndexOf("/") + 1);
        return sId;
    }
}
