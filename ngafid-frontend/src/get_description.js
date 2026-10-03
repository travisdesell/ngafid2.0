import { showErrorModal } from "./error_modal";

const descriptions = {};

/**
 * Returns the description for a named event, serving it from an in-memory cache or fetching it synchronously otherwise.
 * @param eventName name of the event definition whose description is requested
 * @returns the event's description text, caching the result for subsequent lookups
 */
export default function GetDescription(eventName) {
  if (descriptions[eventName]) return descriptions[eventName];

  console.log(`Getting description for ${eventName} from server.`);
  $.ajax({
    type: "GET",
    url: `/api/event/definition/by-name/${encodeURIComponent(eventName)}/description`,
    dataType: "text",
    async: false,
    success: (response) => {
      console.log(`Received response: ${response}`);

      $("#loading").hide();

      if (response.err_msg) {
        showErrorModal(response.err_title, response.err_msg);
      }

      descriptions[eventName] = response;
    },
    error: (jqXHR, textStatus, errorThrown) => {
      showErrorModal("Error Getting Event Description", errorThrown);
    },
  });

  console.log(`Returning text: ${descriptions[eventName]}`);
  return descriptions[eventName];
}
