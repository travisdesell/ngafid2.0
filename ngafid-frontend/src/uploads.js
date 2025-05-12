import 'bootstrap';
import React from "react";
import {createRoot} from "react-dom/client";
import {confirmModal} from "./confirm_modal.js";
import {errorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";
import {Paginator} from "./paginator_component.js";

import SparkMD5 from "spark-md5";
import Button from "react-bootstrap/Button";


var paused = [];

var chunkSize = 2 * 1024 * 1024; //2MB

class Upload extends React.Component {

    constructor(props) {
        super(props);
    }

    downloadUpload() {

        $("#loading").show();

        let uploadInfo = this.props.uploadInfo;

        console.log("Downloading Upload: ", this.props.uploadInfo);

        $.ajax({
            type: 'GET',
            url: `/api/upload/${uploadInfo.id}/file`,
            xhrFields: {
                responseType: 'blob'
            },
            async: true,
            success: function (response) {

                console.log("Download Upload -- Received Response: ", response);

                $("#loading").hide();

                //Encountered an error, display error modal
                if (response.errorTitle) {
                    console.log("Displaying Error Modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                //Build the download link for the received ZIP file
                const FILE_DOWNLOAD_NAME_DEFAULT = "UnknownFlightData.zip";
                let blob = new Blob([response], {type: "application/zip"});
                let link = document.createElement("a");
                link.href = window.URL.createObjectURL(blob);
                link.download = (uploadInfo.filename || FILE_DOWNLOAD_NAME_DEFAULT);
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);

            },
            error: function (jqXHR, textStatus, errorThrown) {
                $("#loading").hide();

                let errorMessage = `${errorThrown}\n\n${textStatus}`;
                console.log("Error Downloading Upload: ", errorMessage);
                errorModal.show("Error Downloading Upload", errorMessage);
            }
        });

        $("#loading").hide();
    }


    removeUpload(uploadInfo) {

        $("#loading").show();

        let thisUpload = this;

        console.log("Removing Upload:", submissionData);

        $.ajax({
            type: 'DELETE',
            url: `/api/upload/${uploadInfo.id}`,
            dataType: 'json',
            async: true,
            success: function (response) {

                console.log("Remove Upload -- Received Response: ", response);

                $("#loading").hide();

                //Encountered an error, display error modal
                if (response.errorTitle) {
                    console.log("Displaying Error Modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                //Remove the upload from the list
                console.log("Remove Upload -- Removed successfully, removing from list...");

                thisUpload.props.removeUpload(uploadInfo);

            },
            error: function (jqXHR, textStatus, errorThrown) {
                $("#loading").hide();

                let errorMessage = `${errorThrown}\n\n${textStatus}`;
                console.log("Error Removing Upload: ", errorMessage);
                errorModal.show("Error Removing Upload", errorMessage);
            }
        });

    }

    confirmRemoveUpload(uploadInfo) {

        console.log("attempting to remove upload!");
        console.log(this.props);

        confirmModal.show("Confirm Delete: '" + uploadInfo.filename + "'",
            "Are you sure you wish to delete this upload?\n\nThis operation will remove it from the server along with all flights and other information from the database. A backup of this upload is not stored on the server and if you wish to retrieve it you will have to re-upload it.",
            () => {
                this.removeUpload(uploadInfo)
            }
        );

    }

    render() {

        let uploadInfo = this.props.uploadInfo;

        let progressSize = uploadInfo.progressSize;
        let totalSize = uploadInfo.totalSize;

        if (progressSize == undefined) progressSize = uploadInfo.bytesUploaded;
        if (totalSize == undefined) totalSize = uploadInfo.sizeBytes;

        const width = ((progressSize / totalSize) * 100).toFixed(2);
        const sizeText = (progressSize / 1000).toFixed(2).toLocaleString() + "/" + (totalSize / 1000).toFixed(2).toLocaleString() + " kB (" + width + "%)";


        let status = uploadInfo.status;

        console.log("[EX] Upload Status: ", status);

        /*

            New Upload Statusses:

            UPLOADING
            UPLOADING_FAILED
            UPLOADED
            ENQUEUED
            PROCESSING
            PROCESSED_OK
            PROCESSED_WARNING
            FAILED_FILE_TYPE
            FAILED_AIRCRAFT_TYPE
            FAILED_ARCHIVE_TYPE
            FAILED_UNKNOWN
            DERIVED                    (Note: Should not be displayed)

            (Note: If the status is not listed, apply the unknown defaults)

        */

        let statusText, progressBarClasses, statusClasses;
        const statusStates = {
            "UPLOADING": {
                "statusText": "Uploading",
                "progressBarClasses": "progress-bar bg-info",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-info text-info"
            },
            "UPLOADING_FAILED": {
                "statusText": "Upload Failed",
                "progressBarClasses": "progress-bar bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "UPLOADED": {
                "statusText": "Uploaded",
                "progressBarClasses": "progress-bar bg-primary",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-primary text-primary"
            },
            "ENQUEUED": {
                "statusText": "Enqueued",
                "progressBarClasses": "progress-bar bg-secondary",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-secondary text-secondary"
            },
            "PROCESSING": {
                "statusText": "Processing",
                "progressBarClasses": "progress-bar bg-warning",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-warning text-warning"
            },
            "PROCESSED_OK": {
                "statusText": "Processed OK",
                "progressBarClasses": "progress-bar bg-success",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-success text-success"
            },
            "PROCESSED_WARNING": {
                "statusText": "Processed With Warnings",
                "progressBarClasses": "progress-bar bg-warning",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-warning text-warning"
            },
            "FAILED_FILE_TYPE": {
                "statusText": "Failed: File Type",
                "progressBarClasses": "progress-bar bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "FAILED_AIRCRAFT_TYPE": {
                "statusText": "Failed: Aircraft Type",
                "progressBarClasses": "progress-bar bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "FAILED_ARCHIVE_TYPE": {
                "statusText": "Failed: Archive Type",
                "progressBarClasses": "progress-bar bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "FAILED_UNKNOWN": {
                "statusText": "Failed: Unknown",
                "progressBarClasses": "progress-bar bg-danger",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-danger text-danger"
            },
            "DERIVED": {
                "statusText": "Derived",
                "progressBarClasses": "progress-bar bg-info",
                "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-info text-info"
            },
        };
        const statusStateUnknownDefaults = {
            "statusText": "...",
            "progressBarClasses": "progress-bar bg-secondary",
            "statusClasses": "p-1 pl-2 pr-2 ml-1 card border-secondary text-secondary"
        }

        //Status is listed, apply the status text and classes
        if (status in statusStates) {
            statusText = statusStates[status].statusText;
            progressBarClasses = statusStates[status].progressBarClasses;
            statusClasses = statusStates[status].statusClasses;

            //Status is not listed, apply the unknown defaults
        } else {
            statusText = statusStateUnknownDefaults.statusText;
            progressBarClasses = statusStateUnknownDefaults.progressBarClasses;
            statusClasses = statusStateUnknownDefaults.statusClasses;
        }


        const progressSizeStyle = {
            width: width + "%",
            height: "34px",
            textAlign: "left",
            whiteSpace: "nowrap"
        };

        console.log("uploadInfo:");
        console.log(uploadInfo);

        //Check if ALL buttons should be disabled
        const buttonDisableStates = ["HASHING", "UPLOADING"];
        let doButtonsDisable = buttonDisableStates.includes(status);

        //Disable Delete buttons with no Upload Access
        let hasDeleteAccess = (isUploader);
        let doDeleteButtonDisable = (doButtonsDisable || !hasDeleteAccess);
        let doDeleteButtonHide = (!hasDeleteAccess);

        //Disable Delete buttons with no View Access
        let hasDownloadAccess = true;
        let doDownloadButtonDisable = (doButtonsDisable || !hasDownloadAccess);
        let doDownloadButtonHide = (!hasDownloadAccess);

        return (
            <div className="m-2">
                <div className="d-flex align-items-start" style={{
                    backgroundColor: "var(--c_entry_bg)",
                    padding: '10px',
                    borderRadius: "10px",
                    position: "relative",
                    border: "1px solid var(--c_border_alt)"
                }}>

                    {/* LEFT ELEMENTS */}
                    <div className="d-flex flex-row"
                         style={{flex: '0 0 15em', minWidth: "35%", maxWidth: "35%", position: "relative"}}>
                        <div className="p-1 mr-1 card"
                             style={{flex: '1 1 0', alignSelf: 'stretch'}}>{uploadInfo.filename}</div>
                        <div className="p-1 mr-1 card" style={{
                            flex: '1 1 0',
                            alignSelf: 'stretch',
                            minWidth: "35%",
                            maxWidth: "35%"
                        }}>{uploadInfo.startTime}</div>
                    </div>

                    {/* CENTER ELEMENTS */}
                    <div className="flex-fill card progress" style={{height: "34px"}}>
                        <div className={progressBarClasses} role="progressbar" style={progressSizeStyle}
                             aria-valuenow={width} aria-valuemin="0" aria-valuemax="100">&nbsp; {sizeText}</div>
                    </div>

                    {/* RIGHT ELEMENTS */}
                    <div className={statusClasses} style={{flex: "0 0 18em"}}>{statusText}</div>

                    {/* VERTICALLY CENTERED DELETE & DOWNLOAD BUTTONS */}
                    <div style={{marginTop: "auto", marginBottom: "auto"}}>
                        {
                            (!doDeleteButtonHide) &&
                            <Button
                                type="button"
                                className={"btn btn-danger btn-sm"}
                                style={{
                                    backgroundColor: (doDeleteButtonDisable ? '#444444' : '#DC3545'),
                                    width: "30px",
                                    height: "30px",
                                    marginLeft: "4px",
                                    padding: "2 4 4 4"
                                }}
                            >
                                <i
                                    className="fa fa-times"
                                    aria-hidden="true"
                                    style={{
                                        padding: "0",
                                        display: "flex",
                                        alignItems: "center",
                                        justifyContent: "center",
                                        marginTop: "1px"
                                    }}
                                    onClick={() => (doDeleteButtonDisable ? undefined : this.confirmRemoveUpload(uploadInfo))}
                                >
                                </i>
                            </Button>
                        }

                        {
                            (!doDownloadButtonHide) &&
                            <Button
                                type="button"
                                className={"btn btn btn-sm"}
                                style={{
                                    backgroundColor: (doDownloadButtonDisable ? '#444444' : '#007BFF'),
                                    width: "30px",
                                    height: "30px",
                                    marginLeft: "4px",
                                    padding: "2 4 4 4"
                                }}
                            >
                                <i
                                    className="fa fa-download"
                                    aria-hidden="true"
                                    style={{
                                        padding: "0",
                                        display: "flex",
                                        alignItems: "center",
                                        justifyContent: "center",
                                        marginTop: "4px"
                                    }}
                                    onClick={() => (doDownloadButtonDisable ? undefined : this.downloadUpload())}
                                >
                                </i>
                            </Button>
                        }
                    </div>

                </div>
            </div>
        );

    }
}

function getUploadeIdentifier(filename, size) {
    return (size + '-' + filename.replace(/[^0-9a-zA-Z_-]/img, ''));
}


class UploadsPage extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            uploads: this.props.uploads,
            pending_uploads: this.props.pending_uploads,

            //needed for paginator
            currentPage: this.props.currentPage,
            numberPages: this.props.numberPages, //this will be set globally in the javascript
            pageSize: 10
        };
    }

    getMD5Hash(file, onFinish, uploadsPage) {

        // console.log(`Processing MD5 Hash for File: "${file.name}" at position ${file.position}`);

        var blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice,
            chunkSize = 2097152,                             // Read in chunks of 2MB
            chunks = Math.ceil(file.size / chunkSize),
            currentChunk = 0,
            spark = new SparkMD5.ArrayBuffer(),
            fileReader = new FileReader();

        fileReader.onload = function (e) {

            console.log('read chunk nr', currentChunk + 1, 'of', chunks);
            spark.append(e.target.result);                   // Append array buffer
            currentChunk++;


            if (currentChunk % 5 == 0) {

                let state = uploadsPage.state;
                console.log("inside onload function!");
                console.log(state);
                console.log(file);
                state.pending_uploads[file.position].progressSize = currentChunk * chunkSize;

                uploadsPage.setState(state);

            }

            if (currentChunk < chunks) {
                loadNext();
            } else {    //Reset progress bar for uploading...

                let state = uploadsPage.state;

                var statusInitial = state.pending_uploads[file.position].status;
                if (statusInitial != "UPLOADING") {

                    state.pending_uploads[file.position].progressSize = 0;
                    state.pending_uploads[file.position].status = "UPLOADING";

                    // console.log(`File with identifier "${file.identifier}" at position ${file.position} transitioning to new status... "${statusInitial}" -> "UPLOADING"`);
                    uploadsPage.setState(state);

                    onFinish(spark.end());

                }
            }
        };

        fileReader.onerror = function () {
            errorModal.show("File Upload Error", "Could not upload file because of an error generating it's MD5 hash. Please reload the page and try again.");
        };

        function loadNext() {
            var start = currentChunk * chunkSize,
                end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;

            fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
        }

        loadNext();
    }

    startUpload(file) {

        // console.log(`Starting upload of file: ${file}`);

        //different versions of firefox have different field names
        var filename = file.webkitRelativePath || file.fileName || file.name;
        var identifier = file.identifier;
        var position = file.position;

        paused[identifier] = false;

        var numberChunks = Math.ceil(file.size / chunkSize);

        var uploadInfo = {};
        uploadInfo.identifier = identifier;
        uploadInfo.filename = filename;
        uploadInfo.uploadedChunks = 0;
        uploadInfo.numberChunks = numberChunks;
        uploadInfo.sizeBytes = file.size;
        uploadInfo.bytesUploaded = 0;
        uploadInfo.status = 'HASHING';

        var uploadsPage = this;

        function onFinish(md5Hash) {

            file.md5Hash = md5Hash;
            console.log("got md5Hash: '" + md5Hash + "'");
            var xhr = new XMLHttpRequest();

            xhr.open('POST', '/api/upload');
            xhr.onload = function () {

                console.log("New upload response: " + xhr.responseText);
                var response = JSON.parse(xhr.responseText);

                var filename = (file.webkitRelativePath || file.fileName || file.name);

                //check and see if there was an error in the response!
                if (response.errorTitle !== undefined) {
                    errorModal.show(response.errorTitle, response.errorMessage + "\n\nOn file: '" + filename + "'");
                    uploadsPage.removePendingUpload(file);

                } else {
                    var uploadInfo = response;
                    uploadInfo.file = file; //set the file in the response uploadInfo so it can be used later
                    uploadInfo.identifier = identifier;
                    uploadInfo.position = position;
                    uploadsPage.updateUpload(uploadInfo);
                }
            };

            var formData = new FormData();
            formData.append("request", "NEW_UPLOAD");
            formData.append("filename", filename);
            formData.append("identifier", identifier);
            formData.append("numberChunks", numberChunks);
            formData.append("sizeBytes", file.size);
            formData.append("md5Hash", md5Hash);
            xhr.send(formData);
        }

        var md5Hash = this.getMD5Hash(file, onFinish, this);
    }


    addUpload(file) {

        const filename = (file.webkitRelativePath || file.fileName || file.name);
        const progressSize = 0;
        const status = "HASHING";
        const totalSize = file.size;
        console.log("adding filename: '" + filename + "'");

        let pendingUploads = this.state.pending_uploads;

        let identifier = getUploadeIdentifier(filename, totalSize);
        console.log("CREATED IDENTIFIER: " + identifier);
        file.identifier = identifier;
        file.position = 0;

        let alreadyExists = false;
        for (var i = 0; i < pendingUploads.length; i++) {

            // console.log(`Pending Upload Identifier (${i}): ${pendingUploads[i].identifier} /// Current Upload Identifier: ${identifier}`);

            //Testing Matching Identifiers
            if (pendingUploads[i].identifier == identifier) {

                //Upload already exists in the list but is incomplete, so we need to restart it
                if (pendingUploads[i].status == "UPLOAD INCOMPLETE") {

                    alreadyExists = true;
                    file.position = i;

                }

                //The file already exists, don't bother adding it
                else {

                    console.log("file already exists, not adding!");
                    return;

                }

            }

            //Testing non-matching identifiers
            else {
                file.position++;
            }

        }

        //No copy of the file exists already, proceed with adding it
        if (alreadyExists == false) {

            //pendingUploads.unshift({
            pendingUploads.push({
                position: file.position,
                identifier: identifier,
                filename: filename,
                status: status,
                totalSize: totalSize,
                progressSize: progressSize
            });

        }

        this.state.pending_uploads = pendingUploads;

        // let uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return `(${uploadItem.identifier},${uploadItem.position})` });
        // console.log(`Updated Pending Uploads after adding new file with identifier "${file.identifier}": [${uploadStringMap}]`);

        if (this.state.numberPages == 0) {
            this.state.numberPages = 1;
            this.state.currentPage = 0;
        }

        this.setState(this.state);
        this.startUpload(file);

    }

    removePendingUpload(file) {

        if (file.position < pending_uploads.length) {

            let pending_uploads = this.state.pending_uploads;

            // let uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return uploadItem.identifier });
            // console.log(`Removing a *pending* file upload! Original State: [${uploadStringMap}]`);

            pending_uploads.splice(file.position, 1);
            for (var i = 0; i < pending_uploads.length; i++) {
                pending_uploads[i].position = i;
            }

            this.state.pending_uploads = pending_uploads;

            // uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return uploadItem.identifier; });
            // console.log(`Removing a *pending* file upload! New State: [${uploadStringMap}]`);

            this.setState(this.state);
        }
    }


    removeUploadProp(uploadInfo) {

        console.log("[EX] Removing Upload Prop: ", uploadInfo);

        try {

            //Upload position is within bounds, remove the upload
            if (uploadInfo.position < uploads.length) {

                let uploads = this.state.uploads;

                //Remove the upload from the list
                uploads.splice(uploadInfo.position, 1);
                for (var i = 0; i < uploads.length; i++) {
                    uploads[i].position = i;
                }

                this.state.uploads = uploads;

                //Trigger state update
                this.setState(this.state);

                //Upload position is out of bounds, throw an error
            } else {
                throw new Error("Upload position is out of bounds!");
            }

        } catch (error) {

            //Display Error Modal
            errorModal.show("Error Removing Upload Prop", error.message);

        }

    }

    updateUpload(uploadInfo) {

        var file = uploadInfo.file;
        var position = uploadInfo.position;
        var filename = uploadInfo.filename;

        var chunkStatus = uploadInfo.chunkStatus;
        var numberChunks = parseInt(uploadInfo.numberChunks);
        var chunkNumber = chunkStatus.indexOf("0");

        console.log("next chunk: " + chunkNumber + " of " + numberChunks);

        uploadInfo.progressSize = uploadInfo.bytesUploaded;
        uploadInfo.totalSize = uploadInfo.sizeBytes;

        this.state.pending_uploads[uploadInfo.position] = uploadInfo;

        this.setState(this.state);

        var uploadsPage = this;

        var startByte = parseInt(chunkNumber) * parseInt(chunkSize);
        var endByte = Math.min(parseInt(startByte) + parseInt(chunkSize), file.size);
        var func = (file.slice ? 'slice' : (file.mozSlice ? 'mozSlice' : (file.webkitSlice ? 'webkitSlice' : 'slice')));
        var bytes = file[func](startByte, endByte, void 0);

        var xhr = new XMLHttpRequest();
        xhr.open('PUT', `/api/upload/${uploadInfo.id}/chunk/${chunkNumber}`);
        xhr.onload = function () {

            console.log("Upload response: " + xhr.responseText);
            var response = JSON.parse(xhr.responseText);

            //Error in response, show error modal
            if (response.errorTitle !== undefined) {
                errorModal.show(response.errorTitle, response.errorMessage + "\n\nOn file: '" + filename + "'");

                //No error encountered, continue with upload
            } else {

                var uploadInfo = response;
                uploadInfo.file = file; //<-- Set the fileObject so we can use it for restarts
                uploadInfo.position = position;

                var numberChunks = Math.ceil(file.size / chunkSize);
                console.log("uploaded chunk " + chunkNumber + " of " + numberChunks);

                var chunkStatus = uploadInfo.chunkStatus;
                chunkNumber = chunkStatus.indexOf("0");

                const CHUNK_NUMBER_END = -1;

                //More chunks to upload, continue with next chunk
                if (chunkNumber > CHUNK_NUMBER_END) {
                    console.log("uploading next chunk with response:");
                    console.log(response);
                    console.log("uploadInfo:");
                    console.log(uploadInfo);

                    uploadsPage.updateUpload(uploadInfo);

                    //All chunks have been uploaded, finish the upload
                } else {
                    console.log("Should be finished upload!");

                    uploadsPage.state.pending_uploads[uploadInfo.position] = uploadInfo;
                    uploadsPage.setState(uploadsPage.state);
                }
            }
        };

        console.log("appending identifier: " + file.identifier);
        var formData = new FormData();
        formData.append("chunk", bytes, file.fileName);
        xhr.send(formData);
    }

    submitFilter() {
        //prep data
        var uploadsPage = this;

        var submissionData = {
            currentPage: this.state.currentPage,
            pageSize: this.state.pageSize
        };

        console.log(submissionData);

        $.ajax({
            type: 'GET',
            url: '/api/upload',
            data: submissionData,
            success: function (response) {

                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                console.log("got response: " + response + " " + response.sizeAll);

                uploadsPage.setState({
                    uploads: response.uploads,
                    numberPages: response.numberPages
                });
            },
            error: function (jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Uploads", errorThrown);
            },
            async: true
        });
    }

    render() {
        console.log("rendering uploads!");

        const hiddenStyle = {
            display: "none"
        };

        //Disable Upload buttons with no Upload Access
        let doUploadButtonHide = (!isUploader);

        return (

            <div style={{display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{display: "flex", flexDirection: "column", minHeight: "100vh"}}>

                    <div style={{flex: "0 0 auto"}}>
                        <SignedInNavbar activePage="uploads" waitingUserCount={waitingUserCount}
                                        fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                        modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                    </div>

                    <div style={{overflowY: "scroll", flex: "1 1 auto", paddingBottom: "70px"}}>

                        <div className="m-1">

                            <input id="upload-file-input" type="file" style={hiddenStyle}/>

                            {/* Render Pending Uploads */}
                            {
                                this.state.pending_uploads.map((uploadInfo, index) => {

                                    // let uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return `(${uploadItem.identifier},${uploadItem.position})` });
                                    // console.log(`Previewing all Pending Uploads: ${uploadStringMap}`);
                                    // console.log(`Delivering new Upload Info with identifier "${uploadInfo.identifier}" and position "${uploadInfo.position}" at index ${index}`);

                                    //uploadInfo.position = index;
                                    return (
                                        <Upload
                                            uploadInfo={uploadInfo}
                                            key={uploadInfo.identifier}
                                            removeUpload={(uploadInfo) => {
                                                this.removePendingUpload(uploadInfo);
                                            }}
                                        />
                                    );
                                })
                            }

                            {/* Render Non-Pending Uploads */}
                            {
                                this.state.uploads.map((uploadInfo, index) => {

                                    //Skip 'Derived' uploads
                                    if (uploadInfo.status == "DERIVED")
                                        return;

                                    uploadInfo.position = index;
                                    return (
                                        <Upload
                                            uploadInfo={uploadInfo}
                                            key={uploadInfo.identifier}
                                            removeUpload={(uploadInfo) => {
                                                this.removeUploadProp(uploadInfo);
                                            }}
                                        />
                                    );
                                })
                            }

                            <input id="upload-file-input" type="file"
                                   style={hiddenStyle}/> {/* <-- Keep this here so the Upload Flights button in the Paginator works */}
                            <div style={{
                                bottom: "0",
                                width: "99%",
                                paddingLeft: "0.5em",
                                paddingBottom: "1.0em",
                                paddingRight: "1.00em",
                                position: "fixed",
                                alignSelf: "center"
                            }}>
                                <Paginator
                                    submitFilter={() => {
                                        this.submitFilter();
                                    }}
                                    items={this.state.uploads}
                                    itemName="uploads"
                                    uploadsPage={this}
                                    currentPage={this.state.currentPage}
                                    numberPages={this.state.numberPages}
                                    pageSize={this.state.pageSize}
                                    updateCurrentPage={(currentPage) => {
                                        this.state.currentPage = currentPage;
                                    }}
                                    updateItemsPerPage={(pageSize) => {
                                        this.state.pageSize = pageSize;
                                    }}
                                    doUploadButtonHide={doUploadButtonHide}
                                />
                            </div>

                        </div>
                    </div>

                </div>
            </div>
        );
    }
}

const root = createRoot(document.querySelector('#uploads-page'));
root.render(<UploadsPage uploads={uploads} pending_uploads={pending_uploads} numberPages={numberPages}
                         currentPage={currentPage}/>);