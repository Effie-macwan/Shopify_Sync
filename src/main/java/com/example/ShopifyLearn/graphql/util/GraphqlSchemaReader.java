package com.example.ShopifyLearn.graphql.util;


import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GraphqlSchemaReader {

//    private final ResourceLoader resourceLoader;
//    private GraphqlSchemaReader(ResourceLoader resourceLoader) {
//        this.resourceLoader = resourceLoader;
//    }
//    public String getSchema(String filename) throws IOException {
//        Resource resource = resourceLoader.getResource("classpath:graphql/" + filename + ".graphql");
//
//        Path path = resource.getFile().toPath();
//        return Files.readString(path, StandardCharsets.UTF_8);
//    }

    public static final String FETCH_STORE_DETAILS = "fetchStoreDetails";

    public static final String FETCH_WEBHOOKS = "fetchWebhookSubscriptions";
    public static final String CREATE_WEBHOOK = "createWebhook";
    public static final String UPDATE_WEBHOOK = "updateWebhook";
    public static final String DELETE_WEBHOOK = "deleteWebhook";

    public static final String BULK_FETCH_CUSTOMERS = "bulkFetchCustomers";
    public static final String BULK_FETCH_PRODUCTS = "bulkFetchProducts";
    public static final String BULK_FETCH_ORDERS = "bulkFetchOrders";

    public static final String FETCH_BULKOPERATION_DATA = "fetchBulkOperationData";


    public static String fetchQueryFromFile(String file) throws IOException {
        try(InputStream ipReader = GraphqlSchemaReader.class.getClassLoader().getResourceAsStream("graphql/" + file + ".graphql");) {
            if( ipReader != null){
                return new String(ipReader.readAllBytes());
            }
        }
        throw new RuntimeException("Graphql Schema reading error");

    }

}
