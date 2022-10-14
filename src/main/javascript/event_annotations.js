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

                <table className="table">
                    <thead>
                    <tr>
                        <th>Fleet ID</th>
                        <th>Event ID</th>
                        <th>Timestamp</th>
                        <th>Classification</th>
                        <th>Notes</th>
                    </tr>
                    </thead>
                    <tbody>
                    {annotations.map((eventAnnotation) => {
                        return (
                            <tr>
                                <td>{eventAnnotation.fleetId}</td>
                                <td>{eventAnnotation.eventId}</td>
                                <td>{EventAnnotations.generateTimestampString(eventAnnotation.timestamp)}</td>
                                <td>{eventAnnotation.classification}</td>
                                <td>{eventAnnotation.notes}</td>
                            </tr>
                        )
                    })}
                    </tbody>
                </table>

            </div>

        );
    }


}

const EventAnnotationsPage = ReactDOM.render(
    <EventAnnotations/>, document.querySelector('#all-event-annotations')
)