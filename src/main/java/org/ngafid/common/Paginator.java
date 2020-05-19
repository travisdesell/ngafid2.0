/**
 * Paginator.java
 * Creates pages of NGAFID elements on the server side
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella, RIT Computer Science</a>
 */
package org.ngafid.common;

import java.lang.Math;
import java.util.*;
import org.ngafid.flights.Flight;
import org.ngafid.filters.*;
import org.ngafid.Database;
import java.sql.SQLException;

public abstract class Paginator{

    //useful for SQL queries
    private static final String LIMIT = "LIMIT";
    protected int pageBuffSize, currentIndex, numPages, numElements;

    /**
     * Default Constructor
     * @param startIndex the index to start at
     * @param pageBuffSize the amount of elements to display per page
     */
    public Paginator(int startIndex, int pageBuffSize){
        this.pageBuffSize = pageBuffSize;
        this.currentIndex = startIndex;
    }

    /**
     * Constructor
     * @param pageBuffSize the amount of elements to display per page
     */
    public Paginator(int pageBuffSize){
        this(0, pageBuffSize); //always start at 0 by default
    }

    /**
     * Sets the number of flights per page by first determining the amount of flights associated with the filter
     */
    protected void setNumElements(int newNumElements) throws SQLException{
        this.numElements = newNumElements;
        this.setNumPerPage(this.pageBuffSize);
    }


    /**
     * Generates a SQL query string for page n
     * @return a String with the appropriate SQL query
     */
    protected String limitString(){
        return LIMIT+" "+(currentIndex * pageBuffSize)+","+pageBuffSize;
    }

    /**
     * Sets the num of flights per page
     * @param numPerPage the new num of flights to disp. per page
     */
    public void setNumPerPage(int numPerPage){
        this.pageBuffSize = numPerPage;
        double quot = this.numElements / (double) numPerPage;
        this.numPages = (int)Math.ceil(quot);
    }


    /**
     * Gets the current page
     * @return the page as a Page instance
     * @throws SQLException if the database can not complete the query
     */
    public abstract Page currentPage() throws SQLException;

    /**
     * Gets the type of paginator (i.e. for flights, uploads, imports etc)
     * @return a string with the paginator type
     */
    protected abstract String paginatorType();

    /**
     * Changes the current page number
     * @param pageNumber the new page to go to
     */
    public void jumpToPage(int pageNumber){
        this.currentIndex = pageNumber;
    }

    /**
     * Returns information about the page collection
     * @return a String used for debugging and logging
     */
    @Override
    public String toString() {
        return paginatorType()+": buffer size: "+this.pageBuffSize+"; "+
            "current index: "+this.currentIndex+"; number of pages: "+this.numPages+
            "; total num of elements: "+this.numElements+"\n";
    }
}
