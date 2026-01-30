package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.DrillGroupKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DrillItemDetailAccumulator {

    final Map<UUID, DrillItemDetail> drillItemDetailMap;

    final Map<DrillGroupKey, DrillGroupRow> drillGroupMap;

    final Map<UserDetailKey, UserDetail> createdByUserMap;

    final Map<UUID, ContactItem> createdByContactMap;

    final Map<UserDetailKey, UserDetail> modifiedByUserMap;

    final Map<UUID, ContactItem> modifiedByContactMap;

    public DrillItemDetailAccumulator() {
        this.drillItemDetailMap = new HashMap<>();
        this.drillGroupMap = new HashMap<>();
        this.createdByUserMap = new HashMap<>();
        this.createdByContactMap = new HashMap<>();
        this.modifiedByUserMap = new HashMap<>();
        this.modifiedByContactMap = new HashMap<>();
    }
}
