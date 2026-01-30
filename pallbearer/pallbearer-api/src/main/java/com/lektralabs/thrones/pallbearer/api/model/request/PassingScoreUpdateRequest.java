package com.lektralabs.thrones.pallbearer.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request model for bulk updating passing scores
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassingScoreUpdateRequest {
    
    /**
     * List of drill updates
     */
    private List<DrillUpdate> updates;
    
    /**
     * Matching options for finding drills
     */
    private MatchOptions options;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrillUpdate {
        /**
         * Level name: Beginner, Intermediate, Advanced, or Elite
         */
        private String level;
        
        /**
         * Level index inside the group (e.g., 1, 2, 3)
         * Optional - if provided, updates will be restricted to this level index
         */
        private Integer levelIndex;
        
        /**
         * Drill name
         */
        private String name;
        
        /**
         * Drill description
         */
        private String description;
        
        /**
         * New passing score value
         */
        private Integer passingScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchOptions {
        /**
         * Whether to match by name
         */
        private Boolean matchByName;
        
        /**
         * Whether to match by description
         */
        private Boolean matchByDescription;
        
        /**
         * Whether matching is case sensitive
         */
        private Boolean caseSensitive;
        
        /**
         * Whether to allow partial matches (LIKE instead of =)
         */
        private Boolean allowPartialMatch;
    }
}

