import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { saveQueriesModal } from "./save_query_modal.js"
import { loadQueriesModal } from "./load_query_modal.js"

class Filter extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            filters : {
                type : "GROUP",
                condition : "AND",
                depth : 0,
                filters : []
            }
        }
    }

    addRule(filter) {
        console.log("adding rule to filter:");
        let emptyRule = {
            type : "RULE",
            depth: filter.depth + 1,
            inputs : [0]
        };
        filter.filters.push(emptyRule);

        this.setState(this.state);
        if (typeof this.props.parentRerender != 'undefined') {
            this.props.parentRerender();
        }
    }

    addGroup(filter) {
        console.log("adding group to filter:");
        console.log(filter);

        let emptyGroup = {
            type : "GROUP",
            condition : "AND",
            depth: filter.depth + 1,
            filters : []
        };
        filter.filters.push(emptyGroup);
        console.log("after adding");
        console.log(filter);

        this.setState(this.state);
        if (typeof this.props.parentRerender != 'undefined') {
            this.props.parentRerender();
        }
    }

    removeFilter(filters, index) {
        console.log("removing filter " + index + " on filters:");
        console.log(filters);
        filters.splice(index, 1);
        this.setState(this.state);
        if (typeof this.props.parentRerender != 'undefined') {
            this.props.parentRerender();
        }
    }

    orClicked(filter, disabled) {
        if (disabled) {
            console.log("not changing because button disabled");
            return;
        }

        console.log("changing group condition to: OR for filter:");
        console.log(filter);
        filter.condition = "OR";
        console.log(filter);
        console.log("state filter:");
        console.log(this.state.filters);
        this.setState(this.state);
    }

    andClicked(filter, disabled) {
        if (disabled) {
            console.log("not changing because button disabled");
            return;
        }

        console.log("changing group condition to: AND for filter:");
        console.log(filter);
        filter.condition = "AND";
        console.log(filter);
        console.log("state filter:");
        console.log(this.state.filters);
        this.setState(this.state);
    }

    getQuery() {
        let query = this.getQueryHelper(this.state.filters);

        return query;
    }

    getQueryHelper(filter) {
        let query = {};

        if (filter.type == "RULE") {
            let inputs = filter.inputs;

            //console.log("rule:");
            let rule = this.props.rules[inputs[0] - 1];
            //console.log(rule);

            query.type = "RULE";
            query.inputs = [];
            query.inputs.push(rule.name);

            for (let i = 1; i < inputs.length; i++) {

                let condition = rule.conditions[i-1];
                //console.log(condition);
                //console.log("inputs[" + i + "]: " + inputs[i]);

                if (condition.type == "select") {
                    query.inputs.push(condition.options[inputs[i]]);
                } else {
                    query.inputs.push(inputs[i]);
                }
            }

        } else if (filter.type == "GROUP") {
            query.type = "GROUP";
            query.condition = filter.condition;
            query.filters = [];

            let filters = filter.filters;
            for (let i = 0; i < filters.length; i++) {
                query.filters.push(this.getQueryHelper(filters[i]));
            }
        } else {
            console.log("UNKNOWN FILTER '" + filter.type + "'");
        }

        return query;
    }

    getRemoveButton(parentFilters, removeIndex) {
        let removeClasses = "btn btn-danger btn-sm";
        return ( <button type="button" className={removeClasses}> <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.removeFilter(parentFilters, removeIndex)}></i> </button> );
    }

    getFilterHeader(filter, includeRemove, parentFilters, removeIndex) {
        let ruleClasses = "btn btn-primary btn-sm mr-1";
        let groupClasses = "btn btn-primary btn-sm";
        let removeButton = "";

        let andFixed = this.state.andFixed;
        let disabledClasses = "";

        if (includeRemove) {
            groupClasses += " mr-1";
            removeButton = this.getRemoveButton(parentFilters, removeIndex);
            andFixed = false;
        }
        if (andFixed) disabledClasses = " disabled";


        let andDefaultChecked = false;
        let orDefaultChecked = false;

        if (filter.groupCondition == "AND") {
            andDefaultChecked = true;
        } else {
            orDefaultChecked = true; 
        }

        let orButton = "";
        if (!andFixed) {
            orButton = (
                <label className={"btn btn-outline-primary btn-sm"} onClick={() => this.orClicked(filter, andFixed)}>
                    <input type="radio" name="options" id="option3" autoComplete="off" defaultChecked={orDefaultChecked} />OR
                </label> 
            );
        }

        // establish depth for load button
        let loadButton = "";
        if (filter.depth == 0) {
            loadButton = (<button type="button" className={ruleClasses} onClick={() => loadQueriesModal.show()}>Load Saved</button>);
        }

        return (
            <div className="d-flex justify-content-between">

                <div className="p-2">
                    <div className="btn-group btn-group-toggle" data-toggle="buttons">
                        <label className={"btn btn-outline-primary btn-sm active"} onClick={() => this.andClicked(filter, andFixed)}>
                            <input type="radio" name="options" id="option1" autoComplete="off" defaultChecked={andDefaultChecked} />AND
                        </label>
                        { orButton }
                    </div>

                </div>

                <div className="p-2">
                    <button type="button" className={ruleClasses} onClick={() => this.addRule(filter)}>Add Rule</button>
                    <button type="button" className={ruleClasses} onClick={() => this.addGroup(filter)}>Add Group</button>
                    { loadButton }
                    { removeButton }
                </div>
            </div>
        );
    }

    ruleChange(currentFilter, inputIndex, event) {
        console.log("changing rule input " + inputIndex + " to: " + event.target.value);

        if (inputIndex == 0) {
            //reset the inputs for this filter because the rule type changed
            console.log("resetting the current filter's inputs");
            currentFilter.inputs = [ 0 ];
        }

        currentFilter.inputs[inputIndex] = event.target.value;
        /*
        console.log("all filters:");
        console.log(this.state.filters);

        console.log("new rule, inputs first:");
        console.log(currentFilter.inputs);
        console.log(this.props.rules[currentFilter.inputs[0] - 1]);
        */

        this.setState(this.state);
        if (typeof this.props.parentRerender != 'undefined') {
            this.props.parentRerender();
        }
    }

    renderRuleSelect(currentFilter) {
        return (
            <select id="stateSelect" type="select" className="form-control" onChange={(event) => this.ruleChange(currentFilter, 0, event)} style={{flexBasis:"180px", flexShrink:0, marginRight:5}} value={currentFilter.inputs[0]}>
                <option value="0">Select Rule</option>
                { 
                    this.props.rules.map((ruleInfo, index) => {
                        //console.log("adding rule: " + ruleInfo.name + ", with value: " + index);
                        return ( <option value={index + 1} key={"rule-" + index}>{ruleInfo.name}</option> );
                    })
                }
            </select>
        );
    }

    renderRule(currentFilter) {
        let inputs = currentFilter.inputs;
        let selectedRule = inputs[0];

        if (selectedRule == 0) {
            return (
                <div style={{display: "flex", flexDirection: "row"}}>
                    { this.renderRuleSelect(currentFilter) }
                </div>
            );

        } else {

            return (
                <div style={{display: "flex", flexDirection: "row"}}>
                    { this.renderRuleSelect(currentFilter) }

                    {
                        this.props.rules[selectedRule - 1].conditions.map((conditionInfo, index) => {
                            if (conditionInfo.type == "select") {
                                if (typeof currentFilter.inputs[index + 1] == 'undefined') currentFilter.inputs[index + 1] = 0;

                                return (
                                    <select id="stateSelect" type={conditionInfo.type} key={"select-" + index} className="form-control" onChange={(event) => this.ruleChange(currentFilter, index + 1, event)} style={{flexBasis:"150px", flexShrink:0, marginRight:5}} value={currentFilter.inputs[index + 1]}>
                                        { 
                                            conditionInfo.options.map((optionInfo, index) => {
                                                //console.log("adding option: " + optionInfo + ", with value: " + index + ", key: " + (conditionInfo.name + "-" + index));
                                                return ( <option value={index} key={conditionInfo.name + "-" + index} >{optionInfo}</option> );
                                            })
                                        }
                                    </select>
                                );

                            } else if (conditionInfo.type == "time" || conditionInfo.type == "date" || conditionInfo.type == "number" || conditionInfo.type == "datetime-local") {
                                if (typeof currentFilter.inputs[index + 1] == 'undefined') {
                                    currentFilter.inputs[index + 1] = "";
                                }

                                return (
                                    <input type={conditionInfo.type} step="any" key={"input-" + index} className={"form-control"} aria-describedby="valueHelp" placeholder={"Enter " + conditionInfo.name} onChange={(event) => this.ruleChange(currentFilter, index + 1, event)} style={{flexBasis:"150px", flexShrink:0, marginRight:5}} value={currentFilter.inputs[index + 1]}/>
                                );
                            }
                        })
                    }
                </div>
            );
        }
    }

    filterValid(filter) {
        if (filter.type == "GROUP") {
            if (filter.filters.length == 0) {
                return "Group has no rules.";
            } else {
                return "";
            }
        } else if (filter.type == "RULE") {
            console.log("checking if rule valid udpated 3, made some other changes! and some more!!: ");
            console.log(filter);
            let inputs = filter.inputs;

            if (inputs[0] == 0) {
                return "Please select a rule.";
            } else {
                let rule = this.props.rules[inputs[0] - 1];
                console.log("checking rule valid, inputs then rule:");
                console.log(inputs);
                console.log(rule);
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

    recursiveValid(filters) {
        if (this.filterValid(filters) != "") return false;

        //console.log("recursiveValid on:");
        //console.log(filters);

        let subFilters = filters.filters;
        for (let i = 0; i < subFilters.length; i++) {
            if (subFilters[i].type == "GROUP") {
                let subValid = this.recursiveValid(subFilters[i]);
                //console.log("GROUP was valid? " + subValid);
                if (!subValid) return false;
            } else if (subFilters[i].type == "RULE") {
                let subValid = (this.filterValid(subFilters[i]) == "");
                //console.log("RULE was valid? " + subValid);
                if (!subValid) return false;
            } else {
                //console.log("type wasn't defined!");
                return false;
            }
        }
        return true;
    }

    isValid() {
        let valid = this.recursiveValid(this.state.filters);
        console.log("isValid? " + valid);
        return valid;
    }

    renderFilters(filters) {
        let filterContent = "";

        if (filters.length == 0) return "";

        let errorMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        return (
            <div>
                {
                    filters.map((currentFilter, index) => {
                        let cardClasses = "card mb-1 m-1";
                        let errorMessage = this.filterValid(currentFilter);
                        console.log("filter error mesage: " + errorMessage);
                        let errorContent = "";

                        if (errorMessage == "") {
                            cardClasses += " border-secondary";
                        } else {
                            cardClasses += " border-danger";
                            errorContent = (
                                <div className="d-flex justify-content-end">
                                    <div className="p-2 flex-fill">
                                        <span style={errorMessageStyle}>{errorMessage}</span>
                                    </div>
                                </div>
                            );
                        }

                        if (currentFilter.type === "GROUP") {

                            return (
                                <div className="card-body p-2" key={index}>
                                    <div className={cardClasses} style={{background : "rgba(248,259,250,0.8)"}}>
                                        { this.getFilterHeader(currentFilter, true, filters, index) }

                                        { this.renderFilters(currentFilter.filters) }

                                        { errorContent }
                                    </div>
                                </div>
                            );

                        } else if (currentFilter.type === "RULE") {

                            return (
                                <div className="card-body p-2" key={index}>
                                    <div className={cardClasses} style={{background : "rgba(248,259,250,0.8)"}}>
                                        <div className="d-flex justify-content-between">
                                            <div className="p-2">
                                                { this.renderRule(currentFilter) } 
                                            </div>

                                            <div className="p-2">
                                                { this.getRemoveButton(filters, index) }
                                            </div>
                                        </div>
                                        { errorContent }
                                    </div>
                                </div>
                            );

                        } else {
                            console.log("unknown filter type: '" + currentFilter.type + "'");
                            return (
                                <div key={index}> EMPTY FILTER </div>
                            );
                        }
                    })
                }
            </div>
        );
    }

    render() {
        let depth = 0;

        let groupClasses = "btn btn-primary btn-sm mr-1";

        let errorMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        let errorHidden = true;
        let errorMessage = "";
        if (this.state.filters.filters.length == 0) {
            errorHidden = false;
            errorMessage = "Group has no rules.";
        }
        let submitDisabled = !this.recursiveValid(this.state.filters);

        let externalSubmit = false;
        if (typeof this.props.externalSubmit != 'undefined') {
            externalSubmit = this.props.externalSubmit;
        }
        console.log(externalSubmit);

        return (
            <div className="card-body p-2" hidden={this.props.hidden} style={{padding:0}}>
                <div className="card mb-1 m-1 border-secondary" style={{background : "rgba(248,259,250,0.8)", margin:0}}>
                    { this.getFilterHeader(this.state.filters, false, null, 0) }

                    { this.renderFilters(this.state.filters.filters) }

                    <div className="d-flex justify-content-end">
                        <div className="p-2 flex-fill">
                            <span style={errorMessageStyle} hidden={errorHidden}>{errorMessage}</span>
                        </div>

                        <div className="p-2">
                            <button type="button" className={groupClasses} disabled={submitDisabled} onClick={() => this.props.submitFilter()} hidden={externalSubmit} >{this.props.submitButtonName}</button>
                            <button type="button" className={groupClasses} disabled={submitDisabled} onClick={() => {saveQueriesModal.updateQuery(this.getQuery()); saveQueriesModal.show();}} hidden={externalSubmit} >Save</button>
                        </div>
                    </div>

                </div>
            </div>
        );
    }
}

export { Filter };
