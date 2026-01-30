package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.SportChallengeTypeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.SportItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.SportItemKey;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.UUID;
import java.util.stream.Stream;

public class SportChallengeTypeDetailRowReducer implements RowReducer<SportChallengeTypeDetailAccumulator, SportChallengeTypeDetail> {

    @Override
    public SportChallengeTypeDetailAccumulator container() {
        return new SportChallengeTypeDetailAccumulator();
    }

    @Override
    public void accumulate(SportChallengeTypeDetailAccumulator container, RowView rowView) {
        UUID sportChallengeTypeId = rowView.getColumn("sct_sportChallengeTypeId", UUID.class);
        UUID sportId = rowView.getColumn("sp_sportId", UUID.class);

        final SportChallengeTypeDetail detail = container.sportChallengeTypeDetailMap.computeIfAbsent(sportChallengeTypeId, pid ->
                rowView.getRow(SportChallengeTypeDetail.class)
        );
        if (sportId != null) {
            final SportItemKey sportItemKey = new SportItemKey(sportChallengeTypeId, sportId);
            final SportItem sportItem = container.sportItemMap.computeIfAbsent(sportItemKey, id -> {
                SportItem item = rowView.getRow(SportItem.class);
                detail.setSportItem(item);
                return item;
            });
        }
    }

    @Override
    public Stream<SportChallengeTypeDetail> stream(SportChallengeTypeDetailAccumulator container) {
        return container.sportChallengeTypeDetailMap.values().stream();
    }
}
