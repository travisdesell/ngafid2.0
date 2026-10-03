package org.ngafid.www;

import java.util.List;

/**
 * A serializable wrapper for one page of results together with the total number of pages in the full result set.
 *
 * <p>Returned by paginated endpoints so clients receive both the current page's items and enough information to
 * render page navigation.
 *
 * @param <T> the type of the items on the page
 */
public class PaginationResponse<T> {
    private List<T> page;
    private int numberPages;

    /**
     * Constructs a paginated response wrapping one page of items together with the total number of pages available.
     *
     * @param page the items on the current page
     * @param numberPages the total number of pages across the full result set
     */
    public PaginationResponse(List<T> page, int numberPages) {
        this.page = page;
        this.numberPages = numberPages;
    }

    public List<T> getPage() {
        return page;
    }

    public int getNumberPages() {
        return numberPages;
    }
}
