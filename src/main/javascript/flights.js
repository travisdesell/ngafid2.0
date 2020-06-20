import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import {Alert, Button, InputGroup, Form, Col} from 'react-bootstrap';

import { confirmModal } from "./confirm_modal.js";
import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";
import { map, styles, layers, Colors } from "./map.js";

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
                request : "GET_DOUBLE_SERIES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
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

class Tags extends React.Component{
    constructor(props) {
        super(props);

        let pTags = [];
        if(props.tags != null){
            pTags = props.tags;
        }

        console.log("constructing Tags, props.tags:");
        console.log(props.tags);

        this.state = {
            tags : pTags,
            unassociatedTags : [],
            flightId : props.flightId,
            activeTag : null,
            infoActive : false,
            addActive : false,
            editing : false,
            detailsActive : false,
            addFormActive : false,
            assocTagActice : false,
            parent : props.parent
        };
    }

    showDetails(index){
        let swTag = this.state.tags[index];
        if(this.state.activeTag == null || this.state.activeTag != swTag){
            this.state.activeTag = swTag;
            this.state.detailsActive = true;
        }else{
            this.state.detailsActive = !this.state.detailsActive; 
        }
        this.setState(this.state);
    }

    addClicked(){
        this.state.addActive = !this.state.addActive;
        this.state.infoActive = !this.state.infoActive;
        if(this.state.addFormActive){
            this.state.addFormActive = false;
        }
        this.setState(this.state);
        this.getUnassociatedTags();
    }

