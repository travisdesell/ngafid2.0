import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import { Filter } from "./filter.jsx"
import { errorModal } from "./error_modal.jsx";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;



// TODO: load query from database and populate filter
// TODO: add button to delete selected query (if user has permissions)

// The LoadQueriesModal component defines a modal interface for the loading of queries
class LoadQueriesModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            noEntriesMessage : "Sorry, No Entries Found",
            selectedGroup : null,
            selectedQuery : null,
            queryText : "",
            queryGroups : new QueryGroups(this),                       // Groups component responsible for displaying available query Groups
            queries : new Map(),                                        // queryName keys to list values [name, text, info]
            queryNamesDisplay : null
        };
    }

    show() {
        this.setState(this.state, () => { $("#load-query-modal").modal('show')});
    }

    // method to construct query name section of modal (load associated query names and info)
    getQueryNames() {
        // ajax call to retrieve queries belonging to selected Group
        var submissionData = {
            fleetID: $("#" + this.state.queryGroups.state.id).val()
        };

        var thisModal = this;

        if (submissionData.fleetID) {
            $.ajax({
                type: 'GET',
                url: '/protected/get_queries',
                data: submissionData,
                success : function(response) {
                    // Parse response string and populate 'this.state.queries' map with queries for one group at a time
                    thisModal.state.queries.clear();
                    if (response != "") {
                        let queries = response.split("--");
                        for (let index = 0; index < queries.length; index++) {
                            let fields = queries[index].split("++");

                            // add query to queries map
                            thisModal.state.queries.set(fields[0], new Query(fields[0], fields[1], fields[2]));

                            // if init set queryText
                            if (thisModal.state.selectedQuery == null) {
                                thisModal.state.selectedQuery = fields[0];
                            }
                        }
                    }
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error retrieving user fleet info", "unsuccessful AJAX call");
                },
                async: false        // has to make sync for render timings to work...
            });
        }
        // handles initialization error, where selectGroups element not formed - JQuery returns undefined
        else {
            thisModal.state.queries.clear();
        }

        if (thisModal.state.queries.size) {
            return (
                <select name="querySelect" id="querySelect" type={"select"} className="form-control" placeholder="Please Select Query" onChange={(event) => { this.state.selectedQuery = querySelect.value; this.setState(this.state);}} style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                    {
                        Array.from(this.state.queries, ([queryName, query]) => {
                            return (<option key={queryName} value={queryName}>{queryName}</option>)
                        })
                    }
                </select>
            )
        } else {
            return (
                <select name="querySelect" id="querySelect" type={"select"} className="form-control" placeholder="Please Select Query" onChange={(event) => { this.state.selectedQuery = querySelect.value; this.setState(this.state);}} style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                    <option key="0" value="0">{"No saved queries available"}</option>
                </select>
            )
        }
    }

    // a method to fetch proper queryText for selected query
    getQueryText() {
        let query = this.state.selectedQuery;
        if (query && this.state.queries.get(query)) {
            return this.state.queries.get(query).state.queryText;
        }

        return "Select a query to be displayed";
    }

    // a method to ensure proper fields selected before initiating a load
    isValidLoad() {
        // method to validate fields before enabling save button
        let valid = false;

        // ensure fields not blank
        if (this.state.selectedGroup && this.state.selectedQuery) {
            valid = true;
        }

        return valid;
    }

    render() {
        const hidden = this.props.hidden;

        let formGroupStyle = {
            marginBottom: '8px'
        };

        let formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px'
        };

        let labelStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'right'
        };

        let validationMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        // generate headers and menu items for display
        this.state.queryNamesDisplay = this.getQueryNames();
        let names = this.state.queryNamesDisplay;
        let headerMessage = "Where would you like to load from?";
        let dropdownLabel = "Source:";
        let submitLabel = "Load";
        this.state.selectedGroup = this.state.queryGroups.state.selectedGroup;
        let submitDisabled = !this.isValidLoad();
        let queryText = this.getQueryText();

        return (
            <div className='modal-content'>

                <div className='modal-header'>
                    <h5 id='save-query-modal-title' className='modal-title'>{headerMessage}</h5>
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='save-query-modal-body' className='modal-body'>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="scope" style={labelStyle}>{dropdownLabel}</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <QueryGroups id='groups' props={this}/>
                            </div>
                        </div>
                    </div>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="queryName" style={labelStyle}>Query Name:</label>
                            </div>
                            <div className="p-2 flex-fill">
                                {names}
                            </div>
                        </div>
                    </div>

                    <div className="d-flex" style={formGroupStyle}>
                        <div className="p-2" style={formHeaderStyle}>
                            <label htmlFor="queryText" style={labelStyle}>Full Query:</label>
                        </div>
                        <div className="p-2 flex-fill" style={formHeaderStyle}>
                            <textarea id="country" className="form-control" name="queryText" rows="5" cols="200" wrap="soft" value={queryText} readOnly>
                            </textarea>
                        </div>
                    </div>

                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                    <button id='submitButton' type='submit' className='btn btn-primary' onClick={() => {/*TODO: CALL TO LOAD QUERY TO FILTERS HERE*/}} disabled={submitDisabled}>{submitLabel}</button>
                </div>
            </div>
        );
    }
}


