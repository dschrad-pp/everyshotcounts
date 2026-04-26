package com.lektralabs.thrones.pallbearer.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDrillMediaRequest {

    private UUID mediaId;
}
