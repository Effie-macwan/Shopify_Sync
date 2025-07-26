package com.example.ShopifyLearn.repository;

import com.example.ShopifyLearn.models.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepo extends JpaRepository<Users, Long> {

//    public Optional<Users> findByUsernameAndStoreId(String name, String storeId);

}
