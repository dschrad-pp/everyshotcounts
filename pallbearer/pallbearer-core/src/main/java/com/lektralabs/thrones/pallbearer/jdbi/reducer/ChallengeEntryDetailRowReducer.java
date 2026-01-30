package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeEntryDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.UUID;
import java.util.stream.Stream;

public class ChallengeEntryDetailRowReducer implements RowReducer<ChallengeEntryDetailAccumulator, ChallengeEntryDetail> {

    @Override
    public ChallengeEntryDetailAccumulator container() {
        return new ChallengeEntryDetailAccumulator();
    }

    @Override
    public void accumulate(ChallengeEntryDetailAccumulator container, RowView rowView) {
        UUID challengeEntryId = rowView.getColumn("ce_challengeEntryId", UUID.class);
        UUID createdByUserId = rowView.getColumn("cu_userId", UUID.class);
        UUID createdByContactId = rowView.getColumn("cc_id", UUID.class);
        UUID modifiedByUserId = rowView.getColumn("mu_userId", UUID.class);
        UUID modifiedByContactId = rowView.getColumn("mc_id", UUID.class);

        final ChallengeEntryDetail challengeEntryDetail = container.challengeEntryDetailMap
                .computeIfAbsent(challengeEntryId, ceId ->
                        rowView.getRow(ChallengeEntryDetail.class)
                );

        if (createdByUserId != null) {
            final UserDetailKey userDetailKey = new UserDetailKey(createdByUserId, challengeEntryId);
            final UserDetail createdByUserDetail = container.createdByUserMap
                    .computeIfAbsent(userDetailKey, cUserId -> {
                        UserDetail ud = rowView.getRow(UserDetail.class);
                        if (createdByContactId != null) {
                            ud.setContactItem(rowView.getRow(ContactItem.class));
                        }
                        challengeEntryDetail.setCreatedByUserDetail(ud);
                        return ud;
                    });
        }

        if (modifiedByUserId != null) {
            final UserDetailKey userDetailKey = new UserDetailKey(modifiedByUserId, challengeEntryId);
            final UserDetail modifiedByUserDetail = container.modifiedByUserMap
                    .computeIfAbsent(userDetailKey, cUserId -> {
                        UserDetail ud = rowView.getRow(UserDetail.class);
                        if (modifiedByContactId != null) {
                            ud.setContactItem(rowView.getRow(ContactItem.class));
                        }
                        challengeEntryDetail.setModifiedByUserDetail(ud);
                        return ud;
                    });
        }
    }

    @Override
    public Stream<ChallengeEntryDetail> stream(ChallengeEntryDetailAccumulator container) {
        return container.challengeEntryDetailMap.values().stream();
    }
}
