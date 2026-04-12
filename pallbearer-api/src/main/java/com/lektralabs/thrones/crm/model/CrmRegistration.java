package com.lektralabs.thrones.crm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmRegistration {
    @JsonProperty("registrationId")
    private Long registrationId;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("role")
    private String role;
    
    @JsonProperty("teamId")
    private UUID teamId;

    @JsonProperty("teamName")
    private String teamName;

    @JsonProperty("paymentStatus")
    private String paymentStatus;
    
    @JsonProperty("subscriptionStartDate")
    private Long subscriptionStartDate;
    
    @JsonProperty("subscriptionEndDate")
    private Long subscriptionEndDate;
    
    @JsonProperty("lastUpdated")
    private Long lastUpdated;
}
