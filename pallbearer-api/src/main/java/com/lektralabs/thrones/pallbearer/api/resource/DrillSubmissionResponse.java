package com.lektralabs.thrones.pallbearer.api.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillSubmissionResponse {
    private UUID drillId;
    private UUID drillItemId;
    private UUID mediaId;
    private UUID createdById;
}
