package com.cristiannustes.domain.user;

import java.util.List;

public record Page<T>(List<T> items, int page, int size, long totalItems) {

    public Page {
        items = List.copyOf(items);
    }

    public long totalPages() {
        return size == 0 ? 0 : (totalItems + size - 1) / size;
    }
}
