import { showErrorModal } from "./error_modal";

/**
 * Synchronously fetches every event definition description from the server, surfacing any error via the error modal.
 * @returns the server response containing all event descriptions, or null if the request produced no result
 */
export default function GetAllDescriptions() {
  let descriptions = null;

  console.log("Getting  all descriptions from the server.");
  $.ajax({
    type: "GET",
    url: "/api/event/definition/description",
    async: false,
    success: (response) => {
      console.log(`Received response: ${response}`);

      $("#loading").hide();

      if (response.err_msg) {
        showErrorModal(response.err_title, response.err_msg);
      }

      descriptions = response;
    },
    error: (jqXHR, textStatus, errorThrown) => {
      showErrorModal("Error Getting Event Description", errorThrown);
    },
  });

  console.log(`Returning descriptions: ${descriptions}`);
  return descriptions;
}
