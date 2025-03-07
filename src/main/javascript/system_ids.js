import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";


const EMPTY_TAIL_STRING = "";

const MISSING_AIRFRAME_NAMES_MESSAGE = `In the event that a desired Airframe Name is not currently supported or listed below, please send a request via email to have it added.`;

class SystemIdsPage extends React.Component {

    constructor(props) {

        super(props);

        //Clear System ID editing state
        systemIds.forEach(systemId => {
            systemId.modified = false;
            systemId.originalTail = systemId.tail;
        });

        this.state = {
            systemIds : systemIds,
            waitingUserCount : this.props.waitingUserCount,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount,
            airframeNamesList : [],
        };

        //Fetch all Airframe Names
        this.fetchAllAirframeNames();

        //Fetch any existing mappings of System ID to Aircraft ID
        this.getSystemIDAircraft();

    }

    fetchAllAirframeNames() {

        /*
            Fetch & populate the list of available Airframe Names to select from
        */ 

        $.ajax({
            type: 'GET',
            url: '/protected/all_aircraft',
            dataType : 'json',
            async: true,
            success : function(response) {

                console.log("all_aircraft -- Received response: ", response);

                let airframeNamesList = [];

                /*
                    TODO: Handle Response...
                */

                this.setState({
                    airframeNamesList : airframeNamesList
                });

            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Fetching Airframe Names", errorThrown);
            }
        });

    }

    setSystemIDToAircraft(systemId) {

        /*
            Map the System ID to the Aircraft ID
        */

        console.log(`setSystemIDToAircraft -- Mapping System ID to Aircraft ID... System ID: ${systemId.systemId}, Aircraft ID: ${systemId.aircraftId}`);
     
        var submissionData = {
            systemId : systemId.systemId,
            aircraftId : systemId.aircraftId
        };

        let systemIdsPage = this;

        $.ajax({
            type: 'POST',
            url: '/protected/set_system_id_aircraft',
            data : submissionData,
            dataType : 'json',
            async: true,
            success : function(response) {
                console.log("setSystemIDAircraft -- Received response: ", response);

                /*
                    TODO: Handle Response...
                */

                //Trigger Re-render
                systemIdsPage.setState(systemIdsPage.state);

            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Mapping System ID to Aircraft", errorThrown);
            }
        });

    }

    getSystemIDAircraft() {

        /*
            Fetch any existing mappings of System ID to Aircraft ID
        */

        $.ajax({
            type: 'GET',
            url: '/protected/get_system_id_aircraft',
            dataType : 'json',
            async: true,
            success : function(response) {

                console.log("getSystemIDAircraft -- Received response: ", response);

                /*
                    TODO: Handle Response...
                */

            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Fetching System ID to Aircraft Mappings", errorThrown);
            }
        });

    }

    validateTail(systemId) {

        let newTail = $("#" + systemId.systemId + "-tail-number-form").val();

        /*
            console.log(`validateTail -- Original Tail: '${systemId.originalTail}', Current Value: '${systemId.tail}', New Value: ${newTail}`);
        */

        //Tail has been confirmed...
        if (systemId.confirmed) {

            //...Tail is empty, not modifying
            if (newTail === EMPTY_TAIL_STRING)
                systemId.modified = false;
            
            //...Tail has been changed, is modifying
            else
                systemId.modified = (systemId.originalTail !== newTail);

        //Tail has not been confirmed, mark as modifying
        } else {

            systemId.modified = (newTail !== EMPTY_TAIL_STRING);

        }

        //Update new tail
        systemId.tail = newTail;

        //Trigger Re-render
        this.setState(this.state);

    }

    updateSystemId(systemId, isConfirmedTable) {

        //Apply Tail Number Update
        this.updateSystemIdTailNumber(systemId, isConfirmedTable);

        //Apply Aircraft ID Update
        this.setSystemIDToAircraft(systemId);

    }

