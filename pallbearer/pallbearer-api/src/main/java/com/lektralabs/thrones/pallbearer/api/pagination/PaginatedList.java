package com.lektralabs.thrones.pallbearer.api.pagination;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement
public class PaginatedList<E> {

    private final Pagination pagination;

    private final List<E> items;

    public PaginatedList(Pagination pagination, List<E> items){
        this.pagination = pagination;
        this.items = items;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public List<E> getItems() {
        return items;
    }

}