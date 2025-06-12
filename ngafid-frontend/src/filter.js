import 'bootstrap';
import React from "react";
import {Colors} from "./map.js";
import { showErrorModal } from './error_modal.js';
import {timeZones} from "./time_zones.js";
import {showConfirmModal} from "./confirm_modal.js";

const MESSAGE_BIND_PERIOD_MS = 3_000;

const FILTER_VALIDATE_SUCCESS = "";

//Used to check names for filter validation
function isEmptyOrSpaces(str) {
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

    //Filter is a group...
    if (filter.type == "GROUP") {

        //Filter is empty / has no rules -> Invalidate
        if (filter.filters.length == 0)
            return "Group has no rules.";

        //Filter is non-empty -> Success 
        else
            return FILTER_VALIDATE_SUCCESS;

        //Filter is a rule...
    } else if (filter.type == "RULE") {

        const inputs = filter.inputs;

        if (inputs[0] == 0) {
            return "Please select a rule.";
        } else {
            //console.log("checking rule valid, inputs then rule:");
            //console.log(inputs);
            const rule = getRuleFromInput(inputs[0], rules);
            //console.log(rule);

            if (rule == null) {
                return "Please select a rule.";
            }

            for (let i = 0; i < rule.conditions.length; i++) {

                if (rule.conditions[i].type == "number") {
                    if (typeof inputs[i + 1] == 'undefined' || inputs[i + 1] == "") {
                        return "Please enter a number.";
                    }

                } else if (rule.conditions[i].type == "time") {
                    if (typeof inputs[i + 1] == 'undefined' || inputs[i + 1] == "") {
                        return "Please enter a time.";
                    }

                } else if (rule.conditions[i].type == "date") {
                    if (typeof inputs[i + 1] == 'undefined' || inputs[i + 1] == "") {
                        return "Please enter a date.";
                    }

                } else if (rule.conditions[i].type == "datetime-local") {
                    if (typeof inputs[i + 1] == 'undefined' || inputs[i + 1] == "") {
                        return "Please enter a date and time.";
                    }
                } else if (rule.conditions[i].options.length === 0) {
                    return "No options available for this condition.";
                }

            }
            return "";
        }
    } else {
        return `Unknown filter type: '${  filter.type  }'`;
    }
}

function recursiveValid(filters, rules) {

    //Filter detected as invalid, return false
    if (filterValid(filters, rules) != FILTER_VALIDATE_SUCCESS)
        return false;

    const subFilters = filters.filters;
    for (let i = 0; i < subFilters.length; i++) {

        if (subFilters[i].type == "GROUP") {

            //Check if subgroup is valid
            const subValid = recursiveValid(subFilters[i], rules);

            //Subgroup was invalid, return false
            if (!subValid)
                return false;

        } else if (subFilters[i].type == "RULE") {

            //Check if rule is valid
            const subValid = (filterValid(subFilters[i], rules) == FILTER_VALIDATE_SUCCESS);

            //Subgroup was invalid, return false
            if (!subValid)
                return false;

        } else {

            console.log("Got unknown filter type: ", subFilters[i].type);
            return false;

        }

    }

    return true;

}

export function isValidFilter(filters, rules) {

    //Recursively validate filter
    const valid = recursiveValid(filters, rules);
    return valid;

}

function getFilterAtTreeIndexHelper(filter, treeIndex) {

    console.log(`Current treeIndex: ${  treeIndex}`);
    console.log("Filter: ", filter);

    const commaIndex = treeIndex.indexOf(",");
    console.log("Current commaIndex: ", commaIndex);

    const COMMA_INDEX_NONE = -1;

    if (commaIndex === COMMA_INDEX_NONE) {

        console.log("Final Filter: ", filter.filters[treeIndex]);
        return filter.filters[treeIndex];

    } else {

        const filterIndex = treeIndex.substr(0, commaIndex);
        const nextIndex = treeIndex.substr(commaIndex + 1);
        console.log(`nextIndex: ${  nextIndex}`);

        return getFilterAtTreeIndexHelper(filter.filters[filterIndex], nextIndex);

    }

}

function getFilterAtTreeIndex(filter, treeIndex) {

    console.log("Starting treeIndex: ", treeIndex);
    console.log("Filter: ", filter);

    //At top level of filter
    if (treeIndex === "root") {

        return filter;

    } else {

        const nextIndex = treeIndex.substr(treeIndex.indexOf(",") + 1);
        return getFilterAtTreeIndexHelper(filter, nextIndex);

    }
}

