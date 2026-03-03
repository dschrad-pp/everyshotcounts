package com.lektralabs.thrones.pallbearer.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request model for updating user profile metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    /**
     * JSON object for user metadata. Stored as-is in DB. Replaces existing when provided.
     */
    private Map<String, Object> metadata;
}
