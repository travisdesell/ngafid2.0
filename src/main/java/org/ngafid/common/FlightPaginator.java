/**
 * FlightPaginator.java
 * Creates pages of flights on the server side
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */

package org.ngafid.common;

import java.lang.Math;
import java.util.*;
import org.ngafid.flights.Flight;
import org.ngafid.filters.*;
import org.ngafid.Database;
import java.sql.SQLException;

public class FlightPaginator{

    //useful for SQL queries
    private static final String LIMIT = "LIMIT";

    private Filter filter;
    private int fleetID, pageBuffSize, currentIndex, numPages, numFlights;

    /**
     * Default Constructor
     * @param pageBuffSize the amount of elements to display per page
     * @param filter the filter specified by the user
     * @param fleetID the fleetID as an int
     * @throws SQLException if setNumFlights() does
     */
    public FlightPaginator(int pageBuffSize, Filter filter, int fleetID) throws SQLException{
        this.filter = filter;
        this.fleetID = fleetID;
        this.pageBuffSize = pageBuffSize;
        this.currentIndex = 0; //always start at 0
        this.setNumFlights();
    }

    /**
     * Constructor
     * @param filter the user specified filter
     * @param fleetID the fleetID as an int
     * @throws SQLException if setNumFlights() does
     */
    public FlightPaginator(Filter filter, int fleetID) throws SQLException{
        this(10, filter, fleetID); //the default page buffer size is 10
    }

    /**
     * Sets the number of flights per page by first determining the amount of flights associated with the filter
     */
    private void setNumFlights() throws SQLException{
        this.numFlights = Flight.getNumFlights(Database.getConnection(), this.fleetID, this.filter);
        this.setNumPerPage(this.pageBuffSize);
    }

    /**
     * Changes the filter used to query flights
     * @param filter the new filter
     */
    public void setFilter(Filter filter) throws SQLException{
        this.filter = filter;
        this.setNumFlights();
    }

    /**
     * Sets the num of flights per page
     * @param numPerPage the new num of flights to disp. per page
     */
    public void setNumPerPage(int numPerPage){
        this.pageBuffSize = numPerPage;
        double quot = this.numFlights / (double) numPerPage;
        this.numPages = (int)Math.ceil(quot);
    }

    /**
     * Generates a SQL query string for page n
     * @return a String with the appropriate SQL query
     */
    private String limitString(){
        return LIMIT+" "+(currentIndex * pageBuffSize)+","+pageBuffSize;
    }

    /**
     * Gets the current page
     * @return the page as a Page instance
     * @throws SQLException if Flight.getFlights does
     */
    public Page currentPage() throws SQLException{
        List<Flight> selectFlights = Flight.getFlights(Database.getConnection(), this.fleetID, this.filter, this.limitString());
        return new Page(numPages, selectFlights, currentIndex);
    }

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
        return "Flight Paginator for: "+this.fleetID+" buffer size: "+this.pageBuffSize+"; "+
            "current index: "+this.currentIndex+"; number of pages: "+this.numPages+
            "; total num of flights: "+this.numFlights+"\n";

    }
}
