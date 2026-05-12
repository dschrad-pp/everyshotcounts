package com.lektralabs.thrones.pallbearer.jdbi.model.generated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TagRow {
    private UUID id;
    private UUID tagCategoryId;
    private String code;
    private String name;
    private Integer displayOrder;
    private UUID drillItemId;
}
