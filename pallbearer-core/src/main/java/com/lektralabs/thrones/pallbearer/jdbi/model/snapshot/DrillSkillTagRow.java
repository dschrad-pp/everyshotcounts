package com.lektralabs.thrones.pallbearer.jdbi.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrillSkillTagRow {
    private UUID drillItemId;
    private String tagCode;
    private String tagName;
}
