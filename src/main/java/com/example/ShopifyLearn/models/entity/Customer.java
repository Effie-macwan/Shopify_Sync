package com.example.ShopifyLearn.models.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String shopifyCustomerId;

    @Column(nullable = false)
    private Long storeId;

    @Column(length = 20, nullable = false)
    private String firstName;

    @Column(length = 20)
    private String lastName;

    @Column(length = 50)
    private String displayName;

    private String defaultEmailAddress;

    private String defaultPhone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CustomerState state;

    private int numberOfOrders;
    private boolean verifiedEmail;
    private boolean taxExempt;
    private boolean dataSaleOptOut;

    private String tags;

    @OneToMany(mappedBy = "customer", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CustomerAddress> customerAddress;

    @CreationTimestamp
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp updatedAt;

    public enum CustomerState {
        ENABLED,
        INVITED,
        DISABLED
    }
}
