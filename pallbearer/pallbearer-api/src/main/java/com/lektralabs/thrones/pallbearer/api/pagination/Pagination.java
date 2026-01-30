package com.lektralabs.thrones.pallbearer.api.pagination;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@XmlRootElement
@Data
@Builder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class Pagination implements PaginationConstants {
    // Example: the are 126 users, we want to view them 20 at a time.
    // The first page, i.e . page 0 [0 indexed], is the first twenty. In this case:
    // pageNum =   0 (0th block of twenty)
    // offset  =   0 (don't skip any)
    // limit   =  20 (take 20 total)
    // count   = 126 (total number)
    // in this example, we would have 7 total pages, the last one having 6 users
    // pageNum and offset change per page, but count and limit should be fixed

    // index of page we are on, 0 based
    private int pageNum;

    // same as in the SQL sense
    private int limit;

    // same as in SQL sense
    private long count;

    @Deprecated
    public static Pagination getPagination(final Integer pageParam, final Integer limitParam, final Integer countParam){
        Integer page = Optional.ofNullable(pageParam).orElse(DEFAULT_PAGE);
        Integer limit = Optional.ofNullable(limitParam).orElse(DEFAULT_LIMIT);
        Integer count = Optional.ofNullable(countParam).orElse(DEFAULT_COUNT);
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        return new Pagination(page, limit, count);
    }


    public int getOffset() {
        return pageNum * limit;
    }


}
