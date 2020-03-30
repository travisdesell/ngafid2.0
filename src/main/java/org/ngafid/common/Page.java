package org.ngafid.common;

public class Page<T>{
    private T [] data;
    private PageConfig configuration;
    private int index;

    public Page(PageConfig pageConfig, T [] data, int index){
       this.configuration = pageConfig;
       this.data = data;
    }

    public T[] getData(){
        return data;
    }

    public int index(){
       return index;
    }

    /**
     * Return the size of the collection of all pages
     * @return an int with the size of the pages
     */
    public int size(){
       return this.configuration.numPages();
    }
}
