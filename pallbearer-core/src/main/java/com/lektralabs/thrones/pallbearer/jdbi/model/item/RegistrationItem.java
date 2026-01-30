package com.lektralabs.thrones.pallbearer.jdbi.model.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationItem implements Serializable {

    private String email;
    private String username;
    private String skillLevel;
    private String sixDigitCode;
    private String password;
    private String confirmPassword;
    private String ageAcknowledgement;

}
