import React from "react";
import ReactDOM from "react-dom";
import SignedInNavbar from "./signed_in_navbar";


export class EventAnnotations extends React.Component {

    constructor(props) {
        super(props);
    }

    static generateTimestampString(dateTime) {
        console.log(dateTime);
        return dateTime.date.year + "-" + dateTime.date.month + "-" + dateTime.date.day + " "
            + dateTime.time.hour + ":" + dateTime.time.minute + ":" + dateTime.time.second;
    }

    render() {
        return (
            <div>
                <SignedInNavbar activePage="event annotations" waitingUserCount={waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                <div className="container-fluid" style={{backgroundColor: "white"}}>
                    <div className="row">
                        <div className="col-md-12">
                            <table className="table table-striped table-bordered table-hover">
                                <thead>
                                <tr>
                                    <th>Event ID</th>
                                    <th>Event Timestamp</th>
                                    <th>Event Classification</th>
                                    <th>Notes</th>
                                </tr>
                                </thead>
                                <tbody>
                                {annotations.map((eventAnnotation) => {
                                    return (
                                        <tr>
                                            <td>{eventAnnotation.eventId}</td>
                                            <td>{EventAnnotations.generateTimestampString(eventAnnotation.timestamp)}</td>
                                            <td>{eventAnnotation.className}</td>
                                            <td>{eventAnnotation.notes}</td>
                                        </tr>
                                    )
                                })}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>


        );
    }


}

const EventAnnotationsPage = ReactDOM.render(
    <EventAnnotations/>, document.querySelector('#all-event-annotations')
)