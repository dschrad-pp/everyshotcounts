package com.lektralabs.thrones.pallbearer.api.model.partial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserPartial {
    private String username;

    private String password;

    private String email;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private Long birthDate;

    private String role;

    private UUID teamId;
}
