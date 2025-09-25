import 'bootstrap';
import React from "react";
import {createRoot} from "react-dom/client";

import {showErrorModal} from "./error_modal";
import SignedInNavbar from "./signed_in_navbar";

import {Paginator} from "./paginator_component.tsx";

class FlightWarning extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const warning = this.props.warning;

        const styleName = {flex: "0 0 25em", textShadow: "1px 1px 1px rgba(0,0,0,0.10)"};
        let filenameClasses = "p-1 mr-1 card border-warning";
        let filenameText = warning.filename;
        if (warning.sameFilename) {
            filenameClasses = "p-1 mr-1";
            filenameText = "";
        }

        return (
            <div className="d-flex flex-row p-0 mt-1 border-warning">
                <div className={filenameClasses} style={styleName}>
                    {filenameText}
                </div>
                <div className="p-1 card border-warning flex-fill" style={styleName}>
                    {warning.message}
                </div>
            </div>
        );
    }
}

class FlightWarnings extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const flightWarnings = this.props.flightWarnings;

        return (
            <div className="m-0">
                {
                    flightWarnings.map((warning) => {
                        return (
                            <FlightWarning warning={warning} key={warning.id}/>
                        );
                    })
                }
            </div>
        );
    }
}

class FlightError extends React.Component {
    constructor(props) {
        super(props);
    }


    render() {
        const error = this.props.error;

        const styleName = {flex: "0 0 25em", textShadow: "1px 1px 1px rgba(0,0,0,0.10)"};
        let filenameClasses = "p-1 mr-1 card border-danger text-danger";
        let filenameText = error.filename;
        if (error.sameFilename) {
            filenameClasses = "p-1 mr-1";
            filenameText = "";
        }

        return (
            <div className="d-flex flex-row p-0 mt-1">
                <div className={filenameClasses} style={styleName}>
                    {filenameText}
                </div>
                <div className="p-1 card border-danger text-danger flex-fill" style={styleName}>
                    {error.message}
                </div>
            </div>
        );
    }
}

class FlightErrors extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const flightErrors = this.props.flightErrors;

        return (
            <div className="m-0">
                {
                    flightErrors.map((error) => {
                        return (
                            <FlightError error={error} key={error.id}/>
                        );
                    })
                }
            </div>
        );
    }
}

class UploadError extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="d-flex flex-row card p-1 border-danger text-danger">
                {this.props.error.message}
            </div>
        );
    }
}

class UploadErrors extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const uploadErrors = this.props.uploadErrors;

        return (
            <div className="m-0 mt-1">
                {
                    uploadErrors.map((error) => {
                        return (
                            <UploadError error={error} key={error.id}/>
                        );
                    })
                }
            </div>
        );
    }
}

class Import extends React.Component {
    constructor(props) {
        super(props);

        let uploadErrors = props.uploadErrors;
        if (uploadErrors == undefined) uploadErrors = [];

        let flightWarnings = props.flightWarnings;
        if (flightWarnings == undefined) flightWarnings = [];

        let flightErrors = props.flightErrors;
        if (flightErrors == undefined) flightErrors = [];


        this.state = {
            expanded: false,
            loaded: false,
            uploadErrors: uploadErrors,
            flightWarnings: flightWarnings,
            flightErrors: flightErrors
        };
    }

    componentDidMount() {
        //console.log("import did mount for filename: '" + this.props.importInfo.filename + "'");
    }

    expandClicked() {

        if (this.state.loaded) {

            console.log("Not fetching import information from the server, already loaded.");
            this.setState((prevState) => ({
                expanded: !prevState.expanded
            }));

        } else {

            console.log("Fetching import information from the server.");

            $.ajax({
                type: 'GET',
                url: `/api/upload/${this.props.importInfo.id}/errors`,
                async: true,
                success: (response) => {

                    console.log("Received response: ", response);

                    if (response.errorTitle !== undefined) {
                        showErrorModal(response.errorTitle, response.errorMessage);
                    } else {

                        this.setState((prevState) => ({
                            loaded: true,
                            expanded: !prevState.expanded,
                            uploadErrors: response.uploadErrors,
                            flightWarnings: response.flightWarnings,
                            flightErrors: response.flightErrors
                        }));
                        console.log(`Expand clicked, now:${  !this.state.expanded}`);
                    }

                },
                error: (jqXHR, textStatus, errorThrown) => {
                    showErrorModal("Error Loading Uploads", errorThrown);
                },
            });
        }

    }

