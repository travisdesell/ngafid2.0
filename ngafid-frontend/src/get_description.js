import {errorModal} from "./error_modal";

const descriptions = {};


export default function GetDescription(eventName) {

    if (descriptions[eventName])
        return descriptions[eventName];

    console.log(`Getting description for ${  eventName  } from server.`);
    $.ajax({
        type: 'GET',
        url: `/api/event/definition/by-name/${encodeURIComponent(eventName)}/description`,
        dataType: 'text',
        async: false,
        success: (response) => {
            console.log(`Received response: ${  response}`);

            $('#loading').hide();

            if (response.err_msg) {
                errorModal.show(response.err_title, response.err_msg);
            }

            descriptions[eventName] = response;
        },
        error: (jqXHR, textStatus, errorThrown) => {
            errorModal.show("Error Getting Event Description", errorThrown);
        },
    });

    console.log(`Returning text: ${  descriptions[eventName]}`);
    return descriptions[eventName];
}
