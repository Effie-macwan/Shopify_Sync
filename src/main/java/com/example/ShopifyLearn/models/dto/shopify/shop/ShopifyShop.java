package com.example.ShopifyLearn.models.dto.shopify.shop;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class ShopifyShop {

    private String id;

    private String name;

    private String email;

    private String myshopifyDomain;

    private String shopOwnerName;

    private String ianaTimezone;

    private String currencyCode;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp createdAt;

    private String url;

    private String weightUnit;

    private List<String> shipsToCountries;

    private GraphqlPlan plan;

}
