package com.example.ShopifyLearn.models.dto.request;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class CustomerDeleteRequest {

    private String id;
    private String phone;
    private List<CustomerAddressRequest> addresses;
    @SerializedName("tax_exemptions")
    private List<Object> taxExemptions;
    @SerializedName("admin_graphql_api_id")
    private String gid;

}
