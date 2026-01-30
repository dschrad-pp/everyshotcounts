package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.AthleteDrillDetailKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.DrillGroupKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.DrillKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;

import java.util.*;

public class AthleteDrillDetailAccumulator {

    final List<AthleteDrillDetail> athleteDrillDetailList;

    final List<AthleteDrillDetailKey> athleteDrillDetailKeyList;

    final Map<AthleteDrillDetailKey, AthleteDrillDetail> athleteDrillDetailMap;

    final Map<DrillKey, DrillDetail> drillDetailMap;

    final Map<DrillGroupKey, DrillGroupRow> drillGroupMap;

    final Map<UserDetailKey, UserDetail> createdByUserMap;

    final Map<UUID, ContactItem> createdByContactMap;

    final Map<UserDetailKey, UserDetail> modifiedByUserMap;

    final Map<UUID, ContactItem> modifiedByContactMap;

    public AthleteDrillDetailAccumulator() {
        this.athleteDrillDetailList = new ArrayList<>();
        this.athleteDrillDetailKeyList = new ArrayList<>();
        this.athleteDrillDetailMap = new HashMap<>();
        this.drillDetailMap = new HashMap<>();
        this.drillGroupMap = new HashMap<>();
        this.createdByUserMap = new HashMap<>();
        this.createdByContactMap = new HashMap<>();
        this.modifiedByUserMap = new HashMap<>();
        this.modifiedByContactMap = new HashMap<>();
    }
}
