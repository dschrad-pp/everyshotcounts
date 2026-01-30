package com.lektralabs.thrones.pallbearer.jdbi.factory;

import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import com.lektralabs.thrones.pallbearer.common.CoreConstants;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.security.SecurityUtils;
import org.joda.time.DateTimeUtils;

import java.util.UUID;

public class UserFactory implements CoreConstants {

    public static UserRow factoryCreateUser(UUID userId, UUID contactId, UUID keycloakId, RegisterUserPartial registerUserPartial) {
        long now = DateTimeUtils.currentTimeMillis();
        String salt = SecurityUtils.createSalt();
        String encPassword = SecurityUtils.hash(registerUserPartial.getPassword(), salt);

        // Username is stored in lowercase for consistency (both in database and Keycloak)
        String username = registerUserPartial.getUsername() != null ? registerUserPartial.getUsername().toLowerCase() : null;

        return UserRow.builder()
                .id(userId)
                .email(registerUserPartial.getEmail())
                .userAlias(username)
                .username(username)
                .contactId(contactId)
                .keycloakId(keycloakId)
                .statusCode(StatusCode.ACTIVE.toString())
                .creationDate(now)
                .modificationDate(now)
                .createdById(SYSTEM_USER_ID)
                .modifiedById(SYSTEM_USER_ID)
                .version(0)
                .build();
    }

}
