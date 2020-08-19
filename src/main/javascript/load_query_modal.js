import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import { Filter } from "./filter.js"
import { errorModal } from "./error_modal.js";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;

// need validation checking to ensure all fields selected
// disable submit button based on val

// validate user has appropriate access
// load query from database and populate filter
// load queryString from db and populate textarea

// The LoadQueriesModal component defines a modal interface for the loading of queries
class LoadQueriesModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            noEntriesMessage : "Sorry, No Entries Found",
            selectedGroup : null,
            selectedQuery : null,
            user_id : null,
            queryText : "",
            groups : new Groups(this)                       // hashtable of groupIDs to available queries* (Queries should be an object)
        };
    }

    show() {
        this.setState(this.state, () => { $("#load-query-modal").modal('show')});
    }

    getNames() {
        // method to construct query name section of modal (load associated query names or create input field for 'naming' query)
        // load up the query names associated with the selected fleet / user (load all if nothing selected?)
        // ajax call to retrieve JSON object / hashmap of groups - names
        let groups = {
            fleetA : ["query1", "query2", "query3"],
            fleetB : ["queryA", "queryB", "queryC"],
            user : ["queryI", "queryII", "queryIII"],
            ngafid : ["query1", "query2", "query3"]
        };

        // display names from selected group
        var names = groups[this.state.selectedGroup];

        if (names) {
            return (
                <select name="querySelect" id="querySelect" type={"select"} className="form-control" placeholder="Please Select Query" onChange={(event) => { this.state.selectedQuery = querySelect.value; this.setState(this.state);}} style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                    {
                        names.map( (queryName, index) =>
                            <option key={index} value={queryName}>{queryName}</option>
                        )
                    }
                </select>
            )
        } else {
            return (
                <select name="querySelect" id="querySelect" type={"select"} className="form-control" onChange={(event) => { this.state.selectedQuery = querySelect.value; }} style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                     <option>Please Select Group</option>
                 </select>
            )
        }
    }

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

        // generate header message
        let groups = this.state.groups;
        let names = this.getNames();
        let headerMessage = "Where would you like to load from?";
        let dropdownLabel = "Source:";
        let submitLabel = "Load";
        let submitDisabled = !this.isValidLoad();


        //console.log("rendering login modal with validation message: '" + validationMessage + "' and validation visible: " + validationHidden);

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
                                <Groups id='groups' props={this}/>
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
                            <textarea id="country" className="form-control" name="queryText" rows="5" cols="200" wrap="soft" value={"Oh Lookie at all the Query Details. Fancy Fancy. Oh, I wonder if the text is gonna wrap. It'd be such a tragedy if the text left the textarea"} readOnly>
                            </textarea>
                        </div>
                    </div>

                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                    <button id='submitButton' type='submit' className='btn btn-primary' onClick={() => {}} disabled={submitDisabled}>{submitLabel}</button>
                </div>
            </div>
        );
    }
}
// for each query in selected Group, display option w/ queryName
// textarea value change depending on name selected*


// need validation that name is not taken*
// load groups user has upload / owner access levels*

// TODO: groups => QueryGroups across DB and code
// TODO: format name taken msg

// The SaveQueriesModal component defines a modal interface for the loading and saving of queries
class SaveQueriesModal extends React.Component {
    constructor(props) {
        super(props);

        let x = new Groups(this);

        this.state = {
            noEntriesMessage : "Sorry, No Entries Found",
            selectedGroup : "user",
            query : null,
            queryText : "",
            user_id : null,             // needed?
            groups : null,
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

    isValidSave() {
        // method to validate fields before enabling save button
        let valid = false;

        // ensure fields not blank
        let nameElement = $("#name")[0];
        if (this.state.query && this.state.selectedGroup && nameElement) {
            if (nameElement.value) {
                valid = true;
            }
        }

        // TODO: ensure name not taken

        return valid;
    }

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

    save(){
        // ajax call to insert query into database
        var submissionData = {
            queryName: $("#name").val(),
            queryText: this.state.queryText,
            query: JSON.stringify(this.state.query),
            fleetID: $("#groupSelect").val()
        };

        let modalDisplay = $("#save-query-modal")
        let thisModal = this;

        $.ajax({
            type: 'POST',
            url: '/protected/save_query',
            data: submissionData,
            dataType : 'json',
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
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        // generate header messages
        let groups = this.state.groups;
        let queryText = this.state.queryText;
        let headerMessage = "Where would you like to save to?";
        let dropdownLabel = "Destination:";
        let submitLabel = "Save";
        let saveDisabled = !this.isValidSave();


        //console.log("rendering login modal with validation message: '" + validationMessage + "' and validation visible: " + validationHidden);

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
                                <Groups id='groups' props={this}/>
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

class Query extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            name : null,
            queryString : null,
            queryText : null
        };
    }

    save(){}

    load(){}

    //getters/setters
    getText(){
        return this.state.queryText;
    }

    render(){
        return null;
    }
}

// class to maintain and organize query "groups" (fleets', user's, and website's queries)
class Groups extends React.Component {
    constructor(props) {
        super(props);

        let grps = [[-1, "my queries"]];
        if (props.props instanceof LoadQueriesModal) {
            grps.push([0, "NGAFID"]);
        }

        this.state = {
            parentModal : props,
            groupsDisplay : grps,
            groups : null,                   //group to Queries hashmap
            user_id : null
        }

        this.getFleet();
    }

    getQueries() {}

    getFleet() {
        let thisGroups = this;
        $.ajax({
            type: 'GET',
            url: '/protected/get_query_groups',
            success : function(response) {
                if($.type(response) === "string"){
                    let fields = response.split(",");
                    thisGroups.state.user_id = parseInt(fields[0]);

                    // add fleet to list if user has appropriate access (or if loading)
                    if (fields[3] == "MANAGER" || fields[3] == "UPLOAD" || this.state.parentModal instanceof LoadQueriesModal) {
                        thisGroups.state.groupsDisplay.push([parseInt(fields[1]), fields[2]]);
                    }

                } else {
                    errorModal.show("Error retrieving user access level", "unexpected AJAX response type");
                }
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error retrieving user access level", "unsuccessful AJAX call");
            },
            async: true
        });
    }

    render(){
        return (
            <select name="groupSelect" id="groupSelect" type={"select"} key={0} className="form-control" onChange={(event) => { this.state.parentModal.props.state.selectedGroup = $("#groupSelect :selected").val(); }} style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                {
                    this.state.groupsDisplay.map( (groupInfo, index) =>
                        <option key={index} value={groupInfo[0]}>{groupInfo[1]}</option>
                    )
                }
            </select>
        )
    }
}

var saveQueriesModal = ReactDOM.render(
    <SaveQueriesModal />,
    document.querySelector("#save-query-modal-content")
);

var loadQueriesModal = ReactDOM.render(
    <LoadQueriesModal />,
    document.querySelector("#load-query-modal-content")
);

export { loadQueriesModal, saveQueriesModal };
