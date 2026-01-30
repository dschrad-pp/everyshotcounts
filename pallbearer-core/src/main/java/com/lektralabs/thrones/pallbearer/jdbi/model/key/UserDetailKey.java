package com.lektralabs.thrones.pallbearer.jdbi.model.key;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class UserDetailKey {
    private UUID userId;
    private UUID parentId;
}
