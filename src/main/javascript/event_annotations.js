import React from "react";
import ReactDOM from "react-dom";
import SignedInNavbar from "./signed_in_navbar";


export class EventAnnotations extends React.Component {

    constructor(props) {
        super(props);
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
                        <th>Classicifation</th>
                        <th>Notes</th>

                    </tr>
                    </thead>
                    <tbody>
                    {annotations.map((val, index) => {
                        return <tr key={index}>
                            <td>{val.fleet_id}</td>
                            <td>{val.eventId}</td>
                            <td>{val.timestamp.toString()}</td>
                            <td>{val.classId}</td>
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