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

                <table class="table">
                    <thead>
                    <tr>
                        <th >Fleet ID</th>
                        <th >Event ID</th>
                        <th >Fleet ID</th>
                        <th >Timestamp</th>
                        <th >Classicifation</th>
                        <th >Notes</th>

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

const EventAnnotationsPage = ReactDOM.render(
    <EventAnnotations/>, document.querySelector('#all-event-annotations')
)