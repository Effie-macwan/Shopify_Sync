package com.example.ShopifyLearn.models.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Variant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String shopifyVariantId;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false, length = 255)
    private String title;

    private String sku;

//    @Digits(integer = 10, fraction = 2, message = "Price have 2 decimal places")
//    @DecimalMin(value = "0.0", inclusive = true)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "selected_options", columnDefinition = "TEXT")
    private String selectedOptions;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private String inventoryItemId;

    @ManyToOne
    @JoinColumn(name = "productId")
    @JsonBackReference
    private Product product;

    @CreationTimestamp
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp updatedAt;

}
