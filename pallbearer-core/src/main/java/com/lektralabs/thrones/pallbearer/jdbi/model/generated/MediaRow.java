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
public class MediaRow implements Serializable {

    private UUID id;
    private Optional<String> name;
    private Optional<String> description;
    // private Optional<byte[]> byteArray;
    private Optional<String> contentUrl;
    private String statusCode;
    private Optional<String> mimeType;
    private Long creationDate;
    private Long modificationDate;
    private UUID createdById;
    private UUID modifiedById;
    private Integer version;
    private static final long serialVersionUID = 1L;

}
