package org.jboss.pnc.bifrost.source.dto;

import org.elasticsearch.search.sort.SortOrder;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public enum Direction {

    ASC(SortOrder.ASC),
    DESC(SortOrder.DESC);

    private SortOrder sortOrder;

    Direction(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
