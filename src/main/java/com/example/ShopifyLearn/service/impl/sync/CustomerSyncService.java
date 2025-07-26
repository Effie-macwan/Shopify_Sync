package com.example.ShopifyLearn.service.impl.sync;

import com.example.ShopifyLearn.models.dto.shopify.bulk.CustomerAddressDto;
import com.example.ShopifyLearn.models.dto.shopify.bulk.CustomerParseBulk;
import com.example.ShopifyLearn.models.entity.Customer;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CustomerSyncService {

    public final String saveCustomerQuery = "INSERT INTO customer (\n" +
            "    shopify_customer_id,\n" +
            "    store_id,\n" +
            "    first_name,\n" +
            "    last_name,\n" +
            "    display_name,\n" +
            "    default_email_address,\n" +
            "    default_phone,\n" +
            "    state,\n" +
            "    number_of_orders,\n" +
            "    verified_email,\n" +
            "    tax_exempt,\n" +
            "    data_sale_opt_out,\n" +
            "    tags,\n" +
            "    created_at,\n" +
            "    updated_at\n" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "store_id              = VALUES(store_id), " +
            "first_name            = VALUES(first_name), " +
            "last_name             = VALUES(last_name), " +
            "display_name          = VALUES(display_name), " +
            "default_email_address = VALUES(default_email_address), " +
            "default_phone         = VALUES(default_phone), " +
            "state                 = VALUES(state), " +
            "number_of_orders      = VALUES(number_of_orders), " +
            "verified_email        = VALUES(verified_email), " +
            "tax_exempt            = VALUES(tax_exempt), " +
            "data_sale_opt_out     = VALUES(data_sale_opt_out), " +
            "tags                  = VALUES(tags), " +
            "created_at            = VALUES(created_at), " +
            "updated_at            = VALUES(updated_at)";


    public final String saveCustomerAddressQuery = "INSERT INTO customer_address (" +
            "shopify_address_id, address1, address2, city, country, zip, province, phone, customer_id, is_default" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "shopify_address_id = VALUES(shopify_address_id), " +
            "address1 = VALUES(address1), " +
            "address2 = VALUES(address2), " +
            "city = VALUES(city), " +
            "country = VALUES(country), " +
            "zip = VALUES(zip), " +
            "province = VALUES(province), " +
            "phone = VALUES(phone), " +
            "customer_id = VALUES(customer_id), " +
            "is_default = VALUES(is_default)";




//    public void saveCustomerToBatch(Customer customer) {
//
//        jdbcTemplate.batchUpdate(saveCustomerQuery, customerList, customerList.size(), (ps, customer) -> {
//            ps.setString(1, customer.getShopifyCustomerId());
//            ps.setLong(2,customer.getStoreId());
//            ps.setString(3, customer.getFirstName());
//            ps.setString(4, customer.getLastName());
//            ps.setString(5, customer.getState().name());
//            ps.setInt(6, customer.getNumberOfOrders());
//            ps.setBoolean(7, customer.isVerifiedEmail());
//            ps.setBoolean(8, customer.isTaxExempt());
//            ps.setBoolean(9, customer.isDataSaleOptOut());
//            ps.setString(10, customer.getTaxExemptions());
//            ps.setString(11, customer.getDefaultAddress());
//            ps.setString(12, customer.getDefaultCity());
//        });
//
//    }

    //    public void setPreparedStatementParameters(PreparedStatement ps, CustomerParseBulk customerParseBulk, Customer customer) throws SQLException {
//
//        String shopifyId = customerParseBulk.shopifyCustomerId();
//        String sId = shopifyId.substring(shopifyId.lastIndexOf("/") + 1);
//
//        ps.setString(1, sId);
//        ps.setLong(2, customer.getStoreId());
//        ps.setString(3, customerParseBulk.firstName());
//        ps.setString(4, customerParseBulk.lastName());
//        ps.setString(5, customerParseBulk.state());
//        ps.setInt(6, customerParseBulk.numberOfOrders());
//        ps.setBoolean(7, customerParseBulk.verifiedEmail());
//        ps.setBoolean(8, customerParseBulk.taxExempt());
//        ps.setBoolean(9, customerParseBulk.dataSaleOptOut());
//        ps.setString(10, customerParseBulk.defaultAddress().address1());
//        ps.setString(11, customerParseBulk.defaultAddress().city());
//        ps.setString(12, String.join(",", customerParseBulk.tags()));
//    }
    public void setPreparedStatementParameters(PreparedStatement ps, CustomerParseBulk customerParseBulk, Customer customer) throws SQLException {

        ps.setString(1, customerParseBulk.shopifyCustomerId());
        ps.setLong(2, customer.getStoreId());
        ps.setString(3, customerParseBulk.firstName());
        ps.setString(4, customerParseBulk.lastName());
        ps.setString(5, customerParseBulk.displayName());
        ps.setString(6, customerParseBulk.email());
        ps.setString(7, customerParseBulk.phone());
        ps.setString(8, customerParseBulk.state());
        ps.setInt(9, customerParseBulk.numberOfOrders());
        ps.setBoolean(10, customerParseBulk.verifiedEmail());
        ps.setBoolean(11, customerParseBulk.taxExempt());
        ps.setBoolean(12, customerParseBulk.dataSaleOptOut());

        String tags = customerParseBulk.tags() != null ? String.join(",", customerParseBulk.tags()) : null;
        ps.setString(13, tags);

        OffsetDateTime createdAtOffset = customerParseBulk.createdAt() != null
                ? OffsetDateTime.parse(customerParseBulk.createdAt())
                : null;
        Timestamp createdAt = createdAtOffset != null
                ? Timestamp.from(createdAtOffset.toInstant())
                : null;

        OffsetDateTime updatedAtOffset = customerParseBulk.updatedAt() != null
                ? OffsetDateTime.parse(customerParseBulk.updatedAt())
                : null;
        Timestamp updatedAt = updatedAtOffset != null
                ? Timestamp.from(updatedAtOffset.toInstant())
                : null;

        ps.setTimestamp(14, createdAt);
        ps.setTimestamp(15, updatedAt);
    }


    public void setAddressParameters(PreparedStatement ps, CustomerAddressDto addressDto, Long customerId, boolean isDefault) throws SQLException {

        ps.setString(1, addressDto.id());
        ps.setString(2, addressDto.address1());
        ps.setString(3, addressDto.address2());
        ps.setString(4, addressDto.city());
        ps.setString(5, addressDto.country());
        ps.setString(6, addressDto.zip());
        ps.setString(7, addressDto.province());
        ps.setString(8, addressDto.phone());
        ps.setLong(9, customerId);
        ps.setBoolean(10, isDefault);
    }

//    public static String extractShopifyAddressId(String gid) {
//        Pattern pattern = Pattern.compile(".*/(\\d+)");
//        Matcher matcher = pattern.matcher(gid);
//        if (matcher.find()) {
//            return matcher.group(1);
//        }
//        throw new IllegalArgumentException("Invalid Shopify GID format: " + gid);
//    }
}
