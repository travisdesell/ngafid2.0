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
    private PageConfig pageConfig;

    public FlightPaginator(int numPerPage, List<Flight> allFlights){
        this.allFlights = allFlights;
        this.pageConfig = new PageConfig(allFlights.size(), numPerPage);
        this.pages = new Flight[pageConfig.numPages()][pageConfig.numPerPage()];

    }

    public void setNumPerPage(int numPerPage){
        this.pageConfig.update(numPerPage, 0); //always start on page 0
    }

    /**
    public FlightPaginator(int numPages, List<Flight> allFlights){
        this( numPages, Math.ceil(allFlights.size() / , allFlights);
    }
    */

    public void paginate(){
        int i = 0;
        while(i < this.allFlights.size()){
            for(int y = 0; y<pageConfig.numPages(); y++){
                for(int x = 0; x<pageConfig.numPerPage(); x++){
                    pages[y][x] = this.allFlights.get(i);
                    i++;
                }
            }
        }
    }

    public List<Flight> currentPage(){
        return Arrays.asList(this.pages[this.pageConfig.page()]);
    }

    public void nextPage(){
        if(pageConfig.page() < this.pages.length - 1){
            this.jumpToPage(pageConfig.page() + 1);
        }
    }

    public void previousPage(){
        if(pageConfig.page() > 0){
            this.jumpToPage(pageConfig.page() - 1);
        }
    }

    public void jumpToPage(int pageNumber){
        pageConfig.changePage(pageNumber);
    }
}
