import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import { confirmModal } from "./confirm_modal.js";
import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import SparkMD5 from "spark-md5";

var navbar = ReactDOM.render(
    <SignedInNavbar activePage="uploads" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);


var paused = [];

var chunkSize = 2 * 1024 * 1024; //2MB

class Upload extends React.Component {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        //console.log("upload did mount for filename: '" + this.props.uploadInfo.filename + "'");
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

                uploadsCard.removeUpload(thisUpload.props.uploadInfo);
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

                    <button type="button" className={"btn btn-danger btn-sm"} style={{width:"34px", marginLeft:"4px", padding:"2 4 4 4"}}> <i className="fa fa-times" aria-hidden="true" style={{padding: "4 4 3 4"}} onClick={() => this.confirmRemoveUpload()}></i> </button>

                </div>
            </div>
        );

    }
}

function getUploadeIdentifier(filename, size) {
    return(size + '-' + filename.replace(/[^0-9a-zA-Z_-]/img, ''));
}


class UploadsCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            uploads : this.props.uploads,
            page : this.props.page,
            buffSize : 10,   //def size of uploads to show per page is 10
            numPages : this.props.numPages
        };

        this.previousPage = this.previousPage.bind(this);
        this.nextPage = this.nextPage.bind(this);
        this.repaginate = this.repaginate.bind(this);

        console.log("constructed UploadsCard, set mainCards");
        console.log("initial index: "+this.state.page);
    }

    getUploadsCard() {
        return this;
    }

    setUploads(uploads){
        this.state.uploads = uploads;
    }

    getMD5Hash(file, onFinish, uploadsCard) {
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

                let state = uploadsCard.state;
                console.log("inside onload function!");
                console.log(state);
                console.log(file);
                state.uploads[file.position].progressSize = currentChunk * chunkSize;

                uploadsCard.setState(state);

                //set_progressbar_percent(file.identifier, percent);
            }

            if (currentChunk < chunks) {
                loadNext();
            } else {
                //reset progress bar for uploading
                let state = uploadsCard.state;
                state.uploads[file.position].progressSize = 0;
                state.uploads[file.position].status = "UPLOADING";
                uploadsCard.setState(state);

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

        var uploadsCard = this;

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
                    uploadsCard.removeUpload(file);

                } else {
                    var uploadInfo = response;
                    uploadInfo.file = file; //set the file in the response uploadInfo so it can be used later
                    uploadInfo.identifier = identifier;
                    uploadInfo.position = position;
                    uploadsCard.updateUpload(uploadInfo);
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

        let uploads = this.state.uploads;

        let identifier = getUploadeIdentifier(filename, totalSize);
        console.log("CREATED IDENTIFIER: " + identifier);
        file.identifier = identifier;
        file.position = uploads.length;

        let alreadyExists = false;
        for (var i = 0; i < uploads.length; i++) {
            if (uploads[i].identifier == identifier) {

                if (uploads[i].status == "UPLOAD INCOMPLETE") {
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
            uploads.push({
                identifier : identifier,
                filename : filename,
                status : status,
                totalSize : totalSize,
                progressSize : progressSize
            });
        }

        let state = this.state;
        state.uploads = uploads;
		if(this.state.numPages == 0){
			this.state.numPages = 1;
			this.state.index = 0;
		}
        this.setState(state);
        this.startUpload(file);
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

        let uploads = this.state.uploads;
        uploads[uploadInfo.position] = uploadInfo;
        let state = this.state;
        this.setState(state);

        var uploadsCard = this;

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
                    //console.log("uploading next chunk with response:");
                    //console.log(response);

                    uploadsCard.updateUpload(uploadInfo);
                } else {

                    let uploads = uploadsCard.state.uploads;
                    uploads[uploadInfo.position] = uploadInfo;
                    let state = uploadsCard.state;
                    uploadsCard.setState(state);
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

    jumpPage(pg){
        if(pg < this.state.numPages && pg >= 0){
            this.state.page = pg;
        }
        this.setState(this.state);
        this.submitPagination();
    }

    setSize(size){
        this.state.numPages = size;
        this.setState(this.state);
    }

    nextPage(){
        this.state.page++;
        this.submitPagination();
    }

    previousPage(){
        this.state.page--;
        this.submitPagination();
    }

    setIndex(index){
        this.state.page = index;
        this.setState(this.state);
    }

    repaginate(pag){
        console.log("Re-Paginating");
        this.state.buffSize = pag;
        this.submitPagination();
    }

    submitPagination(){
        //prep data
        var uploadsCard = this;

        var submissionData = {
            index : this.state.page,
            buffSize : this.state.buffSize
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

                //get page data
                uploadsCard.setUploads(response.data);
                uploadsCard.setIndex(response.index);
                uploadsCard.setSize(response.sizeAll);
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },
            async: true
        });
    }

    genPages(){
        var page = [];
        for(var i = 0; i<this.state.numPages; i++){
            page.push({
                value : i,
                name : "Page "+(i+1)
            });
        }
        return page;
    }

    triggerInput() {
        var uploadsCard = this;

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
                    uploadsCard.addUpload(file);
                }    
            }    
        });  
    }

    render() {
        console.log("rendering uploads!");
        const hiddenStyle = {
            display : "none"
        };

		console.log(this.state.numPages+ " pages");
		let pageStatus = "Page "+(this.state.page + 1)+" of "+(this.state.numPages);
		let noUploads = false;
		if(this.state.numPages == 0){
			pageStatus = "No uploads yet!";
			noUploads = true;
		}

        let uploads = [];
        let pages = this.genPages();
        if (typeof this.state.uploads != 'undefined') {
            uploads = this.state.uploads;
        }

        var begin = this.state.page == 0;
        var end = this.state.page == this.state.numPages-1 || this.state.numPages == 0;
        var prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage}>Previous Page</button>
            var next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage}>Next Page</button>

        if(begin) {
            prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage} disabled>Previous Page</button>
        }
        if(end){
            next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage} disabled>Next Page</button>
        }

        return (
            <div className="card-body">
                <div className="card mb-1 m-1" style={{background : "rgba(248,259,250,0.8)"}}>
                    <div className="card mb-1 m-1 border-secondary">
                        <div className="p-2">
                            <button className="btn btn-sm btn-info pr-2" disabled>{pageStatus}</button>
                            <div className="btn-group mr-1 pl-1" role="group" aria-label="First group">
                                <DropdownButton className="pr-1" id="dropdown-item-button" title={this.state.buffSize + " uploads per page"} size="sm" disabled={noUploads}>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(10)}>10 uploads per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(15)}>15 uploads per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(25)}>25 uploads per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(50)}>50 uploads per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(100)}>100 uploads per page</Dropdown.Item>
                                </DropdownButton>
                                <Dropdown className="pr-1">
                                    <Dropdown.Toggle variant="primary" id="dropdown-basic" size="sm" disabled={noUploads}>
                                        {"Page " + (this.state.page + 1)}
                                    </Dropdown.Toggle>
                                    <Dropdown.Menu style={{ maxHeight: "256px", overflowY: 'scroll' }} >
                                        {
                                            pages.map((pages, index) => {
                                                return (
                                                    <Dropdown.Item key={index} as="button" onClick={() => this.jumpPage(pages.value)}>{pages.name}</Dropdown.Item>
                                                );
                                            })
                                        }
                                    </Dropdown.Menu>
                                </Dropdown>

                                {prev}
                                {next}
                            </div>
                            <button id="upload-flights-button"  className="btn btn-primary btn-sm float-right" onClick={() => this.triggerInput()}>
                                <i className="fa fa-upload"></i> Upload Flights
                            </button>
                            <input id ="upload-file-input" type="file" style={hiddenStyle} />
                        </div>
                    </div>
                    {
                        uploads.map((uploadInfo, index) => {
                            uploadInfo.position = index;
                            return (
                                <Upload uploadInfo={uploadInfo} key={uploadInfo.identifier} />
                            );
                        })
                    }
                    <div className="card mb-1 m-1 border-secondary" hidden={noUploads}>
                        <div className="p-2">
                            <button className="btn btn-sm btn-info pr-2" disabled>{pageStatus}</button>
                            <div className="btn-group mr-2 pl-1" role="group" aria-label="First group">
                                {prev}
                                {next}
                            </div>
                            <button id="upload-flights-button" className="btn btn-primary btn-sm float-right" onClick={() => this.triggerInput()}>
                                <i className="fa fa-upload"></i> Upload Flights
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}


var uploadsCard = ReactDOM.render(
    <UploadsCard uploads={uploads} numPages={numPages} page={index}/>,
    document.querySelector('#uploads-card')
);
