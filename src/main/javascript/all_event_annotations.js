import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

class AllEventAnnotations extends React.Component {
    constructor(props) {
        super(props);
        console.log("EventAnnotations");
        console.log(props);

        this.state = {
        };

        console.log("AllEventAnnotations:");
        console.log(this.state);
    }


    render() {
        return (
            <div></div>
        );
    }
}

console.log("setting preferences page with react!");

var allEventAnnotations = ReactDOM.render(
    <AllEventAnnotations annotations={annotations}/>,
   document.querySelector('#all-event-annotations')
)
