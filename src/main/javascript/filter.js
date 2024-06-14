import 'bootstrap';
import React, { Component, useState, useEffect, useRef, createRef } from "react";
import ReactDOM from "react-dom";
import Overlay from 'react-bootstrap/Overlay';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import Popover from 'react-bootstrap/Popover';
import Tooltip from 'react-bootstrap/Tooltip';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import FormGroup from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import InputGroup from 'react-bootstrap/InputGroup';
import ListGroup from 'react-bootstrap/ListGroup';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import { Colors } from "./map.js";

import { timeZones } from "./time_zones.js";
import { confirmModal } from "./confirm_modal.js";

//Used to check names for filter validation
function isEmptyOrSpaces(str){
    return str === null || str.match(/^ *$/) !== null;
}

function getRuleFromInput(input, rules) {
    let rule = null;
    for (let i = 0; i < rules.length; i++) {
        if (input === rules[i].name) {
            rule = rules[i];
            break;
        }
    }
    return rule;
}

function filterValid(filter, rules) {
    if (filter.type == "GROUP") {
        if (filter.filters.length == 0) {
            return "Group has no rules.";
        } else {
            return "";
        }
    } else if (filter.type == "RULE") {
        //console.log(filter);
        let inputs = filter.inputs;

        if (inputs[0] == 0) {
            return "Please select a rule.";
        } else {
            //console.log("checking rule valid, inputs then rule:");
            //console.log(inputs);
            let rule = getRuleFromInput(inputs[0], rules);
            //console.log(rule);

            if (rule == null) {
                return "Please select a rule.";
            }

            for (let i = 0; i < rule.conditions.length; i++) {
                if (rule.conditions[i].type == "number") {
                    if (typeof inputs[i+1] == 'undefined' || inputs[i+1] == "") {
                        return "Please enter a number.";
                    }

                } else if (rule.conditions[i].type == "time") {
                    if (typeof inputs[i+1] == 'undefined' || inputs[i+1] == "") {
                        return "Please enter a time.";
                    }

                } else if (rule.conditions[i].type == "date") {
                    if (typeof inputs[i+1] == 'undefined' || inputs[i+1] == "") {
                        return "Please enter a date.";
                    }

                } else if (rule.conditions[i].type == "datetime-local") {
                    if (typeof inputs[i+1] == 'undefined' || inputs[i+1] == "") {
                        return "Please enter a date and time.";
                    }
                }
            }
            return "";
        }
    } else {
        return "Unknown filter type: '" + filter.type + "'";
    }
}

function recursiveValid(filters, rules) {
    if (filterValid(filters, rules) != "") return false;

    //console.log("recursiveValid on:");
    //console.log(filters);

    let subFilters = filters.filters;
    for (let i = 0; i < subFilters.length; i++) {
        if (subFilters[i].type == "GROUP") {
            let subValid = recursiveValid(subFilters[i], rules);
            //console.log("GROUP was valid? " + subValid);
            if (!subValid) return false;
        } else if (subFilters[i].type == "RULE") {
            let subValid = (filterValid(subFilters[i], rules) == "");
            //console.log("RULE was valid? " + subValid);
            if (!subValid) return false;
        } else {
            //console.log("type wasn't defined!");
            return false;
        }
    }
    return true;
}

export function isValidFilter(filters, rules) {
    //console.log("isValidFilter?");
    //console.log(rules);
    let valid = recursiveValid(filters, rules);
    //console.log("isValid? " + valid);
    return valid;
}

function getFilterAtTreeIndexHelper(filter, treeIndex) {
    console.log("current treeIndex: " + treeIndex);
    console.log(filter);

    let commaIndex = treeIndex.indexOf(",");
    console.log("commaIndex: " + commaIndex);

    if (commaIndex === -1) {
        console.log("final filter:")
        console.log(filter.filters[treeIndex]);
        return filter.filters[treeIndex];

    } else {
        let filterIndex = treeIndex.substr(0, commaIndex);
        let nextIndex = treeIndex.substr(commaIndex + 1);
        console.log("nextIndex: " + nextIndex);

        return getFilterAtTreeIndexHelper(filter.filters[filterIndex], nextIndex);
    }
}

