package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.*;
import java.util.stream.Stream;

public class DrillItemDetailRowReducerWithoutUser implements RowReducer<Map<UUID, DrillItemDetail>, DrillItemDetail> {

    @Override
    public Map<UUID, DrillItemDetail> container() {
        return new HashMap<>();
    }

    @Override
    public void accumulate(Map<UUID, DrillItemDetail> container, RowView rowView) {
        UUID drillItemId = rowView.getColumn("di_id", UUID.class);

        DrillItemDetail drillItemDetail = container.computeIfAbsent(drillItemId, id -> {
            return DrillItemDetail.builder()
                    .id(rowView.getColumn("id", UUID.class))
                    .teamId(rowView.getColumn("teamId", UUID.class))
                    .name(rowView.getColumn("name", String.class))
                    .description(rowView.getColumn("description", String.class))
                    .mediaId(Optional.ofNullable(rowView.getColumn("mediaId", UUID.class)))
                    .mediaStatus(Optional.ofNullable(rowView.getColumn("mediaStatus", String.class)))
                    .levelIndex(rowView.getColumn("levelIndex", Integer.class))
                    .levelTest(rowView.getColumn("levelTest", Boolean.class))
                    .drillItemOrder(rowView.getColumn("drillItemOrder", Integer.class))
                    .passingScore(rowView.getColumn("passingScore", Integer.class))
                    .shotsMax(rowView.getColumn("shotsMax", Integer.class))
                    .visibilityCode(rowView.getColumn("visibilityCode", String.class))
                    .allowRetryCode(rowView.getColumn("allowRetryCode", String.class))
                    .retryMax(rowView.getColumn("retryMax", Integer.class))
                    .timeLimitMs(rowView.getColumn("timeLimitMs", Long.class))
                    .creationDate(rowView.getColumn("creationDate", Long.class))
                    .modificationDate(rowView.getColumn("modificationDate", Long.class))
                    .version(rowView.getColumn("version", Integer.class))
                    .mediaThumbnail(Optional.ofNullable(rowView.getColumn("mediaThumbnail", String.class)))
                    .build();
        });

        UUID drillGroupId = rowView.getColumn("drillGroup.id", UUID.class);
        if (drillGroupId != null) {
            DrillGroupRow drillGroup = rowView.getRow(DrillGroupRow.class);
            drillItemDetail.setDrillGroup(drillGroup);
        }
    }

    @Override
    public Stream<DrillItemDetail> stream(Map<UUID, DrillItemDetail> container) {
        return container.values().stream();
    }
}
