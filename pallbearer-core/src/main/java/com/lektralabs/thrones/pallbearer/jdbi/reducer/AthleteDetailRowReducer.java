package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.UUID;
import java.util.stream.Stream;

public class AthleteDetailRowReducer implements RowReducer<AthleteDetailAccumulator, AthleteDetail> {

    @Override
    public AthleteDetailAccumulator container() {
        return new AthleteDetailAccumulator();
    }

    @Override
    public void accumulate(AthleteDetailAccumulator container, RowView rowView) {
        UUID athleteId = rowView.getColumn("au_userId", UUID.class);
        UUID athleteContactId = rowView.getColumn("ac_id", UUID.class);

        if (athleteId != null) {
            final AthleteDetail createdByUserDetail = container.athleteDetailMap
                    .computeIfAbsent(athleteId, aUserId -> {
                        AthleteDetail ad = rowView.getRow(AthleteDetail.class);
                        if (athleteContactId != null) {
                            ad.setContactItem(rowView.getRow(ContactItem.class));
                        }
                        return ad;
                    });
        }
    }

    @Override
    public Stream<AthleteDetail> stream(AthleteDetailAccumulator container) {
        return container.athleteDetailMap.values().stream();
    }
}
