import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import { confirmModal } from "./confirm_modal.js";
import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";
import { Paginator } from "./paginator_component.js";

import SparkMD5 from "spark-md5";
import Button from "react-bootstrap/Button";


var paused = [];

var chunkSize = 2 * 1024 * 1024; //2MB

class Upload extends React.Component {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        //console.log("upload did mount for filename: '" + this.props.uploadInfo.filename + "'");
    }

    downloadUpload() {
        $("#loading").show();
        console.log("downloading upload");
        window.open("/protected/download_upload?uploadId=" + this.props.uploadInfo.id + "&md5Hash=" + this.props.uploadInfo.md5Hash);
        $("#loading").hide();

    }


    removeUpload() {
        $("#loading").show();

        var submissionData = {
            uploadId : this.props.uploadInfo.id,
            md5Hash : this.props.uploadInfo.md5Hash
        };

        let thisUpload = this;

        console.log("removing upload:");
        console.log(submissionData);

        $.ajax({
            type: 'POST',
            url: '/protected/remove_upload',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                thisUpload.props.removeUpload(thisUpload.props.uploadInfo);
            },
            error : function(jqXHR, textStatus, errorThrown) {
                $("#loading").hide();
                errorModal.show("Error removing upload", errorThrown);
            },
            async: true
        });
    }

    confirmRemoveUpload() {
        console.log("attempting to remove upload!");
        console.log(this.props);

        confirmModal.show("Confirm Delete: '" + this.props.uploadInfo.filename + "'",
            "Are you sure you wish to delete this upload?\n\nThis operation will remove it from the server along with all flights and other information from the database. A backup of this upload is not stored on the server and if you wish to retrieve it you will have to re-upload it.",
            () => {this.removeUpload()}
        );
    }

    render() {
        let uploadInfo = this.props.uploadInfo;

        let progressSize = uploadInfo.progressSize;
        let totalSize = uploadInfo.totalSize;

        if (progressSize == undefined) progressSize = uploadInfo.bytesUploaded;
        if (totalSize == undefined) totalSize = uploadInfo.sizeBytes;

        const width = ((progressSize / totalSize) * 100).toFixed(2);
        const sizeText = (progressSize/1000).toFixed(2).toLocaleString() + "/" + (totalSize/1000).toFixed(2).toLocaleString()  + " kB (" + width + "%)";

        let statusText = "";

        let progressBarClasses = "progress-bar";
        let statusClasses = "p-1 pl-2 pr-2 ml-1 card bg-light";
        let status = uploadInfo.status;
        if (status == "HASHING") {
            statusText = "Hashing";
            progressBarClasses += " bg-warning";
            statusClasses += " border-warning text-warning";
        } else if (status == "UPLOADED") {
            statusText = "Uploaded";
            progressBarClasses += " bg-primary";
            statusClasses += " border-primary text-primary";
        } else if (status == "UPLOADING") {
            statusText = "Uploading";
        } else if (status == "UPLOAD INCOMPLETE") {
            statusText = "Upload Incomplete";
            progressBarClasses += " bg-warning";
            statusClasses += " border-warning text-warning";
        } else if (status == "ERROR") {
            statusText = "Import Failed";
            progressBarClasses += " bg-danger";
            statusClasses += " border-danger text-danger";
        } else if (status == "IMPORTED") {
            if (uploadInfo.errorFlights == 0 && uploadInfo.warningFlights == 0) {
                statusText = "Imported";
                progressBarClasses += " bg-success";
                statusClasses += " border-success text-success";

            } else if (uploadInfo.errorFlights != 0 && uploadInfo.errorFlights != 0) {
                statusText = "Imported With Errors and Warnings";
                progressBarClasses += " bg-danger";
                statusClasses += " border-danger text-danger ";

            } else if (uploadInfo.errorFlights != 0) {
                statusText = "Imported With Errors";
                progressBarClasses += " bg-danger";
                statusClasses += " border-danger text-danger ";

            } else if (uploadInfo.warningFlights != 0) {
                statusText = "Imported With Warnings";
                progressBarClasses += " bg-warning";
                statusClasses += " border-warning text-warning ";
            }
        }

        const progressSizeStyle = {
            width : width + "%",
            height : "34px",
            textAlign : "left",
            whiteSpace : "nowrap"
        };

        console.log("uploadInfo:");
        console.log(uploadInfo);

        //Disable Download/Delete buttons while Upload HASHING / UPLOADING
        const BUTTON_DISPLAY_DISALLOW_LIST = ["HASHING", "UPLOADING"];
        let doButtonDisplay = !(BUTTON_DISPLAY_DISALLOW_LIST.includes(status));

        return (
            <div className="m-1">
                <div className="d-flex align-items-start" style={{backgroundColor: 'white', padding: '10px', borderRadius: "10px", position:"relative" }}>
        
                    {/* LEFT ELEMENTS */}
                    <div className="d-flex flex-row" style={{ flex: '0 0 15em', minWidth:"35%", maxWidth:"35%", position:"relative" }}>
                        <div className="p-1 mr-1 card bg-light" style={{ flex: '1 1 0', alignSelf: 'stretch' }}>{uploadInfo.filename}</div>
                        <div className="p-1 mr-1 card bg-light" style={{ flex: '1 1 0', alignSelf: 'stretch', minWidth:"35%", maxWidth:"35%" }}>{uploadInfo.startTime}</div>
                    </div>

                    {/* CENTER ELEMENTS */}
                    <div className="flex-fill card progress" style={{height:"34px"}}>
                        <div className={progressBarClasses} role="progressbar" style={progressSizeStyle} aria-valuenow={width} aria-valuemin="0" aria-valuemax="100">&nbsp; {sizeText}</div>
                    </div>
        
                    {/* RIGHT ELEMENTS */}
                    <div className={statusClasses} style={{flex:"0 0 18em"}}>{statusText}</div>                       
                    <Button
                        type="button"
                        className={"btn btn-danger btn-sm"}
                        style={{backgroundColor:(doButtonDisplay ? '#DC3545' : '#444444'), width:"34px", marginLeft:"4px", padding:"2 4 4 4"}}
                        >
                        <i
                            className="fa fa-times"
                            aria-hidden="true"
                            style={{padding: "4 4 3 4"}}
                            onClick={ () => (doButtonDisplay ? this.confirmRemoveUpload() : undefined) }
                            >
                        </i>
                    </Button>
                    <Button
                        type="button"
                        className={"btn btn btn-sm"}
                        style={{backgroundColor:(doButtonDisplay ? '#007BFF' : '#444444'), width:"34px", marginLeft:"4px", padding:"2 4 4 4"}}
                        >
                        <i
                            className="fa fa-download"
                            aria-hidden="true"
                            style={{padding: "4 4 3 4"}}
                            onClick={ () => (doButtonDisplay ? this.downloadUpload() : undefined) }
                            >
                        </i>
                    </Button>
        
                </div>
            </div>
        );

    }
}

