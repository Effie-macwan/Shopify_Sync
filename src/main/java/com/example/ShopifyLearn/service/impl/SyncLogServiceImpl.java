package com.example.ShopifyLearn.service.impl;

import com.example.ShopifyLearn.models.entity.SyncLog;
import com.example.ShopifyLearn.repository.SyncLogRepo;
import com.example.ShopifyLearn.service.SyncLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SyncLogServiceImpl implements SyncLogService {

    @Autowired
    private SyncLogRepo syncLogRepo;

    public boolean isBulkQueryRunning(Long storeId) {

        List<SyncLog> logList = syncLogRepo.findByStoreId(storeId);
        for (SyncLog log : logList) {
            if (log.getAppStatus() == SyncLog.appStatus.PENDING
                    || log.getAppStatus() == SyncLog.appStatus.STARTED
                    || log.getAppStatus() == SyncLog.appStatus.PROCESSING) {
                return true;
            }
        }
        return false;
    }

}