    render() {
        const expanded = this.state.expanded;
        const uploadErrors = this.state.uploadErrors;
        const flightWarnings = this.state.flightWarnings;
        const flightErrors = this.state.flightErrors;

        for (let i = 1; i < flightWarnings.length; i++) {

            if (flightWarnings[i - 1].filename == flightWarnings[i].filename)
                flightWarnings[i].sameFilename = true;
            else
                flightWarnings[i].sameFilename = false;
            
        }

        for (let i = 1; i < flightErrors.length; i++) {

            if (flightErrors[i - 1].filename == flightErrors[i].filename)
                flightErrors[i].sameFilename = true;
            else
                flightErrors[i].sameFilename = false;
            
        }

        const importInfo = this.props.importInfo;

        /*
        console.log("rendering import for filename: '" + importInfo.filename + "'");
        console.log(this.props);
        console.log(this.state);
        */

        let progressSize = importInfo.progressSize;
        let totalSize = importInfo.totalSize;

        if (progressSize == undefined)
            progressSize = importInfo.bytesUploaded;

        if (totalSize == undefined)
            totalSize = importInfo.sizeBytes;

        const styleName = {};
        const styleTime = {flex: "0 0 11em"};
        const styleCount = {
            marginRight: "5px",
            borderRadius: "8px",
            color: "white",
            textAlign: "center",
            alignContent: "center"
        };
        const styleStatus = {flex: "0 0 10em"};
        const styleButton = {};

        const expandButtonClasses = "p-1 btn btn-outline-secondary float-right";
        
        let expandIconClasses = "fa ";
        let expandDivClasses = "";
        if (expanded) {
            expandIconClasses += "fa-angle-double-up";
            expandDivClasses = "m-0 mt-1 mb-4";
        } else {
            expandIconClasses += "fa-angle-double-down";
            expandDivClasses = "m-0";
        }

        const status = importInfo.status;

        /*

            New Statusses:

            UPLOADING,
            UPLOADING_FAILED,
            UPLOADED,
            ENQUEUED,
            PROCESSING,
            PROCESSED_OK,
            PROCESSED_WARNING,
            FAILED_FILE_TYPE,
            FAILED_AIRCRAFT_TYPE,
            FAILED_ARCHIVE_TYPE,
            FAILED_UNKNOWN,
            DERIVED;                    (Note: Should not be displayed)

        */

        let statusText, colorClasses, statusClasses;
        const statusStates = {
            "UPLOADING": {
                "statusText": "Uploading",
                "colorClasses": "bg-info",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-info text-info"
            },
            "UPLOADING_FAILED": {
                "statusText": "Upload Failed",
                "colorClasses": "bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "UPLOADED": {
                "statusText": "Uploaded",
                "colorClasses": "bg-primary",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-primary text-primary"
            },
            "ENQUEUED": {
                "statusText": "Enqueued",
                "colorClasses": "bg-secondary",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-secondary text-secondary"
            },
            "PROCESSING": {
                "statusText": "Processing",
                "colorClasses": "bg-warning",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-warning text-warning"
            },
            "PROCESSED_OK": {
                "statusText": "Processed OK",
                "colorClasses": "bg-success",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-success text-success"
            },
            "PROCESSED_WARNING": {
                "statusText": "Processed With Warnings",
                "colorClasses": "bg-warning",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-warning text-warning"
            },
            "FAILED_FILE_TYPE": {
                "statusText": "Failed: File Type",
                "colorClasses": "bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "FAILED_AIRCRAFT_TYPE": {
                "statusText": "Failed: Aircraft Type",
                "colorClasses": "bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "FAILED_ARCHIVE_TYPE": {
                "statusText": "Failed: Archive Type",
                "colorClasses": "bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "FAILED_UNKNOWN": {
                "statusText": "Failed: Unknown",
                "colorClasses": "bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "DERIVED": {
                "statusText": "Derived",
                "colorClasses": "bg-info",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-info text-info"
            }
        };
        const statusStateUnknownDefaults = {
            "statusText": "Unknown",
            "colorClasses": "bg-secondary",
            "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-secondary text-secondary"
        };

        console.log("[EX] Status: ", status);

        //Status is listed, apply the status text and classes
        if (status in statusStates) {
            statusText = statusStates[status].statusText;
            colorClasses = statusStates[status].colorClasses;
            statusClasses = statusStates[status].statusClasses;

            //Status is not listed, apply the unknown defaults
        } else {
            statusText = statusStateUnknownDefaults.statusText;
            colorClasses = statusStateUnknownDefaults.colorClasses;
            statusClasses = statusStateUnknownDefaults.statusClasses;
        }


        const textClasses = "p-1 mr-1 card";
        const cardClasses = (textClasses + colorClasses);

        console.log("Import Info: ", importInfo);
        const totalFlights = (importInfo.validFlights + importInfo.errorFlights);

        const hasWarnings = (importInfo.warningFlights > 0);

        return (
            <div className="m-2">
                <div className="d-flex flex-row justify-content-between align-items-start" style={{
                    ...styleName,
                    backgroundColor: "var(--c_entry_bg)",
                    padding: '10px',
                    borderRadius: "10px",
                    border: "1px solid var(--c_border_alt)"
                }}>

                    {/* LEFT ELEMENTS */}
                    <div className="d-flex justify-content-start flex-wrap"
                         style={{flexWrap: "wrap", minWidth: "35%", maxWidth: "35%"}}>
                        <div className={textClasses}
                             style={{...styleTime, minWidth: "60%", maxWidth: "60%"}}>{importInfo.filename}</div>
                        <div className={textClasses}
                             style={{...styleTime, minWidth: "35%", maxWidth: "35%"}}>{importInfo.endTime}</div>
                    </div>

                    {/* RIGHT ELEMENTS */}
                    <div className="d-flex justify-content-end flex-wrap"
                         style={{flexFlow: "row wrap", minWidth: "65%"}}>

                        {/* Flights Uploaded With Non-Critical Issues */}
                        <div
                            className="d-flex flex-row"
                            style={{
                                ...styleCount,
                                flex: "0 0 7.5em",
                                padding: "5",
                                paddingLeft: "10",
                                backgroundColor: "var(--c_valid)"
                            }}
                        >
                            {
                                (hasWarnings)
                                    ? <i className="fa fa-check"
                                         style={{alignContent: "center", color: "var(--c_warning)"}}
                                         title="Flights with non-critical Warnings are included as Valid flights."/>
                                    : <i className="fa fa-check" style={{alignContent: "center", color: "white"}}
                                         title="No Flights in this upload have Warnings."/>
                            }
                            <div>&nbsp;Valid:</div>
                            <div style={{textAlign: "end", width: "100%"}}>{importInfo.validFlights}&nbsp;</div>
                        </div>

                        {/* Flights Uploaded With Warnings */}
                        <div
                            className="d-flex flex-row"
                            style={{
                                ...styleCount,
                                flex: "0 0 9.5em",
                                padding: "5",
                                paddingLeft: "10",
                                backgroundColor: "var(--c_warning)"
                            }}
                        >
                            <i className="fa fa-exclamation-triangle" style={{alignContent: "center"}}
                               aria-hidden="true"/>
                            <div>&nbsp;Warnings:</div>
                            <div style={{textAlign: "end", width: "100%"}}>{importInfo.warningFlights}&nbsp;</div>
                        </div>

                        {/* Flights Uploaded With Errors */}
                        <div
                            className="d-flex flex-row"
                            style={{
                                ...styleCount,
                                flex: "0 0 7.75em",
                                padding: "5",
                                paddingLeft: "10",
                                backgroundColor: "var(--c_danger)"
                            }}
                        >
                            <i className="fa fa-exclamation-circle" style={{alignContent: "center"}}
                               aria-hidden="true"/>
                            <div>&nbsp;Errors:</div>
                            <div style={{textAlign: "end", width: "100%"}}>{importInfo.errorFlights}&nbsp;</div>
                        </div>

                        {/* Total Flights Uploaded */}
                        <div
                            className="d-flex flex-row"
                            style={{
                                ...styleCount,
                                flex: "0 0 7.5em",
                                padding: "5",
                                paddingLeft: "10",
                                backgroundColor: "var(--c_info)"
                            }}
                        >
                            <i className="fa fa-upload" style={{alignContent: "center"}} aria-hidden="true"/>
                            <div>&nbsp;Total:</div>
                            <div style={{textAlign: "end", width: "100%"}}>{totalFlights}&nbsp;</div>
                        </div>

                        {/* Upload Status */}
                        <div
                            className={cardClasses + statusClasses}
                            style={{...styleStatus, flex: "0 0 18em", marginLeft: "10px", marginRight: "10px"}}
                        >
                            {statusText}
                        </div>
                        <button className={`${expandButtonClasses  }d-flex justify-content-end flex-wrap`}
                                style={{...styleButton, marginLeft: "10px"}} onClick={() => this.expandClicked()}>
                            <i className={expandIconClasses}/>
                        </button>
                    </div>
                </div>
                <div className={expandDivClasses} hidden={!expanded}>
                    <div className="d-flex flex-row align-items-stretch ml-3">
                        {/* Vertical Line */}
                        <div
                            style={{
                                width: "4px",
                                backgroundColor: "var(--c_border_alt)",
                                marginRight: "10px",
                                borderEndEndRadius: "2px",
                                borderEndStartRadius: "2px"
                            }}
                        />

                        {/* Container for Warnings/Errors */}
                        <div className="d-flex flex-column flex-fill mr-4">
                            <hr style={{
                                borderTop: "4px solid var(--c_border)",
                                borderRadius: "2px",
                                marginTop: "0",
                                marginBottom: "0"
                            }}/>
                            {
                                (uploadErrors.length == 0 && flightWarnings.length == 0 && flightErrors.length == 0)
                                    ? (
                                        <div className="d-flex flex-row p-0 mt-1 mb-1"
                                             style={{minWidth: "100%", width: "100%"}}>
                                            <div className="p-1 card border-success"
                                                 style={{...styleName, minWidth: "100%", width: "100%"}}>
                                                No Errors or Warnings to Show!
                                            </div>
                                        </div>
                                    )
                                    : (
                                        <div>
                                            <UploadErrors expanded={expanded} uploadErrors={uploadErrors}/>
                                            <FlightWarnings expanded={expanded} flightWarnings={flightWarnings}/>
                                            <FlightErrors expanded={expanded} flightErrors={flightErrors}/>
                                        </div>
                                    )
                            }

                            <hr style={{
                                borderTop: "4px solid var(--c_border)",
                                borderRadius: "2px",
                                marginTop: "0",
                                marginBottom: "0"
                            }}/>
                        </div>
                    </div>

                </div>
            </div>
        );

    }
}


class ImportsPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            imports: this.props.imports,

            //needed for paginator
            currentPage: this.props.currentPage,
            numberPages: this.props.numberPages, //this will be set globally in the javascript
            pageSize: 10
        };
    }

    submitFilter() {

        const submissionData = {
            currentPage: this.state.currentPage,
            pageSize: this.state.pageSize
        };

        $.ajax({
            type: 'POST',
            url: '/api/upload/imported',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {

                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("Displaying error modal!");
                    showErrorModal(response.errorTitle, response.errorMessage);
                    return false;
                }

                console.log(`got response: ${  response  } ${  response.size}`);

                this.setState({
                    imports: response.imports,
                    numberPages: response.numberPages
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Flights", errorThrown);
            },
        });
    }

    render() {
        return (
            <div style={{display: "flex", flexDirection: "column", minHeight: "100vh", maxHeight: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage="imports" waitingUserCount={waitingUserCount} fleetManager={fleetManager}
                                    unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess}
                                    plotMapHidden={plotMapHidden}/>
                </div>

                <div style={{overflowY: "scroll", flex: "1 1 auto", paddingBottom: "70px"}}>
                    <div className="m-1">
                        {this.state.imports.map((importInfo) => {
                            return (
                                <Import importInfo={importInfo} key={importInfo.identifier}/>
                            );
                        })}
                    </div>

                    <div style={{
                        bottom: "0",
                        width: "99%",
                        paddingLeft: "0.75em",
                        paddingBottom: "1.0em",
                        paddingRight: "0.75em",
                        position: "fixed",
                        alignSelf: "center"
                    }}>
                        <Paginator
                            submitFilter={() => {
                                this.submitFilter();
                            }}
                            items={this.state.imports}
                            itemName="uploads"
                            currentPage={this.state.currentPage}
                            numberPages={this.state.numberPages}
                            pageSize={this.state.pageSize}
                            updateCurrentPage={(currentPage) => {
                                this.setState({currentPage: currentPage});
                            }}
                            updateItemsPerPage={(pageSize) => {
                                this.setState({pageSize: pageSize});
                            }}
                        />
                    </div>

                </div>

            </div>
        );


    }
}

const container = document.querySelector("#imports-page");
const root = createRoot(container);
root.render(
    <ImportsPage
        imports={imports}
        numberPages={numberPages}
        currentPage={currentPage}
    />
);