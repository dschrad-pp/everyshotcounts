package com.lektralabs.thrones.pallbearer.jdbi.model.key;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;
import java.util.UUID;

/**
 * In the contexts where we are requesting and reducing athlete drill detail
 * data we are either looking for the drill details for a single athlete or
 * we are looking across athletes for drill details in a timeline
 *
 * A key for an instance of an athlete drill detail is a combination of the
 * drill item ID and an optional drill ID: The athlete may or may not have a
 * drill for the associated drill item This should be enough to uniquely
 * identify an athlete drill detail in all required context.
 *
 */
@Data
@Builder
@AllArgsConstructor
public class AthleteDrillDetailKey {
    private UUID drillItemId;
    private Optional<UUID> drillId;
}
