package com.lektralabs.thrones.pallbearer.api.model.display;

import java.util.HashMap;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class CurrentUser {

    private UUID id;

    private UUID keycloakId;

    private UUID organizationId;

    private UUID teamId;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String defaultRole;

    private String role;

    private String registrationStep;

    private Map<String, Object> metadata;

    private Map<String, String> userProperties;

    private Map<String, Map<String, String>> groupProperties;

    // Flattened group properties for easier access
    private Map<String, String> beginnerGroupProperties;
    private Map<String, String> intermediateGroupProperties;
    private Map<String, String> advanceGroupProperties;
    private Map<String, String> eliteGroupProperties;

    public CurrentUser() {
    }

    public CurrentUser(UUID id, UUID keycloakId, UUID organizationId, UUID teamId, String username,
            String email, String firstName, String lastName, String defaultRole, String role,
            String registrationStep, Map<String, Object> metadata, Map<String, String> userProperties,
            Map<String, Map<String, String>> groupProperties,
            Map<String, String> beginnerGroupProperties,
            Map<String, String> intermediateGroupProperties,
            Map<String, String> advanceGroupProperties,
            Map<String, String> eliteGroupProperties) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.organizationId = organizationId;
        this.teamId = teamId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.defaultRole = defaultRole;
        this.role = role;
        this.registrationStep = registrationStep;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.userProperties = userProperties != null ? userProperties : new HashMap<>();
        this.groupProperties = groupProperties != null ? groupProperties : new HashMap<>();
        this.beginnerGroupProperties = beginnerGroupProperties != null ? beginnerGroupProperties : new HashMap<>();
        this.intermediateGroupProperties = intermediateGroupProperties != null ? intermediateGroupProperties : new HashMap<>();
        this.advanceGroupProperties = advanceGroupProperties != null ? advanceGroupProperties : new HashMap<>();
        this.eliteGroupProperties = eliteGroupProperties != null ? eliteGroupProperties : new HashMap<>();
    }

}
