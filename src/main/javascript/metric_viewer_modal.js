import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";
import Popover from 'react-bootstrap/Popover';

import {MetricViewerSettings} from "./metricviewer_preferences.js"

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class MetricViewerSettingsModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            title : "",
            message : ""
        };
    }

    show(){
        $("#metric-settings-modal").modal('show');
        this.getUserPreferences();
    }

    getUserPreferences() {
        let thisFlight = this;

        $.ajax({
            type: 'GET',
            url: '/protected/user_preference',
            dataType : 'json',
            success : function(response) {
                console.log("got user pref response");
                console.log(response);

                thisFlight.setState({
                    decimalPrecision : response.decimalPrecision,
                    selectedMetrics : response.flightMetrics
                });
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log("Error getting upset data:");
                console.log(errorThrown);
            },   
            async: false 
        });  
    }

    modalClicked() {
        console.log("modal submit clicked!");
        this.state.submitMethod();
    }

    render() {
        let formGroupStyle = {
            marginBottom: '8px'
        };

        let formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px'
        };

        var style = {
            //top : this.props.placement[1] + this.state.navbarWidth,
            //left : this.props.placement[0],
            //display : this.props.on,
            minWidth: 320
        }

        let labelStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'right'
        };

        let validationMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        console.log("rendering metric settings modal with confirm title: '" + this.state.title + "' and message: " + this.state.message);

        let settingsDialog = (
            <MetricViewerSettings
                isVertical={false}
                selectedMetrics={this.state.selectedMetrics}
                decimalPrecision={this.state.decimalPrecision}
                >
            </MetricViewerSettings>
        );

        if (this.state.selectedMetrics == null || this.state.decimalPrecision == null) {
            settingsDialog = "";
        }

        return (
            <div className='modal-content'>
                <div className='modal-header'>
                    <h5 id='confirm-modal-title' className='modal-title'>Confirm Operation</h5>
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='confirm-modal-body' className='modal-body'>
                    <h4>Settings</h4>

                    {settingsDialog}
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-primary' data-dismiss='modal' >Confirm</button>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                </div>
            </div>
        );
    }
}

var metricViewerSettingsModal = ReactDOM.render(
    <MetricViewerSettingsModal />,
    document.querySelector("#metric-settings-modal-content")
);

export { metricViewerSettingsModal };
