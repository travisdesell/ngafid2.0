import 'bootstrap';

import React from "react";
import ReactDOM from "react-dom";

import {errorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";


class SystemIdsPage extends React.Component {
    constructor(props) {
        super(props);

        systemIds.forEach(systemId => {
            if (systemId.confirmed === true) {
                systemId.modified = false;
            } else {
                systemId.modified = true;
            }
            systemId.originalTail = systemId.tail;
        });

        this.state = {
            systemIds: systemIds,
            waitingUserCount: this.props.waitingUserCount,
            unconfirmedTailsCount: this.props.unconfirmedTailsCount
        };
    }

    validateTail(systemId) {
        console.log("original tail: '" + systemId.originalTail + "', current value: '" + systemId.tail + "'");

        let newTail = $("#" + systemId.systemId + "-tail-number-form").val();

        if (systemId.confirmed === 1) {
            if (newTail === "") {
                systemId.modified = false;
            } else if (systemId.originalTail !== newTail) {
                systemId.modified = true;
            } else {
                systemId.modified = false;
            }
        } else {
            systemId.modified = true;
        }
        systemId.tail = newTail;

        console.log(systemId);
        this.setState(this.state);
    }

    updateSystemId(systemId) {
        let newTail = $("#" + systemId.systemId + "-tail-number-form").val();
        console.log("updating system id on server -- original tail: '" + systemId.originalTail + "', current value: '" + systemId.tail + "', newTail: '" + newTail + "'");
        if (systemId.tail === "") systemId.tail = systemId.originalTail;

        let systemIdsPage = this;

        $.ajax({
            type: 'PATCH',
            url: `/api/aircraft/system-id/${encodeURIComponent(systemId.systemId)}`,
            data: {tail: systemId.tail},
            dataType: 'json',
            success: function (response) {
                console.log("received response: ");
                console.log(response);

                systemId.confirmed = true;
                systemId.modified = false;
                systemId.tail = response.tail;
                systemId.originalTail = response.tail;

                systemIdsPage.state.unconfirmedTailsCount -= 1;
                systemIdsPage.setState(systemIdsPage.state);
                console.log(systemIdsPage.state);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Updating Tail Number", errorThrown);
            },
            async: true
        });
    }

    getSystemIdsPage(name, type) {
        return (
            <div style={{marginTop: "4", padding: "0 0 0 0"}}>
                <div className="col-sm-12" style={{padding: "0 0 0 0"}}>
                    <div className="card mb-1 m-1">
                        <h5 className="card-header">
                            {name}
                        </h5>

                        <div className="card-body">
                            <div className="form-row align-items-center justify-content-center">
                                <div className="col-sm-5 my-1" style={{margin: "0"}}>
                                    <label style={{marginBottom: "0"}}>System ID</label>
                                </div>
                                <div className="col-sm-6 my-1" style={{margin: "0"}}>
                                    <label style={{marginBottom: "0"}}>Tail Number</label>
                                </div>
                                {
                                    (!type)
                                    &&
                                    <div className="col-sm-1 my-1" style={{margin: "0"}}>
                                        <label style={{marginBottom: "0"}}>Submit </label>
                                    </div>
                                }
                            </div>

                            <hr style={{padding: "0", margin: "0 0 5 0"}}></hr>

                            {
                                systemIds.map((systemId, systemIdIndex) => {
                                    if (systemId.confirmed == type) {
                                        return (
                                            <form key={systemIdIndex} style={{marginBottom: "0px"}}>
                                                <div className="form-row align-items-center justify-content-center">
                                                    <div className="col-sm-5 my-1">
                                                        <label className="sr-only"
                                                               htmlFor={systemId.systemId + "-system-id-form"}>Name</label>
                                                        <input type="text" className="form-control"
                                                               id={systemId.systemId + "-system-id-form"}
                                                               placeholder={systemId.systemId} readOnly></input>
                                                    </div>
                                                    <div className="col-sm-6 my-1">
                                                        <label className="sr-only"
                                                               htmlFor={systemId.systemId + "-tail-number-form"}>Tail
                                                            Number</label>
                                                        <input type="text" className="form-control"
                                                               id={systemId.systemId + "-tail-number-form"}
                                                               placeholder={systemId.originalTail}
                                                               onChange={() => this.validateTail(systemId)}></input>
                                                    </div>

                                                    {
                                                        (systemId.modified)
                                                        &&
                                                        <div className="col-sm-1 my-1">
                                                            <button type="button"
                                                                    className={"btn btn-primary" + (systemId.modified ? "btn-outline-primary" : "btn-outline-secondary")}
                                                                    style={{
                                                                        width: "36",
                                                                        height: "36",
                                                                        backgroundColor: "var(--c_confirm)",
                                                                        color: "white"
                                                                    }} onClick={() => {
                                                                this.updateSystemId(systemId)
                                                            }}>
                                                                <div className="d-flex justify-content-center">
                                                                    <i className='fa fa-check'
                                                                       style={{textAlign: "center"}}/>
                                                                </div>
                                                            </button>
                                                        </div>
                                                    }
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
        //console.log(systemIds);

        let unconfirmedHtml = this.getSystemIdsPage("Unconfirmed System IDs", false);
        let confirmedHtml = this.getSystemIdsPage("Confirmed System IDs", true);

        return (
            <div style={{display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage="account" waitingUserCount={this.state.waitingUserCount}
                                    fleetManager={fleetManager} unconfirmedTailsCount={this.state.unconfirmedTailsCount}
                                    modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                </div>

                <div style={{overflowY: "scroll", flex: "1 1 auto"}}>
                    <div className="container-fluid">
                        <div className="row">
                            <div className="col-lg-6" style={{paddingRight: "0"}}>
                                {unconfirmedHtml}
                            </div>
                            <div className="col-lg-6" style={{paddingLeft: "0"}}>
                                {confirmedHtml}
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        );
    }
}

console.log("setting system ids page with react!");

var systemIdsPage = ReactDOM.render(
    <SystemIdsPage waitingUserCount={waitingUserCount} unconfirmedTailsCount={unconfirmedTailsCount}/>,
    document.querySelector('#system-ids-page')
);
