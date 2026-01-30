package com.lektralabs.thrones.pallbearer.jdbi.model.item;

import com.lektralabs.thrones.pallbearer.api.model.partial.UserPartial;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.EscLeagueAppsMemberActionPartial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.Nested;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscActionItem implements Serializable {

    @Nested("elama")
    EscLeagueAppsMemberActionPartial elama;

    @Nested("user")
    UserPartial user;

}
