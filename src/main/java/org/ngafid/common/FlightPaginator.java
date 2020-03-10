/**
 * FlightPaginator.java
 * Creates pages of flights on the server side
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */


import java.lang.Math;
import java.util.*;

public class FlightPagiantor{
	private List<Flight> allFlights;
	private Flight [][] pages;
	private int numPages, numPerPage;

	//master constructor 
	private FlightPaginator(int numPages, int numPerPage, List<Flight> allFlights){
		this.numPages = numPages;
		this.numPerPage = numPerPage;
		this.allFlights = allFlights;
		this.pages = new Flight[this.numPages][this.numPerPage];
		this.paginate();
	}

	public FlightPaginator(int numPerPage, List<Flight> allFlights){
		this( (allFlights.size() / numPerPage), numPerPage, allFlights);
	}

	public FlightPaginator(int numPages, List<Flight> allFlights){
		this( numPages, Math.ceil(allFlights.size() / , allFlights);
	}

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
}
