package com.lektralabs.thrones.pallbearer.api.model.partial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for bulk drill completion operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCompleteResponse {

    /**
     * List of successfully completed drills with their updated data
     */
    private List<Object> successfulResults;

    /**
     * List of error messages for failed drill completions
     */
    private List<String> errors;

    /**
     * Number of drills that were successfully completed
     */
    private int successCount;

    /**
     * Number of drills that failed to complete
     */
    private int failureCount;

    /**
     * Total number of drills processed
     */
    public int getTotalCount() {
        return successCount + failureCount;
    }
}
