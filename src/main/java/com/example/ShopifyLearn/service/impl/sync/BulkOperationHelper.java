package com.example.ShopifyLearn.service.impl.sync;

import com.example.ShopifyLearn.config.graphql.ShopifyGraphqlConfig;
import com.example.ShopifyLearn.exception.ShopifyException;
import com.example.ShopifyLearn.exception.ShopifyParsingException;
import com.example.ShopifyLearn.graphql.GraphqlClient;
import com.example.ShopifyLearn.graphql.util.GraphqlSchemaReader;
import com.example.ShopifyLearn.models.dto.shopify.GraphqlNode;
import com.example.ShopifyLearn.models.dto.shopify.GraphqlRequest;
import com.example.ShopifyLearn.models.dto.shopify.GraphqlResponseRoot;
import com.example.ShopifyLearn.models.dto.shopify.bulk.*;
import com.example.ShopifyLearn.models.entity.Customer;
import com.example.ShopifyLearn.models.entity.Product;
import com.example.ShopifyLearn.models.entity.Store;
import com.example.ShopifyLearn.models.entity.SyncLog;
import com.example.ShopifyLearn.models.helper.OffsetDateTimeAdapter;
import com.example.ShopifyLearn.repository.SyncLogRepo;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BulkOperationHelper {

    @Autowired
    private SyncLogRepo syncLogRepo;

    @Autowired
    private CustomerSyncService customerSyncService;

    @Autowired
    private ProductSyncProcessor productSyncProcessor;

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

    private final JdbcTemplate jdbcTemplate;

    public BulkOperationHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BulkOperationFinish handleBulkFinish(String payload, Store store) {
        BulkOperationFinish bulkOperationFinish = gson.fromJson(payload, BulkOperationFinish.class);
        if (bulkOperationFinish != null) {

//            String id = bulkOperationFinish.getAdminGraphqlApiId();
//            String bulkId = id.substring(id.lastIndexOf("/") + 1);
            String bulkId = Optional.ofNullable(bulkOperationFinish.getAdminGraphqlApiId())
                    .map(id -> id.substring(id.lastIndexOf("/") + 1))
                    .orElseThrow(() -> new ShopifyException("Invalid Bulk Operation ID format"));
            Optional<SyncLog> oSyncLog = syncLogRepo.findByStoreIdAndBulkOperationId(store.getId(), bulkId);

            String status = bulkOperationFinish.getStatus();
            if ("FAILED".equalsIgnoreCase(status)) {
                log.error("Bulk Operation failed");
                if (bulkOperationFinish.getErrorCode() != null) {
                    log.error("Error in bulk operation : {}", bulkOperationFinish.getErrorCode());
                }
                throw new ShopifyException("Bulk Operation failed");
            }
            if ("CANCELED".equalsIgnoreCase(status)) {
                log.error("Bulk Operation cancelled");
                throw new ShopifyException("Bulk Operation cancelled");
            }
            if ("COMPLETED".equalsIgnoreCase(status)) {
                log.info("Bulk Operation with id {} with status: {}", bulkOperationFinish.getAdminGraphqlApiId(), status);
                return bulkOperationFinish;
            }
        }
        //REMOVE THIS AFTER TESTING THE PARSING ONCE
        log.info("Bulk operation null");
        return null;
    }

    public void startProcessingBulkOperation(BulkOperationFinish bulkOperationFinish, Store store, SyncLog syncLog) {

        try {
            ShopifyGraphqlConfig shopifyGraphqlConfig = new ShopifyGraphqlConfig();
            shopifyGraphqlConfig.setAccessToken(store.getAccessToken());
            shopifyGraphqlConfig.setShopDomain(store.getShopifyDomain());

            String query = GraphqlSchemaReader.fetchQueryFromFile(GraphqlSchemaReader.FETCH_BULKOPERATION_DATA);
            Map<String, Object> variables = new HashMap<>();
            variables.put("id", bulkOperationFinish.getAdminGraphqlApiId());

            GraphqlRequest request = new GraphqlRequest(query, variables);

            GraphqlClient client = new GraphqlClient();
            String response = client.execute(request, shopifyGraphqlConfig);

            if (response != null) {
                TypeToken<GraphqlResponseRoot<GraphqlNode<BulkOperation>>> typeToken = new TypeToken<>() {
                };
                GraphqlResponseRoot<GraphqlNode<BulkOperation>> graphqlResponseRoot = gson.fromJson(response, typeToken);
                GraphqlNode<BulkOperation> graphqlNode = graphqlResponseRoot.getData();
                if (graphqlNode != null && graphqlNode.getNode().getUrl() != null) {
                    syncLog = saveSyncLog(syncLog, graphqlNode.getNode());
                    String url = graphqlNode.getNode().getUrl();
                    log.info("Starting to read jsonl file: {}", url);

                    switch (syncLog.getType()) {
                        case SYNC_CUSTOMER -> readCustomerFile(url,store,syncLog);
                        case SYNC_PRODUCTS -> productSyncProcessor.readProductFile(url, store, syncLog);
                        case SYNC_ORDERS -> readOrderFile(url, store, syncLog);
                    }
                }
            }

        } catch (IOException e) {
            log.error("Error in fetching url");
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Error processing bulk operation: {}", e.getMessage());
            throw new ShopifyException(e.getMessage());
        }
    }


    @Async(value = "syncOrderAsyncExecutor")
    public void readOrderFile(String url, Store store, SyncLog syncLog) {
    }

    @Async(value = "syncCustomersAsyncExecutor")
    public void readCustomerFile(String url, Store store, SyncLog syncLog) {

        syncLog.setAppStatus(SyncLog.appStatus.PROCESSING);
        syncLogRepo.save(syncLog);

        DataSource dataSource = jdbcTemplate.getDataSource();
        long startTime = System.currentTimeMillis();

        final long storeId = store.getId();
        int batchCounter = 0;
        Map<String, List<CustomerAddressDto>> idToAddressMap = new HashMap<>();
        Map<String, CustomerAddressDto> idToDefaultAddress = new HashMap<>();

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(customerSyncService.saveCustomerQuery);
             PreparedStatement addrPs = con.prepareStatement(customerSyncService.saveCustomerAddressQuery);) {
            try (InputStream inputStream = new URL(url).openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));) {
                int count = 0;
                String line;


                //READ NEXT LINE
                while ((line = reader.readLine()) != null) {
                    count++;
                    CustomerParseBulk customerParseBulk = gson.fromJson(line, CustomerParseBulk.class);
                    Customer customer = new Customer();
                    customer.setStoreId(storeId);
                    String customerState = customerParseBulk.state();

                    if (customerState.equalsIgnoreCase(Customer.CustomerState.ENABLED.toString()) || customerState.equalsIgnoreCase(Customer.CustomerState.INVITED.toString())) {
                        customerSyncService.setPreparedStatementParameters(ps, customerParseBulk, customer);
                        ps.addBatch();

                        //store addresses
                        List<CustomerAddressDto> addresses = customerParseBulk.customerAddressDtoList();
                        if (addresses == null) {
                            addresses = new ArrayList<>();
                        }

                        //save default address
                        idToAddressMap.put(customerParseBulk.shopifyCustomerId(), addresses);
                        if (customerParseBulk.defaultAddress() != null) {
                            idToDefaultAddress.put(customerParseBulk.shopifyCustomerId(), customerParseBulk.defaultAddress());
                        }

                    } else {
                        continue;
                    }
                    batchCounter++;
                    if (batchCounter % 500 == 0) {
                        log.info("Executing batch");
                        con.setAutoCommit(false);
                        ps.executeBatch();
                        con.commit();
                        batchCounter = 0;
                        ps.clearBatch();
                    }
                }


                if (batchCounter % 500 != 0) {
                    log.info("Executing for remaining records in batch");
                    con.setAutoCommit(false);
                    ps.executeBatch();
                    con.commit();
                    ps.clearBatch();
                }

                //GET CUSTOMER DETAILS
                Map<String, Long> shopifyToDbIdMap = new HashMap<>();

                List<String> shopifyIds = new ArrayList<>(idToAddressMap.keySet());
                String sql = String.format(
                        "SELECT id, shopify_customer_id FROM customer WHERE shopify_customer_id IN (%s)",
                        shopifyIds.stream().map(id -> "?").collect(Collectors.joining(","))
                );
                List<Object> params = new ArrayList<>(shopifyIds);
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

                for (Map<String, Object> row : rows) {
                    String shopifyId = (String) row.get("shopify_customer_id");
                    Long dbId = ((Number) row.get("id")).longValue();
                    shopifyToDbIdMap.put(shopifyId, dbId);
                }

                for (Map.Entry<String, List<CustomerAddressDto>> entry : idToAddressMap.entrySet()) {
                    String shopifyId = entry.getKey();
                    Long customerDbId = shopifyToDbIdMap.get(shopifyId);
                    if (customerDbId == null) {
                        log.warn("Skipping customer with id {} as it is not found.", customerDbId);
                        continue;
                    }

                    List<CustomerAddressDto> addresses = entry.getValue();
                    if (addresses.isEmpty()) {
                        // Nothing to process for this customer.
                        continue;
                    }

                    // Retrieve the provided default address, if any.
                    CustomerAddressDto defaultAddrDto = idToDefaultAddress.get(shopifyId);

                    for (CustomerAddressDto address : addresses) {
                        boolean isDefault = false;
                        if (addresses.size() == 1) {
                            //only one record
                            isDefault = true;
                        } else if ( defaultAddrDto != null && defaultAddrDto.id().equals(address.id())) {
                            //list of addresses and one of them is default
                            isDefault = true;
                        }
                        // Save address using customerDbId as foreign key
                        customerSyncService.setAddressParameters(addrPs, address, customerDbId, isDefault);
                        addrPs.addBatch();
                    }
                }

                //save address
                log.info("START SAVING ADDRESS");
                con.setAutoCommit(false);
                addrPs.executeBatch();
                con.commit();
                addrPs.clearBatch();
                log.info("SAVED ADDRESSES");




                log.info("Counter: {}", count);
            }

            log.info("Customer Sync Completed, time took = {}ms", (System.currentTimeMillis() - startTime));
            syncLog.setAppStatus(SyncLog.appStatus.SUCCESS);
            syncLogRepo.save(syncLog);
        } catch (MalformedURLException e) {
            syncLog.setAppStatus(SyncLog.appStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepo.save(syncLog);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("IO error while fetching bulk operation URL: {}", e.getMessage());
            syncLog.setAppStatus(SyncLog.appStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepo.save(syncLog);
            throw new ShopifyException("Error while fetching bulk operation data", e);
        } catch (SQLException e) {
            log.error("SQL error while processing bulk operation: {}", e.getMessage());
            syncLog.setAppStatus(SyncLog.appStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepo.save(syncLog);
            throw new ShopifyException("Database error during bulk sync", e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            syncLog.setAppStatus(SyncLog.appStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepo.save(syncLog);
            throw new ShopifyException("There is something wrong while starting the Customer sync process");
        }

    }


    public SyncLog saveSyncLog(SyncLog syncLog, BulkOperation bulkOperation) {
        syncLog.setTotalObjects(bulkOperation.getObjectCount());
        syncLog.setTotalRootObjects(bulkOperation.getRootObjectCount());
        syncLog.setUrl(bulkOperation.getUrl());
        syncLog.setPartialUrl(bulkOperation.getPartialDataUrl());
        syncLog.setErrorMessage(bulkOperation.getErrorCode());
        return syncLogRepo.save(syncLog);
    }
}
