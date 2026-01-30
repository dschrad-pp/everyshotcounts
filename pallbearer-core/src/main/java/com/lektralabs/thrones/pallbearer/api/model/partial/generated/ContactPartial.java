package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ContactRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// This is generated code. If you modify it (and you are welcome to), please
// move out of this package and remove this comment.

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContactPartial implements Serializable, EntityMethods {

    private Optional<UUID> contactId;
    private String contactType;
    private String firstName;
    private Optional<String> middleName;
    private String lastName;
    private String email;
    private String telephone;
    private Long birthDate;
    private String verificationCode;
    private Optional<Integer> version;

    public ContactRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return  ContactRow.builder()
                .id(optionalFactory(contactId, UUID.randomUUID()))
                .contactType(contactType)
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .email(email)
                .telephone(telephone)
                .birthDate(birthDate)
                .verificationCode(verificationCode)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .build();
    }


}