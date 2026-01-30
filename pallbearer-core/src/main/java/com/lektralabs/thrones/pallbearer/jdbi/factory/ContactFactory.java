package com.lektralabs.thrones.pallbearer.jdbi.factory;

import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import com.lektralabs.thrones.pallbearer.common.CoreConstants;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ContactRow;
import org.joda.time.DateTimeUtils;

import java.util.UUID;

public class ContactFactory implements CoreConstants {

    public static ContactRow factoryCreateContact(UUID id, RegisterUserPartial registerUserPartial) {
        long now = DateTimeUtils.currentTimeMillis();
        return ContactRow.builder()
                .id(id)
                .contactType("USER")
                .firstName(registerUserPartial.getFirstName())
                .middleName(null)
                .lastName(registerUserPartial.getLastName())
                .email(registerUserPartial.getEmail())
                .verificationCode("UNVERIFIED")
                .telephone(registerUserPartial.getPhoneNumber())
                .birthDate(registerUserPartial.getBirthDate())
                .creationDate(now)
                .modificationDate(now)
                .createdById(SYSTEM_USER_ID)
                .modifiedById(SYSTEM_USER_ID)
                .version(0)
                .build();
    }

}
