import {errorModal} from "./error_modal";

var descriptions = {};


export default function GetDescription(eventName) {
    if (descriptions[eventName]) {
        return descriptions[eventName];
    }

    var text = "";

    console.log("Getting description for " + eventName + " from server.");
    $.ajax({
        type: 'GET',
        url: `/api/event/definition/by-name/${encodeURIComponent(eventName)}/description`,
        dataType: 'text',
        success: function (response) {
            console.log("received response: " + response);

            $('#loading').hide();

            if (response.err_msg) {
                errorModal.show(response.err_title, response.err_msg);
            }

            descriptions[eventName] = response;
        },
        error: function (jqXHR, textStatus, errorThrown) {
            errorModal.show("Error Getting Event Description", errorThrown);
        },
        async: false
    });

    console.log("Returning text: " + descriptions[eventName]);
    return descriptions[eventName];
}
