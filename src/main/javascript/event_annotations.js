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

    createRow(annotation) {
        return <tr key={index}>
            <td>{annotation.fleet_id}</td>
            <td>{annotation.eventId}</td>
            <td>{this.generateTimestampString(annotation.timestamp)}</td>
            <td>{annotation.name}</td>
            <td>{annotation.notes}</td>
        </tr>

    }

    render() {
        let rows = []


        $.ajax({
            type: 'GET',
            url: '/protected/event_group_annotations',
            dataType: 'json',
            success: function (response) {
                for (let key in response) {
                    rows.push(response[key]);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
            },
            async: false
        });


        console.log(rows);

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

                    {rows}

                    </tbody>
                </table>
            </div>

        )
    }


}

const EventAnnotationsPage = ReactDOM.render(
    <EventAnnotations/>, document.querySelector('#all-event-annotations')
)