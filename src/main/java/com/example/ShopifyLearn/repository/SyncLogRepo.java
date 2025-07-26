package com.example.ShopifyLearn.repository;

import com.example.ShopifyLearn.models.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyncLogRepo extends JpaRepository<SyncLog,Long> {

    public Optional<SyncLog> findByStoreIdAndBulkOperationId(Long storeId, String bulkOperationId);

    public List<SyncLog> findByStoreId(Long storeId);

    public Optional<SyncLog> findByStoreIdAndShopifyStatus(Long id, String shopifyStatus);

}
