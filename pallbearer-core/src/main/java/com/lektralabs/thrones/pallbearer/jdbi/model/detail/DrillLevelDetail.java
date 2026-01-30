package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;

import java.util.List;

public class DrillLevelDetail {

    private Integer levelIndex;

    private DrillGroupRow drillGroup;

    private List<DrillItemDetail> drillItemDetails;
}