function andClicked(filter, treeIndex) {

    console.log("AND clicked at treeIndex: ", treeIndex);
    const targetFilter = getFilterAtTreeIndex(filter, treeIndex);
    console.log(targetFilter);

    //Filter type is GROUP, toggle condition to AND
    if (targetFilter.type === "GROUP")
        targetFilter.condition = "AND";

    return filter;

}

function orClicked(filter, treeIndex) {

    console.log("OR clicked at treeIndex: ", treeIndex);
    const targetFilter = getFilterAtTreeIndex(filter, treeIndex);
    console.log(targetFilter);

    //Filter type is GROUP, toggle condition to OR
    if (targetFilter.type === "GROUP")
        targetFilter.condition = "OR";

    return filter;

}

function addRule(filter, treeIndex) {

    console.log("Adding rule at treeIndex: ", treeIndex);
    const targetFilter = getFilterAtTreeIndex(filter, treeIndex);
    console.log(targetFilter);

    targetFilter.filters.push({
        type: "RULE",
        inputs: []
    });

    return filter;

}

function addGroup(filter, treeIndex) {

    console.log("Adding group at treeIndex: ", treeIndex);
    const targetFilter = getFilterAtTreeIndex(filter, treeIndex);
    console.log(targetFilter);

    targetFilter.filters.push({
        type: "GROUP",
        condition: "AND",
        filters: []
    });

    return filter;

}


function removeFilter(filter, treeIndex) {

    console.log("Removing filter with treeIndex: ", treeIndex);
    console.log("Filter: ", filter);

    //Tree index is the root filter, clear it
    if (treeIndex === "root") {

        //Return an empty group
        return {
            type: "GROUP",
            condition: "AND",
            filters: []
        };

        //Otherwise, get the parent of the treeIndex
    } else {

        const parentIndex = treeIndex.substr(0, treeIndex.lastIndexOf(","));
        const childIndex = treeIndex.substr(treeIndex.lastIndexOf(",") + 1);
        console.log(`parentIndex: ${  parentIndex  }, childIndex: ${  childIndex}`);

        const parentFilter = getFilterAtTreeIndex(filter, parentIndex);
        parentFilter.filters.splice(childIndex, 1);

    }

    return filter;
}

function ruleChange(filter, treeIndex, rules, inputs, event) {

    console.log("Changing rule with treeIndex: ", treeIndex);
    console.log("Inputs: ", inputs);
    console.log("event.target.value: ", event.target.value);
    console.log("Filter: ", filter);

    inputs[0] = event.target.value;
    inputs.splice(1);

    return filter;

}


function ruleValueChange(filter, treeIndex, inputs, index, event) {

    console.log("Changing rule with treeIndex: ", treeIndex);
    console.log("Modified index: ", index);
    console.log("Inputs: ", inputs);
    console.log("event.target.value: ", event.target.value);
    console.log("Filter: ", filter);

    const indexNext = (index + 1);
    inputs[indexNext] = event.target.value;

    return filter;
}


class Rule extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {

        const inputs = this.props.filter.inputs;
        const selectedRule = getRuleFromInput(inputs[0], this.props.rules);

