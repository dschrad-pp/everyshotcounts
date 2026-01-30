package com.lektralabs.thrones.pallbearer.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response model for passing score update operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassingScoreUpdateResponse {
    
    /**
     * Total number of updates requested
     */
    private int totalRequested;
    
    /**
     * Total number of drills successfully updated
     */
    private int totalUpdated;
    
    /**
     * Total number of drills not found
     */
    private int totalNotFound;
    
    /**
     * List of successfully updated drills
     */
    private List<UpdatedDrill> updatedDrills;
    
    /**
     * List of drills that were not found
     */
    private List<NotFoundDrill> notFound;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatedDrill {
        private UUID id;
        private String name;
        private String description;
        private Integer oldPassingScore;
        private Integer newPassingScore;
        private String level;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotFoundDrill {
        private String level;
        private String name;
        private String description;
        private String reason;
    }
}

