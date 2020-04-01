package org.ngafid.common;

public class Page<T>{
    private T [] data;
    private int sizeAll; //the size of all pages in this collection
    private int index;

    public Page(int sizeAll, T [] data, int index){
       this.sizeAll = sizeAll;
       this.data = data;
       this.index = index;
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
       return this.sizeAll;
    }

    @Override
    public String toString(){
        return "Page: "+index+" out of "+sizeAll;
    }
}
