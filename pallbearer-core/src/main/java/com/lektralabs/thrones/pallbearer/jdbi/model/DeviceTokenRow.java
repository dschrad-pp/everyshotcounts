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
public class DeviceTokenRow implements Serializable {

    private UUID id;
    private UUID userId;
    private String token;
    private String platform;
    private Long createdAt;
    private Long updatedAt;
    private static final long serialVersionUID = 1L;

}
