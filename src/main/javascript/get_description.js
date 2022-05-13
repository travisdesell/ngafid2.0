import {errorModal} from "./error_modal";

export default function GetDescription(eventName) {
    var text = "";

        $.ajax({
                type: 'GET',
                url: '/protected/get_event_description',
                data : {eventName : eventName},
                dataType : 'json',
                success : function(response) {
                    console.log("received response: " + response);

                    $('#loading').hide();

                    if (response.err_msg) {
                        errorModal.show(response.err_title, response.err_msg);
                    }

                    text = response + "";
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Getting Event Description", errorThrown);
                },
                async: false
            });

        console.log("returning text: " + text);
        return text;
}