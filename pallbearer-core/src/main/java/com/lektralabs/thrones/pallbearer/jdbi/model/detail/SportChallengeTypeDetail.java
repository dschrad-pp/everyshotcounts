package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import com.lektralabs.thrones.pallbearer.jdbi.model.item.SportItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportChallengeTypeDetail {

    private UUID sportChallengeTypeId;
    private SportItem sportItem;
    private String sportChallengeCode;
    private String name;
    private String description;
}
