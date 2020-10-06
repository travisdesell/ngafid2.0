import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class MapPopup extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
			on : false,
			info : ""
        };
    }

    show(info) {
		this.state.on = true;
		this.state.info = info;
		this.setState(this.state);
    }

    render() {

		let ol_popup = {
			position: 'absolute',
			backgroundColor: 'white',
			boxShadow: '0 1px 4px rgba(0,0,0,0.2)',
			padding: '15px',
			borderRadius: '10px',
			border: '1px solid #cccccc',
			bottom: '12px',
			left: '-50px',
			minWidth: '280px'
		}

		let ol_popup_closer = {
			textDecoration: 'none',
			position: 'absolute',
			top: '2px',
			right: '8px'
		}

		console.log("rendering a map popup");


        return (
			<p> {this.state.info} </p>
        );
    }
}

var mapPopup = ReactDOM.render(
    <MapPopup />,
    document.querySelector("#map_popup_content")
);

export { mapPopup };
