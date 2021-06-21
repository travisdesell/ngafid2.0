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

import { timeZones } from "./time_zones.js";

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

        this.state = {
            showSavePopover : false,
            showLoadPopover : false,
            loadPopoverTarget : "",
            savePopoverTarget : "",
            saveButtonDisabled : true,
            editingFilter : {},
            filterSaved : false,
            filterName : ""
        }
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
    }

    modifyFilter(filter) {
        let currentFilter = this.state.editingFilter;
        let filterName = this.state.filterName;

        console.log("chaning filter : " + currentFilter.name + " with filter : " + currentFilter.name + " to: " + filterName + " with filter: ");
        console.log(filter);

        this.props.modifyFilter(currentFilter.name, filterName);
    }

    saveFilter() {
        console.log("Saving filter with name: ");
        console.log(this.state.filterName);

        this.props.storeFilter(this.state.filterName);

        this.state.saveButtonDisabled = true;
        this.state.showSavePopover = false;
        this.state.filterSaved = true;
        this.setState(this.state);
    }

    editFilter(filter) {
        this.props.setFilter(JSON.parse(filter.filter));
        this.state.editingFilter = filter;
        this.setState(this.state);
       
        //let target = $("load-filter-button").click(function(event){
            //return event.target;
        //});

        //this.state.loadPopoverTarget = target;
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

    render() {
        var validated = false, loadFilterButtonId = "load-filter-button";


        let errorMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        let editingButtonStyle = {
            color : 'green',
            borderColor : 'green'
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


        console.log("filter props:");
        console.log(this.props);

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

        let saveButtonLabel = "Save Filter";
        if (this.state.filterSaved) {
            console.log("Filter was saved, updating button");
            saveButtonLabel = "Filter Saved!";
        }

        let filters = this.props.storedFilters;

        let loadFilterPopoverContent = "";

        let loadFilterFunction = this.handleLoadClick;
        if (this.state.editingFilter == null){
            loadFilterFunction = this.handleLoadClickPersist;
        }

        const UpdatingPopover = React.forwardRef(({ popper, children, show: _, ...props }, ref) => {
                useEffect(() => {
                  console.log('updating!');
                  popper.scheduleUpdate();
                }, [children, popper]);
            
                return (
                <Popover ref={ref} {...props}>
                  {children}
                </Popover>
                );
        });

        if (filters != null && filters.length > 0) {
            loadFilterPopoverContent = (
                filters.map((filter, index) => {
                    let relIndex = index + 1;
                    let isActive = (this.state.activeId - 1 == index);

                    let filterPillStyle = {
                        backgroundColor : filter.color,
                        marginRight : '4px',
                        lineHeight : '4',
                        opacity : '75%',
                        fontSize : '100%'
                    }
                    //Normal
                    let editButton = ( 
                        <button className="m-1 btn btn-outline-secondary align-right" style={styleButtonSq} onClick={() => this.editFilter(filter)} title="Edit Filter">
                            <i className="fa fa-pencil" aria-hidden="true"></i> 
                        </button>
                    );

                    let nameField = (
                        <button type="button" class="btn" onClick={() => this.setFilter(filter)} key={index} style={{lineHeight : '2', fontSize : '100%', marginRight : '4px', backgroundColor : '#e3e3e3', color : '#000000'}} title="Filter Info">
                            <span className="badge badge-pill badge-primary" style={filterPillStyle} >
                                <i className="fa fa-filter" aria-hidden="true"></i>
                            </span>   {filter.name}
                        </button>
                    );

                    if (filter === this.state.editingFilter) {
                        //When editing 
                        let show = true;
                        editButton = (
                            <div className="m-1">
                                <OverlayTrigger
                                  trigger='click'
                                  key="top"
                                  placement="top"
                                  overlay={
                                    <Tooltip id="tooltip-click-me">
                                      Filter Saved!
                                    </Tooltip>
                                  }
                                >
                                <button className="btn btn-outline-secondary align-right" style={editingButtonStyle} onClick={() => this.modifyFilter(filter)}>
                                    <i className='fa fa-check' aria-hidden='true' style={editingButtonStyle}></i>
                                </button>
                                </OverlayTrigger>
                            </div>
                        );

                        nameField = (
                            <Form.Group controlId="edit-filter-name-dialog">
                                <Form.Control placeholder={filter.name} onChange={(e) => this.setState({ filterName : e.target.value })} />
                            </Form.Group>
                        );
                    }

                    return (
                        <ListGroup.Item active={isActive} key={index}>
                            <Container>
                                <Row className="justify-content-md-center">
                                    <Col xs lg="10">
                                        {nameField}
                                    </Col>
                                    <Col xs lg="1">
                                        {editButton}
                                    </Col>
                                    <Col xs lg="1">
                                        <button className="m-1 btn btn-outline-secondary align-right" style={styleButtonSq} onClick={() => this.props.removeFilter(filter.name)} title="Delete this filter">
                                            <i className="fa fa-trash" aria-hidden="true"></i>
                                        </button>
                                    </Col>
                                </Row>
                            </Container>
                        </ListGroup.Item>
                    );
                })
            );
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
                          <Button hidden={submitHidden} onClick={(event) => this.handleLoadClick(event)} id={loadFilterButtonId} size="sm" className="mr-1">
                              Load a Saved Filter
                          </Button>
                          <Overlay
                            show={this.state.showLoadPopover}
                            target={this.state.loadPopoverTarget}
                            placement="bottom"
                            containerPadding={20}
                          >
                            <UpdatingPopover id="popover-contained" style={popoverStyle}>
                              <Popover.Title as="h3">Load Filter</Popover.Title>
                              <Popover.Content>
                                <ListGroup id="listgrp" defaultActiveKey="#custom" style={listGrpStyle}>
                                {loadFilterPopoverContent}
                                </ListGroup>
                              </Popover.Content>
                            </UpdatingPopover>
                          </Overlay>

                          <Button onClick={handleSaveClick} size="sm" hidden={submitHidden} disabled={this.state.saveButtonDisabled} className="mr-1">
                              {saveButtonLabel}
                          </Button>
                          <Overlay
                            show={this.state.showSavePopover}
                            target={this.state.savePopoverTarget}
                            placement="bottom"
                            containerPadding={20}
                          >
                            <UpdatingPopover id="popover-contained" style={{flex : "fill"}}>
                              <Popover.Title as="h3">{saveButtonLabel}</Popover.Title>
                              <Popover.Content>
                                  <InputGroup hasValidation className="mb-3">
                                      <FormControl placeholder="Filter Name" aria-label="Filter Name" aria-describedby="basic-addon2" value={this.state.filterName} onChange={e => this.setState({ filterName : e.target.value })} required/>
                                      <Form.Control.Feedback type="invalid">
                                          A Filter with this name already exists. Please choose a different name.
                                      </Form.Control.Feedback>
                                          <InputGroup.Append>
                                              <Button onClick={() => {this.saveFilter()}} variant="outline-secondary">Save</Button>
                                          </InputGroup.Append>
                                  </InputGroup>
                              </Popover.Content>
                            </UpdatingPopover>
                          </Overlay>
                        <button type="button" className="btn btn-primary btn-sm mr-1" disabled={submitDisabled} onClick={() => this.props.submitFilter(true /*reset current page*/)} hidden={submitHidden} >{this.props.submitButtonName}</button>
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
                    storedFilters={this.props.storedFilters}
                    removeFilter={(name) => this.props.removeFilter(name)}
                    storeFilter={(name) => this.props.storeFilter(name)}
                    modifyFilter={(currentName, newName) => this.props.modifyFilter(currentName, newName)}
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
