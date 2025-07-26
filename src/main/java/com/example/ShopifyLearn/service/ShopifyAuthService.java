package com.example.ShopifyLearn.service;

import com.example.ShopifyLearn.models.dto.request.AuthShopRequest;
import com.example.ShopifyLearn.models.dto.request.CallbackTokenRequest;
import com.example.ShopifyLearn.models.dto.response.AuthShopInstallResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ShopifyAuthService {

    public AuthShopInstallResponse shopInstall(AuthShopRequest authShopRequest);

    public AuthShopInstallResponse getToken(CallbackTokenRequest callbackTokenRequest, HttpServletRequest request, HttpServletResponse response);

    public void handleWebhook(String payload, HttpServletRequest request);
}
