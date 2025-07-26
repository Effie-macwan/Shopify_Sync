package com.example.ShopifyLearn.models.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthShopInstallResponse {

    private String redirectUrl;
    private boolean isRedirected;
    private String shop;
    private String username;

}
