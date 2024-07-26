import React from "react";
import ReactDOM from "react-dom";
import SignedInNavbar from "./signed_in_navbar";
import GetAllDescriptions from "./get_all_descriptions";

class EventDefinitionsDisplayPage extends React.Component {
    constructor(props) {
        super(props);
        this.events = new Map(Object.entries(GetAllDescriptions()));
    }

    render() {
        let rows = [];

        for (let eventName of this.events.keys()) {
            for (let airframe of Object.keys(this.events.get(eventName))) {
                rows.push([eventName, airframe, this.events.get(eventName)[airframe]]);
            }
        }

        return (
            <div style={{width:"100%", overflowX:"hidden"}}>
                <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}
                />
                <div className="m-2" style={{border: "1px solid white", borderRadius:"8px"}}>
                    <table className="table table-striped table-hover" style={{backgroundColor:"var(--c_card_bg)", borderRadius:"8px"}}>
                        <thead>
                            <tr>
                                <th>Event Name</th>
                                <th style={{minWidth:"10%"}}>Aircraft Type</th>
                                <th>Event Definition</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map((row, index) => {
                                return (
                                    <tr key={index}>
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
        )
    }
}


var eventDefinitionsDisplayPage = ReactDOM.render(
    <EventDefinitionsDisplayPage/>, document.querySelector('#event-definitions-display-page')
)