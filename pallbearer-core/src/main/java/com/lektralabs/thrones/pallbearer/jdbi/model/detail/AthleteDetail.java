package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AthleteDetail {

    private UUID userId;

    private String email;

    private String userAlias;

    private String username;

    private ContactItem contactItem;

    private Map<String, String> userProperties;

    private Map<String, Map<String, String>> groupProperties;
}
