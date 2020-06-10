/**
 * Represents a page of data in the NGAFID
 * This is a generic class and can be used beyond flghts if need be
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella, RIT Computer Science</a>
 */

package org.ngafid.common;
import java.util.List;

public class Page<T>{
    private List<T> data; //the page data
    private int sizeAll; //the size of all pages in this collection
    private int index; //the pages index as an int

    /**
     * The default constructor
     * @param sizeAll the size of the ENTIRE collection of pages
     * @param data the data in the page
     * @param
     */
    public Page(int sizeAll, List<T> data, int index){
       this.sizeAll = sizeAll;
       this.data = data;
       this.index = index;
    }

    /**
     * Alternate constructor for "singluar" pages that are not a part of a specific collection
     * @param data the data stored in the page
     */
    public Page(List<T> data){
        this(1, data, 0);   //sizeAll is assumed to be 1 and index is 0;
                            //i.e this is a singular page, not in any collection
    }

    /**
     * Gives the data associated with this page
     * @return the data in the page as a List
     */
    public List<T> getData(){
        return data;
    }

    /**
     * Gives the current index
     * @return the index as an int
     */
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

    /**
     * Returns an element at a given index
     * @param i the index of the page (1...size)
     * @return an object of type T
     */
    public T at(int i){
        return this.data.get(i);
    }

    /**
     * Returns a string represntation of the page, used for debugging and logging
     * @return a String
     */
    @Override
    public String toString(){
        return "Page: "+index+" out of "+sizeAll;
    }
}
