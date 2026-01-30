package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.AthleteDrillDetailKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.DrillGroupKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.DrillKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class AthleteDrillDetailRowReducer implements RowReducer<AthleteDrillDetailAccumulator, AthleteDrillDetail> {

    @Override
    public AthleteDrillDetailAccumulator container() {
        return new AthleteDrillDetailAccumulator();
    }

    @Override
    public void accumulate(AthleteDrillDetailAccumulator container, RowView rowView) {
        UUID drillItemId  = rowView.getColumn("di_drillItemId", UUID.class);
        UUID drillGroupId = rowView.getColumn("dg_id", UUID.class);
        UUID drillRowId   = rowView.getColumn("dr_id", UUID.class);
        UUID createdByUserId     = rowView.getColumn("cu_userId", UUID.class);
        UUID createdByContactId  = rowView.getColumn("cc_id", UUID.class);
        UUID modifiedByUserId    = rowView.getColumn("mu_userId", UUID.class);
        UUID modifiedByContactId = rowView.getColumn("mc_id", UUID.class);

        final AthleteDrillDetailKey athleteDrillDetailKey = new AthleteDrillDetailKey(
                drillItemId, Optional.ofNullable(drillRowId));

        final AthleteDrillDetail athleteDrillDetail = container.athleteDrillDetailMap
                .computeIfAbsent(athleteDrillDetailKey, key ->
                        rowView.getRow(AthleteDrillDetail.class));

        container.athleteDrillDetailKeyList.add(athleteDrillDetailKey);

        if ( drillGroupId != null ) {
            final DrillGroupKey drillGroupKey = new DrillGroupKey(
                    drillGroupId, drillItemId);
            final DrillGroupRow drillGroupRow = container.drillGroupMap
                    .computeIfAbsent(drillGroupKey, key ->
                        rowView.getRow(DrillGroupRow.class));
            athleteDrillDetail.setDrillGroup(drillGroupRow);
        }

        if ( drillRowId != null ) {

            final DrillKey drillKey = new DrillKey(drillRowId, drillItemId);
            final DrillDetail drillDetail = container.drillDetailMap
                    .computeIfAbsent(drillKey, key ->
                        rowView.getRow(DrillDetail.class));

            if (createdByUserId != null) {
                final UserDetailKey userDetailKey = new UserDetailKey(createdByUserId, drillItemId);
                final UserDetail createdByUserDetail = container.createdByUserMap
                        .computeIfAbsent(userDetailKey, cUserId -> {
                            UserDetail ud = rowView.getRow(UserDetail.class);
                            if (createdByContactId != null) {
                                ud.setContactItem(rowView.getRow(ContactItem.class));
                            }
                            return ud;
                        });
                drillDetail.setCreatedByUserDetail(createdByUserDetail);
            }

            if (modifiedByUserId != null) {
                final UserDetailKey userDetailKey = new UserDetailKey(modifiedByUserId, drillItemId);
                final UserDetail modifiedByUserDetail = container.modifiedByUserMap
                        .computeIfAbsent(userDetailKey, cUserId -> {
                            UserDetail ud = rowView.getRow(UserDetail.class);
                            if (modifiedByContactId != null) {
                                ud.setContactItem(rowView.getRow(ContactItem.class));
                            }
                            return ud;
                        });
                drillDetail.setModifiedByUserDetail(modifiedByUserDetail);
            }

            athleteDrillDetail.setDrillDetail(Optional.of(drillDetail));

        } else {
            athleteDrillDetail.setDrillDetail(Optional.empty());
        }
    }

    @Override
    public Stream<AthleteDrillDetail> stream(AthleteDrillDetailAccumulator container) {
        // stream values in the order the keys were added to the key list
        return container.athleteDrillDetailKeyList.stream()
                .map(container.athleteDrillDetailMap::get);
    }
}
