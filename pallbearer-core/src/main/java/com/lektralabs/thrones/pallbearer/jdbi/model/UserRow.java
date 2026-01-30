package com.lektralabs.thrones.pallbearer.jdbi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRow implements Serializable {

    private UUID id;
    private String email;
    private String userAlias;
    private String username;
    private UUID contactId;
    private UUID keycloakId;
    private byte[] avatarByteArray;
    private String avatarMimeType;
    private String statusCode;
    private Long creationDate;
    private Long modificationDate;
    private UUID createdById;
    private UUID modifiedById;
    private Integer version;
    private static final long serialVersionUID = 1L;

}