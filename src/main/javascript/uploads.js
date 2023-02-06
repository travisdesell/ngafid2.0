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

        const submissionData = {
            uploadId : this.props.uploadInfo.uploaderId,
            md5Hash : this.props.uploadInfo.md5Hash
        };

        console.log(submissionData);

        $.ajax({
            type: 'GET',
            url: '/protected/download_upload',
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

            },
            error : function(jqXHR, textStatus, errorThrown) {
                $("#loading").hide();
                errorModal.show("Error downloading upload", errorThrown);
            },
            async: true
        });


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

        return (
            <div className="m-1">
                <div className="d-flex flex-row">
                    <div className="p-1 mr-1 card border-light bg-light" style={{flex:"0 0 15em"}}>{uploadInfo.filename}</div>
                    <div className="p-1 mr-1 card border-light bg-light" style={{flex:"0 0 15em"}}>{uploadInfo.startTime}</div>
                    <div className="flex-fill card progress" style={{height:"34px", padding: "0 0 0 0"}}>
                        <div className={progressBarClasses} role="progressbar" style={progressSizeStyle} aria-valuenow={width} aria-valuemin="0" aria-valuemax="100">&nbsp; {sizeText}</div>
                    </div>
                    <div className={statusClasses} style={{flex:"0 0 18em"}}>{statusText}</div>

                    <button type="button" className={"btn btn btn-sm"} style={{width:"34px", marginLeft:"4px", padding:"2 4 4 4"}}> <i className="fa fa-download" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.downloadUpload()}></i> </button>
                    <button type="button" className={"btn btn-danger btn-sm"} style={{width:"34px", marginLeft:"4px", padding:"2 4 4 4"}}> <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.confirmRemoveUpload()}></i> </button>

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
                //var percent = (currentChunk / chunks) * 100.0;

                let state = uploadsPage.state;
                console.log("inside onload function!");
                console.log(state);
                console.log(file);
                state.pending_uploads[file.position].progressSize = currentChunk * chunkSize;

                uploadsPage.setState(state);

                //set_progressbar_percent(file.identifier, percent);
            }

            if (currentChunk < chunks) {
                loadNext();
            } else {
                //reset progress bar for uploading
                let state = uploadsPage.state;
                state.pending_uploads[file.position].progressSize = 0;
                state.pending_uploads[file.position].status = "UPLOADING";
                uploadsPage.setState(state);

                onFinish(spark.end());
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
                console.log("New upload response: " + xhr.responseText);
                var response = JSON.parse(xhr.responseText);

                var filename = file.webkitRelativePath || file.fileName || file.name;

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
        const filename = file.webkitRelativePath || file.fileName || file.name;
        const progressSize = 0;
        const status = "HASHING";
        const totalSize = file.size;
        console.log("adding filename: '" + filename + "'");

        let pending_uploads = this.state.pending_uploads;

        let identifier = getUploadeIdentifier(filename, totalSize);
        console.log("CREATED IDENTIFIER: " + identifier);
        file.identifier = identifier;
        file.position = 0;

        let alreadyExists = false;
        for (var i = 0; i < pending_uploads.length; i++) {
            if (pending_uploads[i].identifier == identifier) {

                if (pending_uploads[i].status == "UPLOAD INCOMPLETE") {
                    //upload already exists in the list but is incomplete, so we need to restart it
                    alreadyExists = true;
                    file.position = i;
                } else {
                    console.log("file already exists, not adding!");
                    return;
                }
            }
        }

        if (!alreadyExists) {
            pending_uploads.unshift({
                identifier : identifier,
                filename : filename,
                status : status,
                totalSize : totalSize,
                progressSize : progressSize
            });
        }

        let state = this.state;
        state.pending_uploads = pending_uploads;

        if (this.state.numberPages == 0) {
            this.state.numberPages = 1;
            this.state.currentPage = 0;
        }

        this.setState(state);
        this.startUpload(file);
    }

    removePendingUpload(file) {
        if (file.position < pending_uploads.length) {
            let pending_uploads = this.state.pending_uploads;
            pending_uploads.splice(file.position, 1);
            for (var i = 0; i < pending_uploads.length; i++) {
                pending_uploads[i].position = i;
            }

            let state = this.state;
            state.pending_uploads = pending_uploads;
            this.setState(state);
        }
    }


    removeUpload(file) {
        if (file.position < uploads.length) {
            let uploads = this.state.uploads;
            uploads.splice(file.position, 1);
            for (var i = 0; i < uploads.length; i++) {
                uploads[i].position = i;
            }

            let state = this.state;
            state.uploads = uploads;
            this.setState(state);
        }
    }

    updateUpload(uploadInfo) {
        var file = uploadInfo.file;
        var position = uploadInfo.position;

        var numberChunks = parseInt(uploadInfo.numberChunks); 
        var filename = uploadInfo.filename;
        var identifier = uploadInfo.identifier;

        var chunkStatus = uploadInfo.chunkStatus;
        var chunkNumber = chunkStatus.indexOf("0");
        //console.log("chunk status: '" + chunkStatus + "'");
        console.log("next chunk: " + chunkNumber + " of " + numberChunks);

        uploadInfo.progressSize = uploadInfo.bytesUploaded;
        uploadInfo.totalSize = uploadInfo.sizeBytes;

        let pending_uploads = this.state.pending_uploads;
        pending_uploads[uploadInfo.position] = uploadInfo;
        let state = this.state;
        this.setState(state);

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

                    let pending_uploads = uploadsPage.state.pending_uploads;
                    pending_uploads[uploadInfo.position] = uploadInfo;
                    let state = uploadsPage.state;
                    uploadsPage.setState(state);
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
                            uploadInfo.position = index;
                            return (
                                <Upload uploadInfo={uploadInfo} key={uploadInfo.identifier} removeUpload={(uploadInfo) => {this.removePendingUpload(uploadInfo);}} />
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