        //No rule selected...
        if (selectedRule == null) {

            return (
                <div style={{display: "flex", flexDirection: "row"}}>
                    <select id="stateSelect" type="select" className="form-control"
                            onChange={(event) => this.props.setFilter(ruleChange(this.props.getFilter(), this.props.treeIndex, this.props.rules, inputs, event))}
                            style={{flexBasis: "180px", flexShrink: 0, marginRight: 5}} value={inputs[0]}>
                        <option value="Select Rule">Select Rule</option>
                        {
                            this.props.rules.map((ruleInfo, index) => {
                                return (<option value={ruleInfo.name} key={`rule-${  index}`}>{ruleInfo.name}</option>);
                            })
                        }
                    </select>

                    <button type="button" className="btn btn-danger btn-sm"
                            onClick={() => this.props.setFilter(removeFilter(this.props.getFilter(), this.props.treeIndex))}>
                        <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}}/>
                    </button>
                </div>
            );

            //Has selected rule...
        } else {

            while (inputs.length < selectedRule.conditions.length + 1) {

                const lastInput = (inputs.length - 1);

                //Rule is empty, add a new input
                if (selectedRule.conditions[lastInput].type === "select")
                    inputs.push(selectedRule.conditions[lastInput].options[0]);

                //Rule is not empty, add a blank input
                else
                    inputs.push("");

            }

            //Set the time zone automatically
            const TIME_ZONE_INPUTS = [
                "Start Date and Time",
                "End Date and Time",
                "Start Time",
                "End Time",
            ];

            if (TIME_ZONE_INPUTS.includes(inputs[0])) {

                const split = new Date().toString().split(" ");
                console.log(split);

                let timeZoneFormatted = split[split.length - 2];
                timeZoneFormatted = `${timeZoneFormatted.slice(0, 6)  }:${  timeZoneFormatted.slice(6)}`;

                console.log("Current time zone is: ", timeZoneFormatted);

                let timeZoneIndex = 0;
                for (let i = 0; i < timeZones.length; i++) {

                    if (timeZones[i].includes(timeZoneFormatted)) {

                        console.log(`Changing time zone index to ${i}, '${timeZones[i]}'`);
                        timeZoneIndex = i;

                        //Time zone is US, break
                        if (timeZones[i].includes("US"))
                            break;

                    }

                }

                //Set the time zone to the current time zone
                const TIME_ZONE_TIME_INDEX = 3;
                inputs[TIME_ZONE_TIME_INDEX] = timeZones[timeZoneIndex];

            }

            return (
                <div style={{display: "flex", flexDirection: "row"}}>
                    <select id="stateSelect" type="select" className="form-control"
                            onChange={(event) => this.props.setFilter(ruleChange(this.props.getFilter(), this.props.treeIndex, this.props.rules, inputs, event))}
                            style={{flexBasis: "180px", flexShrink: 0, marginRight: 5}} value={inputs[0]}>

                        <option value="Select Rule">Select Rule</option>
                        {
                            this.props.rules.map((ruleInfo, index) => {
                                return (<option value={ruleInfo.name} key={`rule-${  index}`}>{ruleInfo.name}</option>);
                            })
                        }
                    </select>

                    {
                        selectedRule.conditions.map((conditionInfo, index) => {

                            //Track non-select condition types
                            const CONDITION_TYPES_OTHER = [
                                "time",
                                "date",
                                "number",
                                "datetime-local",
                            ];

                            //Condition is a select...
                            if (conditionInfo.type == "select") {

                                const flexBasis = (conditionInfo.name == "timezone") ? "375px" : "150px";

                                return (
                                    <div key={`condition-div-${  index}`}>
                                        {
                                            //No options for the given condition
                                            (conditionInfo.options.length == 0)
                                                ?
                                                <select disabled={true} className="form-control" key={`select-${  index}`}
                                                        value={null}>
                                                    <option value={null} key={`${conditionInfo.name  }-${  index}`}>N/A
                                                    </option>
                                                    &quot;
                                                </select>
                                                :
                                                <select id="stateSelect" type={conditionInfo.type}
                                                        key={`select-${  index}`} className="form-control"
                                                        onChange={(event) => this.props.setFilter(ruleValueChange(this.props.getFilter(), this.props.treeIndex, inputs, index, event))}
                                                        style={{flexBasis: flexBasis, flexShrink: 0, marginRight: 5}}
                                                        value={inputs[index + 1]}>
                                                    {
                                                        conditionInfo.options.map((optionInfo, index) => {
                                                            return (<option value={optionInfo}
                                                                            key={`${conditionInfo.name  }-${  index}`}>{optionInfo}</option>);
                                                        })
                                                    }
                                                </select>
                                        }
                                    </div>
                                );

                                //Otherwise...
                            } else if (CONDITION_TYPES_OTHER.includes(conditionInfo.type)) {

                                return (
                                    <input type={conditionInfo.type} step="any" key={`input-${  index}`}
                                           className={"form-control"} aria-describedby="valueHelp"
                                           placeholder={`Enter ${  conditionInfo.name}`}
                                           onChange={(event) => this.props.setFilter(ruleValueChange(this.props.getFilter(), this.props.treeIndex, inputs, index, event))}
                                           style={{flexBasis: "150px", flexShrink: 0, marginRight: 5}}
                                           value={inputs[index + 1]}/>
                                );
                            }

                        })
                    }

                    <button type="button" className="btn btn-danger btn-sm ml-1"
                            onClick={() => this.props.setFilter(removeFilter(this.props.getFilter(), this.props.treeIndex))}>
                        <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}}/>
                    </button>
                </div>
            );
        }
    }
}


class Group extends React.Component {

