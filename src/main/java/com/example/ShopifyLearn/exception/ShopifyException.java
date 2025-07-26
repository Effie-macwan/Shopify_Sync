package com.example.ShopifyLearn.exception;

public class ShopifyException extends RuntimeException {
    public ShopifyException(String message) {
        super(message);
    }
    public ShopifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
