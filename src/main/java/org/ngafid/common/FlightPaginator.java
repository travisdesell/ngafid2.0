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
    private static final String LIMIT = "LIMIT";

    private Filter filter;
    private int fleetID, pageBuffSize, currentIndex, numPages, numFlights;

    public FlightPaginator(int pageBuffSize, Filter filter, int fleetID) throws SQLException{
        this.filter = filter;
        this.fleetID = fleetID;
        this.currentIndex = 0; //always start at 0
        this.numFlights = Flight.getNumFlights(Database.getConnection(), this.fleetID, this.filter);
        this.setNumPerPage(pageBuffSize);
    }

    public void setNumPerPage(int numPerPage){
        this.pageBuffSize = numPerPage;
        double quot = this.numFlights / (double) numPerPage;
        this.numPages = (int)Math.ceil(quot);
    }

    private String limitString(){
        return LIMIT+" "+(currentIndex * pageBuffSize)+","+pageBuffSize;
    }

    public Page currentPage() throws SQLException{
        List<Flight> selectFlights = Flight.getFlights(Database.getConnection(), this.fleetID, this.filter, this.limitString());
        return new Page(numPages, selectFlights, currentIndex);
    }

    public void jumpToPage(int pageNumber){
        this.currentIndex = pageNumber;
    }
}
