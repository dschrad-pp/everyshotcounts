package com.lektralabs.thrones.pallbearer.jdbi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserPropertyRow implements Serializable {

    private UUID id;
    private UUID userId;
    private String propertyKey;
    private String propertyValue;
    private static final long serialVersionUID = 1L;

}