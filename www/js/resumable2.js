var mosaics = [];

var paused = [];

var chunk_size = 1 * 1024 * 1024; //5MB

function restart_uploads() {
    console.log("number of uploads: " + Object.keys(mosaics).length);
    for (var i in mosaics) {
        console.log("key: " + i);
        console.log("restarting upload for mosaic: " + mosaics[i].filename);
        upload_chunk(mosaics[i]);
    }
}

function set_progressbar_percent(identifier, percent) {
    $("#progress-bar-" + identifier).attr("aria-valuenow", percent);
    $("#progress-bar-" + identifier).attr("style", "width: " + percent + "%");
    $("#progress-bar-" + identifier).text(Number(percent).toFixed(2) + "%");
}

function set_progressbar_color(identifier, color_class) {
    $("#progress-bar-" + identifier).removeClass('bg-warning');
    $("#progress-bar-" + identifier).removeClass('bg-info');
    $("#progress-bar-" + identifier).removeClass('bg-success');
    $("#progress-bar-" + identifier).removeClass('bg-danger');
    $("#progress-bar-" + identifier).addClass(color_class);
}

function set_progressbar_status(identifier, text) {
    $("#mosaic-status-text-" + identifier).text(text);
}

function get_unique_identifier(file) {
    var relativePath = file.webkitRelativePath || file.fileName || file.name; // Some confusion in different versions of Firefox
    var size = file.size;

    return(size + '-' + relativePath.replace(/[^0-9a-zA-Z_-]/img, ''));
}


function set_actions(identifier, actions) {
    $("#actions-td-" + identifier).html("");

    var actions_html = "";
	for (i in actions) {
        var action = actions[i];
        console.log("setting action " + i +  " -- '" + action + "'");

        if (action === 'resume') {
            actions_html += '<button mosaic_identifier="' + identifier + '" class="resume-download-button btn btn-primary btn-sm btn-success"><i class="fa fa-play"></i></button>';
        } else if (action === 'pause') {
			actions_html += '<button mosaic_identifier="' + identifier + '" class="pause-download-button btn btn-primary btn-sm btn-success"><i class="fa fa-pause"></i></button>';
        } else if (action === 'delete') {
            actions_html += '<button mosaic_identifier="' + identifier + '" class="delete-mosaic-button btn btn-primary btn-sm btn-danger float-right"><i class="fa fa-times"></i></button>';
        }
    }

    $("#actions-td-" + identifier).html(actions_html);
    initialize_mosaic_dropdowns();
}

function initialize_mosaic_dropdowns() {
    $('.resume-download-button:not(.bound)').addClass('bound').click(function() {
		var identifier = $(this).attr("mosaic_identifier");

		if (!$('#progress-bar-' + identifier).hasClass("hashed")) {
			$('#mosaic-file-input').trigger('click');
		} else {

			console.log("resuming mosaic " + identifier);
			set_progressbar_status(identifier, "uploading");

			$(".resume-download-button[mosaic_identifier='" + identifier + "']").remove();
			paused[identifier] = false;
			upload_chunk(mosaics[identifier]);

            set_actions(identifier, ["pause", "delete"]);

			$('#progress-bar-' + identifier).addClass('progress-bar-animated');
			initialize_mosaic_dropdowns();
		}
    });

    $('.pause-download-button:not(.bound)').addClass('bound').click(function() {
		var identifier = $(this).attr("mosaic_identifier");
		console.log("clicked pause button for mosaic with id " + identifier);
   		$('.pause-download-button[mosaic_identifier="' + identifier + '"]').remove();
		set_progressbar_status(identifier, "upload paused");

		paused[identifier] = true;
		console.log("setting paused for '" + identifier + "'");

        set_actions(identifier, ["resume", "delete"]);

		$('#progress-bar-' + identifier).removeClass('progress-bar-animated');
		initialize_mosaic_dropdowns();
    });

    $('.delete-mosaic-button:not(.bound)').addClass('bound').click(function() {
		var identifier = $(this).attr("mosaic_identifier");
        console.log("clicked delete button for mosaic with id " + identifier);

        console.log(mosaics);
        var mosaic = mosaics[identifier];
        console.log(mosaic);
        var filename = mosaic.filename;

        $("#confirm-delete-button").attr("md5_hash", mosaic.md5_hash);
        $("#confirm-delete-button").attr("mosaic_id", mosaic.id);
        $("#confirm-delete-button").attr("identifier", identifier);

        $("#confirm-delete-modal-body").html("<p><b>Are you sure you wish to delete '" + filename + "'?</b></p><p>This action cannot be undone. To get the mosaic back in OURepository it will need to be re-uploaded.</p>");
        $("#confirm-delete-modal").modal();
    });

}

