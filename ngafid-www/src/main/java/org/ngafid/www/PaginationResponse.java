package org.ngafid.www;

import java.util.List;

public class PaginationResponse<T> {
    public List<T> page;
    public int numberPages;

    public PaginationResponse(List<T> page, int numberPages) {
        this.page = page;
        this.numberPages = numberPages;
    }
}