function getFilterAtTreeIndex(filter, treeIndex) {
    console.log("starting treeIndex: " + treeIndex);
    console.log(filter);

    if (treeIndex === "root") {
        //this is the top level filter
        return filter;
    } else {
        let nextIndex = treeIndex.substr(treeIndex.indexOf(",") + 1);
        return getFilterAtTreeIndexHelper(filter, nextIndex);
    }
}

function andClicked(filter, treeIndex) {
    console.log("and clicked at treeIndex: " + treeIndex);
    let targetFilter = getFilterAtTreeIndex(filter, treeIndex);
    console.log(targetFilter);
    if (targetFilter.type === "GROUP") {
        targetFilter.condition = "AND";
    }
    return filter;
}

function orClicked(filter, treeIndex) {
    console.log("or clicked at treeIndex: " + treeIndex);
    let targetFilter = getFilterAtTreeIndex(filter, treeIndex);
    console.log(targetFilter);
    if (targetFilter.type === "GROUP") {
        targetFilter.condition = "OR";
    }
    return filter;
}

function addRule(filter, treeIndex) {
    console.log("adding rule at treeIndex: " + treeIndex);
    let targetFilter = getFilterAtTreeIndex(filter, treeIndex);
    console.log(targetFilter);

    targetFilter.filters.push({
        type : "RULE",
        inputs : []
    });
    
    return filter;
}

function addGroup(filter, treeIndex) {
    console.log("adding group at treeIndex: " + treeIndex);
    let targetFilter = getFilterAtTreeIndex(filter, treeIndex);
    console.log(targetFilter);

    targetFilter.filters.push({
        type : "GROUP",
        condition : "AND",
        filters : []
    });
    return filter;
}


function removeFilter(filter, treeIndex) {
    console.log("removing filter with treeIndex: " + treeIndex);
    console.log(filter);

    //first check to see if the treeIndex is the root filter, if so, just clear it
    if (treeIndex === "root") {
        //return an empty group
        return {
            type : "GROUP",
            condition : "AND",
            filters : []
        };

    } else {
        //otherwise get the *parent* of the treeIndex
        let parentIndex = treeIndex.substr(0, treeIndex.lastIndexOf(","));
        let childIndex = treeIndex.substr(treeIndex.lastIndexOf(",") + 1);
        console.log("parentIndex: " + parentIndex + ", childIndex: " + childIndex);

        let parentFilter = getFilterAtTreeIndex(filter, parentIndex);
        parentFilter.filters.splice(childIndex, 1);
    }

    return filter;
}

function ruleChange(filter, treeIndex, rules, inputs, event) {
    console.log("changing rule with treeIndex: " + treeIndex);
    console.log(inputs);
    console.log("event.target.value: " + event.target.value);
    console.log("filter: ");
    console.log(filter);

    inputs[0] = event.target.value;
    inputs.splice(1);

    return filter;
}


function ruleValueChange(filter, treeIndex, inputs, index, event) {
    console.log("changing rule with treeIndex: " + treeIndex);
    console.log("modified index: " + index);
    console.log(inputs);
    console.log("event.target.value: " + event.target.value);
    console.log("filter: ");
    console.log(filter);

    inputs[index + 1] = event.target.value;

    return filter;
}