function add_mosaic_to_table(mosaic_info) {
    var progress_text = "<tr id='uploading-mosaic-row-" + mosaic_info.identifier + "'>"
        + "<td style='vertical-align: middle;'>" + mosaic_info.filename + "</td>"
        + "<td style='width:35%; vertical-align: middle;'><div class='progress'> <div id='progress-bar-" + mosaic_info.identifier + "' class='progress-bar progress-bar-striped progress-bar-animated bg-warning' role='progressbar' aria-valuenow='0' aria-valuemin='0' aria-valuemax='100' style='width:0.00%'></div></div></td>"
        + "<td style='vertical-align: middle;'><div id='progress-bar-text-" + mosaic_info.identifier + "'>0/" + (Number(mosaic_info.size_bytes / 1024).toFixed(0)).toLocaleString() + "kB (0.00%)</div></td>"
        + "<td style='vertical-align: middle;' id='mosaic-status-text-" + mosaic_info.identifier + "'>hashing</td>"

		+ "<td id='actions-td-" + mosaic_info.identifier + "' style='padding-top:0px; padding-bottom:0px; vertical-align: middle; width:88px;'></td>"

        + "</tr>";

    console.log("appending progress text to mosaics table!");
    if ($("#progress-bar-" + mosaic_info.identifier).length == 0) {
        $("#uploading-mosaics-table").append(progress_text);
    } else {
        set_progressbar_color(mosaic_info.identifier, 'bg-warning');
        set_progressbar_percent(mosaic_info.identifier, 0);
        set_progressbar_status(mosaic_info.identifier, "calculating md5 hash");
    }
}

