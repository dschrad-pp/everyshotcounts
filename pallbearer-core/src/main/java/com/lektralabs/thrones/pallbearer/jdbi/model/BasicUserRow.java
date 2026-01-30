package com.lektralabs.thrones.pallbearer.jdbi.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasicUserRow {
    private UUID id;
    private String email;
    private String username;
    private UUID keycloakId;
    private String statusCode;
    private String firstName;
    private String lastName;
    private String telephone;
}

