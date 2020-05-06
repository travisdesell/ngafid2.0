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

public class FlightPaginator extends Paginator{

    //useful for SQL queries
    private static final String LIMIT = "LIMIT";
    private Filter filter;
    private int fleetID;

    /**
     * Default Constructor
     * @param pageBuffSize the amount of elements to display per page
     * @param filter the filter specified by the user
     * @param fleetID the fleetID as an int
     * @throws SQLException if setNumFlights() does
     */
    public FlightPaginator(int pageBuffSize, Filter filter, int fleetID) throws SQLException{
        super(pageBuffSize);
        this.filter = filter;
        this.fleetID = fleetID;
        this.setNumFlights();
    }

    /**
     * Constructor
     * @param filter the user specified filter
     * @param fleetID the fleetID as an int
     * @throws SQLException if setNumFlights() does
     */
    public FlightPaginator(Filter filter, int fleetID) throws SQLException{
        this(10, filter, fleetID);//default buff. size is 10
    }

    /**
     * Sets the number of flights per page by first determining the amount of flights associated with the filter
     */
    private void setNumFlights() throws SQLException{
        super.setNumElements(Flight.getNumFlights(Database.getConnection(), this.fleetID, this.filter));
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
     * Generates a SQL query string for page n
     * @return a String with the appropriate SQL query
     */
    private String limitString(){
        return LIMIT+" "+(currentIndex * pageBuffSize)+","+pageBuffSize;
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Page currentPage() throws SQLException{
        List<Flight> selectFlights = Flight.getFlights(Database.getConnection(), this.fleetID, this.filter, this.limitString());
        return new Page(numPages, selectFlights, currentIndex);
    }

    /**
     * {inheritDoc}
     */
    @Override
    protected String paginatorType(){
        return "Flight Paginator";
    }

}
