import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";
import Popover from 'react-bootstrap/Popover';

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class MapPopup extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
			navbarWidth : 40,
			on : 'none',
			info : "",
			placement : []
        };
    }

    show(info, pixel) {
		let title = "";

		if(info[0] === "PLOCI"){
			title = "Loss of Control Probability Details";
		} else {
			title = "Stall Probability Details";
		}

		this.state = {
			pixel : pixel,
			on : '',
			info : info,
			placement : pixel,
			title : title
		}

		this.setState(this.state);
    }

    render() {

		console.log("rendering a map popup");

		var style = {
			top : this.state.placement[1] + this.state.navbarWidth,
			left : this.state.placement[0],
			display : this.state.on
		}

		if(this.state.on !== "none"){
			return (
				<div style={{ height: 120 }}>
					<Popover
						id="popover-basic"
						style={style}
					>
						<Popover.Title as="h3"> {this.state.title} </Popover.Title>
						<Popover.Content> 
							Probability: {this.state.info[1].toFixed(2)}%
						</Popover.Content>
				  </Popover>
				</div>
			);
		} else {
			return null;
		}
    }
}

var mapPopup = ReactDOM.render(
    <MapPopup />,
    document.querySelector("#map_popup_content")
);

export { mapPopup };
