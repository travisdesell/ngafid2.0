import 'bootstrap';
import React from "react";


class AirSyncSettings extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            selectedMetrics: props.selectedMetrics,
            decimalPrecision: props.decimalPrecision,
            timeout: props.timeout
        }

        console.log(this.state);
    }

    updateTimeout(timeout) {
        var submissionData = {
            timeout: timeout
        }

        console.log(submissionData);

        var theseSettings = this;

        $.ajax({
            type: 'PATCH',
            url: '/api/airsync/timeout',
            data: submissionData,
            dataType: 'json',
            success: function (response) {
                console.log("got airsync_settings response:");
                console.log(response);

                theseSettings.state = {
                    timeout: response.timeout
                };

                theseSettings.setState(theseSettings.state);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.log("error updating the timeout");
            },

            async: false
        });
    }

    timeoutChanged() {
        let timeout = event.target.value;
        console.log("New timeout is: " + timeout);

        this.updateTimeout(timeout);
    }

    render() {
        let labelStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'left'
        };

        let formGroupStyle = {
            marginBottom: '0px',
            padding: '0 4 0 4'
        };

        let formHeaderStyle = {
            flex: '0 0 180px'
        };

        $(function () {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        return (
            <div className="card-body">
                <div className="col" style={{padding: "0 0 0 0"}}>
                    <div className="card" style={{background: "rgba(248,259,250,0.8)"}}>
                        <h6 className="card-header">
                            <span className="badge badge-info mr-1">FLEET-WIDE</span>
                            Your AirSync Settings:
                        </h6>
                        <div className="form-group" style={formGroupStyle}>
                            <div className="d-flex">
                                <div className="p-2 d-flex" style={formHeaderStyle}>
                                    <label htmlFor="selectedMetricsNames" style={labelStyle}>
                                        Sync Frequency
                                        <i className="ml-1 fa fa-question-circle" data-bs-toggle="tooltip"
                                           data-bs-placement="top"
                                           title="This is the amount of time the NGAFID waits to check for new AirSync flights. For example, if this is set to 12 hours, the NGAFID will check with the AirSync servers once every 12 hours."></i>
                                    </label>
                                </div>
                                <div className="p-2 d-flex">
                                    <select id="columnNames" className="form-control"
                                            onChange={() => this.timeoutChanged()} value={this.state.timeout}>
                                        <option key={72}>72 Hours</option>
                                        <option key={48}>48 Hours</option>
                                        <option key={24}>24 Hours</option>
                                        <option key={12}>12 Hours</option>
                                        <option key={6}>6 Hours</option>
                                        <option key={3}>3 Hours</option>
                                        <option key={1}>1 Hour</option>
                                        <option key={0}>30 Minutes</option>
                                    </select>
                                </div>
                                <hr style={{padding: "0", margin: "0 0 0 0"}}></hr>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}


export {AirSyncSettings};
