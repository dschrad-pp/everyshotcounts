package com.lektralabs.thrones.pallbearer.datetime.jdbi;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class DrillItemDetailMapper implements RowMapper<DrillItemDetail> {

    @Override
    public DrillItemDetail map(ResultSet rs, StatementContext ctx) throws SQLException {
        // Instantiate the nested mapper with the correct prefix
        DrillGroupRow drillGroup = new DrillGroupRowMapper("drillGroup_").map(rs, ctx);

        // TODO: Implement UserDetailMapper if createdByUserDetail and modifiedByUserDetail are also null
        // UserDetail createdByUserDetail = new UserDetailMapper("createdBy_").map(rs, ctx); // Example
        // UserDetail modifiedByUserDetail = new UserDetailMapper("modifiedBy_").map(rs, ctx); // Example
        return DrillItemDetail.builder()
                .id(rs.getObject("id", UUID.class))
                .teamId(rs.getObject("teamId", UUID.class))
                .drillGroup(drillGroup) // This is where the DrillGroupRow is set
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .mediaId(Optional.ofNullable(rs.getObject("mediaId", UUID.class)))
                .mediaStatus(Optional.ofNullable(rs.getString("mediaStatus")))
                .orderIndex(rs.getObject("orderIndex", Integer.class))
                .levelIndex(rs.getObject("levelIndex", Integer.class))
                .levelTest(rs.getObject("levelTest", Boolean.class))
                .drillItemOrder(rs.getObject("drillItemOrder", Integer.class))
                .passingScore(rs.getObject("passingScore", Integer.class))
                .shotsMax(rs.getObject("shotsMax", Integer.class))
                .visibilityCode(rs.getString("visibilityCode"))
                .allowRetryCode(rs.getString("allowRetryCode"))
                .retryMax(rs.getObject("retryMax", Integer.class))
                .timeLimitMs(rs.getObject("timeLimitMs", Long.class))
                .creationDate(rs.getObject("creationDate", Long.class))
                .modificationDate(rs.getObject("modificationDate", Long.class))
                .createdByUserDetail(null) // Placeholder: Implement UserDetailMapper if needed
                .modifiedByUserDetail(null) // Placeholder: Implement UserDetailMapper if needed
                .version(rs.getObject("version", Integer.class))
                .mediaThumbnail(Optional.ofNullable(rs.getString("mediaThumbnail"))) // <-- Reverted to this correct form
                .build();
    }
}