    updateSystemIdTailNumber(systemId, isConfirmedTable) {

        let newTail = $("#" + systemId.systemId + "-tail-number-form").val();
        console.log(`updateSystemId -- Updating System ID on server... Original Tail: '${systemId.originalTail}', Current Value: '${systemId.tail}', New Tail: '${newTail}'`);
        
        //Current tail string is empty, set to original value
        if (systemId.tail === EMPTY_TAIL_STRING)
            systemId.tail = systemId.originalTail;

        var submissionData = {
            systemId : systemId.systemId,
            tail : systemId.tail
        };

        let systemIdsPage = this;

        $.ajax({
            type: 'POST',
            url: '/protected/update_tail',
            data : submissionData,
            dataType : 'json',
            async: true,
            success : function(response) {
                console.log("update_tail -- Received response: ", response);

                systemId.confirmed = true;
                systemId.modified = false;
                systemId.originalTail = systemId.tail;

                //Update unconfirmed tails count when Submitting (but not Updating)
                let newUnconfirmedTailsCount = (systemIdsPage.state.unconfirmedTailsCount - (!isConfirmedTable));
                systemIdsPage.setState({
                    unconfirmedTailsCount : newUnconfirmedTailsCount
                });

            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Updating Tail Number", errorThrown);
            }
        });

    }

    getSystemIdsPage(name, isConfirmedTable, sortedSystemIds) {
        return (
            <div style={{marginTop:"1em", marginBottom:"1em", padding:"0 0 0 0"}}>
                <div className="col-sm-12" style={{padding:"0 0 0 0"}}>
                    <div className="card mb-1 m-1">

                        <h5 className="card-header">
                            {name}
                        </h5>

                        {/* Table Headers */}
                        <div className="card-body">
                            <div className="form-row align-items-center justify-content-center">

                                {/* Headers -- System ID */}
                                <div className="col-sm-3 my-1" style={{margin:"0"}}>
                                    <label style={{marginBottom:"0"}}>System ID</label>
                                </div>

                                {/* Headers -- Airframe Name */}
                                <div className="col-sm-4 my-1" style={{margin:"0"}}>
                                    <i className="fa fa-warning mr-2" style={{opacity: "50%"}} title={MISSING_AIRFRAME_NAMES_MESSAGE}></i>
                                    <label style={{marginBottom:"0"}}>Airframe Name</label>
                                </div>

                                {/* Headers -- Tail Number */}
                                <div className="col-sm-4 my-1" style={{margin:"0"}}>
                                    <label style={{marginBottom:"0"}}>Tail Number</label>
                                </div>

                                {/* Headers -- Submit/Update Button */}
                                <div className="col-sm-1 my-1" style={{margin:"0"}}>
                                    <label style={{marginBottom:"0"}}>{isConfirmedTable ? "Update" : "Submit"}</label>
                                </div>
                            </div>

                            <hr style={{padding:"0", margin:"0 0 5 0"}}/>

                            {/* Table Rows */}
                            {
                                sortedSystemIds.map((systemId, systemIdIndex) => {
                                    if (systemId.confirmed == isConfirmedTable) {
                                        return (
                                            <form key={systemIdIndex} style={{marginBottom:"0px"}}>
                                                <div className="form-row align-items-center justify-content-center">

                                                    {/* System ID  */}
                                                    <div className="col-sm-3 my-1">
                                                        <label className="sr-only" htmlFor={systemId.systemId + "-system-id-form"}>
                                                            Name
                                                        </label>
                                                        <input type="text" className="form-control" id={systemId.systemId + "-system-id-form"} value={systemId.systemId} readOnly/>
                                                        <i className="fa fa-lock position-absolute" style={{right: "1em", bottom: "0.5em", color: "var(--c_text_alt)", opacity: "25%"}}/>
                                                    </div>

                                                    {/* Airframe Name Dropdown Menu */}
                                                    <div className="col-sm-4 my-1">
                                                        <label className="sr-only" htmlFor={systemId.systemId + "-airframe-form"}>
                                                            Airframe Name
                                                        </label>
                                                        <select 
                                                            className="form-control" 
                                                            id={systemId.systemId + "-airframe-form"} 
                                                            value={systemId.airframe} 
                                                            onChange={() => this.validateAirframe(systemId)}
                                                        >
                                                            <option value="" style={{color: "var(--c_text_alt)"}}>
                                                                Select Airframe
                                                            </option>
                                                            <hr/>   {/*<-- This gives an error in the browser console, but it's valid HTML I promise*/}
                                                            {
                                                                (this.state.airframeNamesList.length === 0) ? (
                                                                    <option value="" className='font-italic' disabled={true} style={{color: "var(--c_text_alt)"}}>
                                                                        Loading Available Airframe Names...
                                                                    </option>
                                                                ) : (
                                                                    this.state.airframeNamesList.map((airframe, idx) => (
                                                                        <option key={idx} value={airframe}>
                                                                            {airframe}
                                                                        </option>
                                                                    ))
                                                                )
                                                            }
                                                        </select>
                                                    </div>

                                                    {/* Tail Number Input Field */}
                                                    <div className="col-sm-4 my-1">
                                                        <label className="sr-only" htmlFor={systemId.systemId + "-tail-number-form"}>
                                                            Tail Number
                                                        </label>
                                                        <input type="text" className="form-control" id={systemId.systemId + "-tail-number-form"} placeholder={systemId.originalTail} onInput={() => this.validateTail(systemId)}/>
                                                    </div>

                                                    {/* Submit Button */}
                                                    <div className="col-sm-1 my-1">
                                                        <button type="button" className={"btn " + (systemId.modified ? "btn-success" : "btn-outline-success")} disabled={!systemId.modified} style={{width:"36", height:"36"}} onClick={() => {this.updateSystemId(systemId, isConfirmedTable)}}>
                                                            <div className="d-flex justify-content-center">
                                                                <i className='fa fa-check' style={{textAlign:"center"}}/>
                                                            </div>
                                                        </button>
                                                    </div>
                                                </div>
                                            </form>
                                        );
                                    } else {
                                        return ("");
                                    }
                                })
                            }
                        </div>
                    </div>
                </div>
            </div>
        );

    }

