package com.lektralabs.thrones.pallbearer.api.model.partial;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

// This is generated code. If you modify it (and you are welcome to), please
// move out of this package and remove this comment.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPartial implements Serializable, EntityMethods {

    private Optional<UUID> userId;
    private String email;
    private String userAlias;
    private String biography;
    private String username;
    private UUID contactId;
    private byte[] avatarByteArray;
    private String avatarMimeType;
    private Integer version;
    private UUID keycloakId;

    public UserRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  UserRow.builder()
                .id(optionalFactory(userId, UUID.randomUUID()))
                .email(email)
                .userAlias(userAlias)
                .username(username)
                .contactId(contactId)
                .avatarByteArray(avatarByteArray)
                .avatarMimeType(avatarMimeType)
                .statusCode(StatusCode.ACTIVE.toString())
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(version)
                .keycloakId(keycloakId)
                .build();
    }


}