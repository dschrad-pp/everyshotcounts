package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.DrillGroupKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.UUID;
import java.util.stream.Stream;

public class DrillItemDetailRowReducer implements RowReducer<DrillItemDetailAccumulator, DrillItemDetail> {

    @Override
    public DrillItemDetailAccumulator container() {
        return new DrillItemDetailAccumulator();
    }

    @Override
    public void accumulate(DrillItemDetailAccumulator container, RowView rowView) {
        UUID drillItemId = rowView.getColumn("di_id", UUID.class);
        UUID createdByUserId = rowView.getColumn("cu_userId", UUID.class);
        UUID createdByContactId = rowView.getColumn("cc_id", UUID.class);
        UUID modifiedByUserId = rowView.getColumn("mu_userId", UUID.class);
        UUID modifiedByContactId = rowView.getColumn("mc_id", UUID.class);
        UUID drillGroupId = rowView.getColumn("dg_id", UUID.class);

        final DrillItemDetail drillItemDetail = container.drillItemDetailMap
                .computeIfAbsent(drillItemId, diId
                        -> rowView.getRow(DrillItemDetail.class)
                );

        if (drillGroupId != null) {
            final DrillGroupKey drillGroupKey = new DrillGroupKey(drillGroupId, drillItemId);
            final DrillGroupRow DrillGroupRow = container.drillGroupMap
                    .computeIfAbsent(drillGroupKey, dlId -> {
                        DrillGroupRow dgRow = rowView.getRow(DrillGroupRow.class);
                        drillItemDetail.setDrillGroup(dgRow);
                        return dgRow;
                    });
        }

        if (createdByUserId != null) {
            final UserDetailKey userDetailKey = new UserDetailKey(createdByUserId, drillItemId);
            final UserDetail createdByUserDetail = container.createdByUserMap
                    .computeIfAbsent(userDetailKey, cUserId -> {
                        UserDetail ud = rowView.getRow(UserDetail.class);
                        if (createdByContactId != null) {
                            ud.setContactItem(rowView.getRow(ContactItem.class));
                        }
                        drillItemDetail.setCreatedByUserDetail(ud);
                        return ud;
                    });
        }

        if (modifiedByUserId != null) {
            final UserDetailKey userDetailKey = new UserDetailKey(modifiedByUserId, drillItemId);
            final UserDetail modifiedByUserDetail = container.modifiedByUserMap
                    .computeIfAbsent(userDetailKey, cUserId -> {
                        UserDetail ud = rowView.getRow(UserDetail.class);
                        if (modifiedByContactId != null) {
                            ud.setContactItem(rowView.getRow(ContactItem.class));
                        }
                        drillItemDetail.setModifiedByUserDetail(ud);
                        return ud;
                    });
        }
    }

    @Override
    public Stream<DrillItemDetail> stream(DrillItemDetailAccumulator container) {
        return container.drillItemDetailMap.values().stream();
    }
}
