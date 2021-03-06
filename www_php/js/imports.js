class FlightWarning extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let warning = this.props.warning;

        const styleName = { flex : "0 0 25em" };
        let filenameClasses = "p-1 mr-1 card border-warning text-warning";
        let filenameText = warning.filename;
        if (warning.sameFilename) {
            filenameClasses = "p-1 mr-1";
            filenameText = "";
        }

        return (
            <div className="d-flex flex-row p-0 mt-1">
                <div className={filenameClasses} style={styleName} >
                    {filenameText}
                </div>
                <div className="p-1 card border-warning text-warning flex-fill">
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
        let flightWarnings = this.props.flightWarnings;

        return (
            <div className="m-0">
                {
                    flightWarnings.map((warning, index) => {
                        return (
                            <FlightWarning warning={warning} key={warning.id} />
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
        let error = this.props.error;

        const styleName = { flex : "0 0 25em" };
        let filenameClasses = "p-1 mr-1 card border-danger text-danger";
        let filenameText = error.filename;
        if (error.sameFilename) {
            filenameClasses = "p-1 mr-1";
            filenameText = "";
        }

        return (
            <div className="d-flex flex-row p-0 mt-1">
                <div className={filenameClasses} style={styleName} >
                    {filenameText}
                </div>
                <div className="p-1 card border-danger text-danger flex-fill">
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
        let flightErrors = this.props.flightErrors;

        return (
            <div className="m-0">
                {
                    flightErrors.map((error, index) => {
                        return (
                            <FlightError error={error} key={error.id} />
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
        let uploadErrors = this.props.uploadErrors;

        return (
            <div className="m-0 mt-1">
                {
                    uploadErrors.map((error, index) => {
                        return (
                            <UploadError error={error} key={error.id} />
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
            expanded : false,
            loaded : false,
            uploadErrors : uploadErrors,
            flightWarnings: flightWarnings,
            flightErrors: flightErrors
        };
    }

    componentDidMount() {
        //console.log("import did mount for filename: '" + this.props.importInfo.filename + "'");
    }

    expandClicked() {
        var thisImport = this;

        var submission_data = {
            request : "GET_UPLOAD_DETAILS",
            //id_token : id_token,
            id_token : "TEST_ID_TOKEN",
            upload_id : this.props.importInfo.id,
            user_id : 1
        };   

        if (this.state.loaded) {
            console.log("not fetching import information from the server, already loaded.");
            thisImport.state.expanded = !thisImport.state.expanded;
            thisImport.setState(thisImport.state);
        } else {
            console.log("fetching import information from the server.");

            $.ajax({
                type: 'POST',
                url: './request.php',
                data : submission_data,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    if (response.err_title !== undefined) {
                        display_error_modal(response.err_title, response.err_msg);
                    } else {
                        thisImport.state.loaded = true;
                        thisImport.state.expanded = !thisImport.state.expanded;
                        console.log("expand clicked, now:" + thisImport.state.expanded);
                        thisImport.state.uploadErrors = response.details.upload_errors;
                        thisImport.state.flightWarnings = response.details.flight_warnings;
                        thisImport.state.flightErrors = response.details.flight_errors;

                        thisImport.setState(thisImport.state);
                    }

                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    display_error_modal("Error Loading Uploads", errorThrown);
                },   
                async: true 
            });  
        }

    }

    render() {
        let expanded = this.state.expanded;
        let uploadErrors = this.state.uploadErrors;
        let flightWarnings = this.state.flightWarnings;
        let flightErrors = this.state.flightErrors;

        for (var i = 1; i < flightWarnings.length; i++) {
            if (flightWarnings[i-1].filename == flightWarnings[i].filename) {
                flightWarnings[i].sameFilename = true;
            } else {
                flightWarnings[i].sameFilename = false;
            }
        }

        for (var i = 1; i < flightErrors.length; i++) {
            if (flightErrors[i-1].filename == flightErrors[i].filename) {
                flightErrors[i].sameFilename = true;
            } else {
                flightErrors[i].sameFilename = false;
            }
        }

        let importInfo = this.props.importInfo;

        /*
        console.log("rendering import for filename: '" + importInfo.filename + "'");
        console.log(this.props);
        console.log(this.state);
        */

        let progressSize = importInfo.progressSize;
        let totalSize = importInfo.totalSize;

        if (progressSize == undefined) progressSize = importInfo.bytes_imported;
        if (totalSize == undefined) totalSize = importInfo.size_bytes;

        const width = ((progressSize / totalSize) * 100).toFixed(2);
        const sizeText = (progressSize/1000).toFixed(2).toLocaleString() + "/" + (totalSize/1000).toFixed(2).toLocaleString()  + " kB (" + width + "%)";
        const progressSizeStyle = {
            width : width + "%",
            height : "24px",
            textAlign : "left",
            whiteSpace : "nowrap"
        };

        const styleName = { };
        const styleTime = { flex : "0 0 11em" };
        const styleCount = { flex : "0 0 8em" };
        const styleStatus = { flex : "0 0 10em" };
        const styleButton = { };

        let statusText = "";
        let expandButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";
        let expandIconClasses = "fa ";

        let expandDivClasses = "";
        if (expanded) {
            expandIconClasses += "fa-angle-double-up";
            expandDivClasses = "m-0 mt-1 mb-4";
        } else {
            expandIconClasses += "fa-angle-double-down";
            expandDivClasses = "m-0";
        }

        let progressBarClasses = "progress-bar";
        let status = importInfo.status;
        let colorClasses = "";

        if (status == "HASHING") {
            statusText = "Hashing";
            progressBarClasses += " bg-warning";
            colorClasses += " border-warning text-warning";
        } else if (status == "IMPORTED") {
            if (importInfo.n_error_flights == 0 && importInfo.n_warning_flights == 0) {
                statusText = "Imported";
                progressBarClasses += " bg-success";
                colorClasses += " border-success text-success";

            } else if (importInfo.n_error_flights != 0 && importInfo.n_error_flights != 0) {
                statusText = "Imported With Errors and Warnings";
                progressBarClasses += " bg-danger";
                colorClasses += " border-danger text-danger ";

            } else if (importInfo.n_error_flights != 0) {
                statusText = "Imported With Errors";
                progressBarClasses += " bg-danger";
                colorClasses += " border-danger text-danger ";

            } else if (importInfo.n_warning_flights != 0) {
                statusText = "Imported With Warnings";
                progressBarClasses += " bg-warning";
                colorClasses += " border-warning text-warning ";
            }

        } else if (status == "UPLOADING") {
            statusText = "Uploading";
        } else if (status == "UPLOAD INCOMPLETE") {
            statusText = "Upload Incomplete";
            progressBarClasses += " bg-warning";
            colorClasses += " border-warning text-warning";
        } else if (status == "ERROR") {
            statusText = "Import Failed";
            progressBarClasses += " bg-danger";
            colorClasses += " border-danger text-danger";
        }

        let textClasses = "p-1 mr-1 card bg-light";
        let cardClasses = textClasses + colorClasses;

        return (
            <div className="m-1">
                <div className="d-flex flex-row">
                    <div className={textClasses + " flex-fill"} style={styleName}>{importInfo.filename}</div>
                    <div className={textClasses} style={styleTime}>{importInfo.end_time}</div>
                    <div className={textClasses + " text-success"} style={styleCount}>{importInfo.n_valid_flights} valid</div>
                    <div className={textClasses + " text-warning"} style={styleCount}>{importInfo.n_warning_flights} warnings</div>
                    <div className={textClasses + " text-danger"} style={styleCount}>{importInfo.n_error_flights} errors</div>
                    <div className={cardClasses} style={styleStatus}>{statusText}</div>
                    <button className={expandButtonClasses} style={styleButton} onClick={() => this.expandClicked()}><i className={expandIconClasses}></i></button>

                </div>
                <div className={expandDivClasses} hidden={!expanded}>
                    <UploadErrors expanded={expanded} uploadErrors={uploadErrors}/>
                    <FlightWarnings expanded={expanded} flightWarnings={flightWarnings}/>
                    <FlightErrors expanded={expanded} flightErrors={flightErrors}/>
                </div>
            </div>
        );

    }
}


class ImportsCard extends React.Component {
    constructor(props) {
        super(props);

        let imports = props.imports;
        if (imports == undefined) imports = [];

        this.state = {
            imports : imports
        };
    }

    render() {
        const hidden = this.props.hidden;
        const hiddenStyle = {
            display : "none"
        };

        return (
            <div className="card-body" hidden={hidden}>
                {
                    this.state.imports.map((importInfo, index) => {
                        return (
                            <Import importInfo={importInfo} key={importInfo.identifier} />
                        );
                    })
                }
            </div>
        );
    }
}


