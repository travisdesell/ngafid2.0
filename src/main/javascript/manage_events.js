import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";


class EventManager extends React.Component {
    render() {
        console.log("Hello, World!");
        let style = {
            padding : 5
        };

        let bgStyle = {
            background : "rgba(248,259,250,0.8)",
            margin:0
        };

        return (
            <div>
                <SignedInNavbar activePage="event manager" waitingUserCount={waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
            </div>
        );
    }
}

let event_manager = ReactDOM.render(
    <EventManager/>,
    document.querySelector('#manage-events-page')
);
