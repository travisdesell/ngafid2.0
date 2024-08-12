import React from "react";
import ReactDOM from "react-dom";
import Table from "react-bootstrap/Table";
import Col from "react-bootstrap/Col";
import SignedInNavbar from "./signed_in_navbar";
import GetAllDescriptions from "./get_all_descriptions";

class EventDefinitionsDisplayPage extends React.Component {
    constructor(props) {
        super(props);
        this.events = new Map(Object.entries(GetAllDescriptions()));
    }

    render() {
        let rows = [];

        for (let eventName of this.events.keys()) {
            for (let airframe of Object.keys(this.events.get(eventName))) {
                rows.push([eventName, airframe, this.events.get(eventName)[airframe]]);
            }
        }

        return (
            <div style={{overflowX:"hidden", display:"flex", flexDirection:"column", height:"100vh"}}>

                <div style={{flex:"0 0 auto"}}>
                    <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount}
                                    fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                    modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}
                    />
                </div>

                <div className="card-body" style={{margin:10, padding:10, borderRadius:5}}>
                    <div className="row">
                        <div className="col-md-12">
                            <Col>
                                <Table striped bordered hover size="sm">
                                    <table className="table table-striped table-hover" style={{backgroundColor:"var(--c_card_bg)", borderRadius:"8px", marginBottom: "0", borderCollapse:"collapse" }}>
                                        <thead style={{color:"var(--c_text)", backgroundColor:"var(--c_bg)"}}>
                                            <tr>
                                                <th>Event Name</th>
                                                <th style={{minWidth:"10%"}}>Aircraft Type</th>
                                                <th>Event Definition</th>
                                            </tr>
                                        </thead>
                                        <tbody style={{color:"var(--c_text_alt)"}}>
                                            {rows.map((row, index) => {
                                                return (
                                                    <tr key={index} style={{backgroundColor:(index%2 ? "var(--c_row_bg_solid)" : "var(--c_row_bg_alt_solid")}}>
                                                        <th>{row[0]}</th>
                                                        <th>{row[1]}</th>
                                                        <th style={{fontStyle:"normal", fontWeight:"normal", color:"var(--c_text_alt)"}}>{row[2]}</th>
                                                    </tr>
                                                )
                                            })}
                                        </tbody>
                                    </table>
                                </Table>
                            </Col>
                        </div>
                    </div>
                </div>

            </div>
        )
    }
}


var eventDefinitionsDisplayPage = ReactDOM.render(
    <EventDefinitionsDisplayPage/>, document.querySelector('#event-definitions-display-page')
)