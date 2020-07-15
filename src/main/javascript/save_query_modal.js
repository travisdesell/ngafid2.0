import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import { Filter } from "./filter.js"

import $ from 'jquery';
window.jQuery = $;
window.$ = $;

// The SaveQueriesModal component defines a modal interface for the loading and saving of queries
class SaveQueriesModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            noEntriesMessage : "Sorry, No Entries Found",
            selectedGroup : "user",
            query : null,
            queryText : ""
        };
    }

    updateQuery(queryObject) {
        this.state.query = queryObject;
        this.state.queryText = JSON.stringify(queryObject);
    }

    show() {
        this.setState(this.state, () => {$("#save-query-modal").modal('show')});
    }

    getGroups(){
        // method to retrieve groups (fleets) associated with user
        // call to GetSavedQueries Route to retrieve user's associated queries (by fleet_id, user_id, ngafid -> based on if loading or not)
        return (
            <select name="groupSelect" id="groupSelect" type={"select"} key={0} className="form-control" onChange={(event) => { this.state.selectedGroup = groupSelect.value }} style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                <option value="user">user</option>
                <option value="fleetA">fleetA</option>
                <option value="fleetB">fleetB</option>
            </select>
        )
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
        let groups = this.getGroups();
        //let names = this.getNames();
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
                                {groups}
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

                    <div className="d-flex" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="queryText" style={labelStyle}>Your Query Text:</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <textarea id="country" className="form-control" name="queryText" rows="5" cols="200" wrap="soft" value={queryText} readOnly>
                                </textarea>
                            </div>
                        </div>
                    </div>

                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                    <button id='submitButton' type='submit' className='btn btn-primary' onClick={() => {$("#save-query-modal").modal('hide')}} disabled={saveDisabled}>{submitLabel}</button>
                </div>

            </div>
        );
    }
}

var saveQueriesModal = ReactDOM.render(
    <SaveQueriesModal />,
    document.querySelector("#save-query-modal-content")
);

export { saveQueriesModal };


// need validation that name is not taken*
// load groups user has upload / owner access levels
// save query to DB