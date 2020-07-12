import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import {Alert, Button, InputGroup, Form, Col} from 'react-bootstrap';

import { confirmModal } from "./confirm_modal.js";
import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";
import { map, styles, layers, Colors } from "./map.js";
import { View } from 'ol'

import {fromLonLat, toLonLat} from 'ol/proj.js';
import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import Draw from 'ol/interaction/Draw.js';

import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';
import { Filter } from './filter.js';


var moment = require('moment');


import Plotly from 'plotly.js';

var navbar = ReactDOM.render(
    <SignedInNavbar activePage="flights" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);
const cloneDeep = require('clone-deep');


var plotlyLayout = { 
    shapes : []
};

/*
var airframes = [ "PA-28-181", "Cessna 172S", "PA-44-180", "Cirrus SR20"  ];
var tailNumbers = [ "N765ND", "N744ND", "N771ND", "N731ND", "N714ND", "N766ND", "N743ND" , "N728ND" , "N768ND" , "N713ND" , "N732ND", "N718ND" , "N739ND" ];
var doubleTimeSeriesNames = [ "E1 CHT1", "E1 CHT2", "E1 CHT3" ];
var visitedAirports = [ "GFK", "FAR", "ALB", "ROC" ];
*/
// var tagNames = ["Tag A", "Tag B"];

//save the event definitions after the first event load so we can reuse them and not
//have to keep sending them from the server
var eventDefinitionsLoaded = false;
var eventDefinitions = null;

var rules = [
    {
        name : "Airframe",
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "is", "is not" ]
            },
            { 
                type : "select",
                name : "airframes",
                options : airframes
            }
        ]
    },

    {
        name : "Tail Number",
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "is", "is not" ]
            },
            {
                type : "select",
                name : "tail numbers",
                options : tailNumbers
            }
        ]
    },

    {
        name : "System ID",
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "is", "is not" ]
            },
            {
                type : "select",
                name : "system id",
                options : systemIds 
            }
        ]
    },


    {
        name : "Duration",
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type : "number",
                name : "hours"
            },
            {
                type : "number",
                name : "minutes"
            },
            {
                type : "number",
                name : "seconds"
            }
        ]
    },

    {
        name : "Start Date and Time",
        conditions : [
            {
                type : "select",
                name : "condition", 
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "datetime-local",
                name : "date and time"
            }
        ]
    },

    {
        name : "End Date and Time",
        conditions : [
            {
                type : "select",
                name : "condition", 
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "datetime-local",
                name : "date and time"
            }
        ]
    },

    {
        name : "Start Date",
        conditions : [
            {
                type : "select",
                name : "condition", 
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "date",
                name : "date"
            }
        ]
    },

    {
        name : "End Date",
        conditions : [
            {
                type : "select",
                name : "condition", 
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "date",
                name : "date"
            }
        ]
    },


    {
        name : "Start Time",
        conditions : [
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "time",
                name : "time"
            }
        ]
    },

    {
        name : "End Time",
        conditions : [
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "time",
                name : "time"
            }
        ]
    },


    {
        name : "Parameter",
        conditions : [
            {
                type : "select",
                name : "statistic",
                options : [ "min", "avg", "max" ]
            },
            {
                type : "select",
                name : "doubleSeries",
                options : doubleTimeSeriesNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "number",
                name : "number"
            }
        ]
    },

    {
        name : "Airport",
        conditions : [
            { 
                type : "select",
                name : "airports",
                options : visitedAirports
            },
            { 
                type : "select",
                name : "condition",
                options : [ "visited", "not visited" ]
            }
        ]
    },

    {
        name : "Runway",
        conditions : [
            { 
                type : "select",
                name : "runways",
                options : visitedRunways
            },
            { 
                type : "select",
                name : "condition",
                options : [ "visited", "not visited" ]
            }
        ]
    },

    {
        name : "Event Count",
        conditions : [
            {
                type : "select",
                name : "eventNames",
                options : eventNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "number",
                name : "number"
            }
        ]
    },

    {
        name : "Event Severity",
        conditions : [
            {
                type : "select",
                name : "eventNames",
                options : eventNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "number",
                name : "number"
            }
        ]
    },

    {
        name : "Event Duration",
        conditions : [
            {
                type : "select",
                name : "eventNames",
                options : eventNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "number",
                name : "number"
            }
        ]
    },

    {
        name : "Tag",
        conditions : [
            {
                type : "select",
                name : "flight_tags",
                options : tagNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "Is Associated", "Is Not Associated"]
            },
        ]
    },


];

// establish set of RGB values to combine //
let BG_values = ["00", "55", "AA", "FF"];
let R_values = ["FF", "D6", "AB", "80"];                            // heavier on the red for "warmer" colors

// populate hashmap of event definition IDs to RGB values
var eventColorScheme = {};
for (let d = 0; d < 45; d++){
    // iterate through RGB permutations (up to 64)
    let green = d % 4;
    let blue = Math.trunc(d/4) % 4;
    let red = Math.trunc(d/16) % 4;

    eventColorScheme[(d + 1)] = "#" + R_values[red] + BG_values[green] + BG_values[blue];
}

//This will be helpful for text inputs
function invalidString(str){
    return (str == null || str.length < 0 || /^\s*$/.test(str));
}

class Itinerary extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            color : this.props.color
        }
    }

    itineraryClicked(index) {
        flightsCard.showMap();

        var stop = this.props.itinerary[index];
        let modifiedIndex = parseInt(stop.minAltitudeIndex) - this.props.nanOffset;
        //console.log("index: " + stop.minAltitudeIndex + ", nanOffset: " + this.props.nanOffset + ", modifeid_index: " + modifiedIndex);

        let latlon = this.props.coordinates[modifiedIndex];
        //console.log(latlon);

        const coords = fromLonLat(latlon);
        map.getView().animate({center: coords, zoom: 13});
    }

    changeColor(event) {
        this.setState({
            color : event.target.value
        });
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        /*
        let cellClasses = "p-1 card mr-1 flex-fill"
        let itinerary = this.props.itinerary;
        let result = "";
        for (let i = 0; i < itinerary.length; i++) {
            result += itinerary[i].airport + " (" + itinerary[i].runway + ")";
            if (i != (itinerary.length - 1)) result += " => ";
        }
        */

        return (
            <div>
                <b className={"p-1"} style={{marginBottom:"0"}}>Itinerary:</b>
                <div className={cellClasses} style={cellStyle}>
                    <div style={{flex: "0 0"}}>
                        <input type="color" name="itineraryColor" value={this.state.color} onChange={(event) => {this.changeColor(event); this.props.flightColorChange(this.props.parent, event)}} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                    </div>



                    {
                        this.props.itinerary.map((stop, index) => {
                            let identifier = stop.airport;
                            if (stop.runway != null) identifier += " (" + stop.runway + ")";

                            return (
                                <button className={buttonClasses} key={index} style={styleButton} onClick={() => this.itineraryClicked(index)}>
                                    { identifier }
                                </button>
                            );
                        })
                    }
                </div>
            </div>
        );

    }
}

