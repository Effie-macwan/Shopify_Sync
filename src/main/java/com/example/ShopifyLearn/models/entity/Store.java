package com.example.ShopifyLearn.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String shopOwner;

    private String ianaTimezone;

    private String accessToken;

    private String shopifyDomain;

    private String shopifyStoreId;

    private String plan;

    private String currencyCode;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp createdAt;

    private String url;

    private String weightUnit;

    private String shipsToCountries;

    @UpdateTimestamp
    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp updatedAt;

}
