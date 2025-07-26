package com.example.ShopifyLearn.models.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String shopifyAddressId;

    @Column(length = 255, nullable = false)
    private String address1;

    @Column(length = 255)
    private String address2;

    @Column(length = 100, nullable = false)
    private String city;

    @Column(length = 100, nullable = false)
    private String country;

    @Column(length = 20, nullable = false)
    private String zip;

    @Column(length = 20)
    private String province;

    @Column(length = 15)
    private String phone;

    private boolean isDefault;

    @ManyToOne
    @JoinColumn(name = "customerId")
    @JsonBackReference
    private Customer customer;

}