    constructor(props) {

        super(props);

        const colorRand = Colors.randomValue();

        this.state = {
            showSavePopover: false,
            showLoadPopover: false,
            loadPopoverTarget: "",
            savePopoverTarget: "",
            saveButtonDisabled: true,
            storedFilters: props.storedFilters,
            editingFilter: {},
            filterSaved: false,
            filterName: "",
            filterColor: colorRand
        };

        this.handleColorChange = this.handleColorChange.bind(this);
    }

    toggleLoadPopover() {
        this.setState((prevState) => ({
            showLoadPopover: !prevState.showLoadPopover
        }));
    }

    setLoadPopoverTarget(event) {
        console.log("Setting load popover target");
        this.setState({ loadPopoverTarget: event.target });
    }

    toggleSavePopover() {
        this.setState((prevState) => ({
            showSavePopover: !prevState.showSavePopover
        }));
    }

    setSavePopoverTarget(event) {
        this.setState({ savePopoverTarget: event.target });
    }

    setFilter(filter) {

        this.toggleLoadPopover();
        const filterJSON = JSON.parse(filter.filter);
        this.props.setFilter(filterJSON);

        //show resolution tooltip for a max 10s
        setTimeout(function () {
            this.props.submitFilter();
        }.bind(this), 50);
    }

    saveFilter() {

        console.log("Saving filter with name: ", this.state.filterName);
        this.storeFilter(this.state.filterName, this.state.filterColor);

    }

    submitChanges(filter) {

        const submissionData = {
            currentName: filter.name,
            newName: this.state.filterName,
            filterJSON: JSON.stringify(this.props.getFilter()),
            color: this.state.filterColor
        };

        //Submission data is empty...
        if (isEmptyOrSpaces(submissionData.newName)) {

            $('#modify-filter-submit-button').attr('data-title', 'Please make sure the filter name is not empty before saving.').tooltip('show');
            setTimeout(function () {
                $('#modify-filter-submit-button').tooltip('hide');
            }.bind(this), MESSAGE_BIND_PERIOD_MS);

            return;

            //Filter is the same...
        } else if (submissionData.newName === submissionData.currentName && filter.filter === this.props.getFilter() && filter.color === submissionData.color) {

            $('#modify-filter-submit-button').attr('data-title', 'Please make sure the filter name is different from its original name, the filter color is different, or that the filter rules are different.').tooltip('show');
            setTimeout(function () {
                $('#modify-filter-submit-button').tooltip('hide');
            }.bind(this), MESSAGE_BIND_PERIOD_MS);

            return;

        }

        console.log("Mofifying filter ", filter.name);

        $.ajax({
            type: 'PUT',
            url: `/api/filter/${encodeURIComponent(filter.name)}`,
            data: submissionData,
            dataType: 'text',
            timeout: 0,
            async: true,
            success: (response) => {

                if (response === "DUPLICATE_PK") {

                    $('#modify-filter-submit-button').tooltip('show');
                    setTimeout(function () {
                        $('#modify-filter-submit-button').tooltip('hide');
                    }.bind(this), MESSAGE_BIND_PERIOD_MS);

                } else {

                    this.setState({
                        editingFilter: {},
                        showLoadPopover: false
                    });

                    $('#modify-filter-submit-button').tooltip('hide');
                    $('#load-filter-button').tooltip('show');
                    setTimeout(function () {
                        $('#load-filter-button').tooltip('hide');
                    }.bind(this), MESSAGE_BIND_PERIOD_MS);

                }

                this.setState(this.state);

            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("TF: ", this);
                console.log("TFP: ", this.props);
                showErrorModal("Error Loading Flights", errorThrown);
            }
        });
    }

    deleteFilterClicked(name) {

        showConfirmModal(
            `Confirm Delete Filter: '${  name  }'`,
            `Are you sure you wish to delete filter '${  name  }'?\n\nThis operation will remove it from your fleet, meaning it will be deleted for other users as well. This operation cannot be undone!`,
            () => { this.deleteFilter(name); }
        );

    }

    deleteFilter(name) {

        console.log("Removing filter ", name);
        console.log(this.filterRef);

        $.ajax({
            type: 'DELETE',
            url: `/api/filter/${encodeURIComponent(name)}`,
            dataType: 'text',
            timeout: 0,
            async: true,
            success: () => {
                this.setState(this.state);
            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Flights", errorThrown);
            }
        });
    }

