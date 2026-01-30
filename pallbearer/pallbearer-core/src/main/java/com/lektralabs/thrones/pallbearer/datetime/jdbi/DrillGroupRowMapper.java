package com.lektralabs.thrones.pallbearer.datetime.jdbi;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class DrillGroupRowMapper implements RowMapper<DrillGroupRow> {

    private final String prefix;

    public DrillGroupRowMapper(String prefix) {
        this.prefix = prefix;
    }

    public DrillGroupRowMapper() {
        this(""); // No prefix constructor
    }

    @Override
    public DrillGroupRow map(ResultSet rs, StatementContext ctx) throws SQLException {
        // Crucial null check for the nested object:
        // If the ID column of the joined group is null, the entire DrillGroup object should be null.
        // This is especially important if you were using a LEFT JOIN in your SQL.
        // Even with INNER JOIN, if data is bad, this provides a safeguard.
        UUID drillGroupId = (UUID) rs.getObject(prefix + "id");
        if (drillGroupId == null) {
            return null; // No DrillGroup found for this DrillItem
        }

        return DrillGroupRow.builder()
                .id(drillGroupId)
                .teamId(rs.getObject(prefix + "teamId", UUID.class)) // Use getObject(name, type) for UUIDs
                .name(Optional.ofNullable(rs.getString(prefix + "name")))
                .description(Optional.ofNullable(rs.getString(prefix + "description")))
                .drillGroupOrder(rs.getObject(prefix + "drillGroupOrder", Integer.class))
                .drillGroupTypeCode(rs.getString(prefix + "drillGroupTypeCode"))
                .creationDate(rs.getObject(prefix + "creationDate", Long.class))
                .modificationDate(rs.getObject(prefix + "modificationDate", Long.class))
                .createdById(rs.getObject(prefix + "createdById", UUID.class))
                .modifiedById(rs.getObject(prefix + "modifiedById", UUID.class))
                .version(rs.getObject(prefix + "version", Integer.class))
                .build();
    }
}