class TraceButtons extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            parentFlight : this.props.parentFlight
        };
    }

    traceClicked(seriesName) {
        flightsCard.showPlot();

        let parentFlight = this.state.parentFlight;

        //check to see if we've already loaded this time series
        if (!(seriesName in parentFlight.state.traceIndex)) {
            var thisTrace = this;

            console.log(seriesName);
            console.log("seriesName: " + seriesName + ", flightId: " + this.props.flightId);

            var submissionData = {
                flightId : this.props.flightId,
                seriesName : seriesName
            };   

            $.ajax({
                type: 'POST',
                url: '/protected/double_series',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

    

                var trace = {
                        x : response.x,
                        y : response.y,
                        mode : "lines",
                        //marker : { size: 1},
                        name : thisTrace.props.flightId + " - " + seriesName
                    }

                    //set the trace number for this series
                    parentFlight.state.traceIndex[seriesName] = $("#plot")[0].data.length;
                    parentFlight.state.traceVisibility[seriesName] = true;
                    parentFlight.setState(parentFlight.state);

                    Plotly.addTraces('plot', [trace]);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Loading Flight Coordinates", errorThrown);
                },   
                async: true 
            });  
        } else {
            //toggle visibility for this series
            let visibility = !parentFlight.state.traceVisibility[seriesName];
            parentFlight.state.traceVisibility[seriesName] = visibility;
            parentFlight.setState(parentFlight.state);

            console.log("toggled visibility to: " + visibility);

            Plotly.restyle('plot', { visible: visibility }, [ parentFlight.state.traceIndex[seriesName] ])
        }
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        let parentFlight = this.state.parentFlight;

        return (
            <div>
                <b className={"p-1"} style={{marginBottom:"0"}}>Flight Parameters:</b>
                <div className={cellClasses} style={cellStyle}>
                    {
                        parentFlight.state.commonTraceNames.map((traceName, index) => {
                            let ariaPressed = parentFlight.state.traceVisibility[traceName];
                            let active = "";
                            if (ariaPressed) active = " active";

                            return (
                                <button className={buttonClasses + active} key={traceName} style={styleButton} data-toggle="button" aria-pressed={ariaPressed} onClick={() => this.traceClicked(traceName)}>
                                    {traceName}
                                </button>
                            );
                        })
                    }
                </div>
                <div className={cellClasses} style={cellStyle}>
                    {
                        parentFlight.state.uncommonTraceNames.map((traceName, index) => {
                            let ariaPressed = parentFlight.state.traceVisibility[traceName];
                            let active = "";
                            if (ariaPressed) active = " active";

                            return (
                                <button className={buttonClasses + active} key={traceName} style={styleButton} data-toggle="button" aria-pressed={ariaPressed} onClick={() => this.traceClicked(traceName)}>
                                    {traceName}
                                </button>
                            );
                        })
                    }
                </div>
            </div>
        );
    }
}

/**
 * Tags module
 * Houses the state and information for user-defined tags
 * @module flights/Tags
 */
class Tags extends React.Component{

	/**
	 * ReactJS Constructor
	 * Intializes the state of this component
	 * @param props the props that are sent from the parent component
	 */
    constructor(props) {
        super(props);

        let pTags = [];
        if(props.tags != null){
            pTags = props.tags;
        }

        this.state = {
            tags : pTags,
            unassociatedTags : [],
			flightIndex : props.flightIndex,
            flightId : props.flightId,
            activeTag : null,
			editedTag : null,  //the tag currently being edited
            infoActive : false,
            addActive : false,
            editing : false,
			adding : false,
            addFormActive : false,
            assocTagActice : false,
            parent : props.parent
        };
        this.handleFormChange = this.handleFormChange.bind(this);
    }

	/**
	 * called everytime props are updated
	 * @param oldProps the old props before the update
	 */
	componentDidUpdate(oldProps) {
		console.log("props updated");
		const newProps = this.props;
	  	if(oldProps.tags !== newProps.tags) {
			this.state.tags = this.props.tags;
			this.state.addFormActive = false; //close the add form to indicate the tag has been edited or no longer exists
			this.setState(this.state);
	  	}
	}

	/**
	 * Handles the event for which the add button is pressed
	 */
    addClicked(){
        this.state.addActive = !this.state.addActive;
        this.state.infoActive = !this.state.infoActive;
		if(this.state.addFormActive){
            this.state.addFormActive = false;
		}
        this.setState(this.state);
        this.getUnassociatedTags();
    }

