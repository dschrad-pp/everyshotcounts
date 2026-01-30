package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.SportChallengeTypeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ChallengeEntryItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.SportItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.ChallengeEntryItemKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.SportChallengeTypeKey;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.UUID;
import java.util.stream.Stream;

public class ChallengeDetailRowReducer implements RowReducer<ChallengeDetailAccumulator, ChallengeDetail> {

    @Override
    public ChallengeDetailAccumulator container() {
        return new ChallengeDetailAccumulator();
    }

    @Override
    public void accumulate(ChallengeDetailAccumulator container, RowView rowView) {
        UUID challengeId = rowView.getColumn("c_challengeId", UUID.class);
        UUID challengeEntryId = rowView.getColumn("ce_id", UUID.class);
        UUID createdByUserId = rowView.getColumn("cu_userId", UUID.class);
        UUID createdByContactId = rowView.getColumn("cc_id", UUID.class);
        UUID modifiedByUserId = rowView.getColumn("mu_userId", UUID.class);
        UUID modifiedByContactId = rowView.getColumn("mc_id", UUID.class);
        UUID sportChallengeTypeId = rowView.getColumn("sct_sportChallengeTypeId", UUID.class);
        UUID sportId = rowView.getColumn("s_id", UUID.class);

        final ChallengeDetail challengeDetail = container.challengeDetailMap
                .computeIfAbsent(challengeId, cId ->
                        rowView.getRow(ChallengeDetail.class)
                );

        if (sportChallengeTypeId != null) {
            final SportChallengeTypeKey sportChallengeTypeKey = new SportChallengeTypeKey(sportChallengeTypeId, challengeId);
            final SportChallengeTypeDetail sportChallengeTypeDetail = container.sportChallengeTypeDetailMap
                    .computeIfAbsent(sportChallengeTypeKey, sctId -> {
                        SportChallengeTypeDetail sctDetail = rowView.getRow(SportChallengeTypeDetail.class);
                        if ( sportId != null ) {
                            sctDetail.setSportItem(rowView.getRow(SportItem.class));
                        }
                        challengeDetail.setSportChallengeTypeDetail(sctDetail);
                        return sctDetail;
                    });
        }

        if ( challengeEntryId != null ) {
            final ChallengeEntryItemKey challengeEntryItemKey = new ChallengeEntryItemKey(challengeEntryId, challengeId);
            final ChallengeEntryItem challengeEntryItem = container.challengeEntryItemMap
                    .computeIfAbsent(challengeEntryItemKey, ceId -> {
                        ChallengeEntryItem ceItem = rowView.getRow(ChallengeEntryItem.class);
                        challengeDetail.setChallengeEntryItem(ceItem);
                        return ceItem;
                    });
        }

        if (createdByUserId != null) {
            final UserDetailKey userDetailKey = new UserDetailKey(createdByUserId, challengeId);
            final UserDetail createdByUserDetail = container.createdByUserMap
                    .computeIfAbsent(userDetailKey, cUserId -> {
                        UserDetail ud = rowView.getRow(UserDetail.class);
                        if (createdByContactId != null) {
                            ud.setContactItem(rowView.getRow(ContactItem.class));
                        }
                        challengeDetail.setCreatedByUserDetail(ud);
                        return ud;
                    });
        }

        if (modifiedByUserId != null) {
            final UserDetailKey userDetailKey = new UserDetailKey(modifiedByUserId, challengeId);
            final UserDetail modifiedByUserDetail = container.modifiedByUserMap
                    .computeIfAbsent(userDetailKey, cUserId -> {
                        UserDetail ud = rowView.getRow(UserDetail.class);
                        if (modifiedByContactId != null) {
                            ud.setContactItem(rowView.getRow(ContactItem.class));
                        }
                        challengeDetail.setModifiedByUserDetail(ud);
                        return ud;
                    });
        }
    }

    @Override
    public Stream<ChallengeDetail> stream(ChallengeDetailAccumulator container) {
        return container.challengeDetailMap.values().stream();
    }
}
