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
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Roles role;

    private Long storeId;

    private String firstName;

    private String lastName;

    private String username;

    private String email;

    private boolean emailVerified;

    private boolean isAdmin;

    private String locale;

    private boolean collaborator;

    @Column(updatable = false)
    @CreationTimestamp
    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp createdAt;

    @Column(updatable = true)
    @UpdateTimestamp
    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp updatedAt;

}