	/**
	 * Uses a ajax-json call to create a new tag in the server database
	 */
    addTag(){
        let tname = $("#comName").val(); 
        let tdescription = $("#description").val(); 
        let tcolor = $("#color").val(); 

        if(invalidString(tname) || invalidString(tdescription)){
            errorModal.show("Error creating tag!",
                            "Please ensure the name and description fields are correctly filled out!");
            return;
        }

        var submissionData = {
            name : tname,
            description : tdescription,
            color : tcolor,
            id : this.state.flightId
        };
        console.log("Creating a new tag for flight #"+this.state.flightId);

        let thisFlight = this;

        $.ajax({
            type: 'POST',
            url: '/protected/create_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
				if(response != "ALREADY_EXISTS"){
					if(thisFlight.state.tags != null){
						thisFlight.state.tags.push(response);
					}else{
						thisFlight.state.tags = new Array(response);
					}
					thisFlight.state.addFormActive = false;
					thisFlight.setState(thisFlight.state);
					thisFlight.updateParent(thisFlight.state.tags);
				}else{
					errorModal.show("Error creating tag", "A tag with that name already exists! Use the dropdown menu to associate it with this flight or give this tag another name");
				}
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

	/**
	 * Uses a ajax-json call to get the tags that are unassoicated with the current flight 
	 */
    getUnassociatedTags(){
        console.log("getting unassociated tags!")

        var submissionData = {
            id : this.state.flightId
        };

        let thisFlight = this;

        $.ajax({
            type: 'POST',
            url: '/protected/get_unassociated_tags',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                thisFlight.state.unassociatedTags = response;
                thisFlight.setState(thisFlight.state);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

	/**
	 * Handles when the user presses the delete button, and prompts them with @module confirmModal
	 */
    deleteTag(){
        if(this.state.activeTag != null){
            console.log("delete tag invoked!");
            confirmModal.show("Confirm Delete Tag: '" + this.state.activeTag.name + "'",
                            "Are you sure you wish to delete this tag?\n\nThis operation will remove it from this flight as well as all other flights that this tag is associated with. This operation cannot be undone!",
                            () => {this.confirmDelete()}
                            );
        }else{
            errorModal.show("Please select a tag to delete first!",
                            "Cannot delete any tags");
        }

    }


	/**
	 * Handles when the user presses the clear all tags button, and prompts them with @module confirmModal
	 */
    clearTags(){
        confirmModal.show("Confirm action", "Are you sure you would like to remove all the tags from flight #"+this.state.flightId+"?",
                          () => {this.removeTag(-2, false)});
    }

	/**
	 * Used to compare two tags for equality
	 * @param tagA the first tag to compare
	 * @param tagB the second tag to compare
	 * @return a boolean representing whether or not the two tags are equal
	 */
	tagEquals(tagA, tagB){
		return (tagA != null && tagB != null) && //they cant be null!
			tagA.name == tagB.name &&
			tagA.description == tagB.description &&
			tagA.color == tagB.color;
	}

	/**
	 * Prepares to edit a tag by creating a deep copy of the original tag
	 * to be used later on to determine if any changes have been made.
	 * @param tag the tag to edit
	 */
    editTag(tag){
        console.log("Editing tag: "+tag.hashId);
        if(this.state.activeTag == null || this.state.activeTag != tag){
            this.state.editing = true;
            this.state.addFormActive = true;
        }else{
            this.state.editing = !this.state.editing;
            this.state.addFormActive = !this.state.addFormActive;
        }
		this.state.adding = false;

        this.state.activeTag = tag;
		this.state.editedTag = cloneDeep(tag);
        this.setState(this.state);
    }

	/**
	 * Calls the server using ajax-json to notify it of the new tag change
	 */
    submitEdit(){
        console.log("submitting edit for tag: "+this.state.activeTag.hashId);

        var oldTag = this.state.activeTag;
        var submissionData = {
            tag_id : this.state.activeTag.hashId,
            name : this.state.editedTag.name,
            description : this.state.editedTag.description,
            color : this.state.editedTag.color
        };

        let thisFlight = this;

        $.ajax({
            type: 'POST',
            url: '/protected/edit_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                if(response != "NOCHANGE"){
					console.log("tag was edited!");
                    thisFlight.state.activeTag = oldTag;
					let index = thisFlight.state.tags.indexOf(oldTag);
                    thisFlight.state.tags = response.data[thisFlight.state.flightIndex].tags.value;
					console.log(response.data[thisFlight.state.flightIndex]);
                    thisFlight.updateFlights(response.data);
                }else{
                    thisFlight.showNoEditError();
                }
				thisFlight.setState(thisFlight.state);
            },
            error : function(jqXHR, textStatus, errorThrown) {
            },
            async: true
        });
    }

	/**
	 * shows @module errorModal when a tag has not been edited properly
	 */
    showNoEditError(){
        errorModal.show("Error editing tag", "Please make a change to the tag first before pressing submit!");
    }

	/**
	 * invoked by another function when the user has confirmed they would like to delete the tag permanently
	 */
    confirmDelete(){
        this.removeTag(this.state.activeTag.hashId, true);
    }

	/**
	 * Handles state changes for when the 'Create a new tag' option is selected
	 */
	createClicked(){
		this.state = {
			addActive : true,
			adding : true,
			editing : false,
		};
		this.showAddForm();
	}

	/**
	 * Shows the form for adding and/or editing a tag
	 */
    showAddForm(){
        this.state.addFormActive = !this.state.addFormActive;
		this.state.editedTag = {
			name : "",
			description : "",
			color : Colors.randomValue()
		};
        this.setState(this.state);
    }

	/**
	 * removes a tag from a flight, either permanent or just from one flight
	 * @param id the tagid of the tag being removed
	 * @param perm a bool representing whether or not the removal is permanent
	 */
    removeTag(id, perm){
        console.log("un-associating tag #"+id+" with flight #"+this.state.flightId);

        if(id==null || id == -1){
            errorModal.show("Please select a flight to remove first!", "Cannot remove any flights!");
            return;
        }

        let allTags = (id == -2);

        var submissionData = {
            flight_id : this.state.flightId,
            tag_id : id,
            permanent : perm,
            all : allTags
        };
		
		this.state.activeTag = null;

        let thisFlight = this;
		console.log("calling deletion ajax");

        $.ajax({
            type: 'POST',
            url: '/protected/remove_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
				if(perm){
					console.log("permanent deletion, refreshing all flights with: ");
					console.log(response);
					console.log(response.data[thisFlight.state.flightIndex]);
					let allFlights = response.data;
					thisFlight.state.tags = allFlights[thisFlight.state.flightIndex].tags.value;
					thisFlight.state.addFormActive = false;
					thisFlight.updateFlights(allFlights);
				}else{
					thisFlight.state.tags = response;
					thisFlight.setState(thisFlight.state);
					thisFlight.getUnassociatedTags();
					thisFlight.state.addFormActive = false;
					thisFlight.state.addActive = false;
					thisFlight.updateParent(thisFlight.state.tags);
                }
				thisFlight.setState(thisFlight.state);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

	/**
	 * Associates a tag with this flight
	 * @param id the tag id to associate
	 */
    associateTag(id){
        console.log("associating tag #"+id+" with flight #"+this.state.flightId);

        var submissionData = {
            id : this.state.flightId,
            tag_id : id
        };

        let thisFlight = this;

        $.ajax({
            type: 'POST',
            url: '/protected/associate_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                if(thisFlight.state.tags != null){
                    thisFlight.state.tags.push(response);
                }else{
                    thisFlight.state.tags = new Array(response);
					console.log(thisFlight.state.tags);
                }
                thisFlight.getUnassociatedTags();
                thisFlight.updateParent(thisFlight.state.tags);
                thisFlight.setState(thisFlight.state);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

	/**
	 * Updates the parent when the flights on the current page have changed
	 */
	updateFlights(flights){
		console.log("sending new flights to parents");
		console.log(flights);
		this.state.parent.updateFlights(flights);
	}

	/**
	 * Updates the parent of changes ONLY made to this flights tags
	 */
    updateParent(tags){
        this.state.parent.invokeUpdate(tags);
    }

	/**
	 * sets the state value editedTag which is used to make sure that the user has made an edit, before
	 * enabling the submit button
	 * @param e the onChange() event
	 */
    handleFormChange(e) {
        if(e.target.id == 'comName'){
            this.state.editedTag.name = e.target.value;
        }
        else if(e.target.id == 'description'){
            this.state.editedTag.description = e.target.value;
        }
		else if(e.target.id == 'color'){
            this.state.editedTag.color = e.target.value;
        }
        this.setState(this.state);
    }

	/**
	 * Renders the Tags component
	 */
    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let vcellStyle = { "overflowY" : "visible"};
        let addForm = "";
        let addDrop = "";
        let activeTag = this.state.activeTag;
		let editedTag = this.state.editedTag;
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 10 10em",
        };
        const styleButtonSq = {
            flex : "0 2 2em",
        };

        const styleColorInput = {
            height : "38",
        };

        let tags = this.state.tags;
        let unassociatedTags = this.state.unassociatedTags;
        let hasOtherTags = (unassociatedTags != null);

        let activeId = -1;
        if(this.state.activeTag != null){
            activeId = activeTag.hashId;
        }

        let defName = "", defDescript = "", defColor=Colors.randomValue(), defAddAction = (() => this.addTag()), tagStat = "";
        if(tags == null || tags.length == 0){
            tagStat = (<div><b className={"p-2"} style={{marginBottom:"2"}}>No tags yet!</b>
                <button className={buttonClasses} style={styleButtonSq} data-toggle="button" title="Add a tag to this flight" onClick={() => this.addClicked()}>Add a tag</button>
            </div>);
        }else{
           tagStat = ( 
                <div className={cellClasses} style={cellStyle}>
                {
                    tags.map((tag, index) => {
                        var cStyle = {
                            flex : "0 10 10em",
                            color : tag.color, 
                            fontWeight : '650'
                        };
                        return (
                                <button key={index} className={buttonClasses} onClick={() => this.editTag(tag)}>
									<i className="fa fa-tag p-1" style={{color : tag.color, marginRight : '10px'}}></i>
									{tag.name}
								</button>
                        );
                    })
                }
                <button className={buttonClasses} style={styleButtonSq} aria-pressed={this.state.addActive} title="Add a tag to this flight" onClick={() => this.addClicked()}><i className="fa fa-plus" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Remove the selected tag from this flight" onClick={() => this.removeTag(activeId, false)}><i className="fa fa-minus" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Permanently delete the selected tag from all flights" onClick={() => this.deleteTag()}><i className="fa fa-trash" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Clear all the tags from this flight" onClick={() => this.clearTags()}><i className="fa fa-eraser" aria-hidden="true"></i></button>
                </div> );
        }

        let tagInfo = "";
        if(this.state.editing){
            defName = this.state.editedTag.name;
            defDescript = this.state.editedTag.description;
            defColor = this.state.editedTag.color;
            defAddAction = (
                (() => this.submitEdit())
            );
        }

		if(this.state.adding){
            defName = this.state.editedTag.name;
            defDescript = this.state.editedTag.description;
            defColor = this.state.editedTag.color;
            defAddAction = (
                (() => this.addTag())
            );
		}

		let submitButton = (
						<button className="btn btn-outline-secondary" style={styleButtonSq} onClick={defAddAction} disabled>
                            <i className="fa fa-check" aria-hidden="true"></i>
                                Submit
						</button> );
		if(!this.state.editing || !this.tagEquals(activeTag, editedTag)){
			submitButton = (
						<button className="btn btn-outline-secondary" style={styleButtonSq} onClick={defAddAction} >
							<i className="fa fa-check" aria-hidden="true"></i>
								Submit
						</button> );
		}


        if(this.state.addActive){
            addDrop =
                <DropdownButton className={cellClasses + {maxHeight: "256px", overflowY: 'scroll'}} id="dropdown-item-button" variant="outline-secondary" title="Add a tag to this flight">
                    <Dropdown.Item as="button" onSelect={() => this.createClicked()}>Create a new tag</Dropdown.Item>
                    {unassociatedTags != null &&
                        <Dropdown.Divider />
                    }
                    {unassociatedTags != null &&
                        unassociatedTags.map((tag, index) => {
                            let style = {
                                backgroundColor : tag.color,
								fontSize : "110%"
                            }
                            return (
                                    <Dropdown.Item key={index} as="button" onSelect={() => this.associateTag(tag.hashId)}>
										<div className="row">
											<div className="col-xs-1 text-center">
												<span className="badge badge-pill badge-primary" style={style}>
													<i className="fa fa-tag" aria-hidden="true"></i>
												</span>
											</div>
											<div className="col text-center">
												{tag.name}
											</div>
										</div>
									</Dropdown.Item>
                            );
                        })
                    }
                    </DropdownButton>
        }
        if(this.state.addFormActive){
            addForm =
            <div className="row p-4">
                <div className="col-">
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <span className="input-group-text">
                                <span className="fa fa-tag"></span>
                            </span>
                        </div>
                        <input type="text" id="comName" className="form-control" onChange={this.handleFormChange} value={defName} placeholder="Common Name"/>
                    </div>
                </div>
                <div className="col-sm">
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <span className="input-group-text">
                                <span className="fa fa-list"></span>
                            </span>
                        </div>
                      <input type="text" id="description" className="form-control" onChange={this.handleFormChange} value={defDescript} placeholder="Description"/>
                    </div>
                </div>
                <div className="col-">
                    <div style={{flex: "0 0"}}>
                      <input type="color" name="eventColor" value={defColor} onChange={this.handleFormChange} id="color" style={styleColorInput}/>
                    </div>
                </div>
                <div className="col-sm">
                    <div className="input-group">
						{submitButton}
                    </div>
                </div>
            </div>
        }


        return (
            <div>
                <div>
                    <b className={"p-1"} style={{styleButton}}>Associated Tags:</b>
                </div>
                {tagStat} 
                <div className="flex-row p-1">
                    {addDrop}{addForm}
                </div>
            </div>
        );
    }
}

class Events extends React.Component {
    constructor(props) {
        super(props);

        console.log("constructing Events, props.events:");
        console.log(props.events);

        let definitionsPresent = [];

        for (let i = 0; i < props.events.length; i++) {
            if (!definitionsPresent.includes(props.events[i].eventDefinition)) {
                definitionsPresent.push(props.events[i].eventDefinition);
            }

            // assign color scheme to events, based on definition ID
            props.events[i].color = eventColorScheme[props.events[i].eventDefinitionId];
        }

        this.state = {
            events : props.events,
            definitions : definitionsPresent
        };
    }

    updateEventDisplay(index, toggle) {
            // Draw rectangles on plot
        var event = this.state.events[index];
        console.log("drawing plotly rectangle from " + event.startLine + " to " + event.endLine);
        let shapes = plotlyLayout.shapes;

        let update = {
            id: event.id,
            type: 'rect',
            // x-reference is assigned to the x-values
            xref: 'x',
            // y-reference is assigned to the plot paper [0,1]
            yref: 'paper',
            x0: event.startLine - 1,
            y0: 0,
            x1: event.endLine + 1,
            y1: 1,
            fillcolor: event.color,
            'opacity': 0.5,
            line: {
                'width': 0,
            }
        };

        let found = false;
        for (let i = 0; i < shapes.length; i++) {
            if (shapes[i].id == event.id) {
                if (toggle) {
                    shapes.splice(i, 1);
                    found = true;
                } else {
                    shapes[i] = update;
                    found = true;
                    break;
                }
            }
        }

        if (!found && toggle) {
            shapes.push(update);
        }

        Plotly.relayout('plot', plotlyLayout);


        // Toggle visibility of clicked event's Feature //

        // create eventStyle & hiddenStyle
        var eventStyle = new Style({                                                   // create style getter methods**
            stroke: new Stroke({
                color: event.color,
                width: 3
            })
        });

        var outlineStyle = new Style({                                                   // create style getter methods**
            stroke: new Stroke({
                color: "#000000",
                width: 5
            })
        });

        var hiddenStyle = new Style({
            stroke: new Stroke({
                color: [0,0,0,0],
                width: 3
            })
        });

        // get event info from flight
        let flight = this.props.parent;
        let eventMapped = flight.state.eventsMapped[index];
        let pathVisible = flight.state.pathVisible;
        let eventPoints = flight.state.eventPoints;
        let eventOutline = flight.state.eventOutlines[index];
        event = eventPoints[index];                                 //override event var w/ event Feature

        //toggle eventLayer style
        if (!eventMapped) {                             // if event hidden
            event.setStyle(eventStyle);
            eventOutline.setStyle(outlineStyle);
            flight.state.eventsMapped[index] = !eventMapped;

            // center map view on event location
            let coords = event.getGeometry().getFirstCoordinate();
            if (coords.length > 0 && pathVisible) {
                map.getView().setCenter(coords);
            }

        } else {                                        // if event displayed
            event.setStyle(hiddenStyle);
            eventOutline.setStyle(hiddenStyle);
            flight.state.eventsMapped[index] = !eventMapped;
        }
    }

    changeColor(e, index) {
        this.state.events[index].color = e.target.value;
        this.setState({
            events : this.state.events
        });
        this.updateEventDisplay(index, false);
    }
	

    eventClicked(index) {
        this.updateEventDisplay(index, true);
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        let eventType = "type";

        let eventTypeSet = new Set();
        let eventTypeButtons = [];
        let thisFlight = this.props.parent;

        this.state.events.map((event, index) => {
            if (!eventTypeSet.has(event.eventDefinitionId)) {
                // add new eventDef to types set
                eventTypeSet.add(event.eventDefinitionId);

                // create new button for toggle
                let type =
                        (
                            <button className={buttonClasses} style={{flex : "0 0 10em", "backgroundColor": eventColorScheme[event.eventDefinitionId], "color" : "#000000"}} data-toggle="button" aria-pressed="false" key={index}
                                        onClick={() =>
                                            {
                                                let flight = this.props.parent;
                                                let eventsMapped = flight.state.eventsMapped;
                                                let displayStatus = false;
                                                let displayStatusSet = false;

                                                // update eventDisplay for every event concerned
                                                for (let e = 0; e < this.state.events.length; e++) {
                                                    if (this.state.events[e].eventDefinitionId == event.eventDefinitionId) {
                                                        // ensure unified display
                                                        if (!displayStatusSet) {
                                                            displayStatus = !eventsMapped[e];
                                                            displayStatusSet = true;
                                                        }
                                                        // eventsMapped[e] = displayStatus;
                                                        // this.updateEventDisplay(e);

                                                        if (eventsMapped[e] != displayStatus) {
                                                            document.getElementById("_" + flight.props.flightInfo.id + e).click();
                                                        }
                                                    }
                                                }
                                            }
                                        }>
                                <b>{event.eventDefinition.name}</b>
                            </button>
                        );
                eventTypeButtons.push(type);
            }
        })

        return (
            <div>
                <b className={"p-1"} style={{marginBottom:"0"}}>Events:</b>

                <div className={"eventTypes"}>
                    {
                        eventTypeButtons.map( (button) => {
                            return (
                                button
                            )
                        })
                    }
                </div>

                {
                    this.state.events.map((event, index) => {
                        let buttonID = "_" + this.props.parent.props.flightInfo.id + index;
                        return (
                            <div className={cellClasses} style={cellStyle} key={index}>
                                <div style={{flex: "0 0"}}>
                                    <input type="color" name="eventColor" value={event.color} onChange={(e) => {this.changeColor(e, index); }} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                                </div>

                                <button id={buttonID} className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.eventClicked(index)}>
                                    <b>{event.eventDefinition.name}</b> {" -- " + event.startTime + " to " + event.endTime }
                                </button>
                            </div>
                        );
                    })
                }

            </div>
        );

    }
}


class Flight extends React.Component {
    constructor(props) {
        super(props);

        let color = Colors.randomValue();
        console.log("flight color: " );
        console.log(color);

        this.state = {
            pathVisible : false,
			pageIndex : props.pageIndex,
            mapLoaded : false,
            eventsLoaded : false,
            commonTraceNames : null,
            uncommonTraceNames : null,
            traceIndex : [],
            traceVisibility : [],
            traceNamesVisible : false,
            eventsVisible : false,
            tagsVisible : false,
            itineraryVisible : false,
            tags : props.tags,
            layer : null,
            parent : props.parent,
            color : color,

            eventsMapped : [],                              // Bool list to toggle event icons on map flightpath
            eventPoints : [],                               // list of event Features
            eventLayer : null,
            itineraryLayer : null,
            eventOutlines : [],
            eventOutlineLayer : null
        }
    }

    componentWillUnmount() {
        console.log("unmounting:");
        console.log(this.props.flightInfo);

        if (this.props.flightInfo.has_coords === "0") return;

        console.log("hiding flight path");
        this.state.pathVisible = false;
        this.state.itineraryVisible = false;
        if (this.state.layer) {
            this.state.layer.setVisible(false);
        }

        // hiding events
        // map
        if (this.state.eventLayer) {
            this.state.eventLayer.setVisible(false);
            this.state.eventOutlineLayer.setVisible(false);
            this.state.itineraryLayer.setVisible(false);

            // plot
            let shapes = plotlyLayout.shapes;
            shapes.length = 0;
        }


        console.log("hiding plots");
        if (this.state.commonTraceNames) {
            let visible = false;

            for (let i = 0; i < this.state.commonTraceNames.length; i++) {
                let seriesName = this.state.commonTraceNames[i];

                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }

            for (let i = 0; i < this.state.uncommonTraceNames.length; i++) {
                let seriesName = this.state.uncommonTraceNames[i];

                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }
        }
        this.state.traceNamesVisible = false;
    }

    plotClicked() {
        if (this.state.commonTraceNames == null) {
            var thisFlight = this;

            var submissionData = {
                flightId : this.props.flightInfo.id
            };

            $.ajax({
                type: 'POST',
                url: '/protected/double_series_names',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    var names = response.names;

                    /*
                     * Do these common trace parameters first:
                     * Altitiude AGL
                     * Altitude MSL
                     * E1 MAP
                     * E2 MAP
                     * E1 RPM
                     * E2 RPM
                     * IAS
                     * Normal Acceleration
                     * Pitch
                     * Roll
                     * Vertical Speed
                     */
                    var preferredNames = ["AltAGL", "AltMSL", "E1 MAP", "E2 MAP", "E1 RPM", "E2 RPM", "IAS", "NormAc", "Pitch", "Roll", "VSpd"];
                    var commonTraceNames = [];
                    var uncommonTraceNames = [];

                    for (let i = 0; i < response.names.length; i++) {
                        let name = response.names[i];

                        //console.log(name);
                        if (preferredNames.includes(name)) {
                            commonTraceNames.push(name);
                        } else {
                            uncommonTraceNames.push(name);
                        }
                    }

                    //set the trace number for this series
                    thisFlight.state.commonTraceNames = commonTraceNames;
                    thisFlight.state.uncommonTraceNames = uncommonTraceNames;
                    thisFlight.state.traceNamesVisible = true;
                    thisFlight.setState(thisFlight.state);
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    this.state.commonTraceNames = null;
                    this.state.uncommonTraceNames = null;
                    errorModal.show("Error Getting Potentail Plot Parameters", errorThrown);
                },
                async: true
            });
        } else {
            let visible = !this.state.traceNamesVisible;

            for (let i = 0; i < this.state.commonTraceNames.length; i++) {
                let seriesName = this.state.commonTraceNames[i];

                //check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }

            for (let i = 0; i < this.state.uncommonTraceNames.length; i++) {
                let seriesName = this.state.uncommonTraceNames[i];

                //check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }


            this.state.traceNamesVisible = !this.state.traceNamesVisible;
            this.setState(this.state);
        }
    }

    flightColorChange(target, event) {
        console.log("trace color changed!");
        console.log(event);
        console.log(event.target);
        console.log(event.target.value);

        let color = event.target.value;
        target.state.color = color;

        console.log(target);
        console.log(target.state);

        target.state.layer.setStyle(new Style({
            stroke: new Stroke({
                color: color,
                width: 1.5
            })
        }));
    }

    downloadClicked() {
        window.open("/protected/get_kml?flight_id=" + this.props.flightInfo.id);
    }

    exclamationClicked() {
        console.log ("exclamation clicked!");

        if (!this.state.eventsLoaded) {
            console.log("loading events!");

            this.state.eventsLoaded = true;
            this.state.eventsVisible = true;

            var thisFlight = this;

            var submissionData = {
                flightId : this.props.flightInfo.id,
                eventDefinitionsLoaded : eventDefinitionsLoaded
            };

            $.ajax({
                type: 'POST',
                url: '/protected/events',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    if (!eventDefinitionsLoaded) {
                        eventDefinitions = response.definitions;
                        eventDefinitionsLoaded = true;
                    }

                    var events = response.events;
                    for (let i = 0; i < events.length; i++) {
                        for (let j = 0; j < eventDefinitions.length; j++) {

                            if (events[i].eventDefinitionId == eventDefinitions[j].id) {
                                events[i].eventDefinition = eventDefinitions[j];
                                console.log("set events[" + i + "].eventDefinition to:");
                                console.log(events[i].eventDefinition);
                            }
                        }
                    }
                    thisFlight.state.events = events;

                    // create list of event Features to display on map //
                    for (let i = 0; i < events.length; i++) {
                        var points;
                        var eventPoint;
                        var eventOutline;
                        let event = events[i];

                        // Create Feature for event
                        if (!thisFlight.state.mapLoaded){              // if points (coordinates) have not been fetched
                            // create eventPoint with placeholder coordinates
                            eventPoint = new Feature({
                                geometry : new LineString( [0,0] ),
                                name: 'Event'
                            });

                            // create outlines
                            eventOutline = new Feature({
                                geometry : new LineString( [0,0] ),
                                name: 'EventOutline'
                            });

                        } else {
                            // create eventPoint with preloaded coordinates
                            points = thisFlight.state.points;
                            eventPoint = new Feature({
                                 geometry: new LineString(points.slice(event.startLine, event.endLine + 2)),
                                 name: 'Event'
                            });

                            // create outlines
                            eventOutline = new Feature({
                                 geometry: new LineString(points.slice(event.startLine, event.endLine + 2)),
                                 name: 'EventOutline'
                            });
                        }

                        // add eventPoint to flight
                        thisFlight.state.eventsMapped.push(false);
                        thisFlight.state.eventPoints.push(eventPoint);
                        thisFlight.state.eventOutlines.push(eventOutline);
                    }

                    // create eventLayer & add eventPoints
                    thisFlight.state.eventLayer = new VectorLayer({
                        style: new Style({
                            stroke: new Stroke({
                                color: [0,0,0,0],
                                width: 3
                            })
                        }),

                        source : new VectorSource({
                            features: thisFlight.state.eventPoints
                        })
                    });

                    // create eventLayer & add eventPoints
                    thisFlight.state.eventOutlineLayer = new VectorLayer({
                        style: new Style({
                            stroke: new Stroke({
                                color: [0,0,0,0],
                                width: 4
                            })
                        }),

                        source : new VectorSource({
                            features: thisFlight.state.eventOutlines
                        })
                    });

                    //thisFlight.state.eventLayer.flightState = thisFlight;
                    thisFlight.state.eventOutlineLayer.setVisible(true);
                    thisFlight.state.eventLayer.setVisible(true);

                    // add to map only if flightPath loaded
                    if (thisFlight.state.mapLoaded){
                        map.addLayer(thisFlight.state.eventOutlineLayer);
                        map.addLayer(thisFlight.state.eventLayer);
                    }

                    thisFlight.setState(thisFlight.state);
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    thisFlight.state.mapLoaded = false;
                    thisFlight.setState(thisFlight.state);

                    errorModal.show("Error Loading Flight Events", errorThrown);
                },
                async: true
            });

        } else {
            console.log("events already loaded!");

            //toggle visibility if already loaded
            this.state.eventsVisible = !this.state.eventsVisible;
            this.setState(this.state);
        }
    }

    cesiumClicked() {
        window.open("/protected/ngafid_cesium?flight_id=" + this.props.flightInfo.id);
    }

    tagClicked(){
        console.log ("tag clicked!");

        if (!this.state.eventsLoaded) {
            console.log("loading events!");

            var thisFlight = this;

            var submissionData = {
                flightId : this.props.flightInfo.id,
            };

            $.ajax({
                type: 'POST',
                url: '/protected/flight_tags',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    if(response != null){
                        thisFlight.state.tags = response;
                    }

                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    thisFlight.state.mapLoaded = false;
                    thisFlight.setState(thisFlight.state);

                    errorModal.show("Error Loading Flight Tags", errorThrown);
                },   
                async: true 
            });  

        } else {
            console.log("tags already loaded!");

            //toggle visibility if already loaded
            this.state.tagsVisible = !this.state.tagsVisible;
            this.setState(this.state);
        }
        this.state.tagsVisible = !this.state.tagsVisible;
    }

    globeClicked() {
        if (this.props.flightInfo.has_coords === "0") return;

        if (!this.state.mapLoaded) {
            flightsCard.showMap();
            this.state.mapLoaded = true;

            var thisFlight = this;

            var submissionData = {
                request : "GET_COORDINATES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flightId : this.props.flightInfo.id,
            };

            $.ajax({
                type: 'POST',
                url: '/protected/coordinates',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    //console.log("received response: ");
                    //console.log(response);

                    var coordinates = response.coordinates;

                    var points = [];
                    for (var i = 0; i < coordinates.length; i++) {
                        var point = fromLonLat(coordinates[i]);
                        points.push(point);
                    }

                    var color = thisFlight.state.color;
                    console.log(color);

                    thisFlight.state.trackingPoint = new Feature({
                                    geometry : new Point(points[0]),
                                    name: 'TrackingPoint'
                                });

                    thisFlight.state.layer = new VectorLayer({
                        style: new Style({
                            stroke: new Stroke({
                                color: color,
                                width: 3
                            }),
                            image: new Circle({
                                radius: 5,
                                //fill: new Fill({color: [0, 0, 0, 255]}),
                                stroke: new Stroke({
                                    color: [0, 0, 0, 0],
                                    width: 2
                                })
                            })
                        }),

                        source : new VectorSource({
                            features: [
                                new Feature({
                                    geometry: new LineString(points),
                                    name: 'Line'
                                }),
                                thisFlight.state.trackingPoint
                            ]
                        })
                    });

                    thisFlight.state.layer.flightState = thisFlight;

                    thisFlight.state.layer.setVisible(true);
                    thisFlight.state.pathVisible = true;
                    thisFlight.state.itineraryVisible = true;
                    thisFlight.state.nanOffset = response.nanOffset;
                    thisFlight.state.coordinates = response.coordinates;
                    thisFlight.state.points = points;

                    map.addLayer(thisFlight.state.layer);

                    // adding itinerary (approaches and takeoffs) to flightpath 
                    var itinerary = thisFlight.props.flightInfo.itinerary;
                    var flight_phases = [];

                    // Create flight phase styles
                    var takeoff_style = new Style({
                                stroke: new Stroke({
                                    color: "#34eb52",
                                    width: 3
                                })
                            });

                    var approach_style = new Style({
                                stroke: new Stroke({
                                    color: "#347deb",
                                    width: 3
                                })
                            });

                    // create and add Features to flight_phases for each flight phase in itinerary
                    for (let i = 0; i < itinerary.length; i++) {
                        var stop = itinerary[i];
                        var approach = null;
                        var takeoff = null;

                        // creating Linestrings
                        if (stop.startOfApproach != -1 && stop.endOfApproach != -1) {
                            approach = new LineString( points.slice( stop.startOfApproach, stop.endOfApproach ) );
                        }
                        if (stop.startOfTakeoff != -1 && stop.endOfTakeoff != -1) {
                            takeoff = new LineString( points.slice( stop.startOfTakeoff, stop.endOfTakeoff ) );
                        }

                        // set styles and add phases to flight_phases list
                        if (approach != null) {
                            let phase = new Feature({
                                             geometry: approach,
                                             name: 'Approach'
                                         });
                            phase.setStyle(approach_style);
                            flight_phases.push( phase );
                        }
                        if (takeoff != null) {
                            let phase = new Feature({
                                             geometry: takeoff,
                                             name: 'Takeoff'
                                         });
                            phase.setStyle(takeoff_style);
                            flight_phases.push( phase );
                        }
                    }

                    // create itineraryLayer
                    thisFlight.state.itineraryLayer = new VectorLayer({
                        style: new Style({
                            stroke: new Stroke({
                                color: [1,1,1,1],
                                width: 3
                            })
                        }),

                        source : new VectorSource({
                            features: flight_phases
                        })
                    });

                    // add itineraryLayer to map
                    map.addLayer(thisFlight.state.itineraryLayer);

                    // adding coordinates to events, if needed //
                    var events = [];
                    var eventPoints = [];
                    var eventOutlines = [];
                    if (thisFlight.state.eventsLoaded){
                        events = thisFlight.state.events;
                        eventPoints = thisFlight.state.eventPoints;
                        eventOutlines = thisFlight.state.eventOutlines;
                        for (let i = 0; i < events.length; i++){
                            let line = new LineString(points.slice(events[i].startLine, events[i].endLine + 2));
                            eventPoints[i].setGeometry(line);                   // set geometry of eventPoint Features
                            eventOutlines[i].setGeometry(line);
                        }

                        // add eventLayer to front of map
                        let eventLayer = thisFlight.state.eventLayer;
                        let outlineLayer = thisFlight.state.eventOutlineLayer;
                        map.addLayer(outlineLayer);
                        map.addLayer(eventLayer);
                    }

                    let extent = thisFlight.state.layer.getSource().getExtent();
                    console.log(extent);
                    map.getView().fit(extent, map.getSize());

                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    thisFlight.state.mapLoaded = false;
                    thisFlight.setState(thisFlight.state);

                    errorModal.show("Error Loading Flight Coordinates", errorThrown);
                },   
                async: true 
            });  
        } else {
            //toggle visibility if already loaded
            this.state.pathVisible = !this.state.pathVisible;
            this.state.itineraryVisible = !this.state.itineraryVisible;
            this.state.layer.setVisible(this.state.pathVisible);


            // toggle visibility of events
            if (this.state.eventLayer != null) {
                this.state.eventLayer.setVisible(!this.state.eventLayer.getVisible());
                this.state.eventOutlineLayer.setVisible(!this.state.eventOutlineLayer.getVisible());
            }
            // toggle visibility of itinerary
            this.state.itineraryLayer.setVisible(this.state.pathVisible);

            if (this.state.pathVisibile) {
                flightsCard.showMap();
            }

            this.setState(this.state);

            if (this.state.pathVisible) {
                let extent = this.state.layer.getSource().getExtent();
                console.log(extent);
                map.getView().fit(extent, map.getSize());
            }
        }
    }

	/**
	 * Changes all the flights on a given page by calling the parent function
	 */
	updateFlights(flights){
		this.props.updateParentState(flights);
	}

	/**
	 * Changes the tags associated with this flight
	 */
	invokeUpdate(tags){
		this.state.tags = tags;
		this.setState(this.state);
	}

	/**
	 * Called when props are updated
	 * changes state if props have in fact changed
	 * @param oldProps the old props before the update
	 */
	componentDidUpdate(oldProps) {
		console.log("props updated");
		const newProps = this.props;
	  	if(oldProps.tags !== newProps.tags) {
			this.state.tags = this.props.tags;
			this.setState(this.state);
	  	}
	}

    render() {
        let buttonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        let lastButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";
        const styleButton = { };

        let firstCellClasses = "p-1 card mr-1"
        let cellClasses = "p-1 card mr-1"

        let flightInfo = this.props.flightInfo;

        let startTime = moment(flightInfo.startDateTime);
        let endTime = moment(flightInfo.endDateTime);

        let globeClasses = "";
        let traceDisabled = false;
        let globeTooltip = "";

        let tagTooltip = "Click to tag a flight for future queries and grouping";

        //console.log(flightInfo);
        if (!flightInfo.hasCoords) {
            //console.log("flight " + flightInfo.id + " doesn't have coords!");
            globeClasses += " disabled";
            globeTooltip = "Cannot display flight on the map because the flight data did not have latitude/longitude.";
            traceDisabled = true;
        } else {
            globeTooltip = "Click the globe to display the flight on the map.";
        }

        let visitedAirports = [];
        for (let i = 0; i < flightInfo.itinerary.length; i++) {
            if ($.inArray(flightInfo.itinerary[i].airport, visitedAirports) < 0) {
                visitedAirports.push(flightInfo.itinerary[i].airport);
            }
        }

        let itineraryRow = "";
        if (this.state.itineraryVisible) {
            itineraryRow = (
                <Itinerary itinerary={flightInfo.itinerary} color={this.state.color} coordinates={this.state.coordinates} nanOffset={this.state.nanOffset} parent={this} flightColorChange={this.flightColorChange}/>
            );
        }

        let eventsRow = "";
        if (this.state.eventsVisible) {
            eventsRow = (
                <Events events={this.state.events} parent={this} />
            );
        }

        let tagsRow = "";
        if (this.state.tagsVisible) {
            tagsRow = (
                    <Tags tags={this.state.tags} flightIndex={this.state.pageIndex} flightId={flightInfo.id} parent={this} />
            );
        }

        let tracesRow = "";
        if (this.state.traceNamesVisible) {
            tracesRow = 
                (
                    <TraceButtons parentFlight={this} flightId={flightInfo.id}/>
                );
        }

        let tagPills = "";
        if(this.state.tags != null){
            tagPills = 
            this.state.tags.map((tag, index) => {
                let style = {
                    backgroundColor : tag.color,
                    marginRight : '4px',
                    lineHeight : '2',
					opacity : '75%'
                }
                return(
					<span key={index} className="badge badge-primary" style={{lineHeight : '1.5', marginRight : '4px', backgroundColor : '#e3e3e3', color : '#000000'}} title={tag.description}>
                        <span className="badge badge-pill badge-primary" style={style} page={this.state.page}>
							<i className="fa fa-tag" aria-hidden="true"></i>
						</span>   {tag.name}
					</span>
                );
            });
        }

        return (
            <div className="card mb-1">
                <div className="card-body m-0 p-0">
                    <div className="d-flex flex-row p-1">
                        <div className={firstCellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            <i className="fa fa-plane p-1"> {flightInfo.id}</i>
                        </div>

                        <div className={cellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            {flightInfo.tailNumber}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            {flightInfo.systemId}
                        </div>


                        <div className={cellClasses} style={{flexBasis:"120px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.airframeType}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.startDateTime}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.endDateTime}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"80px", flexShrink:0, flexGrow:0}}>

                            {moment.utc(endTime.diff(startTime)).format("HH:mm:ss")}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>
                            {visitedAirports.join(", ")}
                        </div>

                        <div className={cellClasses} style={{
							flexGrow:1,
							//textShadow: '-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000'
						}}>

                            <div>
                                {tagPills}
                            </div>
                        </div>

                        <div className="p-0">
                            <button className={buttonClasses} data-toggle="button" aria-pressed="false" style={styleButton} onClick={() => this.exclamationClicked()}>
                                <i className="fa fa-exclamation p-1"></i>
                            </button>

                            <button className={buttonClasses} data-toggle="button" title={tagTooltip} aria-pressed="false" style={styleButton} onClick={() => this.tagClicked()}>
                                <i className="fa fa-tag p-1"></i>
                            </button>

                            <button className={buttonClasses + globeClasses} data-toggle="button" title={globeTooltip} aria-pressed="false" style={styleButton} onClick={() => this.globeClicked()}>
                                <i className="fa fa-map-o p-1"></i>
                            </button>

                            <button className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.plotClicked()}>
                                <i className="fa fa-area-chart p-1"></i>
                            </button>

                            <button className={buttonClasses + globeClasses} disabled={traceDisabled} style={styleButton} onClick={() => this.cesiumClicked()}>
                                <i className="fa fa-globe p-1"></i>
                            </button>

                            <button className={buttonClasses + " disabled"} style={styleButton} onClick={() => this.replayClicked()}>
                                <i className="fa fa-video-camera p-1"></i>
                            </button>

                            <button className={lastButtonClasses + globeClasses} disabled={traceDisabled} style={styleButton} onClick={() => this.downloadClicked()}>
                                <i className="fa fa-download p-1"></i>
                            </button>
                        </div>
                    </div>

                    {itineraryRow}

                    {tagsRow}

                    {eventsRow}

                    {tracesRow}
                </div>
            </div>
        );
    }
}


class FlightsCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            mapVisible : false,
            plotVisible : false,
            filterVisible : true,
            page : 0,
            buffSize : 10,   //def size of flights to show per page is 10
            numPages : 0
        };

       this.previousPage = this.previousPage.bind(this);
       this.nextPage = this.nextPage.bind(this);
       this.repaginate = this.repaginate.bind(this);
       this.updateState = this.updateState.bind(this);
       this.filterRef = React.createRef();
    }

    setFlights(flights) {
        this.state.flights = flights;
        this.setState(this.state);
    }

	//used to update the state from a child component
	updateState(newFlights){
		this.setFlights(newFlights);
	}

    setIndex(index){
        this.state.page = index;
        this.setState(this.state);
    }

    setSize(size){
        this.state.numPages = size;
        this.setState(this.state);
    }

    mapSelectChanged(style) {
        //layers and styles from plots.js
        for (var i = 0, ii = layers.length; i < ii; ++i) {

            console.log("setting layer " + i + " to:" + (styles[i] === style));
            layers[i].setVisible(styles[i] === style);
        }   
    }

    showMap() {
        if (this.state.mapVisible) return;

        if ( !$("#map-toggle-button").hasClass("active") ) { 
            $("#map-toggle-button").addClass("active");
            $("#map-toggle-button").attr("aria-pressed", true);
        }

        this.state.mapVisible = true;
        this.setState(this.state);

        $("#plot-map-div").css("height", "50%");
        $("#map").show();

        if (this.state.plotVisible) {
            $("#map").css("width", "50%");
            map.updateSize();
            $("#plot").css("width", "50%");
            Plotly.Plots.resize("plot");
        } else {
            $("#map").css("width", "100%");
            map.updateSize();
        }

    }

    hideMap() {
        if (!this.state.mapVisible) return;

        if ( $("#map-toggle-button").hasClass("active") ) { 
            $("#map-toggle-button").removeClass("active");
            $("#map-toggle-button").attr("aria-pressed", false);
        }   

        this.state.mapVisible = false;
        this.setState(this.state);

        $("#map").hide();

        if (this.state.plotVisible) {
            $("#plot").css("width", "100%");
            var update = { width : "100%" };
            Plotly.Plots.resize("plot");
        } else {
            $("#plot-map-div").css("height", "0%");
        }
    }

    toggleMap() {
        if (this.state.mapVisible) {
            this.hideMap();
        } else {
            this.showMap();
        }
    }

    showPlot() {
        if (this.state.plotVisible) return;

        if ( !$("#plot-toggle-button").hasClass("active") ) { 
            $("#plot-toggle-button").addClass("active");
            $("#plot-toggle-button").attr("aria-pressed", true);
        }

        this.state.plotVisible = true;
        this.setState(this.state);

        $("#plot").show();
        $("#plot-map-div").css("height", "50%");

        if (this.state.mapVisible) {
            $("#map").css("width", "50%");
            map.updateSize();
            $("#plot").css("width", "50%");
            Plotly.Plots.resize("plot");
        } else {
            $("#plot").css("width", "100%");
            Plotly.Plots.resize("plot");
        }
    }

    hidePlot() {
        if (!this.state.plotVisible) return;

        if ( $("#plot-toggle-button").hasClass("active") ) { 
            $("#plot-toggle-button").removeClass("active");
            $("#plot-toggle-button").attr("aria-pressed", false);
        }   

        this.state.plotVisible = false;
        this.setState(this.state);

        $("#plot").hide();

        if (this.state.mapVisible) {
            $("#map").css("width", "100%");
            map.updateSize();
        } else {
            $("#plot-map-div").css("height", "0%");
        }
    }

    togglePlot() {
        if (this.state.plotVisible) {
            this.hidePlot();
        } else {
            this.showPlot();
        }
    }

    showFilter() {
        if (this.state.filterVisible) return;

        if ( !$("#filter-toggle-button").hasClass("active") ) { 
            $("#filter-toggle-button").addClass("active");
            $("#filter-toggle-button").attr("aria-pressed", true);
        }

        this.state.filterVisible = true;
        this.setState(this.state);

        //$("#filter").show();
    }

    hideFilter() {
        if (!this.state.filterVisible) return;

        if ( $("#filter-toggle-button").hasClass("active") ) { 
            $("#filter-toggle-button").removeClass("active");
            $("#filter-toggle-button").attr("aria-pressed", false);
        }   

        this.state.filterVisible = false;
        this.setState(this.state);

        //$("#filter").hide();
    }

    toggleFilter() {
        if (this.state.filterVisible) {
            this.hideFilter();
        } else {
            this.showFilter();
        }
    }

	/**
	 * Jumps to a page in this collection of queried flights
	 * @param pg the page to jump to
	 */
    jumpPage(pg){
        if(pg < this.state.numPages && pg >= 0){
            this.state.page = pg;
            this.submitFilter();
        }
    }

	/**
	 * jumps to the next page in this collection of queried flights
	 */
    nextPage(){
        this.state.page++;
        this.submitFilter();
    }

	/**
	 * jumps to the previous page in this collection of queried flights
	 */
    previousPage(){
        this.state.page--;
        this.submitFilter();
    }

	/**
	 * Repaginates the page configuration when the numPerPage field has been changed by the user
	 */
    repaginate(pag){
        console.log("Re-Paginating");
        this.state.buffSize = pag;
        this.submitFilter();
    }

    submitFilter() {
        //console.log( this.state.filters );

        let query = this.filterRef.current.getQuery();

        console.log("Submitting filters:");
        console.log( query );

        $("#loading").show();

        var submissionData = {
            filterQuery : JSON.stringify(query),
            pageIndex : this.state.page,
            numPerPage : this.state.buffSize
        };

        console.log(submissionData);

        $.ajax({
            type: 'POST',
            url: '/protected/get_flights',
            data : submissionData,
            dataType : 'json',
            success : function(response) {

                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                console.log("got response: "+response+" "+response.size);

                //get page data
                flightsCard.setFlights(response.data);
                flightsCard.setIndex(response.index);
                flightsCard.setSize(response.sizeAll);
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },   
            async: true 
        });  
    }

	/**
	 * Generates an array representing all the pages in this collection of 
	 * queried flights
	 * @return an array of String objects containing page names
	 */
    genPages(){
        var page = [];
        for(var i = 0; i<this.state.numPages; i++){
            page.push({
                value : i,
                name : "Page "+(i+1)
            });
        }
        return page;
    }
	
	/**
	 * Renders the flightsCard
	 */
    render() {
        console.log("rendering flights!");

        let flights = [];
        if (typeof this.state.flights != 'undefined') {
            flights = this.state.flights;

        }

        let pages = this.genPages();

        let style = null;
        if (this.state.mapVisible || this.state.plotVisible) {
            console.log("rendering half");
            style = { 
                overflow : "scroll",
                height : "calc(50% - 56px)"
            };  
        } else {
            style = { 
                overflow : "scroll",
                height : "calc(100% - 56px)"
            };  
        }

        style.padding = "5";
        if(flights == null || flights.length > 0){
            var begin = this.state.page == 0;
            var end = this.state.page == this.state.numPages-1;
            var prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage}>Previous Page</button>
            var next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage}>Next Page</button>

            if(begin) {
                prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage} disabled>Previous Page</button>
            }
            if(end){
                next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage} disabled>Next Page</button>
            }


            return (
                <div className="card-body" style={style}>
                    <Filter ref={this.filterRef} hidden={!this.state.filterVisible} depth={0} baseIndex="[0-0]" key="[0-0]" parent={null} type="GROUP" submitFilter={() => {this.submitFilter()}} rules={rules} submitButtonName="Apply Filter"/>
                        <div className="card mb-1 m-1 border-secondary">
                            <div className="p-2">
                                <button className="btn btn-sm btn-info pr-2" disabled>Page: {this.state.page + 1} of {this.state.numPages}</button>
                                <div className="btn-group mr-1 pl-1" role="group" aria-label="First group">
                                    <DropdownButton  className="pr-1" id="dropdown-item-button" title={this.state.buffSize + " flights per page"} size="sm">
                                        <Dropdown.Item as="button" onClick={() => this.repaginate(10)}>10 flights per page</Dropdown.Item>
                                        <Dropdown.Item as="button" onClick={() => this.repaginate(15)}>15 flights per page</Dropdown.Item>
                                        <Dropdown.Item as="button" onClick={() => this.repaginate(25)}>25 flights per page</Dropdown.Item>
                                        <Dropdown.Item as="button" onClick={() => this.repaginate(50)}>50 flights per page</Dropdown.Item>
                                        <Dropdown.Item as="button" onClick={() => this.repaginate(100)}>100 flights per page</Dropdown.Item>
                                    </DropdownButton>
                                  <Dropdown className="pr-1">
                                    <Dropdown.Toggle variant="primary" id="dropdown-basic" size="sm">
                                        {"Page " + (this.state.page + 1)}
                                    </Dropdown.Toggle>
                                    <Dropdown.Menu  style={{ maxHeight: "256px", overflowY: 'scroll' }}>
                                            {
                                                pages.map((pages, index) => {
                                                    return (
                                                            <Dropdown.Item key={index} as="button" onClick={() => this.jumpPage(pages.value)}>{pages.name}</Dropdown.Item>
                                                    );
                                                })
                                            }
                                    </Dropdown.Menu>
                                  </Dropdown>
                                    {prev}
                                    {next}
                                </div>
                            </div>
                        </div>
						{
							flights.map((flightInfo, index) => {
								if(flightInfo != null){
									return (
										<Flight flightInfo={flightInfo} pageIndex={index}
										updateParentState={(newFlights) => this.updateState(newFlights)}
										parent={this} tags={flightInfo.tags.value} key={flightInfo.id}/>
									);
								}
							})
						}
                        <div className="card mb-1 m-1 border-secondary">
                            <div className="p-2">
                                <button className="btn btn-sm btn-info pr-2" disabled>Page: {this.state.page + 1} of {this.state.numPages}</button>
                                <div className="btn-group mr-2 pl-1" role="group" aria-label="First group">
                                    {prev}
                                    {next}
                                </div>
						</div>
                    </div>


                    <div id="load-more"></div>
                </div>
            );
        } else {
            return(
                <div className="card-body" style={style}>
                <Filter ref={this.filterRef} hidden={!this.state.filterVisible} depth={0} baseIndex="[0-0]" key="[0-0]" parent={null} type="GROUP" submitFilter={() => {this.submitFilter()}} rules={rules} submitButtonName="Apply Filter"/>
                    </div>
            );
        }
    }
}

