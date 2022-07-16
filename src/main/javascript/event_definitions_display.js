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
        this.events = {};

        for (let i = 0; i < eventNames.length; i++) {
            this.events[eventNames[i]] = GetDescription(eventNames[i]);
        }
    }

    render() {
        let rows = [];

        for (let key in this.events) {
            rows.push(<EventDefinition eventName={key} eventDef={this.events[key]} />)
        }

        return (
            <div>
                <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                {rows}
            </div>
        )
    }
}


var eventDefinitionsDisplayPage = ReactDOM.render(
    <EventDefinitionsDisplayPage/>, document.querySelector('#event-definitions-display-page')
)