function get_md5_hash(file, on_finish) {

    var blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice,
    chunkSize = 2097152,                             // Read in chunks of 2MB
    chunks = Math.ceil(file.size / chunkSize),
    currentChunk = 0,
    spark = new SparkMD5.ArrayBuffer(),
    fileReader = new FileReader();

	$('#actions-td-' + file.identifier).html("");
	$('#progress-bar-' + file.identifier).addClass('progress-bar-animated');
	set_progressbar_status(file.identifier, "hashing");

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

    add_mosaic_to_table(mosaic_info);
	initialize_mosaic_dropdowns();

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

function upload_chunk(mosaic_info) {
    //store the mosaic info in case the upload needs to be restarted
    mosaics[mosaic_info.identifier] = mosaic_info;
    var file = mosaic_info.file;

    if (paused[mosaic_info.identifier] == true) return;

    //console.log(response);

    var number_chunks = parseInt(mosaic_info.number_chunks); 
    var filename = mosaic_info.filename;

    var chunk_status = mosaic_info.chunk_status;
    var chunk_number = chunk_status.indexOf("0");
    //console.log("chunk status: '" + chunk_status + "'");
    console.log("next chunk: " + chunk_number + " of " + number_chunks);

    set_progressbar_color(file.identifier, 'bg-info');
    set_progressbar_percent(file.identifier, 100.0 * (chunk_number / number_chunks));
    set_progressbar_status(file.identifier, "uploading");

    var fileReader = new FileReader();

    var startByte = parseInt(chunk_number) * parseInt(chunk_size);
    var endByte = Math.min(parseInt(startByte) + parseInt(chunk_size), file.size);
    //console.log("startByte: " + startByte + ", endByte: " + endByte + ", chunk_size: " + chunk_size);

    var func = (file.slice ? 'slice' : (file.mozSlice ? 'mozSlice' : (file.webkitSlice ? 'webkitSlice' : 'slice')));
    var bytes = file[func](startByte, endByte, void 0);

    //console.log(bytes);


    var xhr = new XMLHttpRequest();
    xhr.open('POST', './request.php');
    //xhr.setRequestHeader('Content-Type', 'application/octet-stream');
    xhr.onload = function() {
        //console.log("Upload response: " + xhr.responseText);

        var response = JSON.parse(xhr.responseText);
        if (response.err_title !== undefined) {
            display_error_modal(response.err_title, response.err_msg + "<br>On file: '" + filename + "'");

        } else {
            var mosaic_info = response.mosaic_info;
            mosaic_info.file = file; //set the fileObject so we can use it for restarts

            var bytes_uploaded = mosaic_info.bytes_uploaded;
            var size_bytes = mosaic_info.size_bytes;

            var percent = (bytes_uploaded / size_bytes) * 100.0;

            set_progressbar_percent(file.identifier, percent);

            $("#progress-bar-text-" + file.identifier).html(Number(Number(bytes_uploaded / 1024).toFixed(0)).toLocaleString() + "/" + Number((file.size / 1024).toFixed(0)).toLocaleString() + "kB (" + Number(percent).toFixed(2) + "%)");

             var number_chunks = Math.ceil(file.size / chunk_size);
             console.log("uploaded chunk " + chunk_number + " of " + number_chunks);

             var chunk_status = mosaic_info.chunk_status;
             chunk_number = chunk_status.indexOf("0");
             //console.log("chunk status: '" + chunk_status + "'");
             //console.log("next chunk: " + chunk_number);
             //chunk_number = chunk_number + 1;

             if (chunk_number > -1) {
                 //console.log("uploading next chunk with response:");
                 //console.log(response);

                 upload_chunk(mosaic_info);
             } else {
                 set_actions(mosaic_info.identifier, ["delete"]);

                 $('#progress-bar-' + mosaic_info.identifier).removeClass('progress-bar-animated');
                 set_progressbar_color(mosaic_info.identifier, 'bg-success');
                 set_progressbar_status(mosaic_info.identifier, "queued for tiling");

                 update_tiling_progress(mosaic_info);
             }
        }
    };

    var formData = new FormData();
    formData.append("id_token", id_token);
    formData.append("request", "UPLOAD");
    formData.append("chunk", chunk_number);
    formData.append("identifier", file.identifier);
    formData.append("md5_hash", file.md5_hash);
    formData.append("part", bytes, file.fileName);
    xhr.send(formData);
}

function ordinal_suffix(n) {
    var original = n;
    n = parseInt(n) % 100; // protect against large numbers
    if (n < 11 || n > 13) {
        switch(n % 10) {
            case 1: return original + 'st';
            case 2: return original + 'nd';
            case 3: return original + 'rd';
        }   
    }   
    return original + 'th';
}

jQuery.fn.sort_sub = function sort_sub(sort_by) {
    $("> " + sort_by, this[0]).sort(dec_sort).appendTo(this[0]);
    function dec_sort(a, b){
        var contentA = $(a).attr('data-sort');
        var contentB = $(b).attr('data-sort');
        return (contentA < contentB) ? -1 : (contentA > contentB) ? 1 : 0;
    }
}


function update_tiling_progress(mosaic_info) {
    function process_update(responseText) {
        //console.log("response was: " + responseText);
        console.log("updating tiling progress");

        var response = JSON.parse(responseText);
        if (response.err_msg) {
            display_error_modal(response.err_title, response.err_msg);  
            return;
        }
        var mosaic_info = response.mosaic_info;

        if (mosaic_info.status == 'TILED') {
            $('#uploading-mosaic-row-' + mosaic_info.identifier).remove();

            function add_card(responseText) {
                var response = JSON.parse(responseText);
                if (response.err_msg) {
                    display_error_modal(response.err_title, response.err_msg);  
                    return;
                }

                //console.log("response was: " + response.html);
                //console.log("mosaic card row is:");
                //console.log( $("#mosaic-card-row") );
                $(".mosaic-card-row[folder_id='-1']").append(response.html);
                $(".mosaic-card-row[folder_id='-1']").sort_sub("div");

                initialize_mosaic_cards();
            }

            serverRequest("MOSAIC_CARD&mosaic_id=" + mosaic_info.id, add_card);

        } else if (mosaic_info.status == 'TILING') {
            set_progressbar_percent(mosaic_info.identifier, mosaic_info.tiling_progress);
            set_progressbar_color(mosaic_info.identifier, 'bg-warning');
            set_progressbar_status(mosaic_info.identifier, "tiling");

            setTimeout(request_update, 5000);
        } else if (mosaic_info.status == 'UPLOADED') {
            set_progressbar_status(mosaic_info.identifier, ordinal_suffix(mosaic_info.queue_position) + " in tiling queue");
            setTimeout(request_update, 5000);
        }
    }

    function request_update() {
        serverRequest("TILE_PROGRESS&md5_hash=" + mosaic_info.md5_hash, process_update);
    }

    setTimeout(request_update, 5000);
}
