package org.ngafid.www;

import java.util.List;

public class PaginationResponse<T> {
    private List<T> page;
    private int numberPages;

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