// The SaveQueriesModal component defines a modal interface for the loading and saving of queries
class SaveQueriesModal extends React.Component {
    constructor(props) {
        super(props);

        let x = new QueryGroups(this);

        this.state = {
            noEntriesMessage : "Sorry, No Entries Found",
            selectedGroup : "user",
            query : null,
            queryText : "",
            queryGroups : null,
            nameTakenMsg : null
        };
    }

    updateQuery(queryObject) {
        this.state.query = queryObject;
        this.state.queryText = this.getHumanReadable(queryObject);
    }

    show() {
        this.setState(this.state, () => {$("#save-query-modal").modal('show')});
    }

    // method to validate fields before enabling save button
    isValidSave() {
        let valid = false;

        // ensure fields not blank
        let nameElement = $("#name")[0];
        if (this.state.query && nameElement) {
            if (nameElement.value) {
                valid = true;
            }
        }

        return valid;
    }

    // A method to convert JSON query info into readable text for display
    getHumanReadable(queryJSON){
        if (queryJSON.type == "RULE") {
            let string = "";
            let inputs = queryJSON.inputs;
            for (let i = 0; i < inputs.length; i++) {
                if (i > 0) string += " ";
                string +=  inputs[i];
            }

            return string;

        } else if (queryJSON.type == "GROUP") {
            let string = "";
            let condition = queryJSON.condition;
            let rules = queryJSON.filters;
            for (let i = 0; i < rules.length; i++) {
                if (i > 0) string += " " + condition + " ";
                string += this.getHumanReadable(rules[i]);
            }

            return "(" + string + ")";

        } else {
            LOG.severe("Attempted to convert a filter to a String with an unknown type: '" + type + "'");
            System.exit(1);
        }
    }

    // A method to insert current query info into the saved_queries table
    save(){
        // ajax call to insert query into database
        var submissionData = {
            queryName: $("#name").val(),
            queryText: this.state.queryText,
            query: JSON.stringify(this.state.query),
            fleetID: $("#saveGroupSelect").val()
        };

        let modalDisplay = $("#save-query-modal")
        let thisModal = this;

        $.ajax({
            type: 'POST',
            url: '/protected/save_query',
            data: submissionData,
            dataType .json',
            success : function(response) {
                // handle if name already taken
                if (response != "success") {           // if name taken
                    thisModal.state.nameTakenMsg = response;
                    thisModal.setState(thisModal.state);
                } else {
                    // hide modal
                    modalDisplay.modal('hide');
                }
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log(errorThrown);
                console.log(textStatus);
                errorModal.show("Error inserting query into database", "unsuccessful AJAX call");
            },
            async: true
        });
    }

    render() {
        const hidden = this.props.hidden;

        let formGroupStyle = {
            marginBottom: '8px'
        };


        let formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px'
        };

