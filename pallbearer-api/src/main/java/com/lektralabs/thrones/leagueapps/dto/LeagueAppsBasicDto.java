package com.lektralabs.thrones.leagueapps.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LeagueAppsBasicDto {
    String name;
    String email;
    String phone;
    String username;
    String keycloakId;
    String statusCode;
    String actionCode;
    String paymentStatus;
}