let flightsCard = null;
//check to see if flights has been defined already. unfortunately
//the navbar includes flights.js (bad design) for the navbar buttons
//to toggle flights, etc. So this is a bit of a hack.
if (typeof flights !== 'undefined') {
    flightsCard = ReactDOM.render(
        <FlightsCard />,
        document.querySelector('#flights-card')
    );
    navbar.setFlightsCard(flightsCard);

    console.log("rendered flightsCard!");

    $(document).ready(function() {

        Plotly.newPlot('plot', [], plotlyLayout);

        var myPlot = document.getElementById("plot");
        console.log("myPlot:");
        console.log(myPlot);

        myPlot.on('plotly_hover', function(data){
            var xaxis = data.points[0].xaxis,
                yaxis = data.points[0].yaxis;

            /*
            var infotext = data.points.map(function(d){
                return ('width: '+xaxis.l2p(d.x)+', height: '+yaxis.l2p(d.y));
            });
            */

            //console.log("in hover!");
            //console.log(data);
            let x = data.points[0].x;

            //console.log("x: " + x);

            map.getLayers().forEach(function(layer) {
                if (layer instanceof VectorLayer) {
                    if ('flightState' in layer) {
                        //console.log("VECTOR layer:");

                        var hiddenStyle = new Style({
                            stroke: new Stroke({
                                color: layer.flightState.state.color,
                                width: 1.5
                            }),
                            image: new Circle({
                                radius: 5,
                                stroke: new Stroke({
                                    color: [0,0,0,0],
                                    width: 2
                                })
                            })
                        });

                        var visibleStyle = new Style({
                            stroke: new Stroke({
                                color: layer.flightState.state.color,
                                width: 1.5
                            }),
                            image: new Circle({
                                radius: 5,
                                stroke: new Stroke({
                                    color: layer.flightState.state.color,
                                    width: 2
                                })
                            })
                        });

                        if (layer.getVisible()) {
                            if (x < layer.flightState.state.points.length) {
                                console.log("need to draw point at: " + layer.flightState.state.points[x]);
                                layer.flightState.state.trackingPoint.setStyle(visibleStyle);
                                layer.flightState.state.trackingPoint.getGeometry().setCoordinates(layer.flightState.state.points[x]);
                            } else {
                                console.log("not drawing point x: " + x + " >= points.length: " + layer.flightState.state.points.length);
                                layer.flightState.state.trackingPoint.setStyle(hiddenStyle);
                            }
                        }
                    }
                }
            });
        });

    });
}

export { flightsCard };
