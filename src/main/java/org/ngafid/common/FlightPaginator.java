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

    private List<Flight> allFlights;
    private Filter filter;
    private int fleetID;
    private Map<Integer, Page<Flight>> pages;
    private int pageBuffSize, currentIndex, numPages;

    public FlightPaginator(int pageBuffSize, Filter filter, int fleetID) throws SQLException{
        this.filter = filter;
        this.fleetID = fleetID;
        this.currentIndex = 0; //always start at 0
        allFlights = Flight.getFlights(Database.getConnection(), this.fleetID, this.filter, 50);
        this.setNumPerPage(pageBuffSize);
    }

    public int getNumFlights(){
        return -1;
    }

    public void setNumPerPage(int numPerPage){
        this.pageBuffSize = numPerPage;
        double quot = this.allFlights.size() / (double) numPerPage;
        this.numPages = (int)Math.ceil(quot);
    }

    public void paginate(){
        int i = 0;
        this.pages = new HashMap<>();
        while(i < allFlights.size()){
            for(int y = 0; y<numPages; y++){
                Flight [] data = new Flight[pageBuffSize];
                for(int x = 0; x<pageBuffSize; x++){
                    if(i == allFlights.size()){
                        break;
                    }
                    data[x] = this.allFlights.get(i);
                    i++;
                }
                Page<Flight> page = new Page(numPages, data, y);
                this.pages.put(new Integer(y), page);
            }
        }
    }

    private String limitString(){
        return "placeholder";
    }

    public Page currentPage(){
        Page p = this.pages.get(new Integer(this.currentIndex));
        System.out.println(this.pages);
        return p;
    }

    public void jumpToPage(int pageNumber){
        this.currentIndex = pageNumber;
    }
}
