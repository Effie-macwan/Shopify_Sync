package com.example.ShopifyLearn.models.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CallbackTokenRequest {

    private String code;
    private String hmac;
    private String state;
    private String shop;
    private String shopDomain;
    private String timestamp;

}
