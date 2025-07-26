package com.example.ShopifyLearn.service.impl.sync;

import com.example.ShopifyLearn.config.graphql.ShopifyGraphqlConfig;
import com.example.ShopifyLearn.exception.ShopifyParsingException;
import com.example.ShopifyLearn.graphql.GraphqlClient;
import com.example.ShopifyLearn.graphql.util.GraphqlSchemaReader;
import com.example.ShopifyLearn.models.dto.shopify.GraphqlRequest;
import com.example.ShopifyLearn.models.dto.shopify.GraphqlResponseRoot;
import com.example.ShopifyLearn.models.dto.shopify.bulk.BulkOperation;
import com.example.ShopifyLearn.models.dto.shopify.bulk.BulkOperationResponseRoot;
import com.example.ShopifyLearn.models.entity.Store;
import com.example.ShopifyLearn.models.entity.SyncLog;
import com.example.ShopifyLearn.repository.SyncLogRepo;
import com.example.ShopifyLearn.service.impl.SyncLogServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Array;

@Slf4j
@Service
public class SyncService {

    @Autowired
    private SyncLogRepo syncLogRepo;

    @Autowired
    private SyncLogServiceImpl syncLogServiceImpl;

    Gson gson = new Gson();

    public void startSync(Store store) {

        log.info("STARTING SYNC SERVICE");

        startCustomerSync(store);

        try {
            while (syncLogRepo.findByStoreIdAndShopifyStatus(store.getId(), "CREATED").isPresent()) {
                log.warn("A bulk operation is already running for store {}. Waiting...", store.getId());
                Thread.sleep(5000);
            }
            startProductSync(store);

//            while (syncLogRepo.findByStoreIdAndShopifyStatus(store.getId(), "CREATED").isPresent()) {
//                log.warn("A bulk operation is already running for store {}. Waiting...", store.getId());
//                Thread.sleep(5000);
//            }
//            startOrderSync(store);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void startCustomerSync(Store store) {
        log.info("Starting bulk operation for customer");
        String file = GraphqlSchemaReader.BULK_FETCH_CUSTOMERS;
        startSync(store,file, SyncLog.Title.SYNC_CUSTOMER);
    }

    private void startProductSync(Store store) {
        log.info("Starting bulk operation for product");
        String file = GraphqlSchemaReader.BULK_FETCH_PRODUCTS;
        startSync(store,file, SyncLog.Title.SYNC_PRODUCTS);
    }

    private void startOrderSync(Store store) {
        log.info("Starting bulk operation for order");
        String file = GraphqlSchemaReader.BULK_FETCH_ORDERS;
        startSync(store,file, SyncLog.Title.SYNC_ORDERS);
    }

    private void startSync(Store store, String file, SyncLog.Title title) {

        try {
            ShopifyGraphqlConfig shopifyGraphqlConfig = new ShopifyGraphqlConfig();
            shopifyGraphqlConfig.setAccessToken(store.getAccessToken());
            shopifyGraphqlConfig.setShopDomain(store.getShopifyDomain());

            String query = GraphqlSchemaReader.fetchQueryFromFile(file);
            GraphqlRequest request = new GraphqlRequest();
            request.setQuery(query);

            GraphqlClient client = new GraphqlClient();
            String response = client.execute(request, shopifyGraphqlConfig);

            TypeToken<GraphqlResponseRoot<BulkOperationResponseRoot>> typeToken = new TypeToken<>() {
            };
            GraphqlResponseRoot<BulkOperationResponseRoot> responseRoot = gson.fromJson(response, typeToken);
            if(responseRoot.getData().getBulkOperationRunQuery().getBulkOperation() == null) {
                if(responseRoot.getData().getBulkOperationRunQuery().getUserErrors() != null) {
                    log.error("User error: {}",responseRoot.getData().getBulkOperationRunQuery().getUserErrors());
                    throw new ShopifyParsingException("User error: "+responseRoot.getData().getBulkOperationRunQuery().getUserErrors());
                }
                log.error("Error parsing data");
                throw new ShopifyParsingException("Error parsing error");
            }
            BulkOperation bulkOperation = responseRoot.getData().getBulkOperationRunQuery().getBulkOperation();
            if (bulkOperation != null) {
                log.info("Bulk Operation id: {} with status {}",bulkOperation.getId(), bulkOperation.getStatus());
                SyncLog syncLog = new SyncLog();
                syncLog.setStoreId(store.getId());
                syncLog.setShopifyStatus(bulkOperation.getStatus());
                syncLog.setType(title);
                syncLog.setBulkOperationId(bulkOperation.getId());
                syncLog.setAppStatus(SyncLog.appStatus.PENDING);
                syncLogRepo.save(syncLog);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error starting bulk operation", e);
        }

    }


}
