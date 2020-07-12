import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;

// The SaveQueriesModal component defines a modal interface for the loading and saving of queries
class LoadQueriesModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            noEntriesMessage : "Sorry, No Entries Found",
            selectedGroup : null,
            selectedQuery : null,
            user_id : null
        };
    }

    show() {
        $("#load-query-modal").modal('show');
    }

    getGroups(){
        // method to retrieve groups (fleets) associated with user
        // call to GetSavedQueries Route to retrieve user's associated queries (by fleet_id, user_id, ngafid -> based on if loading or not)
        return (
            <select name="groupSelect" id="groupSelect" type={"select"} className="form-control" onChange={(event) => {this.state.selectedGroup = event.target.value; this.setState(this.state);} } style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                <option key="0">Please Select</option>
                <option key="1" value="user">user</option>
                <option key="2" value="fleetA">fleetA</option>
                <option key="3" value="fleetB">fleetB</option>
                <option key="4" value="ngafid">NGAFID</option>
            </select>
        )
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
                <select name="querySelect" id="querySelect" type={"select"} className="form-control" onChange={(event) => { this.state.selectedQuery = querySelect.value; }} style={{flexBasis:"150px", flexShrink:0, marginRight:5}}>
                    <option>Please Select</option>
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
                     <option selected="">Please Select Group</option>
                 </select>
            )
        }
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
        let names = this.getNames();
        let headerMessage = "Where would you like to load from?";
        let dropdownLabel = "Source:";
        let submitLabel = "Load";


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
                                {names}
                            </div>
                        </div>
                    </div>

                    <div className="d-flex" style={formGroupStyle}>
                        <div className="p-2" style={formHeaderStyle}>
                            <label htmlFor="queryText" style={labelStyle}>Full Query:</label>
                        </div>
                        <div className="p-2 flex-fill" style={formHeaderStyle}>
                            <textarea id="country" className="form-control" name="queryText" rows="5" cols="200" wrap="soft" readOnly>
                                {"Oh Lookie at all the Query Details. Fancy Fancy. Oh, I wonder if the text is gonna wrap. It'd be such a tragedy if the text left the textarea"}
                            </textarea>
                        </div>
                    </div>

                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                    <button id='submitButton' type='submit' className='btn btn-primary' onClick={() => {}} disabled={true}>{submitLabel}</button>
                </div>
            </div>
        );
    }
}

var loadQueriesModal = ReactDOM.render(
    <LoadQueriesModal />,
    document.querySelector("#load-query-modal-content")
);

export { loadQueriesModal };


// need validation checking to ensure all fields selected & appropriate access
// disable Select button based on val