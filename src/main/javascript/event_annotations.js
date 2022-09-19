import React from "react";
import ReactDOM from "react-dom";
import SignedInNavbar from "./signed_in_navbar";


export class EventAnnotations extends React.Component {

    constructor(props) {
        super(props);
    }

    generateTimestampString(dateTime) {
        return dateTime.date.year + "-" + dateTime.date.month + "-" + dateTime.date.day + " "
            + dateTime.time.hour + ":" + dateTime.time.minute + ":" + dateTime.time.second;
    }

    render() {
        return (
            <div>
                {/*<SignedInNavbar activePage="event annotations" waitingUserCount={waitingUserCount}*/}
                {/*                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}*/}
                {/*                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>*/}

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
                    {annotations.map((val, index) => {
                        return <tr key={index}>
                            <td>{val.fleet_id}</td>
                            <td>{val.eventId}</td>
                            <td>{this.generateTimestampString(val.timestamp)}</td>
                            <td>{val.name}</td>
                            <td>{val.notes}</td>

                        </tr>
                    })}
                    </tbody>
                </table>
            </div>

        )
    }


}

const EventAnnotationsPage = ReactDOM.render(
    <EventAnnotations/>, document.querySelector('#all-event-annotations')
)