package com.lektralabs.thrones.pallbearer.jdbi.reducer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps a JSON string column to Map&lt;String, Object&gt; for metadata storage.
 */
public class JsonMetadataColumnMapper implements ColumnMapper<Map<String, Object>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public Map<String, Object> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        String json = r.getString(columnNumber);
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(json, MAP_TYPE);
            return parsed != null ? parsed : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
