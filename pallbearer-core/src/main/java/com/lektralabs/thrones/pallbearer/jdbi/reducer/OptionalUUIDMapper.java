package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class OptionalUUIDMapper implements ColumnMapper<Optional<UUID>> {

    @Override
    public Optional<UUID> map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
        UUID value = rs.getObject(columnNumber, UUID.class);
        return Optional.ofNullable(value);
    }
}
