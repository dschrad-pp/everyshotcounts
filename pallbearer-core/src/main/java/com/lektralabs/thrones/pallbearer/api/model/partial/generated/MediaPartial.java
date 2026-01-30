package com.lektralabs.thrones.pallbearer.api.model.partial.generated;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.common.StatusCode;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.entity.EntityMethods;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaRow;
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
@Builder(toBuilder = true)
public class MediaPartial implements Serializable, EntityMethods {

    private Optional<UUID> mediaId;
    private Optional<String> name;
    private Optional<String> description;
    private Optional<String> contentUrl;
    private String statusCode;
    private Optional<String> mimeType;
    private Optional<Integer> version;

    public MediaRow toRow(CurrentUser auditUser) {
        long now = DateTimeUtils.now().getMillis();
        return MediaRow.builder()
                .id(optionalFactory(mediaId, UUID.randomUUID()))
                .name(name)
                .description(description)
                .contentUrl(contentUrl)
                .statusCode(statusCode)
                .mimeType(mimeType)
                .creationDate(now)
                .modificationDate(now)
                .createdById(auditUser.getId())
                .modifiedById(auditUser.getId())
                .version(optionalFactory(version, 1))
                .build();
    }

}
