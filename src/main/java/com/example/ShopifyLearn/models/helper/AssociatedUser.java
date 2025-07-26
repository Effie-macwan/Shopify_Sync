package com.example.ShopifyLearn.models.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssociatedUser {

    private String id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    @JsonProperty("account_owner")
    private Boolean accountOwner;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("collaborator")
    private Boolean collaborator;

}
