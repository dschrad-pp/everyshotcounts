package com.lektralabs.thrones.pallbearer.api.util;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder(toBuilder = true)
public class FindOptions {
    private int limit;
    private int offset;
    private Optional<String> orderBy;
    private String direction;

    public FindOptions(int limit, int offset, Optional<String> orderBy, String direction) {
        this.limit = limit;
        this.offset = offset;
        this.orderBy = orderBy;
        this.direction = direction;
    }

    public FindOptions() {
        this(40, 0, Optional.empty(), "ASC");
    }

    public FindOptions(UriInfo uriInfo) {
        this(uriInfo.getQueryParameters());
    }

    public FindOptions(MultivaluedMap<String, String> queryParams) {
        this(
                getQueryInt(queryParams, "limit", 40),
                getQueryInt(queryParams, "offset", 0),
                Optional.ofNullable(queryParams.getFirst("orderBy")),
                Optional.ofNullable(queryParams.getFirst("dir")).orElse("ASC")
        );
    }

    public static int getQueryInt(MultivaluedMap<String, String> queryParams, String key, int defaultValue) {
        return Optional.ofNullable(queryParams.getFirst(key)).map(Integer::valueOf).orElse(defaultValue);
    }

    public String toLimitOffsetSql() {
        return String.format(" LIMIT %s OFFSET %s ", limit, offset);
    }

    public String getSql() {
        return orderBy.map( order ->
            String.format(" ORDER BY %s %s ", order, direction.toUpperCase())
        ).orElse("") + toLimitOffsetSql();
    }
}
