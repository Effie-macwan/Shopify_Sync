package com.example.ShopifyLearn.exception;

public class ShopifyAuthException extends RuntimeException {
  public ShopifyAuthException(String message) {
    super(message);
  }

  public ShopifyAuthException(String message, Throwable cause) {
    super(message, cause);
  }
}

