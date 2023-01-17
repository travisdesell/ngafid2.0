class AirsyncUploadsCard extends React.Component {
    constructor(props) {
        super(props);

        let uploads = props.uploads;
        if (uploads == undefined) uploads = [];

        this.state = {
            uploads : uploads
        };
    }

    getUploadsCard() {
        return this;
    }

    removeUpload(file) {
        console.log("does nothing");
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

                if (!filename.match(/^[a-zA-Z0-9_.-]*$/)) {
                    display_error_modal("Malformed Filename", "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods.");
                } else {
                    uploadsCard.addUpload(file);
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
                    this.state.uploads.map((uploadInfo, index) => {
                        return (
                            <Upload uploadInfo={uploadInfo} key={uploadInfo.identifier} />
                        );
                    })
                }
                <div className="d-flex justify-content-center mt-2">
                    <div className="p-0">
                        <input id ="upload-file-input" type="file" style={hiddenStyle} />
                        <button id="upload-flights-button" className="btn btn-primary" onClick={() => this.triggerInput()}>
                            <i className="fa fa-upload"></i> Upload Flights
                        </button>
                    </div>
                </div>

            </div>
        );
    }
}


