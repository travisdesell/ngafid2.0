import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import Form from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import ListGroup from 'react-bootstrap/ListGroup';

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";


class PreferencesPage extends React.Component {
    constructor(props) {
        super(props);
       
        this.state = {
            preferences : userPreferences,
            waitingUserCount : this.props.waitingUserCount,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount
        };

		console.log("this users prefs:");
		console.log(this.state.preferences);
	}

    updatePreference(systemId) {
        let newTail = $("#" + systemId.systemId + "-tail-number-form").val();
        console.log("updating system id on server -- original tail: '" + systemId.originalTail + "', current value: '" + systemId.tail + "', newTail: '" + newTail + "'");
        if (systemId.tail === "") systemId.tail = systemId.originalTail;

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
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                systemId.confirmed = true;
                systemId.modified = false;

                systemIdsPage.setState({
                    unconfirmedTailsCount : (systemIdsPage.state.unconfirmedTailsCount - 1)
                });
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Updating Tail Number", errorThrown);
            },   
            async: true 
        });  
    }

    render() {
		let selectedMetrics = this.state.preferences.flightMetrics;

        return (
            <div>
                <SignedInNavbar activePage="account" waitingUserCount={this.state.waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={this.state.unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <div className="container-fluid">
                    <div className="row">
                        <div className="col" style={{paddingRight:"16"}}>
							 <div style={{marginTop:"4", padding:"0 0 0 0"}}>
									<div className="col" style={{padding:"0 0 0 0"}}>
										<div className="card" style={{background : "rgba(248,259,250,0.8)"}}>
											<h5 className="card-header">
												Your Preferences:
											</h5>
											<div className="card-body">
												<div className="form-row align-items-left justify-content-left">
													<Form.Group controlId="exampleForm.ControlInput1">
														<Form.Label>Decimal Precision:</Form.Label>
														<Form.Control as="select">
															<option>1</option>
															<option>2</option>
															<option>3</option>
															<option>4</option>
															<option>5</option>
														</Form.Control>	
													</Form.Group>
												</div>
												<div className="form-row justify-content-left">
													<ListGroup style={{flexDirection:'row'}} horizontal>
													{
														selectedMetrics.map((metric, key) => {
															return (
																<ListGroup.Item action onClick={this.updatePreference(1)}>{metric}</ListGroup.Item>
															);
														})
													}
													</ListGroup>
												</div>
												<div className="form-row align-items-left justify-content-left">
													available metrics
												</div>
											<hr style={{padding:"0", margin:"0 0 5 0"}}></hr>
										</div>
									</div>
								</div>
							</div>



                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

console.log("setting preferences page with react!");

var preferencesPage = ReactDOM.render(
    <PreferencesPage waitingUserCount={waitingUserCount} unconfirmedTailsCount={unconfirmedTailsCount}/>,
   document.querySelector('#preferences-page')
)
