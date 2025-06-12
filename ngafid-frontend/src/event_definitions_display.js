import React from "react";
import { createRoot } from 'react-dom/client';
import Table from "react-bootstrap/Table";
import Col from "react-bootstrap/Col";
import SignedInNavbar from "./signed_in_navbar";
import GetAllDescriptions from "./get_all_descriptions";

class EventDefinitionsDisplayPage extends React.Component {

    constructor(props) {

        super(props);

        this.events = null;

        const allDescriptions = GetAllDescriptions();
        console.log("All Descriptions: ", allDescriptions);
        if (allDescriptions)
            this.events = new Map(Object.entries(allDescriptions));

        console.log("Event Definitions: ", this.events);

    }

    render() {

        const ROW_INDEX_EVENT_NAME = 0;
        const ROW_INDEX_AIRFRAME = 1;
        const ROW_INDEX_EVENT_DEFINITION = 2;

        const rowsGeneric = [];
        const rowsSpecific = [];

        console.log(this.events);

        // Add all events to the rows array
        for (const eventName of this.events?.keys()??[]) {

            for (const airframe of Object.keys(this.events.get(eventName))) {
                console.log(airframe);
                console.log(typeof airframe);
                console.log(airframe == null);
                //Got a generic event, add it to the generic rows
                if (airframe == "Any")
                    rowsGeneric.push([eventName, airframe, this.events.get(eventName)[airframe]]);
                //Otherwise, add it to the specific rows
                else
                    rowsSpecific.push([eventName, airframe, this.events.get(eventName)[airframe]]);

            }

        }

        const PREVIOUS_EVENT_NAME_DEFAULT = "";
        let previousEventName = PREVIOUS_EVENT_NAME_DEFAULT;
        const PADDING_AMT_UPDATED = "32px";
        const PADDING_AMT_NONE = "0px";
        let eventPadding = PADDING_AMT_NONE;
        const eventNameUpdated = (eventName) => {

            //Got new event name, apply padding
            if (eventName !== previousEventName) {
                eventPadding = PADDING_AMT_UPDATED;
                previousEventName = eventName;
                return;
            }

            //Otherwise, clear padding
            eventPadding = PADDING_AMT_NONE;

        };

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                {/* Navbar */}
                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount}
                                    fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                    modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}
                    />
                </div>

                {/* Main Content */}
                <div style={{overflowY: "auto", flex: "1 1 auto"}}>
                    <div className="card-body" style={{margin: 24, padding: 12, borderRadius: 5}}>
                        <div className="row">
                            <div className="col-md-12">
                                <Col>

                                    {/* Generic Entries */}
                                    <h1 className="ml-1 mb-3 mt-1">
                                        Generic Event Definitions
                                    </h1>
                                    <Table striped bordered hover size="sm" className="rounded-0">
                                        <thead style={{color: "var(--c_text)", backgroundColor: "var(--c_bg)"}}>
                                        <tr>
                                            <th>Event Name</th>
                                            {/* <th style={{minWidth: "10%"}}>Aircraft Type</th> */}
                                            <th>Event Definition</th>
                                        </tr>
                                        </thead>
                                        <tbody style={{color: "var(--c_text_alt)"}}>
                                        {rowsGeneric.map((row, index) => (
                                            <tr
                                                key={index}
                                                style={{
                                                    backgroundColor:
                                                        index % 2 ? "var(--c_row_bg_solid)" : "var(--c_row_bg_alt_solid)",
                                                }}
                                            >

                                                {/* Event Definition Name */}
                                                <th>
                                                    {row[ROW_INDEX_EVENT_NAME]}
                                                </th>

                                                {/*
                                                    Event Definition Aircraft Type
                                                    (Not displayed for generic events)

                                                    <th>
                                                        {row[ROW_INDEX_AIRFRAME]}
                                                    </th>
                                                    */}

                                                {/* Event Definition Text */}
                                                <th
                                                    style={{
                                                        fontStyle: "normal",
                                                        fontWeight: "normal",
                                                        color: "var(--c_text_alt)",
                                                    }}
                                                >
                                                    {row[ROW_INDEX_EVENT_DEFINITION]}
                                                </th>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </Table>

                                    <div style={{marginTop: "2rem", marginBottom: "1rem"}}>
                                        <hr style={{backgroundColor: "var(--c_text)"}}/>
                                    </div>

                                    {/* Specific Entries */}
                                    <h1 className="ml-1 mb-3 mt-1">
                                        Per-Airframe Event Definitions
                                    </h1>
                                    <Table striped bordered hover size="sm">
                                        <thead style={{color: "var(--c_text)", backgroundColor: "var(--c_bg)"}}>
                                        <tr>
                                            <th>Event Name</th>
                                            <th style={{minWidth: "10%"}}>Aircraft Type</th>
                                            <th>Event Definition</th>
                                        </tr>
                                        </thead>
                                        <tbody style={{color: "var(--c_text_alt)"}}>
                                        {rowsSpecific.map((row, index) => (
                                            <tr
                                                key={index}
                                                style={{
                                                    backgroundColor: index % 2 ? "var(--c_row_bg_solid)" : "var(--c_row_bg_alt_solid)",

                                                }}
                                            >
                                                {eventNameUpdated(row[ROW_INDEX_EVENT_NAME])}

                                                {/* Event Definition Name */}
                                                <th style={{
                                                    paddingTop: eventPadding,
                                                }}>
                                                    {row[ROW_INDEX_EVENT_NAME]}
                                                </th>

                                                {/* Event Definition Aircraft Type */}
                                                <th style={{
                                                    paddingTop: eventPadding,
                                                }}>
                                                    {row[ROW_INDEX_AIRFRAME]}
                                                </th>

                                                {/* Event Definition Text */}
                                                <th
                                                    style={{
                                                        fontStyle: "normal",
                                                        fontWeight: "normal",
                                                        color: "var(--c_text_alt)",
                                                        paddingTop: eventPadding,
                                                    }}
                                                >
                                                    {row[ROW_INDEX_EVENT_DEFINITION]}
                                                </th>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </Table>
                                </Col>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        );
    }
}


const container = document.querySelector("#event-definitions-display-page");
const root = createRoot(container);
root.render(<EventDefinitionsDisplayPage />);