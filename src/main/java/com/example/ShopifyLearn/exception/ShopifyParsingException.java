package com.example.ShopifyLearn.exception;

public class ShopifyParsingException extends RuntimeException {
    public ShopifyParsingException(String message) {
        super(message);
    }
    public ShopifyParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