class Rule extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let inputs = this.props.filter.inputs;
        let selectedRule = getRuleFromInput(inputs[0], this.props.rules);

        //console.log("rendering rule");
        //console.log(inputs);
        //console.log(selectedRule);

        if (selectedRule == null) {
            return (
                <div style={{display: "flex", flexDirection: "row"}}>
                    <select id="stateSelect" type="select" className="form-control" onChange={(event) => this.props.setFilter(ruleChange(this.props.getFilter(), this.props.treeIndex, this.props.rules, inputs, event))} style={{flexBasis:"180px", flexShrink:0, marginRight:5}} value={inputs[0]}>
                        <option value="Select Rule">Select Rule</option>
                        { 
                            this.props.rules.map((ruleInfo, index) => {
                                //console.log("adding rule: " + ruleInfo.name + ", with value: " + index);
                                return ( <option value={ruleInfo.name} key={"rule-" + index}>{ruleInfo.name}</option> );
                            })
                        }
                    </select>

                    <button type="button" className="btn btn-danger btn-sm"> <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.props.setFilter(removeFilter(this.props.getFilter(), this.props.treeIndex))}></i> </button>
                </div>
            );

        } else {
            //console.log("selectedRule.conditions:");
            //console.log(selectedRule.conditions);
            //console.log("inputs:");
            //console.log(inputs);

            while (inputs.length < selectedRule.conditions.length + 1) {
                let lastInput = inputs.length - 1;
                if (selectedRule.conditions[lastInput].type == "select") {
                    inputs.push(selectedRule.conditions[lastInput].options[0]);
                } else {
                    inputs.push("");
                }
            }

            //set the time zone automatically
            if ((inputs[0] == "Start Date and Time") ||
                (inputs[0] == "End Date and Time") ||
                (inputs[0] == "Start Time") ||
                (inputs[0] == "End Time")) {

                var split = new Date().toString().split(" ");
                console.log(split);
                //var timeZoneFormatted = split[split.length - 2] + " " + split[split.length - 1];
                var timeZoneFormatted = split[split.length - 2];
                timeZoneFormatted = timeZoneFormatted.slice(0, 6) + ":" + timeZoneFormatted.slice(6);

                console.log("current time zone is: " + timeZoneFormatted);

                let timeZoneIndex = 0;
                for (let i = 0; i < timeZones.length; i++) {
                    if (timeZones[i].includes(timeZoneFormatted)) {
                        console.log("changing time zone index to " + i + ", '" + timeZones[i] + "'");
                        timeZoneIndex = i;
                        if (timeZones[i].includes("US")) {
                            break;
                        }
                    }
                }

                //set the time zone to the current time zone
                inputs[3] = timeZones[timeZoneIndex];
            }

            //console.log("fixed inputs:");
            //console.log(inputs);


            return (
                <div style={{display: "flex", flexDirection: "row"}}>
                    <select id="stateSelect" type="select" className="form-control" onChange={(event) => this.props.setFilter(ruleChange(this.props.getFilter(), this.props.treeIndex, this.props.rules, inputs, event))} style={{flexBasis:"180px", flexShrink:0, marginRight:5}} value={inputs[0]}>

                        <option value="Select Rule">Select Rule</option>
                        { 
                            this.props.rules.map((ruleInfo, index) => {
                                //console.log("adding rule: " + ruleInfo.name + ", with value: " + index);
                                return ( <option value={ruleInfo.name} key={"rule-" + index}>{ruleInfo.name}</option> );
                            })
                        }
                    </select>

                    {
                        selectedRule.conditions.map((conditionInfo, index) => {
                            if (conditionInfo.type == "select") {
                                let flexBasis = "150px";
                                if (conditionInfo.name == "timezone") {
                                    flexBasis = "375px";
                                }

                                return (
                                    <select id="stateSelect" type={conditionInfo.type} key={"select-" + index} className="form-control" onChange={(event) => this.props.setFilter(ruleValueChange(this.props.getFilter(), this.props.treeIndex, inputs, index, event))} style={{flexBasis:flexBasis, flexShrink:0, marginRight:5}} value={inputs[index + 1]}>
                                        { 
                                            conditionInfo.options.map((optionInfo, index) => {
                                                return ( <option value={optionInfo} key={conditionInfo.name + "-" + index} >{optionInfo}</option> );
                                            })
                                        }
                                    </select>
                                );

                            } else if (conditionInfo.type == "time" || conditionInfo.type == "date" || conditionInfo.type == "number" || conditionInfo.type == "datetime-local") {

                                return (
                                    <input type={conditionInfo.type} step="any" key={"input-" + index} className={"form-control"} aria-describedby="valueHelp" placeholder={"Enter " + conditionInfo.name} onChange={(event) => this.props.setFilter(ruleValueChange(this.props.getFilter(), this.props.treeIndex, inputs, index, event))} style={{flexBasis:"150px", flexShrink:0, marginRight:5}} value={inputs[index + 1]}/>
                                );
                            }
                        })
                    }

                    <button type="button" className="btn btn-danger btn-sm"> <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.props.setFilter(removeFilter(this.props.getFilter(), this.props.treeIndex))}></i> </button>

                </div>
            );
        }
    }
}


