package com.lektralabs.thrones.pallbearer.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request model for completing drills by group name and level/order_index
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupLevelCompleteRequest {
    
    /**
     * Group name: BEGINNER, INTERMEDIATE, ADVANCE, or ELITE
     */
    private String groupName;
    
    /**
     * Level index (e.g., 1, 2, 3)
     * Optional - if provided, only drills at this level will be completed
     */
    private Integer levelIndex;
    
    /**
     * Order index (e.g., 1, 2, 3, 4)
     * Optional - if provided, only drills at this order_index will be completed
     * Note: Either levelIndex or orderIndex should be provided, not both
     */
    private Integer orderIndex;
    
    /**
     * Media ID to associate with completed drills
     * Optional - if provided, will be set for all completed drills
     */
    private UUID mediaId;
}
