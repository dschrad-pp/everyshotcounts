package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeParticipantDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.UserDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.ContactItem;
import com.lektralabs.thrones.pallbearer.jdbi.model.key.UserDetailKey;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.UUID;
import java.util.stream.Stream;

public class ChallengeParticipantDetailRowReducer implements RowReducer<ChallengeParticipantDetailAccumulator, ChallengeParticipantDetail> {

    @Override
    public ChallengeParticipantDetailAccumulator container() {
        return new ChallengeParticipantDetailAccumulator();
    }

    @Override
    public void accumulate(ChallengeParticipantDetailAccumulator container, RowView rowView) {
        UUID challengeParticipantId = rowView.getColumn("cp_challengeParticipantId", UUID.class);
        UUID participantUserId = rowView.getColumn("u_userId", UUID.class);
        UUID participantContactId = rowView.getColumn("c_contactId", UUID.class);

        final ChallengeParticipantDetail challengeParticipantDetail = container.challengeParticipantDetailMap
                .computeIfAbsent(challengeParticipantId, cpId ->
                        rowView.getRow(ChallengeParticipantDetail.class)
                );

        if (participantUserId != null) {
            final UserDetailKey participantUserDetailKey = new UserDetailKey(participantUserId, challengeParticipantId);
            final UserDetail participantUserDetail = container.challengeParticipantUserMap
                    .computeIfAbsent(participantUserDetailKey, userId -> {
                        UserDetail ud = rowView.getRow(UserDetail.class);
                        if (participantContactId != null) {
                            ud.setContactItem(rowView.getRow(ContactItem.class));
                        }
                        challengeParticipantDetail.setParticipantDetail(ud);
                        return ud;
                    });
        }
    }

    @Override
    public Stream<ChallengeParticipantDetail> stream(ChallengeParticipantDetailAccumulator container) {
        return container.challengeParticipantDetailMap.values().stream();
    }
}
