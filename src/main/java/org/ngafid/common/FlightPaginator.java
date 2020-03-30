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
    private PageConfig pageConfig;

    public FlightPaginator(int numPerPage, List<Flight> allFlights){
        this.allFlights = allFlights;
        this.pageConfig = new PageConfig(allFlights.size(), numPerPage);
        this.pages = new HashMap<>();

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
        int numPerPage = pageConfig.numPerPage();
        while(i < this.allFlights.size()){
            for(int y = 0; y<pageConfig.numPages(); y++){
                Flight [] data = new Flight[numPerPage];
                for(int x = 0; x<numPerPage; x++){
                    data[x] = this.allFlights.get(i);
                    i++;
                }
                Page<Flight> page = new Page(this.pageConfig, data, y);
                this.pages.put(new Integer(y), page);
            }
        }
    }

    public Page currentPage(){
        Page p = this.pages.get(new Integer(this.pageConfig.page()));
        System.out.println(this.pages);
        return p;
    }

    public void nextPage(){
        if(pageConfig.page() < pages.entrySet().size() - 1){
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
