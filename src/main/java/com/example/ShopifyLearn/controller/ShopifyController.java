package com.example.ShopifyLearn.controller;

import com.example.ShopifyLearn.models.dto.request.AuthShopRequest;
import com.example.ShopifyLearn.models.dto.request.CallbackTokenRequest;
import com.example.ShopifyLearn.models.dto.response.AuthShopInstallResponse;
import com.example.ShopifyLearn.service.ShopifyAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/shopify")
public class ShopifyController {

    @Autowired
    private ShopifyAuthService shopifyAuthService;

    @GetMapping("/app/install")
    public void install(
            @RequestParam(required = true, name = "shop") String shop,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthShopRequest authShopRequest = new AuthShopRequest();
        authShopRequest.setShop(shop);
        try {
            AuthShopInstallResponse authShopInstallResponse = shopifyAuthService.shopInstall(authShopRequest);
            log.info("User will be redirected to url:"+authShopInstallResponse.getRedirectUrl());
            response.sendRedirect(authShopInstallResponse.getRedirectUrl());
        } catch (IOException ioException) {
            log.error("IOException occurred at controller:"+ ioException);
        }
    }

    @GetMapping("/callback")
    public void callback(
            @RequestParam(required = true, name = "code") String code,
            @RequestParam(required = true, name = "hmac") String hmac,
            @RequestParam(required = true, name = "state") String state,
            @RequestParam(required = true, name = "shop") String shopDomain,
            @RequestParam(required = true, name = "timestamp") String timestamp,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        String receivedShop = shopDomain.substring(0, shopDomain.indexOf(".myshopify.com"));
        log.info("Received shop"+receivedShop);
        CallbackTokenRequest callbackTokenRequest = new CallbackTokenRequest();
        callbackTokenRequest.setShop(receivedShop);
        callbackTokenRequest.setCode(code);
        callbackTokenRequest.setHmac(hmac);
        callbackTokenRequest.setTimestamp(timestamp);
        callbackTokenRequest.setShopDomain(shopDomain);
        callbackTokenRequest.setState(state);
        try {
            AuthShopInstallResponse authShopInstallResponse = shopifyAuthService.getToken(callbackTokenRequest, request, response);
            model.addAttribute("auth",authShopInstallResponse);
            response.sendRedirect(authShopInstallResponse.getRedirectUrl());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Object> handleWebhook(
            @RequestBody String payload,
            HttpServletRequest request
    ) {
        try {
            log.info("Payload: {}",payload);
            long start = System.currentTimeMillis();
            shopifyAuthService.handleWebhook(payload,request);
            log.info("Webhook processed in {}ms", System.currentTimeMillis() - start);
            return ResponseEntity.ok("Webhook received and processed.");
        } catch (Exception e) {
            log.error("Error in handling webhook: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in handling webhook");
        }

    }

}
