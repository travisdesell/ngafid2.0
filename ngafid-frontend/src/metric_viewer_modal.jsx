import 'bootstrap';

import React from "react";
import { createRoot } from 'react-dom/client';

import {MetricViewerSettings} from "./metricviewer_preferences";

import $ from 'jquery';

window.jQuery = $;
window.$ = $;


class MetricViewerSettingsModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            title: "",
            message: "",
            show: false
        };
    }

    show(parent) {
        $("#metric-settings-modal").modal('show');
        this.getUserPreferences();

        this.setState({
            show: true,
            parent: parent
        });
    }

    getUserPreferences() {

        $.ajax({
            type: 'GET',
            url: '/api/user/me/metric-prefs',
            async: false,
            success: (response) => {
                console.log("Got user pref response", response);

                this.setState({
                    decimalPrecision: response.decimalPrecision,
                    selectedMetrics: response.flightMetrics
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("Error getting upset data:", errorThrown);
            },
        });
    }

    modalClicked() {
        console.log("Modal submit clicked!");
        this.state.submitMethod();
    }

    render() {

        console.log(
            "Rendering metric settings modal with confirm title: '", this.state.title,
            "' and message: ", this.state.message
        );

        let settingsDialog = (
            <MetricViewerSettings
                isVertical={false}
                selectedMetrics={this.state.selectedMetrics}
                decimalPrecision={this.state.decimalPrecision}
            >
            </MetricViewerSettings>
        );

        if (this.state.selectedMetrics == null || this.state.decimalPrecision == null)
            settingsDialog = "";

        return (
            <div className='modal-content'>

                <div className='modal-header'>
                    <h5 id='confirm-modal-title' className='modal-title'>Change Your Metric Viewer Settings:</h5>
                    <button type='button' className='close' data-bs-dismiss='modal'
                            onClick={() => this.state.parent.show()} aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='confirm-modal-body' className='modal-body' style={{flex: 'fill'}}>
                    {settingsDialog}
                </div>
            </div>
        );
    }
}

const container = document.querySelector("#metric-settings-modal-content");
const metricViewerSettingsModal = createRoot(container);
metricViewerSettingsModal.render(<MetricViewerSettingsModal/>);

export {metricViewerSettingsModal};
