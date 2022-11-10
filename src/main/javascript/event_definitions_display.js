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
            <div>
                <td>{this.eventName}</td>
                <td>{this.airframeName}</td>
                <td>{this.eventDef}</td>
            </div>
        )
    }
}


class EventDefinitionsDisplayPage extends React.Component {
    constructor(props) {
        super(props);
        this.events = new Map(Object.entries(GetAllDescriptions()));
        console.log(this.events);

        // for (let key in Object.keys(this.events)) {
        //     for (let id in Object.keys(this.events[key])) {
        //
        //         console.log(this.events[key][id]);
        //     }
        // }
        //
        // console.log(this.events);
    }

    createRows() {
        let rows = [];

        for (let key in Object.keys(this.events)) {
            for (let id in Object.keys(this.events[key])) {
                console.log(this.events[key][id]);
                rows.push(<EventDefinition eventName={this.events[key][id].name} airframeName={this.events[key][id].airframe} eventDef={this.events[key][id].description}/>);
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
                                {this.createRows()}
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