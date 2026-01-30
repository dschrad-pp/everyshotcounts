package com.lektralabs.thrones.pallbearer.api.model.response;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for drill list with total count
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillListResponse {
    
    /**
     * List of drill items
     */
    private List<DrillItemDetail> drills;
    
    /**
     * Total count of drills
     */
    private int totalCount;
    
    /**
     * Level name (Beginner, Intermediate, Advanced, Elite)
     */
    private String level;
}

