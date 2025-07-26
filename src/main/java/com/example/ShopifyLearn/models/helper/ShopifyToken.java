package com.example.ShopifyLearn.models.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopifyToken {

    @JsonProperty("access_token")
    private String accessToken;

    private String scope;

    @JsonProperty("expires_in")
    @Nullable
    private String expiresIn;

    @JsonProperty("associated_user_scope")
    @Nullable
    private String associatedUserScope;

    @JsonProperty("associated_user")
    @Nullable
    private AssociatedUser associatedUser;

}
