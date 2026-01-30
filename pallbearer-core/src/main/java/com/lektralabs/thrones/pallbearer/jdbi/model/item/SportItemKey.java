package com.lektralabs.thrones.pallbearer.jdbi.model.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class SportItemKey {
    private UUID parentId;
    private UUID sportItemId;
}
