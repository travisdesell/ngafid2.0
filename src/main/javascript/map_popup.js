import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";
import Popover from 'react-bootstrap/Popover';
import Table from 'react-bootstrap/Table';

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

		this.setState({
			pixel : pixel,
			on : '',
			info : info,
			placement : pixel,
			title : title
		});
    }

    render() {
		console.log("rendering a map popup with info:");
		console.log(this.state.info);

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
						<Popover.Title as="h3"> Flight Metric Details </Popover.Title>
						<Popover.Content> 
							<Table striped bordered hover size="sm">
								<thead>
									<tr>
										<th>Metric Name</th>
										<th>Data</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<td>Time Index (s):</td>
										<td>{this.state.info[0]}</td>
									</tr>
									<tr>
										<td>Stall Probability:</td>
										<td>{this.state.info[1].toFixed(2)}</td>
									</tr>
									<tr>
										<td>LOC-I Probability:</td>
										<td>{this.state.info[2].toFixed(2)}</td>
									</tr>
									<tr>
										<td>Roll</td>
										<td>{this.state.info[3].toFixed(3)}</td>
									</tr>
									<tr>
										<td>Pitch</td>
										<td>{this.state.info[4].toFixed(3)}</td>
									</tr>
									<tr>
										<td>IAS</td>
										<td>{this.state.info[5].toFixed(3)} kts</td>
									</tr>
								</tbody>
							</Table>
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
