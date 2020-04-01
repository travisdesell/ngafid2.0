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
    private Map<Integer, Page<Flight>> pages;
    private int pageBuffSize, currentIndex, numPages;

    public FlightPaginator(int pageBuffSize, List<Flight> allFlights){
        this.allFlights = allFlights;
        this.currentIndex = 0; //always start at 0
        this.setNumPerPage(pageBuffSize);

    }

    public void setNumPerPage(int numPerPage){
        this.pageBuffSize = numPerPage;
        this.numPages = this.allFlights.size() / numPerPage;
    }

    /**
    public FlightPaginator(int numPages, List<Flight> allFlights){
        this( numPages, Math.ceil(allFlights.size() / , allFlights);
    }
    */

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

    public Page currentPage(){
        Page p = this.pages.get(new Integer(this.currentIndex));
        System.out.println(this.pages);
        return p;
    }

    public void nextPage(){
        if(numPages < pages.entrySet().size() - 1){
            this.jumpToPage(this.currentIndex + 1);
        }
    }

    public void previousPage(){
        if(numPages > 0){
            this.jumpToPage(this.currentIndex - 1);
        }
    }

    public void jumpToPage(int pageNumber){
        this.currentIndex = pageNumber;
    }
}
