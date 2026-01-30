package com.lektralabs.thrones.pallbearer.api.model.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class FindUserResponse {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("createdTimestamp")
    private long createdTimestamp;
    @JsonProperty("username")
    private String username;
    @JsonProperty("enabled")
    private boolean enabled;
    @JsonProperty("totp")
    private boolean totp;
    @JsonProperty("emailVerified")
    private boolean emailVerified;
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("lastName")
    private String lastName;
    @JsonProperty("email")
    private String email;
    @JsonProperty("disableableCredentialTypes")
    private List<String> disableableCredentialTypes;
    @JsonProperty("requiredActions")
    private List<String> requiredActions;
    @JsonProperty("notBefore")
    private int notBefore;
    @JsonProperty("access")
    private FindUserResponseAccess access;

    public FindUserResponse() {
    }

    public FindUserResponse(UUID id, long createdTimestamp, String username, boolean enabled, boolean totp, boolean emailVerified, String firstName, String lastName, String email, List<String> disableableCredentialTypes, List<String> requiredActions, int notBefore, FindUserResponseAccess access) {
        this.id = id;
        this.createdTimestamp = createdTimestamp;
        this.username = username;
        this.enabled = enabled;
        this.totp = totp;
        this.emailVerified = emailVerified;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.disableableCredentialTypes = disableableCredentialTypes;
        this.requiredActions = requiredActions;
        this.notBefore = notBefore;
        this.access = access;
    }
}
