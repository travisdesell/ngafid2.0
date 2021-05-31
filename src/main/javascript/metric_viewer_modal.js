import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

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

    show(title, message, submitMethod) {
        this.state.title = title;
        this.state.message = message;
        this.state.submitMethod = submitMethod;
        this.setState(this.state);

        $("#confirm-modal").modal('show');
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

        return (
            <MetricViewerSettings
                isVertical={true}>
            </MetricViewerSettings>
        );
    }
}

var MetricViewerSettingsModal = ReactDOM.render(
    <MetricViewerSettingsModal />,
    document.querySelector("#confirm-modal-content")
);

export { MetricViewerSettingsModal };