    storeFilter(name, color) {

        const submissionData = {
            name: name,
            filterJSON: JSON.stringify(this.props.getFilter()),
            color: color
        };

        if (isEmptyOrSpaces(name)) {

            console.log('Empty String');
            $('#save-filter-button-card').attr('data-title', 'Please make sure the filter name is not empty before saving.').tooltip('show');
            setTimeout(function () {
                $('#save-filter-button-card').tooltip('hide');
            }.bind(this), MESSAGE_BIND_PERIOD_MS);

            return;
        }

        console.log("Storing filter ", name);

        $.ajax({
            type: 'POST',
            url: '/api/filter',
            data: submissionData,
            dataType: 'text',
            timeout: 0,
            async: true,
            success: (response) => {
                if (response === "DUPLICATE_PK") {
                    console.log("duplicate pk detected");
                    $('#save-filter-button-card').tooltip('show');
                    setTimeout(function () {
                        $('#save-filter-button-card').tooltip('hide');
                    }.bind(this), MESSAGE_BIND_PERIOD_MS);
                } else {
                    $('#save-filter-button-card').tooltip('hide');
                    $('#save-filter-button').tooltip('show');

                    this.setState({
                        saveButtonDisabled: true,
                        showSavePopover: false,
                        filterSaved: true
                    });

                    setTimeout(function () { //show success tooltip for a max 5s
                        $('#save-filter-button').tooltip('hide');
                    }.bind(this), 5000);
                }

                this.setState(this.state);
            },

            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Flights", errorThrown);
            }
        });
    }

    getStoredFilters() {

        let storedFilters = [];

        $.ajax({
            type: 'GET',
            url: '/api/filter',
            async: false,
            success: (response) => {
                console.log("Received filters response: ", response);

                storedFilters = response;
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("Error getting stored filters: ", errorThrown);
                showErrorModal("Error Getting Stored Filters", errorThrown);
            },
        });

        return storedFilters;
    }

    editFilter(filter) {

        this.props.setFilter(JSON.parse(filter.filter));
        this.setState({
            editingFilter: filter,
            filterName: filter.name,
            filterColor: filter.color
        });

    }

    setStoredFilters(filters) {
        this.setState({
            storedFilters: filters
        });
    }

    handleLoadClick(event) {
        this.toggleLoadPopover();
        this.setLoadPopoverTarget(event);
    }

    setSelectedFilter(filter) {
        this.props.setFilter(JSON.parse(filter));
    }

    handleColorChange(event) {
        const color = event.target.value;
        console.log(color);

        this.setState({
            filterColor: color
        });
    };

    render() {
        
        const loadFilterButtonId = "load-filter-button";

        const errorMessageStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        const styleButtonSq = {
            flex: "right",
            float: "auto"
        };

        const filterPillStyle = {
            marginRight: '4px',
            lineHeight: '4',
            opacity: '75%',
            fontSize: '100%',
        };

        let errorHidden = true;
        let errorMessage = "";
        if (this.props.filters.length == 0) {
            errorHidden = false;
            errorMessage = "Group has no rules.";
        }


        //console.log("GROUP: index: " + this.props.treeIndex);
        //console.log(this.props.filters);

        const andChecked = (this.props.filters.condition === "AND");
        let andActive = "";
        let orActive = "";
        if (andChecked) {
            andActive = "active";
        } else {
            orActive = "active";
        }


        const handleSaveClick = (event) => {
            this.toggleSavePopover();
            this.setSavePopoverTarget(event);
        };

        const filters = this.state.storedFilters;
        console.log(`Rendering Filters: ${  filters  }`);

        /*
            [EX]
            TODO: Figure out what to do with this,
            updating state in render is illegal
        */  

        const submitHidden = true;
        const submitDisabled = true;
        // if (typeof this.props.submitButtonName !== 'undefined') {
        //     submitHidden = false;
        //     submitDisabled = !isValidFilter(this.props.filters, this.props.rules);
        //     this.setState({
        //         saveButtonDisabled: !isValidFilter(this.props.filters, this.props.rules) || this.state.filterSaved,
        //         filterSaved: false
        //     });
        // }

        let saveCard = "";
        if (this.state.showSavePopover) {
            saveCard = (
                <div className="card m-1 float-right" style={{minWidth: "500px"}}>
                    <div className="card-header float-left">Save Current Filter:
                        <button type="button" className="mr-1 btn btn-danger btn-sm float-right"
                                onClick={() => this.setState({showSavePopover: false})}>
                            <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}}/>
                        </button>
                    </div>
                    <div className="card-body">
                        <div className="input-group mb-3">
                            <div className="input-group-prepend">
                                <button type="button" className="btn btn-outline-secondary"
                                        title="Assign a color to this filter"
                                        onClick={() => $("#color-picker-filter").click()}>
                                    <span className="badge badge-pill badge-primary" style={{
                                        ...filterPillStyle,
                                        backgroundColor: this.state.filterColor,
                                        verticalAlign: 'text-bottom'
                                    }}>
                                        <i className="fa fa-filter" aria-hidden="true"></i>
                                    </span>
                                </button>
                                <input key="cc-0" type="color" className="hidden" style={{display: "none"}}
                                       name="eventColor" onChange={e => this.setState({filterColor: e.target.value})}
                                       value={this.state.filterColor} id="color-picker-filter"/>
                            </div>
                            <input type="text" className="form-control" placeholder="Filter Name"
                                   aria-label="Filter Name" aria-describedby="basic-addon2"
                                   value={this.state.filterName}
                                   onChange={e => this.setState({filterName: e.target.value})}/>
                            <div className="input-group-append">
                                <button
                                    type="button"
                                    id='save-filter-button-card'
                                    className="btn btn-outline-secondary"
                                    onClick={() => {
                                        this.saveFilter();
                                    }}
                                    data-bs-toggle="tooltip"
                                    data-bs-trigger='manual'
                                    data-bs-placement="top"
                                    data-title='A filter with that name already exists in your fleet. Please provide a unique name.'
                                    disabled={submitDisabled}
                                >
                                    Save
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            );
        }

        let loadCard = "";
        if (this.state.showLoadPopover) {
            // TODO: this sends a GET to the server every time the component is rendered...
            const filters = this.getStoredFilters();
            if (filters != null && filters.length > 0) {
                loadCard = (
                    <div className="card m-1 float-right" style={{minWidth: "800px"}}>
                        <div className="card-header float-left">Saved Filters:
                            <button type="button" className="mr-2 btn btn-danger btn-sm float-right"
                                    onClick={() => this.setState({showLoadPopover: false})}>
                                <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}}/>
                            </button>
                        </div>
                        <div className="card-body" style={{maxHeight: "200px", overflowY: "auto"}}>
                            {
                                filters.map((filter, index) => {

                                    //Normal
                                    let editButton = (
                                        <button className="m-1 btn btn-primary align-right" style={styleButtonSq}
                                                onClick={() => this.editFilter(filter)} title="Edit Filter">
                                            <i className="fa fa-pencil" aria-hidden="true"></i>
                                        </button>
                                    );
                                    const deleteButton = (
                                        <button className="m-1 btn btn-danger align-right" style={styleButtonSq}
                                                onClick={() => this.deleteFilterClicked(filter.name)}
                                                title="Delete this filter">
                                            <i className="fa fa-trash" aria-hidden="true"></i>
                                        </button>
                                    );

                                    let nameField = (
                                        <div style={{width: "100%"}} className="d-flex flex-row">

                                            {/* Primary Button (+ Name) */}
                                            <button type="button" className="m-1 btn btn-secondary"
                                                    onClick={() => this.setFilter(filter)} key={index} style={{
                                                width: "100%",
                                                lineHeight: '1',
                                                fontSize: '100%',
                                                backgroundColor: 'var(--c_tag_badge)',
                                                color: 'var(--c_text)'
                                            }} title="Filter Info">

                                                <div className="d-flex flex-row">
                                                    <span className="badge badge-pill badge-primary" style={{
                                                        ...filterPillStyle,
                                                        backgroundColor: filter.color,
                                                        verticalAlign: 'text-bottom'
                                                    }}>
                                                        <i className="fa fa-filter" aria-hidden="true"></i>
                                                    </span>
                                                    <span className="ml-2 font-weight-bold pt-1">
                                                        {filter.name}
                                                    </span>
                                                </div>

                                            </button>

                                            {/* Editing & Delete Buttons */}
                                            <div className="d-flex flex-row ml-auto gap-8">
                                                {editButton}
                                                {deleteButton}
                                            </div>
                                        </div>
                                    );

                                    if (filter.name == this.state.editingFilter.name) {

                                        //When editing 
                                        editButton = (
                                            <button
                                                className="m-1 btn btn-success align-right"
                                                id='modify-filter-submit-button'
                                                onClick={() => this.submitChanges(filter)}
                                                data-bs-toggle="tooltip" data-bs-trigger='manual'
                                                data-bs-placement="top"
                                                data-title="A filter in your fleet already exists with that name! Please choose another name."
                                                title="Submit Changes"
                                            >
                                                <i className='fa fa-check' aria-hidden='true'></i>
                                            </button>
                                        );

                                        nameField = (
                                            <>
                                                <div style={{width: "100%"}} className="d-flex flex-row">

                                                    {/* Editing Fields */}
                                                    <div className="input-group m-1">
                                                        <div className="input-group-prepend">
                                                            <button type="button" className="btn btn-outline-secondary"
                                                                    title="Assign a different color to this filter"
                                                                    onClick={() => $("#color-picker-filter-mod").click()}>
                                                                <span className="badge badge-pill badge-primary"
                                                                      style={{
                                                                          ...filterPillStyle,
                                                                          backgroundColor: this.state.filterColor,
                                                                          verticalAlign: 'text-bottom'
                                                                      }}>
                                                                    <i className="fa fa-filter" aria-hidden="true"></i>
                                                                </span>
                                                            </button>
                                                            <input key="cc-1" type="color" className="hidden"
                                                                   style={{display: "none"}} name="eventColor"
                                                                   onChange={e => this.setState({filterColor: e.target.value})}
                                                                   value={this.state.filterColor}
                                                                   id="color-picker-filter-mod"/>
                                                        </div>
                                                        <input type="text" className="form-control"
                                                               aria-label="Filter Name" aria-describedby="basic-addon2"
                                                               value={this.state.filterName}
                                                               onChange={e => this.setState({filterName: e.target.value})}/>
                                                    </div>

                                                    {/* Editing & Delete Buttons */}
                                                    <div className="d-flex flex-row">
                                                        {editButton}
                                                        {deleteButton}
                                                    </div>
                                                </div>
                                            </>
                                        );
                                    }

                                    return (
                                        <div key={index} className="container">
                                            <div className="row justify-content-md-center">
                                                {nameField}
                                            </div>
                                        </div>
                                    );
                                })
                            }
                        </div>
                    </div>
                );
            } else {
                loadCard = (
                    <div className="card m-1 float-right" style={{maxWidth: "500px"}}>
                        <div className="card-header float-left">No filters stored yet.
                            <button type="button" className="mr-2 btn btn-danger btn-sm float-right"
                                    onClick={() => this.setState({showLoadPopover: false})}>
                                <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}}/>
                            </button>
                        </div>
                        <div className="card-body">
                            No filters stored yet for this fleet.
                        </div>
                    </div>
                );
            }
        }


        const filterList = Array.isArray(this.props.filters.filters)
            ? this.props.filters.filters
            : [];


        return (
            <div id="search-filter-card" className="card border-secondary" style={{margin: 0, width: "100%"}}>
                <div className="d-flex justify-content-between">

                    <div className="p-2 d-flex flex-row justify-content-center align-items-center">
                        <div className="btn-group btn-group-toggle" data-bs-toggle="buttons">
                            <label className={`btn btn-outline-primary btn-sm ${  andActive}`}
                                   onClick={() => this.props.setFilter(andClicked(this.props.getFilter(), this.props.treeIndex))}>
                                <input type="radio" name="options" id="option1" autoComplete="off"
                                       defaultChecked={andChecked}/>AND
                            </label>
                            <label className={`btn btn-outline-primary btn-sm ${  orActive}`}
                                   onClick={() => this.props.setFilter(orClicked(this.props.getFilter(), this.props.treeIndex))}>
                                <input type="radio" name="options" id="option2" autoComplete="off"
                                       defaultChecked={!andChecked}/>OR
                            </label>
                        </div>

                        {/* Indicate Flight ID Filter Group */}
                        {
                            this.props.isFlightIdGroup &&
                            <div className="ml-4 d-flex flex-row align-items-center p-2 rounded"
                                 style={{backgroundColor: "#FFFFFF22"}}>
                                <i className="fa fa-search"
                                   style={{fontSize: "1.25em", userSelect: "none", opacity: "0.75"}}/>
                                <div className="ml-2 font-weight-bold"
                                     style={{fontSize: "1.00em", userSelect: "none", opacity: "0.76"}}>
                                    Flight ID Filter Group
                                </div>
                            </div>
                        }

                    </div>

                    <div className="p-2">
                        <button type="button" className="btn btn-primary btn-sm mr-1"
                                onClick={() => this.props.setFilter(addRule(this.props.getFilter(), this.props.treeIndex))}>Add
                            Rule
                        </button>
                        <button type="button" className="btn btn-primary btn-sm mr-1"
                                onClick={() => this.props.setFilter(addGroup(this.props.getFilter(), this.props.treeIndex))}>Add
                            Group
                        </button>
                        <button type="button" className="btn btn-danger btn-sm"
                                onClick={() => this.props.setFilter(removeFilter(this.props.getFilter(), this.props.treeIndex))}>
                            <i className="fa fa-times" aria-hidden="true"
                               style={{padding: "4 4 3 4"}}/> {this.props.treeIndex === "root" ? "Clear All" : "Delete Group"}
                        </button>
                    </div>
                </div>

                {
                    filterList.map((filterInfo, index) => {

                        if (filterInfo.type === "GROUP") {

                            return (
                                <div className="p-2" key={`${this.props.treeIndex  },${  index}`}>
                                    <Group
                                        key={`${this.props.treeIndex  },${  index}`}
                                        treeIndex={`${this.props.treeIndex  },${  index}`}
                                        rules={this.props.rules}
                                        filters={filterInfo}
                                        getStoredFilters={() => this.props.getStoredFilters()}
                                        getFilter={() => {
                                            return this.props.getFilter();
                                        }}
                                        setFilter={(filter) => this.props.setFilter(filter)}
                                        setSortByColumn={(sortColumn) => this.props.setSortByColumn(sortColumn)}
                                        getSortByColumn={() => {
                                            return this.props.getSortByColumn;
                                        }}
                                        isFlightIdGroup={filterInfo.isFlightIdGroup}
                                        copyFilterURL={() => this.props.copyFilterURL()}
                                    />
                                </div>
                            );

                        } else if (filterInfo.type === "RULE") {

                            return (
                                <div className="p-2" key={`${this.props.treeIndex  },${  index}`}>
                                    <Rule
                                        key={`${this.props.treeIndex  },${  index}`}
                                        treeIndex={`${this.props.treeIndex  },${  index}`}
                                        rules={this.props.rules}
                                        filter={filterInfo}
                                        getStoredFilters={() => this.props.getStoredFilters()}
                                        getFilter={() => {
                                            return this.props.getFilter();
                                        }}
                                        setFilter={(filter) => this.props.setFilter(filter)}
                                    />
                                </div>
                            );

                        }

                    })
                }

                <div className="d-flex justify-content-end">
                    <div className="p-2 flex-fill">
                        <span style={errorMessageStyle} hidden={errorHidden}>{errorMessage}</span>
                    </div>

                    <div className="p-2">

                        {/* Filter URL Copy Button */}
                        <button type="button" className="btn btn-primary btn-sm mr-1"
                                onClick={() => this.props.copyFilterURL()} hidden={submitHidden}>
                            <i className='fa fa-clipboard mr-2'/>
                            Copy Filter URL
                        </button>

                        {/* Filter Load Button */}
                        <button type="button" className="btn btn-primary btn-sm mr-1" hidden={submitHidden}
                                onClick={(event) => this.handleLoadClick(event)} id={loadFilterButtonId}
                                data-bs-toggle='tooltip' data-bs-placement='top' data-bs-trigger='manual'
                                data-title='Changes Saved!'>
                            Load a Saved Filter
                        </button>

                        {/* Filter Save Button */}
                        <button id="save-filter-button" type="button" className="btn btn-primary btn-sm mr-1"
                                onClick={handleSaveClick} hidden={submitHidden} disabled={this.state.saveButtonDisabled}
                                data-bs-toggle="tooltip" data-bs-trigger='manual' data-bs-placement="top"
                                title="Filter saved successfully">
                            Save Current Filter
                        </button>

                        {/* Filter Submit Button */}
                        <button type="button" className="btn btn-primary btn-sm mr-1" disabled={submitDisabled}
                                onClick={() => this.props.submitFilter(true /*reset current page*/)}
                                hidden={submitHidden}>
                            {this.props.submitButtonName}
                        </button>
                    </div>

                </div>
                <div className="container" style={{maxWidth: '100%', marginRight: "0"}}>
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
            <div>
                <Group
                    treeIndex="root"
                    submitButtonName={this.props.submitButtonName}
                    rules={this.props.rules}
                    filters={this.props.filters}
                    getFilter={() => {
                        return this.props.getFilter();
                    }}
                    setFilter={(filter) => this.props.setFilter(filter)}
                    submitFilter={() => this.props.submitFilter()}
                    setSortByColumn={(sortColumn) => this.props.setSortByColumn(sortColumn)}
                    getSortByColumn={() => {
                        return this.props.getSortByColumn;
                    }}
                    errorModal={this.props.errorModal}
                    copyFilterURL={() => this.props.copyFilterURL()}
                />

            </div>
        );
    }
}

export {Filter};