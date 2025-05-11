import {errorModal} from "./error_modal";


export default function GetAllDescriptions() {
    let descriptions = null;

    console.log("Getting  all descriptions from the server.");
    $.ajax({
        type: 'GET',
        url: '/api/event/definition/description',
        data: {},
        dataType: 'json',
        success: function (response) {
            console.log("received response: " + response);

            $('#loading').hide();

            if (response.err_msg) {
                errorModal.show(response.err_title, response.err_msg);
            }

            descriptions = response;
        },
        error: function (jqXHR, textStatus, errorThrown) {
            errorModal.show("Error Getting Event Description", errorThrown);
        },
        async: false
    });

    console.log("Returning descriptions: " + descriptions);
    return descriptions;
}