class Group extends React.Component {
    constructor(props) {
        super(props);

        let colorRand = Colors.randomValue();

        this.state = {
            showSavePopover : false,
            showLoadPopover : false,
            loadPopoverTarget : "",
            savePopoverTarget : "",
            saveButtonDisabled : true,
            storedFilters : props.storedFilters,
            editingFilter : {},
            filterSaved : false,
            filterName : "",
            filterColor : colorRand
        }

        this.handleColorChange = this.handleColorChange.bind(this);
    }

    toggleLoadPopover() {
        this.state.showLoadPopover = !this.state.showLoadPopover;
        this.setState(this.state);
    }

    setLoadPopoverTarget(event) {
        this.state.loadPopoverTarget = event.target;
        console.log("setting load popover target");
        this.setState(this.state);
    }

    toggleSavePopover() {
        this.state.showSavePopover = !this.state.showSavePopover;
        this.setState(this.state);
    }

    setSavePopoverTarget(event) {
        this.state.savePopoverTarget = event.target;
        this.setState(this.state);
    }

    setFilter(filter) {
        this.toggleLoadPopover();
        let filterJSON = JSON.parse(filter.filter);
        this.props.setFilter(filterJSON);

        setTimeout(function() { //show resolution tooltip for a max 10s
            this.props.submitFilter();
         }.bind(this), 50)
    }

    saveFilter() {
        console.log("Saving filter with name: ");
        console.log(this.state.filterName);

        this.storeFilter(this.state.filterName, this.state.filterColor);
    }

    submitChanges(filter) {
        let thisFilter = this;

        let submissionData = {
            currentName : filter.name,
            newName : this.state.filterName,
            filterJSON : JSON.stringify(this.props.getFilter()),
            color : this.state.filterColor
        };

        if (isEmptyOrSpaces(submissionData.newName)) {
            $('#modify-filter-submit-button').attr('data-title', 'Please make sure the filter name is not empty before saving.').tooltip('show');
            setTimeout(function() { //show resolution tooltip for a max 10s
                $('#modify-filter-submit-button').tooltip('hide');
             }.bind(this), 10000)

            return;
        } else if (submissionData.newName === submissionData.currentName && filter.filter === this.props.getFilter() && filter.color === submissionData.color) {
            $('#modify-filter-submit-button').attr('data-title', 'Please make sure the filter name is different from its original name, the filter color is different, or that the filter rules are different.').tooltip('show');
            setTimeout(function() { //show resolution tooltip for a max 10s
                $('#modify-filter-submit-button').tooltip('hide');
             }.bind(this), 10000)

            return;
        }

        console.log("Mofifying filter " + filter.name);

        $.ajax({
            type: 'POST',
            url: '/protected/modify_filter',
            data : submissionData,
            dataType : 'json',
            timeout : 0, 
            success : function(response) {
                if (response === "DUPLICATE_PK") {
                    $('#modify-filter-submit-button').tooltip('show');
                    setTimeout(function() { //show resolution tooltip for a max 10s
                        $('#modify-filter-submit-button').tooltip('hide');
                     }.bind(this), 10000)
                } else {
                    thisFilter.state.editingFilter = {};
                    thisFilter.state.showLoadPopover = false;
                    
                    $('#modify-filter-submit-button').tooltip('hide');
                    $('#load-filter-button').tooltip('show');
                    setTimeout(function() { //show resolution tooltip for a max 10s
                        $('#load-filter-button').tooltip('hide');
                     }.bind(this), 10000)
                }

                thisFilter.setState(thisFilter.state);
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },   
            async: true 
        });  
    }

    deleteFilterClicked(name) {
        confirmModal.show("Confirm Delete Filter: '" + name + "'",
                        "Are you sure you wish to delete filter '" + name +"'?\n\nThis operation will remove it from your fleet, meaning it will be deleted for other users as well. This operation cannot be undone!",
                        () => {this.deleteFilter(name)});
    }

    deleteFilter(name) {
        let thisFilter = this;

        let submissionData = {
            name : name,
        };

        console.log("Removing filter " + name);
        console.log(this.filterRef);

        $.ajax({
            type: 'POST',
            url: '/protected/remove_filter',
            data : submissionData,
            dataType : 'json',
            timeout : 0, 
            success : function(response) {
                thisFilter.setState(thisFilter.state);
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },   
            async: true 
        });  
    }

