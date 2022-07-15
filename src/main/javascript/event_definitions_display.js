import React from "react";
import {EventDefinitionCard} from "./event_definition";
import GetDescription from "./get_description";
import ReactDOM from "react-dom";
import {Paginator} from "./paginator_component";
import SignedInNavbar from "./signed_in_navbar";

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
            <div className="m-1">
                <div className="d-flex flex-row">
                    {/*<div className={textClasses} {events}></div>*/}
                    {/*<div className={textClasses} {descriptions}></div>*/}
                </div>
            </div>
        )
    }
}


var eventDefinitionsDisplayPage = ReactDOM.render(
    <EventDefinitionsDisplayPage/>, document.querySelector('#event-definitions-page')
)