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
			let info = new Array();

			info[0] = this.props.info[0];
			for (let i = 1; i < this.props.info.length; i++) {
				if (this.props.info[i] == null) {
					info[i] = "Not Available";
				} else if (i < 3) {
					//show 2 S.Fs for probabilities and display as a decimal value
					info[i] = this.props.info[i].toFixed(2);
				} else {
					//only show 1 S.F. for all other params
					info[i] = this.props.info[i].toFixed(1);
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
										<td>{info[0]}</td>
									</tr>
									<tr>
										<td>Stall Probability:</td>
										<td>{info[1]}</td>
									</tr>
									<tr>
										<td>LOC-I Probability:</td>
										<td>{info[2]}</td>
									</tr>
									<tr>
										<td>Roll (degrees)</td>
										<td>{info[3]}</td>
									</tr>
									<tr>
										<td>Pitch (degrees)</td>
										<td>{info[4]}</td>
									</tr>
									<tr>
										<td>IAS (knots)</td>
										<td>{info[5]}</td>
									</tr>
									<tr>
										<td>Altitude (MSL) [ft]</td>
										<td>{info[6]}</td>
									</tr>
									<tr>
										<td>Altitude (AGL) [ft]</td>
										<td>{info[7]}</td>
									</tr>
									<tr>
										<td>Angle of Attack (simple) [degrees]</td>
										<td>{info[8]}</td>
									</tr>
									<tr>
										<td>Engine 1 RPM</td>
										<td>{info[9]}</td>
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
