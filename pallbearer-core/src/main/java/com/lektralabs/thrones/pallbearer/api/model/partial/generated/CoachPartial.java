package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoachPartial implements Serializable {

    private UUID id;
    private String email;
    private String userAlias;
    private String username;
    private UUID contactId;
    private byte[] avatarByteArray;
    private String avatarMimeType;
    private Integer version;
    private UUID keycloakId;

    private String firstName;
    private String lastName;
    private String roleName;

    private static final long serialVersionUID = 1L;

}
