import React from "react";
import GetDescription from "./get_description";
import ReactDOM from "react-dom";
import SignedInNavbar from "./signed_in_navbar";

class EventDefinition extends React.Component {
    constructor(props) {
        super(props);

        this.eventName = props.eventName;
        this.eventDef = props.eventDef;
    }

    render() {
        const styleName = {flex: "0 0 11em"};
        const styleDefinition = {};

        let textClasses = "p-1 mr-1 card bg-light";
        let eventNameText = this.eventName;
        let eventDefText = this.eventDef;

        return (
            <div className="m-1">
                <div className="d-flex flex-row">
                    <div className={textClasses} style={styleName}>{eventNameText}</div>
                    <div className={textClasses + " flex-fill"} style={styleDefinition}>{eventDefText}</div>
                </div>
            </div>

        )
    }
}


class EventDefinitionsDisplayPage extends React.Component {
    constructor(props) {
        super(props);
        let events = {};

        for (const element of eventNames) {
            this.events[element] = GetDescription(element);
        }

        this.events = new Map(Object.entries(events));
    }

    render() {
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
                                     <th>Event ID</th>
                                     <th>Event Name</th>
                                     <th>Aircraft Type</th>
                                     <th>Event Definition</th>
                                 </tr>
                                 <tbody>
                                {this.events.map((description) => {
                                    return (
                                        <tr>
                                            <td>1</td>
                                            <td>2</td>
                                            <td>3</td>
                                            <td>4</td>
                                        </tr>
                                    )
                                })}
                                </tbody>
                                 </thead>
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