    render() {

        const sortedSystemIds = [...this.state.systemIds].sort((a,b) => parseInt(a.systemId) - parseInt(b.systemId));
        const { unconfirmedTailsCount } = this.state;

        /*
            console.log("System IDs: ", sortedSystemIds);
        */

        const totalSystemIDCount = sortedSystemIds.length;
        const confirmedIDCount = (totalSystemIDCount - unconfirmedTailsCount);

        let unconfirmedHtml = this.getSystemIdsPage(`Unconfirmed System IDs (${unconfirmedTailsCount} / ${totalSystemIDCount})`, false, sortedSystemIds);
        let confirmedHtml = this.getSystemIdsPage(`Confirmed System IDs (${confirmedIDCount} / ${totalSystemIDCount})`, true, sortedSystemIds);
        
        return (
            <div style={{display:"flex", flexDirection:"column", height:"100vh"}}>

                <div style={{flex:"0 0 auto"}}>
                    <SignedInNavbar activePage="account" waitingUserCount={this.state.waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={this.state.unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                </div>

                <div style={{overflowY:"scroll", flex:"1 1 auto"}}>
                    <div className="container-fluid">
                        <div className="row">
                            <div className="col-lg-6" style={{paddingRight:"0"}}>
                                {unconfirmedHtml}
                            </div>
                            <div className="col-lg-6" style={{paddingLeft:"0"}}>
                                {confirmedHtml}
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        );
    }
}

console.log("Setting System IDs page with React!");

var systemIdsPage = ReactDOM.render(
    <SystemIdsPage waitingUserCount={waitingUserCount} unconfirmedTailsCount={unconfirmedTailsCount}/>,
   document.querySelector('#system-ids-page')
);