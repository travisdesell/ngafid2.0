/**
 * FlightPaginator.java
 * Creates pages of flights on the server side
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */

package org.ngafid.common;

import java.lang.Math;
import java.util.*;
import org.ngafid.flights.Flight;

public class FlightPaginator{
    private List<Flight> allFlights;
    private Flight [][] pages;
    private int numPages, numPerPage;

    private int currentIndex;

	//master constructor
    private FlightPaginator(int numPages, int numPerPage, List<Flight> allFlights, int currentIndex){
        this.numPages = numPages;
        this.numPerPage = numPerPage;
        this.allFlights = allFlights;
        this.pages = new Flight[this.numPages][this.numPerPage];
        this.currentIndex = currentIndex;
        this.paginate();
    }

    public FlightPaginator(int numPerPage, List<Flight> allFlights){
        this((allFlights.size() / numPerPage), numPerPage, allFlights, 0);
    }

    /**
    public FlightPaginator(int numPages, List<Flight> allFlights){
        this( numPages, Math.ceil(allFlights.size() / , allFlights);
    }
    */

    private void paginate(){
        int i = 0;
        while(i < this.allFlights.size()){
            for(int y = 0; y<numPages; y++){
                for(int x = 0; x<numPerPage; x++){
                    pages[y][x] = this.allFlights.get(i);
                    i++;
                }
            }
        }
    }

    public List<Flight> currentPage(){
        return Arrays.asList(this.pages[currentIndex]);
    }

    public void nextPage(){
        if(this.currentIndex < this.pages.length - 1){
            this.jumpToPage(this.currentIndex + 1);
        }
    }

    public void previousPage(){
        if(this.currentIndex > 0){
            this.jumpToPage(this.currentIndex - 1);
        }
    }

    public void jumpToPage(int pageNumber){
        this.currentIndex = pageNumber;
    }
}