    storeFilter(name, color) {
        let thisFilter = this;

        let submissionData = {
            name : name,
            filterJSON : JSON.stringify(this.props.getFilter()),
            color : color
        };

        if (isEmptyOrSpaces(name)) {
            console.log('empty str');
            $('#save-filter-button-card').attr('data-title', 'Please make sure the filter name is not empty before saving.').tooltip('show');
            setTimeout(function() { //show resolution tooltip for a max 10s
                $('#save-filter-button-card').tooltip('hide');
             }.bind(this), 10000)

            return;
        }

        console.log("Storing filter " + name);

        $.ajax({
            type: 'POST',
            url: '/protected/store_filter',
            data : submissionData,
            dataType : 'json',
            timeout : 0, 
            success : function(response) {
                if (response === "DUPLICATE_PK") {
                    console.log("duplicate pk detected");
                    $('#save-filter-button-card').tooltip('show');
                    setTimeout(function() { //show resolution tooltip for a max 10s
                        $('#save-filter-button-card').tooltip('hide');
                     }.bind(this), 10000)
                } else {
                    $('#save-filter-button-card').tooltip('hide');
                    $('#save-filter-button').tooltip('show');

                    thisFilter.state.saveButtonDisabled = true;
                    thisFilter.state.showSavePopover = false;
                    thisFilter.state.filterSaved = true;

                    setTimeout(function() { //show success tooltip for a max 5s
                        $('#save-filter-button').tooltip('hide');
                     }.bind(this), 5000)
                }

                thisFilter.setState(thisFilter.state);
            },

            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },   
            async: true 
        });  
    }

    getStoredFilters() {
        let storedFilters = [];

        $.ajax({
            type: 'GET',
            url: '/protected/stored_filters',
            dataType : 'json',
            success : function(response) {
                console.log("received filters response: ");
                console.log(response);
                
                storedFilters = response;
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: false 
        });  

        return storedFilters;
    }

    editFilter(filter) {
        this.props.setFilter(JSON.parse(filter.filter));
        this.state.editingFilter = filter;
        this.state.filterName = filter.name;
        this.state.filterColor = filter.color;
        this.setState(this.state);
       
        //let target = $("load-filter-button").click(function(event){
            //return event.target;
        //});

        //this.state.loadPopoverTarget = target;
    }

    setStoredFilters(filters) {
        this.setState({
            storedFilters : filters
        });
    }

    handleLoadClick(event) {
        this.toggleLoadPopover();
        this.setLoadPopoverTarget(event);
    }

    setSelectedFilter(filter) {
        this.props.setFilter(JSON.parse(filter));
    }
   
    renderFilterSelector(getFiltersMethod, submitMethod) {
        let filters = getFiltersMethod();

        loadFilterModal.show(filters, submitMethod);
    }

    handleColorChange(event) {
        let color = event.target.value;
        console.log(color);

        this.setState({
            filterColor : color
        });
    };

    render() {
        var validated = false, loadFilterButtonId = "load-filter-button";

        let errorMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        let tooltipStyle = {
            backgroundColor : 'green'
        }

        let styleButtonSq = {
            flex : "right",
            float : "auto"
        };

        let listStyle = {
            maxHeight: "400px",
            overflowY: "scroll"
        }

        let popoverStyle = {
            maxHeight: "400px",
            minWidth: "800px"
        }

        let listGrpStyle = {
            maxHeight: "400px"
        }

        let filterPillStyle = {
            marginRight : '4px',
            lineHeight : '4',
            opacity : '75%',
            fontSize : '100%',
        }

        let errorHidden = true;
        let errorMessage = "";
        if (this.props.filters.length == 0) {
            errorHidden = false;
            errorMessage = "Group has no rules.";
        }


        //console.log("GROUP: index: " + this.props.treeIndex);
        //console.log(this.props.filters);

        let andChecked = this.props.filters.condition === "AND";
        let andActive = "";
        let orActive = "";
        if (andChecked) {
            andActive = "active";
        } else {
            orActive = "active";
        }


        const handleSaveClick = (event) => {
            this.toggleSavePopover();
            validated = true;
            this.setSavePopoverTarget(event);
        };

        //if (this.state.filterSaved) {
            //$('#save-filter-button').tooltip('show');
            //this.state.filterSaved = false;
        //} else {
            //// wait 15s to hide success msg
            //setTimeout(function() { //start the timer
                //$('#save-filter-button').tooltip('hide');
             //}.bind(this), 15000)
        //}

        let filters = this.state.storedFilters;

        console.log(filters);

        let loadFilterPopoverContent = "";

        let loadFilterFunction = this.handleLoadClick;
        if (this.state.editingFilter == null){
            loadFilterFunction = this.handleLoadClickPersist;
        }

        var saveCard = "";
        if (this.state.showSavePopover) {
            saveCard = (
                <div className="card m-1 float-right" style={{minWidth : "500px"}}>
                    <div className="card-header float-left">Store Filter:
                        <button type="button" className="mr-1 btn btn-danger btn-sm float-right">
                            <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.setState({showSavePopover : false})}></i> 
                        </button>
                    </div>
                <div className="card-body">
                      <div className="input-group mb-3">
                            <div className="input-group-prepend">
                                  <button type="button" className="btn btn-outline-secondary" title="Assign a color to this filter" onClick={(e) => $("#color-picker-filter").click()}>
                                      <span className="badge badge-pill badge-primary" style={filterPillStyle , {backgroundColor : this.state.filterColor, verticalAlign : 'text-bottom'}}>
                                          <i className="fa fa-filter" aria-hidden="true"></i>
                                      </span>
                                  </button>
                                  <input key="cc-0" type="color" className="hidden" style={{display: "none"}} name="eventColor" onChange={e => this.setState({filterColor: e.target.value})} value={this.state.filterColor} id="color-picker-filter"/>
                            </div>
                                <input type="text" className="form-control" placeholder="Filter Name" aria-label="Filter Name" aria-describedby="basic-addon2" value={this.state.filterName} onChange={e => this.setState({ filterName : e.target.value })} />
                            <div className="input-group-append">
                                  <button type="button" id='save-filter-button-card' className="btn btn-outline-secondary" onClick={() => {this.saveFilter()}} data-toggle="tooltip" data-trigger='manual' data-placement="top" data-title='A filter with that name already exists in your fleet. Please provide a unique name.'>Save</button>
                            </div>
                      </div>
                  </div>
              </div>
            );
        }

        var loadCard = "";
        if (this.state.showLoadPopover) {
            let filters = this.getStoredFilters();
            if(filters != null && filters.length > 0) {
                loadCard = (
                    <div className="card m-1 float-right" style={{minWidth : "800px"}}>
                            <div className="card-header float-left">Saved Filters:
                                <button type="button" className="mr-2 btn btn-danger btn-sm float-right">
                                    <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.setState({showLoadPopover : false})}></i> 
                                </button>
                            </div>
                        <div className="card-body" style={{maxHeight : "400px", overflowY : "scroll"}}>
                            <ul className="list-group">
                            {
                                filters.map((filter, index) => {
                                    let initColor = filter.color;
                                    let newColor = JSON.parse(JSON.stringify(this.state.filterColor)); //make copy for each index
                                    //Normal
                                    let editButton = ( 
                                        <button className="m-1 btn btn-outline-primary align-right" style={styleButtonSq} onClick={() => this.editFilter(filter)} title="Edit Filter">
                                            <i className="fa fa-pencil" aria-hidden="true"></i> 
                                        </button>
                                    );

                                    let nameField = (
                                        <div className="col-lg-10">
                                            <button type="button" className="m-1 btn btn-secondary" onClick={() => this.setFilter(filter)} key={index} style={{lineHeight : '1', fontSize : '100%', marginRight : '4px', backgroundColor : '#e3e3e3', color : '#000000'}} title="Filter Info">
                                                <span className="badge badge-pill badge-primary" style={filterPillStyle , {backgroundColor : filter.color, verticalAlign : 'text-bottom'}}>
                                                    <i className="fa fa-filter" aria-hidden="true"></i>
                                                </span>
                                                <span className="ml-2 font-weight-bold">
                                                    {filter.name}
                                                </span>
                                            </button>
                                        </div>
                                    );

                                    if (filter.name == this.state.editingFilter.name) {
                                        //When editing 
                                        editButton = (
                                            <button className="m-1 btn btn-outline-success align-right" id='modify-filter-submit-button' onClick={() => this.submitChanges(filter)} data-toggle="tooltip" data-trigger='manual' data-placement="top" title="Submit Changes" data-title="A filter in your fleet already exists with that name! Please choose another name.">
                                                <i className='fa fa-check' aria-hidden='true'></i>
                                            </button>
                                        );

                                        nameField = (
                                              <><div className="col-lg-9 input-group mb-3">
                                                    <div className="input-group-prepend">
                                                          <button type="button" className="btn btn-outline-secondary" title="Assign a different color to this filter" onClick={(e) => $("#color-picker-filter-mod").click()}>
                                                              <span className="badge badge-pill badge-primary" style={filterPillStyle , {backgroundColor : this.state.filterColor, verticalAlign : 'text-bottom'}}>
                                                                  <i className="fa fa-filter" aria-hidden="true"></i>
                                                              </span>
                                                          </button>
                                                          <input key="cc-1" type="color" className="hidden" style={{display: "none"}} name="eventColor" onChange={e => this.setState({filterColor: e.target.value})} value={this.state.filterColor} id="color-picker-filter-mod"/>
                                                    </div>
                                                    <input type="text" className="form-control" aria-label="Filter Name" aria-describedby="basic-addon2" value={this.state.filterName} onChange={e => this.setState({ filterName : e.target.value })} />
                                              </div>
                                              <div className="col-lg-1">
                                                    <button className="m-1 btn btn-outline-danger align-right" style={styleButtonSq} onClick={e => this.setState({editingFilter : {}})} title="Discard Changes">
                                                        <i className="fa fa-times" aria-hidden="true"></i>
                                                    </button>
                                              </div></>
                                        );
                                    }

                                    return (
                                        <li className="list-group-item" key={index}>
                                            <div className="container">
                                                <div className="row justify-content-md-center">
                                                    {nameField}
                                                    <div className="col-lg-1">
                                                        {editButton}
                                                    </div>
                                                    <div className="col-lg-1">
                                                        <button className="m-1 btn btn-outline-primary align-right" style={styleButtonSq} onClick={() => this.deleteFilterClicked(filter.name)} title="Delete this filter">
                                                            <i className="fa fa-trash" aria-hidden="true"></i>
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        </li>
                                    );
                                })
                            }
                        </ul>
                    </div>
                </div>
            );
            } else {
                loadCard = (
                    <div className="card m-1 float-right" style={{maxWidth : "500px"}}>
                        <div className="card-header float-left">No filters stored yet.
                            <button type="button" className="mr-2 btn btn-danger btn-sm float-right">
                                <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.setState({showLoadPopover : false})}></i> 
                            </button>
                        </div>
                        <div className="card-body">
                            No filters stored yet for this fleet.
                        </div>
                    </div>
                );
            }
        }

        let submitHidden = true;
        let submitDisabled = true;
        if (typeof this.props.submitButtonName !== 'undefined') {
            submitHidden = false;
            submitDisabled = !isValidFilter(this.props.filters, this.props.rules);
            this.state.saveButtonDisabled = !isValidFilter(this.props.filters, this.props.rules) || this.state.filterSaved;
            this.state.filterSaved = false;
        }

        return (
            <div className="card mb-1 m-1 border-secondary" style={{background : "rgba(248,259,250,0.8)", margin:0}}>
                <div className="d-flex justify-content-between">

                    <div className="p-2">
                        <div className="btn-group btn-group-toggle" data-toggle="buttons">
                            <label className={"btn btn-outline-primary btn-sm " + andActive} onClick={() => this.props.setFilter(andClicked(this.props.getFilter(), this.props.treeIndex))}>
                                <input type="radio" name="options" id="option1" autoComplete="off" defaultChecked={andChecked} />AND
                            </label>
                            <label className={"btn btn-outline-primary btn-sm " + orActive} onClick={() => this.props.setFilter(orClicked(this.props.getFilter(), this.props.treeIndex))}>
                                <input type="radio" name="options" id="option2" autoComplete="off" defaultChecked={!andChecked} />OR
                            </label> 
                        </div>

                    </div>

                    <div className="p-2">
                        <button type="button" className="btn btn-primary btn-sm mr-1" onClick={() => this.props.setFilter(addRule(this.props.getFilter(), this.props.treeIndex))}>Add Rule</button>
                        <button type="button" className="btn btn-primary btn-sm mr-1" onClick={() => this.props.setFilter(addGroup(this.props.getFilter(), this.props.treeIndex))}>Add Group</button>
                        <button type="button" className="btn btn-danger btn-sm"> <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.props.setFilter(removeFilter(this.props.getFilter(), this.props.treeIndex))}></i> </button>
                    </div>
                </div>

                {
                    this.props.filters.filters.map((filterInfo, index) => {
                        if (filterInfo.type === "GROUP") {
                            return (
                                <div className="p-2" key={this.props.treeIndex + "," + index}>
                                    <Group
                                        key={this.props.treeIndex + "," + index}
                                        treeIndex={this.props.treeIndex + "," + index}
                                        rules={this.props.rules}
                                        filters={filterInfo}
                                        getStoredFilters={() => this.props.getStoredFilters()}
                                        getFilter={() => {return this.props.getFilter()}}
                                        setFilter={(filter) => this.props.setFilter(filter)}
                                        setSortByColumn={(sortColumn) => this.props.setSortByColumn(sortColumn)}
                                        getSortByColumn={() => {return this.props.getSortByColumn}}
                                    />
                                </div>);

                        } else if (filterInfo.type === "RULE") {
                            return (
                                <div className="p-2" key={this.props.treeIndex + "," + index}>
                                    <Rule
                                        key={this.props.treeIndex + "," + index}
                                        treeIndex={this.props.treeIndex + "," + index}
                                        rules={this.props.rules}
                                        filter={filterInfo}
                                        getStoredFilters={() => this.props.getStoredFilters()}
                                        getFilter={() => {return this.props.getFilter()}}
                                        setFilter={(filter) => this.props.setFilter(filter)}
                                    />
                                </div>);
                        }
                    })
                }

                <div className="d-flex justify-content-end">
                    <div className="p-2 flex-fill">
                        <span style={errorMessageStyle} hidden={errorHidden}>{errorMessage}</span>
                    </div>

                    <div className="p-2">
                          <button type="button" className="btn btn-primary btn-sm mr-1" hidden={submitHidden} onClick={(event) => this.handleLoadClick(event)} id={loadFilterButtonId} data-toggle='tooltip' data-placement='top' data-trigger='manual' data-title='Changes Saved!'>
                              Load a Saved Filter
                          </button>
                          <button id="save-filter-button" type="button" className="btn btn-primary btn-sm mr-1" onClick={handleSaveClick} hidden={submitHidden} disabled={this.state.saveButtonDisabled} data-toggle="tooltip" data-trigger='manual' data-placement="top" title="Filter saved successfully">
                              Save Filter
                          </button>
                        <button type="button" className="btn btn-primary btn-sm mr-1" disabled={submitDisabled} onClick={() => this.props.submitFilter(true /*reset current page*/)} hidden={submitHidden} >
                            {this.props.submitButtonName}
                        </button>
                    </div>

                </div>
                <div className="container" style={{maxWidth : '100%'}}>
                    <div className="d-flex flex-row-reverse">
                        {saveCard}
                        {loadCard}
                    </div>
                </div>
            </div>

        );
    }
}

class Filter extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="card-body" hidden={!this.props.filterVisible} style={{padding:0, margin:0}}>
                <Group
                    treeIndex="root" 
                    submitButtonName={this.props.submitButtonName}
                    rules={this.props.rules}
                    filters={this.props.filters}
                    getFilter={() => {return this.props.getFilter()}}
                    setFilter={(filter) => this.props.setFilter(filter)}
                    submitFilter={() => this.props.submitFilter()}
                    setSortByColumn={(sortColumn) => this.props.setSortByColumn(sortColumn)}
                    getSortByColumn={() => {return this.props.getSortByColumn}}
                />

            </div>
        );
    }
}

export { Filter };
