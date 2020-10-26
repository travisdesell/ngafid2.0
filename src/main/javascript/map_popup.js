import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";
import Popover from 'react-bootstrap/Popover';
import Table from 'react-bootstrap/Table';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import Button from 'react-bootstrap/Button';

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class MapPopup extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
			navbarWidth : 40,
			status : '',
			info : "",
			placement : []
        };
    }

    show() {
		this.setState({status : ""});
    }

	close() {
		console.log("closing the popup!");
		this.setState({status : 'none'});
	}

	isPinned() {
		return this.state.status === 'pinned';
	}

	pin() {
		if (this.state.status == 'pinned') {
			console.log("unpinning the popup!")
			this.setState({status : 'none'});
		} else {
			console.log("pinning the popup!");
			this.setState({status : 'pinned'});
		}
	}

    render() {
		console.log("rendering a map popup with info:");
		console.log(this.props.info);

		var style = {
			top : this.props.placement[1] + this.state.navbarWidth,
			left : this.props.placement[0],
			display : this.props.on
		}

		if(this.state.status !== "none"){
			let info = this.props.info;

			for (let i = 0; i < this.state.info.length; i++) {
				if (info[i] == null) {
					info[i] = "Not Available";
				}
			}

			return (
				<div style={{ height: 120 }}>
					<Popover
						id="popover-basic"
						style={style}
					>
						<Popover.Title as="h3"> 
							<Container>
								<Row>
									<Col sm={7}>Filght Metrics</Col>
									<Col sm={2}>
										<Button onClick={() => this.pin()} data-toggle="button" variant="outline-secondary" size="sm">
											<i className="fa fa-thumb-tack p-1"></i>
										</Button>
									</Col>
									<Col sm={2}>
										<Button onClick={() => this.close()} variant="outline-secondary" size="sm">
											<i className="fa fa-times p-1"></i>
										</Button>
									</Col>
								</Row>
							</Container>
						</Popover.Title>
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
										<td>{this.props.info[0]}</td>
									</tr>
									<tr>
										<td>Stall Probability:</td>
										<td>{(info[1] / 100).toFixed(2)}</td>
									</tr>
									<tr>
										<td>LOC-I Probability:</td>
										<td>{(info[2] / 100).toFixed(2)}</td>
									</tr>
									<tr>
										<td>Roll</td>
										<td>{info[3].toFixed(1)}</td>
									</tr>
									<tr>
										<td>Pitch</td>
										<td>{info[4].toFixed(1)}</td>
									</tr>
									<tr>
										<td>IAS</td>
										<td>{info[5].toFixed(1)} kts</td>
									</tr>
									<tr>
										<td>Altitude (MSL)</td>
										<td>{info[6].toFixed(1)} ft</td>
									</tr>
									<tr>
										<td>Altitude (AGL)</td>
										<td>{info[7].toFixed(1)} ft</td>
									</tr>
									<tr>
										<td>Angle of Attack (simple)</td>
										<td>{info[8].toFixed(1)}</td>
									</tr>
									<tr>
										<td>Engine 1 RPM</td>
										<td>{info[9].toFixed(1)}</td>
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

export { MapPopup };
