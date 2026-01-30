package com.lektralabs.thrones.pallbearer.api.model.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List; // Import List
import java.util.Objects;

public class OpenIdResponse {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expires_in")
    private int expiresIn;
    @JsonProperty("refresh_expires_in")
    private int refreshExpiresIn;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("not-before-policy")
    private int notBeforePolicy;
    @JsonProperty("session_state")
    private String sessionState;
    @JsonProperty("scope")
    private String scope;
    // New field for roles
    @JsonProperty("roles")
    String role; // Add this line

    public OpenIdResponse() {
    }

    // Update the constructor to include roles
    public OpenIdResponse(String accessToken, int expiresIn, int refreshExpiresIn, String refreshToken, String tokenType, int notBeforePolicy, String sessionState, String scope, String role) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.notBeforePolicy = notBeforePolicy;
        this.sessionState = sessionState;
        this.scope = scope;
        this.role = role; // Initialize roles
    }

    public String getAccessToken() {
        return accessToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public int getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public int getNotBeforePolicy() {
        return notBeforePolicy;
    }

    public String getSessionState() {
        return sessionState;
    }

    public String getScope() {
        return scope;
    }

    // New getter for roles
    public String getRole() {
        return role;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setRefreshExpiresIn(int refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public void setNotBeforePolicy(int notBeforePolicy) {
        this.notBeforePolicy = notBeforePolicy;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    // New setter for roles
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenIdResponse that = (OpenIdResponse) o;
        return expiresIn == that.expiresIn
                && refreshExpiresIn == that.refreshExpiresIn
                && notBeforePolicy == that.notBeforePolicy
                && Objects.equals(accessToken, that.accessToken)
                && Objects.equals(refreshToken, that.refreshToken)
                && Objects.equals(tokenType, that.tokenType)
                && Objects.equals(sessionState, that.sessionState)
                && Objects.equals(scope, that.scope)
                && Objects.equals(role, that.role); // Include roles in equals
    }

    @Override
    public int hashCode() {
        // Include roles in hashCode
        return Objects.hash(accessToken, expiresIn, refreshExpiresIn, refreshToken, tokenType, notBeforePolicy, sessionState, scope, role);
    }

    @Override
    public String toString() {
        return "OpenIdResponse{"
                + "accessToken='" + accessToken + '\''
                + ", expiresIn=" + expiresIn
                + ", refreshExpiresIn=" + refreshExpiresIn
                + ", refreshToken='" + refreshToken + '\''
                + ", tokenType='" + tokenType + '\''
                + ", notBeforePolicy=" + notBeforePolicy
                + ", sessionState='" + sessionState + '\''
                + ", scope='" + scope + '\''
                + ", role=" + role
                + // Include roles in toString
                '}';
    }
}