    editClicked(){
        console.log("edit clicked!");
    }

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
					thisFlight.state.tags.push(response);
					thisFlight.setState(thisFlight.state);
				}else{
					errorModal.show("Error creating tag", "A tag with that name already exists! Use the dropdown menu to associate it with this flight or give this tag another name");
				}
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

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
                //TODO: resolve duplicate tag creation here
            },   
            async: true 
        });  
    }

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

    clearTags(){
        confirmModal.show("Confirm action", "Are you sure you would like to remove all the tags from flight #"+this.state.flightId+"?",
                          () => {this.removeTag(-2, false)});
    }

    editTag(tag){
        console.log("Editing tag: "+tag.hashId);
        // let tdescription = $("#description").val(); 
        // let tcolor = $("#color").val(); 
        if(this.state.activeTag == null || this.state.activeTag != tag){
            this.state.editing = true;
            this.state.addFormActive = true;
        }else{
            this.state.editing = !this.state.editing;
            this.state.addFormActive = !this.state.addFormActive;
        }

        this.state.activeTag = tag;
        this.setState(this.state);
    }

    submitEdit(){
        console.log("editing tag: "+this.state.activeTag.hashId);

        var oldTag = this.state.activeTag;
        var submissionData = {
            tag_id : this.state.activeTag.hashId,
            name : $("#comName").val(),
            description : $("#description").val(),
            color : $("#color").val()
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
                    thisFlight.state.activeTag = oldTag;
                    thisFlight.state.tags[thisFlight.state.tags.indexOf(oldTag)] = response;
                    thisFlight.state.detailsActive = false;
                    thisFlight.setState(thisFlight.state);
                    thisFlight.state.parent.updateTags(thisFlight.state.tags);
                }else{
                    thisFlight.showNoEditError();
                }
            },
            error : function(jqXHR, textStatus, errorThrown) {
            },
            async: true
        });
    }

    showNoEditError(){
        errorModal.show("Error editing tag", "Please make a change to the tag first before pressing submit!");
    }

    confirmDelete(){
        console.log("delete is confirmed!");
        this.removeTag(this.state.activeTag.hashId, true);
    }

    showAddForm(){
        console.log("displaying add form!");
        this.state.addFormActive = !this.state.addFormActive;
        this.setState(this.state);
        this.toggleAssociateTag();
    }

    toggleAssociateTag(){
        console.log("displaying tag association!");
        this.state.assocTagActive = !this.state.assocTagActive;
        this.setState(this.state);
    }

    removeTag(id, perm){
        console.log("un-associating tag #"+id+" with flight #"+this.state.flightId);

        if(id == -1){
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

        let thisFlight = this;

        $.ajax({
            type: 'POST',
            url: '/protected/remove_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
				if(perm){
					let allFlights = response;
					thisFlight.state.parent.invokeUpdate(allFlights);
				}else{
					thisFlight.state.tags = response;
					thisFlight.setState(thisFlight.state);
					thisFlight.getUnassociatedTags();
					thisFlight.state.detailsActive = false;
					thisFlight.state.addFormActive = false;
					thisFlight.state.addActive = false;
					thisFlight.setState(thisFlight.state);
				}
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

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
                }
                thisFlight.getUnassociatedTags();
                thisFlight.setState(thisFlight.state);
                thisFlight.state.parent.updateTags(thisFlight.state.tags);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                //TODO: resolve duplicate tag creation here
            },   
            async: true 
        });  
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let vcellStyle = { "overflowY" : "visible"};
        let addForm = "";
        let addDrop = "";
        let submitButton = "";
        let activeTag = this.state.activeTag;
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
        console.log("usac tags: "+unassociatedTags);
        let hasOtherTags = unassociatedTags != null;

        let activeId = -1;
        if(this.state.activeTag != null){
            activeId = activeTag.hashId;
        }

        let tagStat = "";
        if(tags == null || tags.length == 0){
            tagStat = <div><b className={"p-2"} style={{marginBottom:"2"}}>No tags yet!</b>
                <button className={buttonClasses} style={styleButtonSq} data-toggle="button" title="Add a tag to this flight" onClick={() => this.addClicked()}>Add a tag</button>
            </div>
        }else{
           tagStat =  
                <div className={cellClasses} style={cellStyle}>
                {
                    tags.map((tag, index) => {
                        var cStyle = {
                            flex : "0 10 10em",
                            backgroundColor : tag.color,
                            color : 'white',
                            fontWeight : '550'
                        };
                        return (
                                <button className={buttonClasses} style={cStyle} data-toggle="button" onClick={() => this.editTag(tag)}>{tag.name}</button>
                        );
                    })
                }
                <button className={buttonClasses} style={styleButtonSq} data-toggle="button" aira-pressed={this.state.addActive} title="Add a tag to this flight" onClick={() => this.addClicked()}><i class="fa fa-plus" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Remove the selected tag from this flight" onClick={() => this.removeTag(activeId, false)}><i class="fa fa-minus" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Permanently delete the selected tag from all flights" onClick={() => this.deleteTag()}><i class="fa fa-trash" aria-hidden="true"></i></button>
                <button className={buttonClasses} style={styleButtonSq} title="Clear all the tags from this flight" onClick={() => this.clearTags()}><i class="fa fa-eraser" aria-hidden="true"></i></button>
                </div>
        }
        let tagInfo = "";
        console.log(tags);
       
        let details = "";
        if(this.state.detailsActive){
            details = 
                <Alert variant="primary">
                    {this.state.activeTag.description}
                </Alert>
        }

        let defName = "", defDescript = "", defColor=Colors.randomValue(), defAddAction = (() => this.addTag());
        if(this.state.editing){
            console.log("EDITING THE FORMS");
            defName = this.state.activeTag.name;
            defDescript = this.state.activeTag.description;
            defColor = this.state.activeTag.color;
            console.log(this.state.addFormActive);
            defAddAction = (
                (() => this.submitEdit())
            );
        }


        if(this.state.addActive){
            addDrop =
                <DropdownButton className={cellClasses} id="dropdown-item-button" variant="outline-secondary" title="Add a tag to this flight">
                    <Dropdown.Item as="button" onSelect={() => this.showAddForm()}>Create a new tag</Dropdown.Item>
                    {unassociatedTags != null &&
                        <Dropdown.Divider />
                    }
                    {unassociatedTags != null &&
                        unassociatedTags.map((tag, index) => {
                            let style = {
                                backgroundColor : tag.color,
                                color : 'white'
                            }
                            return (
                                    <Dropdown.Item as="button" style={style}  onSelect={() => this.associateTag(tag.hashId)}>{tag.name}</Dropdown.Item>
                            );
                        })
                    }
                    </DropdownButton>
        }
        if(this.state.addFormActive){
            addForm =
            <div class="row p-4">
                <div class="col-">
                    <div class="input-group">
                        <div class="input-group-prepend">
                            <span class="input-group-text">
                                <span class="fa fa-tag"></span>
                            </span>
                        </div>
                        <input type="text" id="comName" class="form-control" defaultValue={defName} placeholder="Common Name"/>
                    </div>
                </div>
                <div class="col-sm">
                    <div class="input-group">
                        <div class="input-group-prepend">
                            <span class="input-group-text">
                                <span class="fa fa-list"></span>
                            </span>
                        </div>
                        <input type="text" id="description" class="form-control" defaultValue={defDescript} placeholder="Description"/>
                    </div>
                </div>
                <div class="col-">
                    <div style={{flex: "0 0"}}>
                        <input type="color" name="eventColor" defaultValue={defColor} id="color" style={styleColorInput}/>
                    </div>
                </div>
                <div class="col-sm">
                    <div class="input-group">
                    <button className="btn btn-outline-secondary" style={styleButtonSq} onClick={defAddAction}>
                            <i class="fa fa-check" aria-hidden="true"></i>
                                Submit
                        </button>
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
                <div class="flex-row p-1">
                    {details}{addDrop}{addForm}{submitButton}
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
            props.events[i].color = Colors.randomValue();
        }

        this.state = {
            events : props.events,
            definitions : definitionsPresent
        };
    }

    updateEventDisplay(index, toggle) {
        let event = this.state.events[index];
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
                    shapes = shapes.splice(i, 1);
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

        return (
            <div>
                <b className={"p-1"} style={{marginBottom:"0"}}>Events:</b>

                {
                    this.state.events.map((event, index) => {
                        return (
                            <div className={cellClasses} style={cellStyle} key={index}>
                                <div style={{flex: "0 0"}}>
                                    <input type="color" name="eventColor" value={event.color} onChange={(e) => {this.changeColor(e, index); }} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                                </div>

                                <button className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.eventClicked(index)}>
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

		//TODO: the error with st not changing has to do with this not being called
        let color = Colors.randomValue();
        console.log("flight color: " );
        console.log(color);
		console.log("!! FLIGHT CONSTRUCTED !!");
        console.log("TAGS for: "+props.flightInfo.id+props.tags);

        this.state = {
            pathVisible : false,
            mapLoaded : false,
            eventsLoaded : false,
            commonTraceNames : null,
            uncommonTraceNames : null,
            traceIndex : [],
            traceVisibility : [],
            tags : null,
            traceNamesVisible : false,
            eventsVisible : false,
            tagsVisible : false,
            itineraryVisible : false,
            tags : props.tags.value,
            layer : null,
			parent : props.parent,
            color : color
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
                request : "GET_DOUBLE_SERIES_NAMES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
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

                    let events = response.events;
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

                    errorModal.show("Error Loading Flight Coordinates", errorThrown);
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
                                width: 1.5
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

	invokeUpdate(flights){
		this.state.parent.invokeUpdate(flights);
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
        let globeTooltip = "";

        let tagTooltip = "Click to tag a flight for future queries and grouping";

        //console.log(flightInfo);
        if (!flightInfo.hasCoords) {
            //console.log("flight " + flightInfo.id + " doesn't have coords!");
            globeClasses += " disabled";
            globeTooltip = "Cannot display flight on the map because the flight data did not have latitude/longitude.";
        } else {
            globeTooltip = "Click the globe to display the flight on the map.";
        }

        let visitedAirports = [];
        for (let i = 0; i < flightInfo.itinerary.length; i++) {
            if ($.inArray(flightInfo.itinerary[i].airport, visitedAirports) < 0) {
                visitedAirports.push(flightInfo.itinerary[i].airport);
            }
        }

		var tags = [];
        if(this.state.tags != null){
            tags = this.state.tags;
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
            console.log("tags are visible");
            tagsRow = (
                    <Tags tags={this.state.tags} flightId={flightInfo.id} parent={this} />
            );
        }

        // let

        let tracesRow = "";
        if (this.state.traceNamesVisible) {
            tracesRow = 
                (
                    <TraceButtons parentFlight={this} flightId={flightInfo.id}/>
                );
        }

        let tagPills = "";
        if(this.state.tags != null){
			console.log("tags in card for "+flightInfo.id+": "+this.state.tags);
            tagPills = 
            tags.map((tag, index) => {
                let style = {
                    backgroundColor : tag.color,
                    marginRight : '4px',
                    lineHeight : '1.5'
                }
                return(
                        <span class="badge badge-pill badge-primary" style={style} page={this.state.page}>{tag.name}</span>
                );
            });
        }

        return (
            <div className="card mb-1">
                <div className="card-body m-0 p-0">
                    <div className="d-flex flex-row p-1"
>
                        <div className={firstCellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            <i className="fa fa-plane p-1"> {flightInfo.id}</i>
                        </div>

                        <div className={cellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            {flightInfo.tailNumber}
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

                        <div className={cellClasses} style={{flexGrow:1}}>
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

                            <button className={buttonClasses + globeClasses} style={styleButton} onClick={() => this.cesiumClicked()}>
                                <i className="fa fa-globe p-1"></i>
                            </button>

                            <button className={buttonClasses + " disabled"} style={styleButton} onClick={() => this.replayClicked()}>
                                <i className="fa fa-video-camera p-1"></i>
                            </button>

                            <button className={lastButtonClasses + globeClasses} style={styleButton} onClick={() => this.downloadClicked()}>
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
       this.filterRef = React.createRef();
    }

    setFlights(flights) {
        this.state.flights = flights;
        this.setState(this.state);
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

    jumpPage(pg){
        if(pg < this.state.numPages && pg >= 0){
            this.state.page = pg;
            this.submitFilter();
        }
    }

    nextPage(){
        this.state.page++;
        this.submitFilter();
    }

    previousPage(){
        this.state.page--;
        this.submitFilter();
    }

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
	
	invokeUpdate(flights){
		this.setFlights(flights);
	}

    render() {
        console.log("rendering flights!");

        let flights = [];
        if (typeof this.state.flights != 'undefined') {
            flights = this.state.flights;

        }

        let pages = this.genPages();
		console.log("Flight State Changed!!");

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
            var prev = <button class="btn btn-primary btn-sm" type="button" onClick={this.previousPage}>Previous Page</button>
            var next = <button class="btn btn-primary btn-sm" type="button" onClick={this.nextPage}>Next Page</button>

            if(begin) {
                prev = <button class="btn btn-primary btn-sm" type="button" onClick={this.previousPage} disabled>Previous Page</button>
            }
            if(end){
                next = <button class="btn btn-primary btn-sm" type="button" onClick={this.nextPage} disabled>Next Page</button>
            }


            console.log(this.state.end);
            return (
                <div className="card-body" style={style}>
                    <Filter ref={this.filterRef} hidden={!this.state.filterVisible} depth={0} baseIndex="[0-0]" key="[0-0]" parent={null} type="GROUP" submitFilter={() => {this.submitFilter()}} rules={rules} submitButtonName="Apply Filter"/>
                        <div class="card mb-1 m-1 border-secondary">
                            <div class="p-2">
                                <button className="btn btn-sm btn-info pr-2" disabled>Page: {this.state.page + 1} of {this.state.numPages}</button>
                                <div class="btn-group mr-1 pl-1" role="group" aria-label="First group">
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
                                                            <Dropdown.Item as="button" onClick={() => this.jumpPage(pages.value)}>{pages.name}</Dropdown.Item>
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
								console.log("NEW FLIGHTS MAPPED!!");
								if(flightInfo != null){
									return (
										<Flight flightInfo={flightInfo} parent={this} tags={flightInfo.tags} key={flightInfo.id}/>
									);
								}
							})
						}
                        <div class="card mb-1 m-1 border-secondary">
                            <div class="p-2">
                                <button className="btn btn-sm btn-info pr-2" disabled>Page: {this.state.page + 1} of {this.state.numPages}</button>
                                <div class="btn-group mr-2 pl-1" role="group" aria-label="First group">
                                    {prev}
                                    {next}
                                </div>
						</div>
                    </div>


                    <div id="load-more"></div>
                </div>
            );
        }else{
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
            });
        });

    });
}

export { flightsCard };
