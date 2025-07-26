package com.example.ShopifyLearn.models.dto.request;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CustomerRequest(
        @NotNull(message = "Shopify id should not be null")
        String id,
        @Email(message = "Invalid email format")
        String email,
        @Size(max = 15, message = "Phone number must be at most 15 characters")
        String phone,
        @SerializedName("created_at")
        @NotBlank(message = "Created date is required")
        String createdAt,
        @SerializedName("updated_at")
        String updatedAt,
        @SerializedName("first_name")
        @Size(max = 25)
        String firstName,
        @SerializedName("last_name")
        @Size(max = 25)
        String lastName,
        @NotNull(message = "Customer state is required")
        String state,
        @SerializedName("verified_email")
        boolean verifiedEmail,
        @SerializedName("tax_exempt")
        boolean taxExempt,
        @SerializedName("default_address")
        CustomerAddressRequest defaultAddress,
        @NotNull(message = "Address list cannot be null")
        @Size(min = 1, message = "At least one address is required")
        List<CustomerAddressRequest> addresses
) {
}