        let labelStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'right'
        };

        let validationMessageStyle = {
            padding : '7 0 7 0',
            margin : '8px',
            display: 'block',
            textAlign: 'right',
            color: 'red'
        };

        // generate header messages
        let queryGroups = this.state.queryGroups;
        let queryText = this.state.queryText;
        let headerMessage = "Where would you like to save to?";
        let dropdownLabel = "Destination:";
        let submitLabel = "Save";
        let saveDisabled = !this.isValidSave();


        return (
            <div className='modal-content'>

                <div className='modal-header'>
                    <h5 id='save-query-modal-title' className='modal-title'>{headerMessage}</h5>
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='save-query-modal-body' className='modal-body'>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="scope" style={labelStyle}>{dropdownLabel}</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <QueryGroups id='groups' props={this}/>
                            </div>
                        </div>
                    </div>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="queryName" style={labelStyle}>Query Name:</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="name" className="form-control" id="name" placeholder="Name your query!" onChange={() => this.setState(this.state)} required={true} />
                            </div>
                        </div>
                    </div>

                    <div id="nameTaken"  style={validationMessageStyle}>
                        {this.state.nameTakenMsg}
                    </div>

                    <div className="d-flex" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="queryText" style={labelStyle}>Your Query Text:</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <textarea id="queryText" className="form-control" name="queryText" rows="5" cols="200" wrap="soft" value={queryText} readOnly>
                                </textarea>
                            </div>
                        </div>
                    </div>

                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                    <button id='submitButton' type='submit' className='btn btn-primary' onClick={() => {this.save();}} disabled={saveDisabled}>{submitLabel}</button>
                </div>

            </div>
        );
    }
}

// A class to capture relevant Query information
class Query {
    constructor(name, text, info) {
        this.state = {
            queryName : name,
            queryInfo : info,
            queryText : text
        };
    }

    save(){
    }

    load(){
    }
}

// class to maintain and organize Query Groups (fleets', user's, and website's queries)
class QueryGroups extends React.Component {
    constructor(props) {
        super(props);

        let grps = [[-1, "my queries"]];
        let id = "saveGroupSelect";

        this.state = {
            parentModal : props,
            id : id,
            groupsDisplay : grps,
            queries : new Map(),                   //selectedGroup's Queries
            selectedGroup : -1                      // default: "my queries"
        }

        this.getFleet();
        if (props instanceof LoadQueriesModal || props.props instanceof LoadQueriesModal) {
            // add Site-Wide queries
            this.state.groupsDisplay.push([0, "NGAFID"]);

            // specify groupSelect dropdown id
            this.state.id = "loadGroupSelect";
        }
    }

    // a method to retrieve the fleetID and name of the fleet the user belongs to
    getFleet() {
        let thisGroups = this;
        $.ajax({
            type: 'GET',
            url: '/protected/get_query_groups',
            success : function(response) {
                if($.type(response) === "string"){
                    let fields = response.split(",");

                    // add fleet to list if user has appropriate access (or if loading)
                    if (fields[3] == "MANAGER" || fields[3] == "UPLOAD" || this.state.parentModal instanceof LoadQueriesModal) {
                        thisGroups.state.groupsDisplay.push([parseInt(fields[1]), fields[2]]);
                    }

                } else {
                    errorModal.show("Error retrieving user fleet info", "unexpected AJAX response type");
                }
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error retrieving user fleet info", "unsuccessful AJAX call");
            },
            async: true
        });
    }

    // a method to handle necessary state changes and rerenderings after selection of a new group
    handleGroupSelectChange() {
        this.state.parentModal.props.state.selectedGroup = $(this.state.id).val();
        this.state.selectedGroup = $(this.state.id).val();
        this.state.parentModal.props.getQueryNames();
        this.state.parentModal.props.setState(this.state.parentModal.props.state);
    }

    render(){
        return (
            <select name="groupSelect" id={this.state.id} type={"select"} key={0} className="form-control" onChange={(event) => { this.handleGroupSelectChange();}} style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                {
                    this.state.groupsDisplay.map( (groupInfo, index) =>
                        <option key={index} value={groupInfo[0]}>{groupInfo[1]}</option>
                    )
                }
            </select>
        )
    }
}

// export modals
var saveQueriesModal = ReactDOM.render(
    <SaveQueriesModal />,
    document.querySelector("#save-query-modal-content")
);

var loadQueriesModal = ReactDOM.render(
    <LoadQueriesModal />,
    document.querySelector("#load-query-modal-content")
);

export { loadQueriesModal, saveQueriesModal };
