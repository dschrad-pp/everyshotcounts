package com.lektralabs.thrones.pallbearer.jdbi.model.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactItem {

    private UUID id;

    private String contactType;

    private String firstName;

    private String middleName;

    private String lastName;

    private String email;

    private Long birthDate;

    private Integer version;
}
