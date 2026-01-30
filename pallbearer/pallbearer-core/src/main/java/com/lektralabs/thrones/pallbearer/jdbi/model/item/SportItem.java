package com.lektralabs.thrones.pallbearer.jdbi.model.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportItem {

    private UUID id;
    private String name;
    private String sportCode;
}
