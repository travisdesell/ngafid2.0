import 'bootstrap';

import React from "react";
import { createRoot } from 'react-dom/client';

import $ from "jquery";
import SignedInNavbar from "./signed_in_navbar.js";
import { showAjaxErrorModal } from './extract_ajax_error_message.js';

window.jQuery = $;
window.$ = $;

const eventStats = [];
airframeMap[0] = "Generic";

const eventDefinitionsMap = {};
for (let i = 0; i < eventDefinitions.length; i++) {
    const eventDefinition = eventDefinitions[i];
    const airframeId = eventDefinition.airframeNameId;

    if (!(airframeId in eventDefinitionsMap)) {
        eventDefinitionsMap[airframeId] = [];
        console.log(`map did not have airframeId: ${  airframeId}`);
    }
    eventDefinitionsMap[airframeId].push(eventDefinition);
}

console.log(eventDefinitionsMap);

class AirframeCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            expanded: false,
            isLoaded: false,
            isLoading: false,
        };
    }

    toggleEventInfo(eventInfo) {
        console.log(`eventInfo.infoHidden is: ${  eventInfo.infoHidden}`);
        eventInfo.infoHidden = !eventInfo.infoHidden;
        console.log(`eventInfo.infoHidden changed to: ${  eventInfo.infoHidden}`);

        this.setState(this.state);
    }

    expandClicked() {
        this.setState(
            (prevState) => ({
                expanded: !prevState.expanded
            }),
            () => {
                if (this.state.expanded && !this.state.isLoaded && !this.state.isLoading) {
                    this.getStats();
                }
            }
        );
    }

    getStats() {

        console.log("Acquiring event stats");
        this.setState({isLoading: true});

        $.ajax({
            type: 'GET',
            url: `/api/event/count/by-airframe/${this.props.airframeId}`,
            dataType: 'json',
            async: true,
            success: (response) => {
                if (response.events != null) {
                    console.log("Successfully acquired event stats for airframe");
                    this.setState({
                        isLoaded: true,
                        isLoading: false,
                        eventStats: response
                    });
                } else {
                    console.log("Bad juju, must investigate");
                    this.setState({isLoading: false});
                }
            },
            error: (jqXHR, textStatus, errorThrown) => {
                this.setState({isLoading: false});
                showAjaxErrorModal(jqXHR, errorThrown, "Error Getting Event Statistics");
            },
        });
        
    }

    render() {

        let marginTop = 0;
        if (!this.props.first) {
            marginTop = 14;
        }

        const styleButton = {};
        const expandButtonClasses = "p-1 btn btn-outline-secondary float-right";
        let expandIconClasses = "fa ";

        if (this.state.expanded) {
            expandIconClasses += "fa-angle-double-up";
        } else {
            expandIconClasses += "fa-angle-double-down"; ;
        }


        return (
            <div className="m-2" style={{marginTop: marginTop, padding: "0 5 0 5", overflowX: "hidden"}}>
                <div className="card mb-1 m-1" style={{padding: "10 10 10 10"}}>
                    <h5 style={{marginBottom: 0}}>
                        {`${this.props.airframeName  } Event Statistics`}
                        <button className={expandButtonClasses} style={styleButton}
                                onClick={() => this.expandClicked()}><i className={expandIconClasses}></i></button>
                    </h5>
                </div>

                <div className="row" style={{padding: "0 15 0 15"}}>
                    {
                        (this.state.expanded && this.state.isLoading) ? (
                            <div className="col-sm-12" style={{padding: "0 0 0 0"}}>
                                <div className="card mb-1 m-1">
                                    <div className="card-body" style={{display: "flex", alignItems: "center", gap: "10px"}}>
                                        <div className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></div>
                                        <span>Loading event statistics...</span>
                                    </div>
                                </div>
                            </div>
                        ) : ""
                    }
                    {
                        (!this.state.expanded || this.state.eventStats == null) ? "" : this.state.eventStats.events.map((eventInfo, eventIndex) => {
                            const processedPercentage = (100.0 * parseFloat(eventInfo.processedFlights) / parseFloat(eventInfo.totalFlights)).toFixed(2);
                            if (typeof eventInfo.infoHidden == 'undefined') eventInfo.infoHidden = true;

                            return (
                                <div className="col-sm-12" key={eventIndex} style={{padding: "0 0 0 0"}}>
                                    <div className="card mb-1 m-1">
                                        <h5 className="card-header">
                                            <div className="d-flex">
                                                <div style={{flexBasis: "30%", flexShrink: 0, flexGrow: 0}}>
                                                    {eventInfo.eventName}
                                                </div>
                                                <button type="button" className="btn btn-outline-secondary"
                                                        style={{padding: "3 8 3 8", marginRight: "5"}} onClick={() => {
                                                    this.toggleEventInfo(eventInfo);
                                                }}>
                                                    <i className='fa fa-info'></i>
                                                </button>
                                                <div className="progress flex-fill" style={{height: "24px"}}>
                                                    <div className="progress-bar" role="progressbar"
                                                         style={{width: `${processedPercentage  }%`}}
                                                         aria-valuenow={processedPercentage} aria-valuemin="0"
                                                         aria-valuemax="100"> &nbsp; {`${eventInfo.processedFlights  } / ${  eventInfo.totalFlights  } (${  processedPercentage  }%) flights processed`} </div>
                                                </div>
                                            </div>
                                        </h5>

                                        <div className="card-body">
                                            <p hidden={eventInfo.infoHidden}>
                                                {eventInfo.humanReadable}
                                            </p>

                                            <table style={{width: "100%", color: "var(--c_text_alt)"}}>
                                                <thead>
                                                <tr>
                                                    <th></th>
                                                    <th style={{
                                                        textAlign: "center",
                                                        paddingRight: 25,
                                                        borderBottom: "1px solid grey",
                                                        borderRight: "1px solid grey"
                                                    }} colSpan="4">Your Fleet
                                                    </th>
                                                    <th style={{textAlign: "center", borderBottom: "1px solid grey"}}
                                                        colSpan="4">Other Fleets
                                                    </th>
                                                </tr>

                                                <tr>
                                                    <th></th>
                                                    <th style={{textAlign: "right"}}>Flights</th>
                                                    <th style={{textAlign: "right"}}>Total</th>
                                                    <th style={{textAlign: "right"}}>Severity</th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        paddingRight: 25,
                                                        borderRight: "1px solid grey"
                                                    }}>Duration (s)
                                                    </th>
                                                    <th style={{textAlign: "right"}}>Flights</th>
                                                    <th style={{textAlign: "right"}}>Total</th>
                                                    <th style={{textAlign: "right"}}>Severity</th>
                                                    <th style={{textAlign: "right"}}>Duration (s)</th>
                                                </tr>

                                                <tr>
                                                    <th></th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        borderBottom: "1px solid grey"
                                                    }}>With Event
                                                    </th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        borderBottom: "1px solid grey"
                                                    }}>Events
                                                    </th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        borderBottom: "1px solid grey"
                                                    }}>(Min/Avg/Max)
                                                    </th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        paddingRight: 25,
                                                        borderBottom: "1px solid grey",
                                                        borderRight: "1px solid grey"
                                                    }}>(Min/Avg/Max)
                                                    </th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        borderBottom: "1px solid grey"
                                                    }}>With Event
                                                    </th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        borderBottom: "1px solid grey"
                                                    }}>Events
                                                    </th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        borderBottom: "1px solid grey"
                                                    }}>(Min/Avg/Max)
                                                    </th>
                                                    <th style={{
                                                        textAlign: "right",
                                                        borderBottom: "1px solid grey"
                                                    }}>(Min/avg/Max)
                                                    </th>
                                                </tr>
                                                </thead>

                                                <tbody>
                                                {
                                                    eventInfo.monthStats.map((stats, monthIndex) => {
                                                        const eventPercentage = (100.0 * parseFloat(stats.flightsWithEvent) / parseFloat(stats.flightsWithoutError)).toFixed(2);
                                                        const flightsWithEventStr = `${stats.flightsWithEvent  } / ${  stats.flightsWithoutError  } (${  eventPercentage  }%)`;

                                                        const aggEventPercentage = (100.0 * parseFloat(stats.aggFlightsWithEvent) / parseFloat(stats.aggFlightsWithoutError)).toFixed(2);
                                                        const aggFlightsWithEventStr = `${stats.aggFlightsWithEvent  } / ${  stats.aggFlightsWithoutError  } (${  aggEventPercentage  }%)`;

                                                        return (
                                                            <tr key={monthIndex}>
                                                                <td>{stats.rowName}</td>
                                                                <td style={{textAlign: "right"}}>{flightsWithEventStr}</td>
                                                                <td style={{textAlign: "right"}}>{stats.totalEvents}</td>
                                                                <td style={{textAlign: "right"}}>{`${stats.minSeverity.toFixed(2)  } / ${  stats.avgSeverity.toFixed(2)  } / ${  stats.maxSeverity.toFixed(2)}`}</td>
                                                                <td style={{
                                                                    textAlign: "right",
                                                                    paddingRight: 25,
                                                                    borderRight: "1px solid grey"
                                                                }}>{`${stats.minDuration.toFixed(2)  } / ${  stats.avgDuration.toFixed(2)  } / ${  stats.maxDuration.toFixed(2)}`}</td>
                                                                <td style={{textAlign: "right"}}>{aggFlightsWithEventStr}</td>
                                                                <td style={{textAlign: "right"}}>{stats.aggTotalEvents}</td>
                                                                <td style={{textAlign: "right"}}>{`${stats.aggMinSeverity.toFixed(2)  } / ${  stats.aggAvgSeverity.toFixed(2)  } / ${  stats.aggMaxSeverity.toFixed(2)}`}</td>
                                                                <td style={{textAlign: "right"}}>{`${stats.aggMinDuration.toFixed(2)  } / ${  stats.aggAvgDuration.toFixed(2)  } / ${  stats.aggMaxDuration.toFixed(2)}`}</td>
                                                            </tr>
                                                        );
                                                    })
                                                }
                                                </tbody>

                                            </table>

                                        </div>
                                    </div>
                                </div>
                            );
                        })
                    }
                </div>
            </div>

        );
    }
}

class DashboardCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            eventStats: eventStats
        };
    }

    toggleEventInfo(eventInfo) {
        console.log(`eventInfo.infoHidden is: ${  eventInfo.infoHidden}`);
        eventInfo.infoHidden = !eventInfo.infoHidden;
        console.log(`eventInfo.infoHidden changed to: ${  eventInfo.infoHidden}`);

        this.setState(this.state);
    }

    render() {
        console.log(airframeMap);

        const airframeIds = Object.keys(airframeMap);

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>
                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage="event statistics" waitingUserCount={waitingUserCount}
                                    fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                    modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                </div>

                <div style={{overflowY: "scroll", flex: "1 1 auto"}}>
                    {
                        airframeIds.map((airframeId, airframeIndex) => {
                            let first = true;
                            if (airframeIndex > 0) first = false;
                            return (
                                <AirframeCard
                                    key={airframeIndex}
                                    first={first}
                                    airframeId={airframeId}
                                    airframeName={airframeMap[airframeId]}
                                />);
                        })
                    }
                </div>
            </div>
        );
    }

    renderOld() {
        return (
            <div>
                <SignedInNavbar activePage="event statistics" waitingUserCount={waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                {
                    this.state.eventStats.map((airframeStats, airframeIndex) => {
                        let marginTop = 4;
                        if (airframeIndex > 0) {
                            marginTop = 14;
                        }
                        return (
                            <div key={airframeIndex} style={{marginTop: marginTop, padding: "0 5 0 5"}}>
                                <div className="card mb-1 m-1" style={{padding: "10 10 10 10"}}>
                                    <h5 style={{marginBottom: 0}}>
                                        {`${airframeStats.airframeName  } Events`}
                                    </h5>
                                </div>

                                <div className="row" style={{padding: "0 15 0 15"}}>

                                    {
                                        airframeStats.events.map((eventInfo, eventIndex) => {
                                            const processedPercentage = (100.0 * parseFloat(eventInfo.processedFlights) / parseFloat(eventInfo.totalFlights)).toFixed(2);
                                            if (typeof eventInfo.infoHidden == 'undefined') eventInfo.infoHidden = true;

                                            return (
                                                <div className="col-sm-12" key={eventIndex}
                                                     style={{padding: "0 0 0 0"}}>
                                                    <div className="card mb-1 m-1">
                                                        <h5 className="card-header">
                                                            <div className="d-flex">
                                                                <div style={{
                                                                    flexBasis: "30%",
                                                                    flexShrink: 0,
                                                                    flexGrow: 0
                                                                }}>
                                                                    {eventInfo.eventName}
                                                                </div>
                                                                <button type="button"
                                                                        className="btn btn-outline-secondary"
                                                                        style={{padding: "3 8 3 8", marginRight: "5"}}
                                                                        onClick={() => {
                                                                            this.toggleEventInfo(eventInfo);
                                                                        }}>
                                                                    <i className='fa fa-info'></i>
                                                                </button>
                                                                <div className="progress flex-fill"
                                                                     style={{height: "24px"}}>
                                                                    <div className="progress-bar" role="progressbar"
                                                                         style={{width: `${processedPercentage  }%`}}
                                                                         aria-valuenow={processedPercentage}
                                                                         aria-valuemin="0"
                                                                         aria-valuemax="100"> &nbsp; {`${eventInfo.processedFlights  } / ${  eventInfo.totalFlights  } (${  processedPercentage  }%) flights processed`} </div>
                                                                </div>
                                                            </div>
                                                        </h5>

                                                        <div className="card-body">
                                                            <p hidden={eventInfo.infoHidden}>
                                                                {eventInfo.humanReadable}
                                                            </p>

                                                            <table style={{width: "100%"}}>
                                                                <thead>
                                                                <tr>
                                                                    <th></th>
                                                                    <th style={{
                                                                        textAlign: "center",
                                                                        paddingRight: 25,
                                                                        borderBottom: "1px solid grey",
                                                                        borderRight: "1px solid grey"
                                                                    }} colSpan="4">Your Fleet
                                                                    </th>
                                                                    <th style={{
                                                                        textAlign: "center",
                                                                        borderBottom: "1px solid grey"
                                                                    }} colSpan="4">Other Fleets
                                                                    </th>
                                                                </tr>

                                                                <tr>
                                                                    <th></th>
                                                                    <th style={{textAlign: "right"}}>Flights</th>
                                                                    <th style={{textAlign: "right"}}>Total</th>
                                                                    <th style={{textAlign: "right"}}>Severity</th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        paddingRight: 25,
                                                                        borderRight: "1px solid grey"
                                                                    }}>Duration (s)
                                                                    </th>
                                                                    <th style={{textAlign: "right"}}>Flights</th>
                                                                    <th style={{textAlign: "right"}}>Total</th>
                                                                    <th style={{textAlign: "right"}}>Severity</th>
                                                                    <th style={{textAlign: "right"}}>Duration (s)</th>
                                                                </tr>

                                                                <tr>
                                                                    <th></th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        borderBottom: "1px solid grey"
                                                                    }}>With Event
                                                                    </th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        borderBottom: "1px solid grey"
                                                                    }}>Events
                                                                    </th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        borderBottom: "1px solid grey"
                                                                    }}>(Min/Avg/Max)
                                                                    </th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        paddingRight: 25,
                                                                        borderBottom: "1px solid grey",
                                                                        borderRight: "1px solid grey"
                                                                    }}>(Min/Avg/Max)
                                                                    </th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        borderBottom: "1px solid grey"
                                                                    }}>With Event
                                                                    </th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        borderBottom: "1px solid grey"
                                                                    }}>Events
                                                                    </th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        borderBottom: "1px solid grey"
                                                                    }}>(Min/Avg/Max)
                                                                    </th>
                                                                    <th style={{
                                                                        textAlign: "right",
                                                                        borderBottom: "1px solid grey"
                                                                    }}>(Min/avg/Max)
                                                                    </th>
                                                                </tr>
                                                                </thead>

                                                                <tbody>
                                                                {
                                                                    eventInfo.monthStats.map((stats, monthIndex) => {
                                                                        const eventPercentage = (100.0 * parseFloat(stats.flightsWithEvent) / parseFloat(stats.flightsWithoutError)).toFixed(2);
                                                                        const flightsWithEventStr = `${stats.flightsWithEvent  } / ${  stats.flightsWithoutError  } (${  eventPercentage  }%)`;

                                                                        const aggEventPercentage = (100.0 * parseFloat(stats.aggFlightsWithEvent) / parseFloat(stats.aggFlightsWithoutError)).toFixed(2);
                                                                        const aggFlightsWithEventStr = `${stats.aggFlightsWithEvent  } / ${  stats.aggFlightsWithoutError  } (${  aggEventPercentage  }%)`;

                                                                        return (
                                                                            <tr key={monthIndex}>
                                                                                <td>{stats.rowName}</td>
                                                                                <td style={{textAlign: "right"}}>{flightsWithEventStr}</td>
                                                                                <td style={{textAlign: "right"}}>{stats.totalEvents}</td>
                                                                                <td style={{textAlign: "right"}}>{`${stats.minSeverity.toFixed(2)  } / ${  stats.avgSeverity.toFixed(2)  } / ${  stats.maxSeverity.toFixed(2)}`}</td>
                                                                                <td style={{
                                                                                    textAlign: "right",
                                                                                    paddingRight: 25,
                                                                                    borderRight: "1px solid grey"
                                                                                }}>{`${stats.minDuration.toFixed(2)  } / ${  stats.avgDuration.toFixed(2)  } / ${  stats.maxDuration.toFixed(2)}`}</td>
                                                                                <td style={{textAlign: "right"}}>{aggFlightsWithEventStr}</td>
                                                                                <td style={{textAlign: "right"}}>{stats.aggTotalEvents}</td>
                                                                                <td style={{textAlign: "right"}}>{`${stats.aggMinSeverity.toFixed(2)  } / ${  stats.aggAvgSeverity.toFixed(2)  } / ${  stats.aggMaxSeverity.toFixed(2)}`}</td>
                                                                                <td style={{textAlign: "right"}}>{`${stats.aggMinDuration.toFixed(2)  } / ${  stats.aggAvgDuration.toFixed(2)  } / ${  stats.aggMaxDuration.toFixed(2)}`}</td>
                                                                            </tr>
                                                                        );
                                                                    })
                                                                }
                                                                </tbody>

                                                            </table>

                                                        </div>
                                                    </div>
                                                </div>
                                            );
                                        })
                                    }

                                </div>


                            </div>
                        );
                    })
                }

            </div>
        );
    }
}


const container = document.querySelector("#event-statistics-page");
const root = createRoot(container);
root.render(<DashboardCard/>);