function getUploadeIdentifier(filename, size) {
    return(size + '-' + filename.replace(/[^0-9a-zA-Z_-]/img, ''));
}


class UploadsPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            uploads : this.props.uploads,
            pending_uploads : this.props.pending_uploads,

            //needed for paginator
            currentPage : this.props.currentPage,
            numberPages : this.props.numberPages, //this will be set globally in the javascript
            pageSize : 10
        };
    }

    getMD5Hash(file, onFinish, uploadsPage) {

        // console.log(`[EX] Processing MD5 Hash for File: "${file.name}" at position ${file.position}`);

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
                }
            
            //Reset progress bar for uploading...
            else {

                let state = uploadsPage.state;

                var statusInitial = state.pending_uploads[file.position].status;
                if (statusInitial != "UPLOADING") {

                    state.pending_uploads[file.position].progressSize = 0;
                    state.pending_uploads[file.position].status = "UPLOADING";

                    // console.log(`[EX] File with identifier "${file.identifier}" at position ${file.position} transitioning to new status... "${statusInitial}" -> "UPLOADING"`);
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

        // console.log(`[EX] Starting upload of file: ${file}`);

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

            xhr.open('POST', '/protected/new_upload');
            xhr.onload = function() {
                
                // console.log("[EX] New upload response: " + xhr.responseText);
                var response = JSON.parse(xhr.responseText);

                var filename = (file.webkitRelativePath || file.fileName || file.name);

                //check and see if there was an error in the response!
                if (response.errorTitle !== undefined) {
                    errorModal.show(response.errorTitle, response.errorMessage + "<br>On file: '" + filename + "'");
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

            // console.log(`[EX] Pending Upload Identifier (${i}): ${pendingUploads[i].identifier} /// Current Upload Identifier: ${identifier}`);

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
                position : file.position,
                identifier : identifier,
                filename : filename,
                status : status,
                totalSize : totalSize,
                progressSize : progressSize
            });

        }

        this.state.pending_uploads = pendingUploads;

        // let uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return `(${uploadItem.identifier},${uploadItem.position})` });
        // console.log(`[EX] Updated Pending Uploads after adding new file with identifier "${file.identifier}": [${uploadStringMap}]`);

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
            // console.log(`[EX] Removing a *pending* file upload! Original State: [${uploadStringMap}]`);

            pending_uploads.splice(file.position, 1);
            for (var i = 0; i < pending_uploads.length; i++) {
                pending_uploads[i].position = i;
                }

            this.state.pending_uploads = pending_uploads;

            // uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return uploadItem.identifier; });
            // console.log(`[EX] Removing a *pending* file upload! New State: [${uploadStringMap}]`);

            this.setState( this.state );
        }
    }


    removeUpload(file) {

        if (file.position < uploads.length) {

            let uploads = this.state.uploads;

            // let uploadStringMap = this.state.uploads.map(function(uploadItem) { return uploadItem.identifier });
            // console.log(`[EX] Removing a file upload! Original State: [${uploadStringMap}]`);

            uploads.splice(file.position, 1);
            for (var i = 0; i < uploads.length; i++) {
                uploads[i].position = i;
            }


            this.state.uploads = uploads;

            // uploadStringMap = this.state.uploads.map(function(uploadItem) { return uploadItem.identifier });
            // console.log(`[EX] Removing a file upload! New State: [${uploadStringMap}]`);

            this.setState( this.state );
        }
    }

    updateUpload(uploadInfo) {

        // console.log(`[EX] Updating Upload Info: ${uploadInfo.identifier}`);

        // let uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return `(${uploadItem.identifier},${uploadItem.position})` });
        // console.log(`[EX] Before... : ${uploadStringMap}`);


        var file = uploadInfo.file;
        var position = uploadInfo.position;

        var numberChunks = parseInt(uploadInfo.numberChunks);
        var filename = uploadInfo.filename;
        var identifier = uploadInfo.identifier;

        var chunkStatus = uploadInfo.chunkStatus;
        var chunkNumber = chunkStatus.indexOf("0");
        console.log("next chunk: " + chunkNumber + " of " + numberChunks);

        uploadInfo.progressSize = uploadInfo.bytesUploaded;
        uploadInfo.totalSize = uploadInfo.sizeBytes;

        this.state.pending_uploads[uploadInfo.position] = uploadInfo;

        //uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return `(${uploadItem.identifier},${uploadItem.position})` });
        // console.log(`[EX] After... : ${uploadStringMap}`);

        this.setState( this.state );


        var uploadsPage = this;

        var fileReader = new FileReader();

        var startByte = parseInt(chunkNumber) * parseInt(chunkSize);
        var endByte = Math.min(parseInt(startByte) + parseInt(chunkSize), file.size);
        //console.log("startByte: " + startByte + ", endByte: " + endByte + ", chunkSize: " + chunkSize);

        var func = (file.slice ? 'slice' : (file.mozSlice ? 'mozSlice' : (file.webkitSlice ? 'webkitSlice' : 'slice')));
        var bytes = file[func](startByte, endByte, void 0);
        //console.log(bytes);

        var xhr = new XMLHttpRequest();
        xhr.open('POST', '/protected/upload');
        //xhr.setRequestHeader('Content-Type', 'application/octet-stream');
        xhr.onload = function() {
            console.log("Upload response: " + xhr.responseText);

            var response = JSON.parse(xhr.responseText);
            if (response.errorTitle !== undefined) {
                errorModal.show(response.errorTitle, response.errorMessage + "<br>On file: '" + filename + "'");

            } else {
                var uploadInfo = response;
                uploadInfo.file = file; //set the fileObject so we can use it for restarts
                uploadInfo.position = position;

                var numberChunks = Math.ceil(file.size / chunkSize);
                console.log("uploaded chunk " + chunkNumber + " of " + numberChunks);

                var chunkStatus = uploadInfo.chunkStatus;
                chunkNumber = chunkStatus.indexOf("0");
                //console.log("chunk status: '" + chunkStatus + "'");
                //console.log("next chunk: " + chunkNumber);
                //chunkNumber = chunkNumber + 1;

                if (chunkNumber > -1) {
                    console.log("uploading next chunk with response:");
                    console.log(response);
                    console.log("uploadInfo:");
                    console.log(uploadInfo);

                    uploadsPage.updateUpload(uploadInfo);
                } else {
                    console.log("Should be finished upload!");

                    uploadsPage.state.pending_uploads[uploadInfo.position] = uploadInfo;
                    uploadsPage.setState( uploadsPage.state );
                }
            }
        };

        console.log("appending identifier: " + file.identifier);
        var formData = new FormData();
        formData.append("request", "UPLOAD");
        formData.append("chunkNumber", chunkNumber);
        formData.append("identifier", file.identifier);
        formData.append("md5Hash", file.md5Hash);
        formData.append("chunk", bytes, file.fileName);
        xhr.send(formData);
    }

    submitFilter() {
        //prep data
        var uploadsPage = this;

        var submissionData = {
            currentPage : this.state.currentPage,
            pageSize : this.state.pageSize
        };

        console.log(submissionData);

        $.ajax({
            type: 'POST',
            url: '/protected/uploads',
            data : submissionData,
            dataType : 'json',
            success : function(response) {

                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                console.log("got response: "+response+" "+response.sizeAll);

                uploadsPage.setState({
                    uploads : response.uploads,
                    numberPages : response.numberPages
                });
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Uploads", errorThrown);
            },
            async: true
        });
    }

    triggerInput() {
        console.log("input triggered!");

        var uploadsPage = this;

        $('#upload-file-input').trigger('click');

        $('#upload-file-input:not(.bound)').addClass('bound').change(function() {
            console.log("number files selected: " + this.files.length);
            console.log( this.files );

            if (this.files.length > 0) {
                var file = this.files[0];
                var filename = file.webkitRelativePath || file.fileName || file.name;

                const isZip = file['type'].includes("zip");
                console.log("isZip: " + isZip);

                if (!filename.match(/^[a-zA-Z0-9_.-]*$/)) {
                    errorModal.show("Malformed Filename", "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");
                } else if (!isZip) {
                    errorModal.show("Malformed Filename", "Uploaded files must be zip files. The zip file should contain directories which contain flight logs (csv files). The directories should be named for the tail number of the airfraft that generated the flight logs within them.");
                } else {
                    uploadsPage.addUpload(file);
                }
            }
        });
    }

    render() {
        console.log("rendering uploads!");

        const hiddenStyle = {
            display : "none"
        };

        return (

            <div>
                <SignedInNavbar activePage="uploads" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <div className="p-1">
                    <input id ="upload-file-input" type="file" style={hiddenStyle} />

                    <div className="card mb-1 border-secondary">
                        <div className="p-2">
                            {
                                this.state.pending_uploads.length > 0
                                    ? ( <button className="btn btn-sm btn-info pr-2" disabled>Pending Uploads</button> )
                                    : ""
                            }
                            <button id="upload-flights-button" className="btn btn-primary btn-sm float-right" onClick={() => this.triggerInput()}>
                                <i className="fa fa-upload"></i> Upload Flights
                            </button>
                        </div>
                    </div>

                    {
                        this.state.pending_uploads.map((uploadInfo, index) => {

                            // let uploadStringMap = this.state.pending_uploads.map(function(uploadItem) { return `(${uploadItem.identifier},${uploadItem.position})` });
                            // console.log(`[EX] Previewing all Pending Uploads: ${uploadStringMap}`);
                            // console.log(`[EX] Delivering new Upload Info with identifier "${uploadInfo.identifier}" and position "${uploadInfo.position}" at index ${index}`);

                            //uploadInfo.position = index;
                            return (
                                <Upload
                                    uploadInfo={ uploadInfo }
                                    key={ uploadInfo.identifier }
                                    removeUpload={ (uploadInfo) => { this.removePendingUpload(uploadInfo); } }
                                    />
                            );
                        })
                    }

                    <Paginator
                        submitFilter={() => {this.submitFilter();}}
                        items={this.state.uploads}
                        itemName="uploads"
                        currentPage={this.state.currentPage}
                        numberPages={this.state.numberPages}
                        pageSize={this.state.pageSize}
                        updateCurrentPage={(currentPage) => {
                            this.state.currentPage = currentPage;
                        }}
                        updateItemsPerPage={(pageSize) => {
                            this.state.pageSize = pageSize;
                        }}
                    />

                    {
                        this.state.uploads.map((uploadInfo, index) => {
                            uploadInfo.position = index;
                            return (
                                <Upload uploadInfo={uploadInfo} key={uploadInfo.identifier} removeUpload={(uploadInfo) => {this.removeUpload(uploadInfo);}} />
                            );
                        })
                    }

                    <Paginator
                        submitFilter={() => {this.submitFilter();}}
                        items={this.state.uploads}
                        itemName="uploads"
                        currentPage={this.state.currentPage}
                        numberPages={this.state.numberPages}
                        pageSize={this.state.pageSize}
                        updateCurrentPage={(currentPage) => {
                            this.state.currentPage = currentPage;
                        }}
                        updateItemsPerPage={(pageSize) => {
                            this.state.pageSize = pageSize;
                        }}
                    />

                </div>

            </div>
        );
    }
}


var uploadsPage = ReactDOM.render(
    <UploadsPage uploads={uploads} pending_uploads={pending_uploads} numberPages={numberPages} currentPage={currentPage}/>,
    document.querySelector('#uploads-page')
);
