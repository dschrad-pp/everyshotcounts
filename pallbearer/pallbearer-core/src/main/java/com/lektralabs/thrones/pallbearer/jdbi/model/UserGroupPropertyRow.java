package com.lektralabs.thrones.pallbearer.jdbi.model;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupPropertyRow implements Serializable {

    private UUID id;
    private UUID userId;
    private UUID drillGroupId;
    private String propertyKey;
    private String propertyValue;
    private static final long serialVersionUID = 1L;
}
