package com.lektralabs.thrones.pallbearer.jdbi.model.generated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// This is generated code. If you modify it (and you are welcome to), please
// move out of this package and remove this comment.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeTokenRow implements Serializable {

    private UUID id;
    private UUID challengeId;
    private UUID userId;
    private String tokenStatusCode;
    private String tokenTypeCode;
    private static final long serialVersionUID = 1L;

}