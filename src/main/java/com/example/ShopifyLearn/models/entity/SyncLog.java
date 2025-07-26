package com.example.ShopifyLearn.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bulkOperationId;

    private Long storeId;

    @Enumerated(EnumType.STRING)
    private Title type;

    private String shopifyStatus;

    @Enumerated(EnumType.STRING)
    private appStatus appStatus;

    private int totalObjects;

    private int totalRootObjects;

    @Column(columnDefinition = "LONGTEXT")
    private String url;

    private String partialUrl;

    private String errorMessage;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp startedAt;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp completedAt;

    @CreationTimestamp
    @Column(insertable = true, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(insertable = true, updatable = true)
    private Timestamp updated_at;

    public enum appStatus {
        PENDING,
        STARTED,
        PROCESSING,
        SUCCESS,
        FAILED
    }

    public enum Title {
        SYNC_CUSTOMER,
        SYNC_PRODUCTS,
        SYNC_ORDERS
    }

}
