var paused = [];

var chunk_size = 1 * 1024 * 1024; //5MB




class Upload extends React.Component {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        console.log("upload did mount for filename: '" + this.props.filename);
    }

    render() {
        console.log("rendering upload for filename: '" + this.props.filename);

        const width = ((this.props.progressSize / this.props.totalSize) * 100).toFixed(2);
        const sizeText = (this.props.progressSize/1000).toFixed(2).toLocaleString() + "/" + (this.props.totalSize/1000).toFixed(2).toLocaleString()  + " kB (" + width + "%)";
        const progressSizeStyle = {
            width : width + "%",
            height : "24px",
            textAlign : "left",
            whiteSpace : "nowrap"
        };

        const fixedFlexStyle1 = {
            flex : "0 0 15em"
        };

        const fixedFlexStyle2 = {
            //flex : "0 0 75em",
            height : "34px",
            padding : "4 0 4 0"
        };

        const fixedFlexStyle3 = {
            flex : "0 0 10em"
        };


        let progressBarClasses = "progress-bar";
        let statusClasses = "p-1 pl-2 pr-2 ml-1 card bg-light";
        if (this.props.status == "Hashing") {
            progressBarClasses += " bg-warning";
            statusClasses += " border-warning text-warning";
        } else if (this.props.status == "Uploading") {
        } else if (this.props.status == "Error") {
            progressBarClasses += " bg-danger";
            statusClasses += " border-danger text-danger";
        }

        return (
            <div className="m-1">
                <div className="d-flex flex-row">
                    <div className="p-1 mr-1 card border-light bg-light" style={fixedFlexStyle1}>{this.props.filename}</div>
                    <div className="p-1 flex-fill card progress" style={fixedFlexStyle2}>
                        <div className={progressBarClasses} role="progressbar" style={progressSizeStyle} aria-valuenow={width} aria-valuemin="0" aria-valuemax="100">{sizeText}</div>
                    </div>
                    <div className={statusClasses} style={fixedFlexStyle3}>{this.props.status}</div>
                </div>
            </div>
        );

    }
}


class UploadsCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            uploads : []
        };
    }

    get_unique_identifier(filename, size) {
        return(size + '-' + filename.replace(/[^0-9a-zA-Z_-]/img, ''));
    }


    get_md5_hash(file, on_finish) {
        var blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice,
            chunkSize = 2097152,                             // Read in chunks of 2MB
            chunks = Math.ceil(file.size / chunkSize),
            currentChunk = 0,
            spark = new SparkMD5.ArrayBuffer(),
            fileReader = new FileReader();

        fileReader.onload = function (e) {
            //console.log('read chunk nr', currentChunk + 1, 'of', chunks);
            spark.append(e.target.result);                   // Append array buffer
            currentChunk++;

            if (currentChunk % 5 == 0) {
                var percent = (currentChunk / chunks) * 100.0;
                set_progressbar_percent(file.identifier, percent);
            }

            if (currentChunk < chunks) {
                //console.log('loaded chunk ' + currentChunk + ' of ' + chunks);
                loadNext();
            } else {
                //console.log('finished loading');
                //console.info('computed hash', spark.end());  // Compute hash

                //reset progress bar for uploading
                var percent = 0.0;

                set_progressbar_color(file.identifier, 'bg-warning');
                set_progressbar_percent(file.identifier, percent);
                set_progressbar_status(file.identifier, "uploading");
                on_finish(spark.end());
            }
        };

        fileReader.onerror = function () {
            $("#error-modal-title").html("File Upload Error");
            $("#error-modal-body").html("Could not upload file because of an error generating it's MD5 hash. Please reload the page and try again.");
            $("#error-modal").modal();
        };

        function loadNext() {
            var start = currentChunk * chunkSize,
                end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;

            fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
        }

        loadNext();
    }

    function start_upload(file) {
        var identifier = get_unique_identifier(file);
        //different versions of firefox have different field names
        var filename = file.webkitRelativePath || file.fileName || file.name;
        file.identifier = identifier;
        paused[identifier] = false;

        var number_chunks = Math.ceil(file.size / chunk_size);

        var mosaic_info = {};
        mosaic_info.identifier = identifier;
        mosaic_info.filename = filename;
        mosaic_info.uploaded_chunks = 0;
        mosaic_info.number_chunks = number_chunks;
        mosaic_info.size_bytes = file.size;
        mosaic_info.bytes_uploaded = 0;
        mosaic_info.status = 'HASHING';

        function on_finish(md5_hash) {
            $('#progress-bar-' + identifier).addClass("hashed");

            set_actions(identifier, ["pause", "delete"]);

            $('#progress-bar-' + identifier).addClass('progress-bar-animated');
            initialize_mosaic_dropdowns();

            file.md5_hash = md5_hash;
            console.log("got md5_hash: '" + md5_hash + "'");
            var xhr = new XMLHttpRequest();

            xhr.open('POST', './request.php');
            xhr.onload = function() {
                console.log("New upload response: " + xhr.responseText);
                var response = JSON.parse(xhr.responseText);

                var filename = file.webkitRelativePath || file.fileName || file.name;

                //check and see if there was an error in the response!
                if (response.err_title !== undefined) {
                    display_error_modal(response.err_title, response.err_msg + "<br>On file: '" + filename + "'");
                    $('#uploading-mosaic-row-' + identifier).remove();

                    } else {
                        var mosaic_info = response.mosaic_info;
                        mosaic_info.file = file; //set the file in the response mosaic_info so it can be used later
                        upload_chunk(mosaic_info);
                        }
                };

            var formData = new FormData();
            formData.append("id_token", id_token);
            formData.append("request", "NEW_UPLOAD");
            formData.append("filename", filename);
            formData.append("identifier", identifier);
            formData.append("number_chunks", number_chunks);
            formData.append("size_bytes", file.size);
            formData.append("md5_hash", md5_hash);
            xhr.send(formData);
            };

        var md5_hash = get_md5_hash(file, on_finish);
    }


    addUpload(file) {
        const filename = file.webkitRelativePath || file.fileName || file.name;
        const progressSize = 0;
        const status = "Hashing";
        const totalSize = file.size;
        console.log("adding filename: '" + filename + "'");

        let uploads = this.state.uploads;
        for (let i = 0; i < this.state.uploads.length; i++) {
            if (uploads[i].filename == filename && uploads[i].totalSize == totalSize) {
                console.log("file already exists, not adding!");
                return;
            }
        }

        uploads.push({
            id : get_unique_identifier(filename, totalSize);
            filename : filename,
            status : status,
            totalSize : totalSize,
            progressSize : progressSize
        });

        const state = this.state;
        this.setState(
            state : state
        );
    }

    updateUpload() {
    }


    triggerInput() {
        var uploadsCardElement = this;

        $('#mosaic-file-input').trigger('click');

        $('#mosaic-file-input:not(.bound)').addClass('bound').change(function() {
            //console.log("number files selected: " + $(this).files.length);
            console.log( this.files );

            if (this.files.length > 0) { 
                var file = this.files[0];
                var filename = file.webkitRelativePath || file.fileName || file.name;

                if (!filename.match(/^[a-zA-Z0-9_.-]*$/)) {
                    display_error_modal("Malformed Filename", "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");
                } else {
                    uploadsCardElement.addUpload(file);
                    //start_upload(file);
                }    
            }    
        });  
    }

    render() {
        const hidden = this.props.hidden;
        const hiddenStyle = {
            display : "none"
        };

        return (
            <div className="card-body" hidden={hidden}>
                {
                    this.state.uploads.map((file, index) => {
                        return (
                            <Upload key={file.id} filename={file.filename} status={file.status} totalSize={file.totalSize} progressSize={file.progressSize} />
                        );
                    })
                }
                <div className="d-flex justify-content-center mt-2">
                    <div className="p-0">
                        <input id ="mosaic-file-input" type="file" style={hiddenStyle} />
                        <button id="upload-flights-button" className="btn btn-primary" onClick={() => this.triggerInput()}>
                            <i className="fa fa-upload"></i> Upload Flights
                        </button>
                    </div>
                </div>

            </div>
        );
    }
}


