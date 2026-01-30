package com.lektralabs.thrones.pallbearer.api.pagination;

import java.util.List;

public class PaginationResultFactory {

    public static <T> PaginatedList<T> createResults(List<T> items, Pagination _pagination, long _count) {
        Pagination pagination = _pagination.toBuilder().count(_count).build();
        return new PaginatedList<>(pagination, items);
    }

}
