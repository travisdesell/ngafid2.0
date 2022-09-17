import React from "react";
import ReactDOM from "react-dom";
import SignedInNavbar from "./signed_in_navbar";


export class EventAnnotations extends React.Component {
    render() {
        return (
            <div>
                <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <table class="table">
                    <thead>
                    <tr>
                        <th scope={col}>Fleet ID</th>
                        <th scope={col}>Event ID</th>
                        <th scope={col}>Fleet ID</th>
                        <th scope={col}>Timestamp</th>
                        <th scope={col}>Classicifation</th>
                        <th scope={col}>Notes</th>

                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <th scope="row">1</th>
                        <td>0</td>
                        <td>0</td>
                        <td>0</td>
                        <td>12-31-99</td>
                        <td>Stall</td>
                        <td>Blah</td>
                    </tr>
                    <tr>
                        <th scope="row">1</th>
                        <td>0</td>
                        <td>0</td>
                        <td>0</td>
                        <td>12-31-99</td>
                        <td>Stall</td>
                        <td>Blah</td>
                    </tr>
                    <tr>
                        <th scope="row">1</th>
                        <td>0</td>
                        <td>0</td>
                        <td>0</td>
                        <td>12-31-99</td>
                        <td>Stall</td>
                        <td>Blah</td>
                    </tr>
                    </tbody>
                </table>
            </div>

        )
    }


}

var EventAnnotationsPage = ReactDOM.render(
    <EventAnnotations/>, document.querySelector('#event-annotations-page')
)