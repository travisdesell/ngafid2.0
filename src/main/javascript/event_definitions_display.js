import React from "react";
import ReactDOM from "react-dom";
import SignedInNavbar from "./signed_in_navbar";
import GetAllDescriptions from "./get_all_descriptions";

class EventDefinition extends React.Component {
    constructor(props) {
        super(props);

        this.eventName = props.eventName;
        this.airframeName = props.airframeName;
        this.eventDef = props.eventDef;
    }

    render() {
        return (
            <React.Fragment>
                <td>{this.eventName}</td>
                <td>{this.airframeName}</td>
                <td>{this.eventDef}</td>
            </React.Fragment>
        )
    }
}


class EventDefinitionsDisplayPage extends React.Component {
    constructor(props) {
        super(props);
        this.events = new Map(Object.entries(GetAllDescriptions()));
        console.log(this.events);
    }

    createRows() {
        let rows = [];

        for (let eventName of this.events.keys()) {
            console.log(eventName);
            console.log(this.events.get(eventName));
            for (let airframe in Object.keys(this.events.get(eventName))) {
                rows.push([eventName, airframe, this.events.get(eventName)[airframe]]);
            }
        }

        return rows;
    }

    render() {
        let rows = this.createRows();
        console.log(rows);
        return (
            <div>
                <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                <div className="container-fluid" style={{backgroundColor: "white"}}>
                    <div className="row">
                        <div className="col-md-12">
                            <table className="table table-striped table-bordered table-hover">
                                <thead>
                                <tr>
                                    <th>Event Name</th>
                                    <th>Aircraft Type</th>
                                    <th>Event Definition</th>
                                </tr>
                                </thead>
                                <tbody>
                                {rows.map((row, index) => {
                                    return (
                                        <tr>
                                            <th>{row[0]}</th>
                                            <th>{row[1]}</th>
                                            <th>{row[2]}</th>

                                        </tr>
                                    )
                                })}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

            </div>
        )
    }
}


var eventDefinitionsDisplayPage = ReactDOM.render(
    <EventDefinitionsDisplayPage/>, document.querySelector('#event-definitions-display-page')
)