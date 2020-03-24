package org.ngafid.common;

public class PageConfig{
    private int numElements, numPages, numPerPage, currentIndex;

    public PageConfig(int numElements, int numPerPage){
        this.numPerPage = numPerPage;
        this.numElements = numElements;
        this.numPages = (numElements / numPerPage);
        this.currentIndex = 0;
    }

    public void update(int numPerPage, int index){
        this.numPerPage = numPerPage;
        this.numPages = (numElements / numPerPage);
        this.changePage(index);
    }

    public void changePage(int newPage){
        this.currentIndex = newPage;
    }

    public int page(){
        return this.currentIndex;
    }

    public int numPerPage(){
        return this.numPerPage;
    }

    public int numPages(){
        return this.numPages;
    }
}
