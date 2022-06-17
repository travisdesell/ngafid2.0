import React from "react";
import {EventDefinitionCard} from "./event_definition";
import GetDescription from "./get_description";

class EventDefinitionsPage extends React.Component {
    constructor(props) {
        super(props);
        let events = {};

        for (let i = 0; i < eventNames.length; i++) {
            events[eventNames[i]] = GetDescription(eventNames[i]);
        }
    }
}


var eventDefinitionsPage = ReactDOM.render(
    <EventDefinitionsPage/>, document.querySelector('#event-definitions-page')
)