import React from "react";
import GetDescription from "./get_description";
import ReactDOM from "react-dom";
import SignedInNavbar from "./signed_in_navbar";

console.log(eventNames);

class EventNameCard extends React.Component {
    constructor(props) {
        super(props);

        this.eventName = props.eventName;
    }

    render() {
        const style = { flex : "0 0 11em" };

        let textClasses = "p-1 mr-1 card bg-light";
        let eventNameText = this.eventName;

        return (
            <div>
                <div className={textClasses + " flex-fill"} style={style}>{eventNameText}</div>
            </div>
        )
    }
}

class EventDefinitionCard extends React.Component {
    constructor(props) {
        super(props);

        this.eventDef = props.eventDef;
    }

    render() {
        const style = { };

        let textClasses = "p-1 mr-1 card bg-light";
        let eventDefText = this.eventDef;

        return (
            <div className="m-1">
                <div className={textClasses + " flex-fill"} style={style}>{eventDefText}</div>
            </div>

        )
    }
}


class EventDefinitionsDisplayPage extends React.Component {
    constructor(props) {
        super(props);
        let events = {};

        for (let i = 0; i < eventNames.length; i++) {
            events[eventNames[i]] = GetDescription(eventNames[i]);
        }
    }


    render() {
        let textClasses = "p-1 mr-1 card bg-light";
        let events = [];
        let descriptions = [];

        for (let key in this.props.events) {
            events.push(key);
            descriptions.push(this.props.events[key]);
        }


        return (
            <div>
                <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <div className="m-1">
                    <div className="row" style={{padding:"0 15 0 15"}}>
                    </div>
                </div>
            </div>
        )
    }
}


var eventDefinitionsDisplayPage = ReactDOM.render(
    <EventDefinitionsDisplayPage/>, document.querySelector('#event-definitions-display-page')
)