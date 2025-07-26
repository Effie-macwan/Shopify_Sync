package com.example.ShopifyLearn.graphql;

import com.example.ShopifyLearn.config.graphql.ShopifyGraphqlConfig;
import com.example.ShopifyLearn.models.dto.shopify.GraphqlRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class GraphqlClient {

//    @Setter
//    private ShopifyGraphqlConfig shopifyGraphqlConfig;

    private WebClient getWebClient(ShopifyGraphqlConfig shopifyGraphqlConfig) {
        return WebClient.builder()
                .baseUrl("https://"+shopifyGraphqlConfig.getShopDomain()+"/admin/api/"+shopifyGraphqlConfig.getVersion()+"/graphql.json")
                .defaultHeaders( httpHeaders -> {
                    httpHeaders.add("Content-Type","application/json");
                    httpHeaders.add("X-Shopify-Access-Token",shopifyGraphqlConfig.getAccessToken());
                })
                .build();
    }

    public String execute(GraphqlRequest graphqlRequest, ShopifyGraphqlConfig graphqlConfig) {
        try {
            log.info("Request body sent");
//            log.info("Config: {}, {}",graphqlConfig.getAccessToken(),graphqlConfig.getShopDomain());

            String response = getWebClient(graphqlConfig).post()
                    .bodyValue(graphqlRequest)
                    .retrieve()
                    .bodyToMono(String.class)
//                    .toString();
                    .block();

            log.info("Response is {}",response);

            //handle error and throw if any
            return response;

        } catch (RuntimeException e) {
            log.error("Error in sending request: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
