// ngafid-frontend/src/multifleet/multifleet_select.tsx
import React from "react";

import { showErrorModal } from "../error_modal";
import type { accessType, MultifleetSelectWithAccess } from "../types";



import '../index.css';          //<-- include Tailwind
import { showConfirmModal } from "../confirm_modal";


function leaveSelectedFleet() {

    $.ajax({
        type: 'PUT',
        url: `/api/user/leave-fleet`,
        async: true,
        success: (response) => {
            console.log("Successfully left selected fleet:", response);
            //Reload page to update everything
            window.location.reload();
        },
        error: (jqXHR, textStatus, errorThrown) => {
            console.error("Error leaving the selected fleet:", errorThrown);
            showErrorModal("Error Leaving Selected Fleet", errorThrown);
        }
    });


}


function confirmLeaveSelectedFleet() {

    showConfirmModal(
        "Confirm Leave Fleet",
        "Are you sure you want to leave this fleet? You will lose access to its data.",
        () => { leaveSelectedFleet(); }
    );

}



interface MultifleetSelectBadgeProps {
    isSelected: boolean;
    fleetName: string;
    fleetId: number;
    accessType: accessType;
    updateSelectedFleet: (fleetId: number) => void;
}
function MultifleetSelectBadge({ isSelected, fleetName, fleetId, accessType, updateSelectedFleet }: MultifleetSelectBadgeProps) {

    const isDeniedOrWaiting = (accessType === "DENIED" || accessType === "WAITING");
    const selectDisabled = (isDeniedOrWaiting);
    const accessIcon = (()=>{
        switch (accessType) {
            case "DENIED": return 'fa-exclamation-circle';
            case "WAITING": return 'fa-hourglass-half';
            case "VIEW": return 'fa-eye';
            case "UPLOAD": return 'fa-upload';
            case "MANAGER": return 'fa-users';
            default: return 'fa-question';
        }
    })();

    return (
        <div
            key={fleetName}
            className={`
                flex flex-row bg-[var(--c_tag_badge)] rounded-lg items-center p-2 pr-3
                ${isDeniedOrWaiting ? 'opacity-25 pointer-events-none' : ''}
                ${isSelected ? 'outline-2 outline-blue-500' : ''}
            `}
        >

            <div className="mr-3 ml-2">
                <div className="flex flex-row gap-2 font-bold items-center">
                    <i className="fa fa-paper-plane" />
                    {fleetName}
                </div>
                <span className="opacity-50 capitalize">
                    <i className={`fa ${accessIcon} mr-2`} />
                    {accessType.toLowerCase()}
                </span>
            </div>

            {/* Select Button */}
            {
                (!isSelected)
                &&
                <button
                    className={`ml-1 btn btn-primary ${isDeniedOrWaiting ? 'button-disabled' : ''}`}
                    disabled={selectDisabled}
                    onClick={() => {updateSelectedFleet(fleetId);}}
                >
                    <i className="fa fa-check mr-2" />
                    Select
                </button>
        }

        </div>
    );

}




type MultifleetSelectProps = {
    fleetsWithAccess: MultifleetSelectWithAccess[];
    fleetSelected: number;
    updateSelectedFleet: (fleetId: number) => void;
};
export function MultifleetSelect({ fleetsWithAccess, fleetSelected, updateSelectedFleet }: MultifleetSelectProps) {

    console.log("Rendering MultifleetSelect with fleets:", fleetsWithAccess);
    const fleetsWithAccessCount = fleetsWithAccess.length;

    //No fleets to select from, something is wrong
    if (fleetsWithAccessCount === 0) {
        console.log("Fleets wtih access count is 0, showing error modal");
        showErrorModal("Error loading fleet selection!", "You do not have access to any fleets! Please contact an administrator.");
        return null;
    }

    //No (other) fleets to select from, show nothing
    if (fleetsWithAccessCount === 1)
        return null;

    //Check if the user is a manager of the selected fleet
    const selectedFleetAccess = fleetsWithAccess.find(fleet => fleet.fleetId === fleetSelected);
    const managesSelectedFleet = (selectedFleetAccess?.accessType === "MANAGER");

    //Check if the fleets (other than the selected one) are all denied or waiting
    const otherFleets = fleetsWithAccess.filter(fleet => fleet.fleetId !== fleetSelected);
    const allOtherFleetsDeniedOrWaiting = otherFleets.every(fleet => fleet.accessType === "DENIED" || fleet.accessType === "WAITING");

    //Check if the leave button needs to be disabled
    const leaveDisabled = (managesSelectedFleet || allOtherFleetsDeniedOrWaiting);
    const leaveTitle =
        managesSelectedFleet ? "Unable to leave a Fleet you manage!"
        : allOtherFleetsDeniedOrWaiting ? "No other non-waiting/non-denied fleets to select!"
        : "Leave the current fleet.";

    //Otherwise, show fleet select badges
    return (
        <div className="card-body">
            <div className="col" style={{ padding: "0 0 0 0" }}>
                <div className="card-alt card">

                    {/* Card Header Area */}
                    <h6 className="card-header flex flex-row items-center justify-between">

                        {/* Fleet Selection Header */}
                        <div>
                            <i className="fa fa-paper-plane mr-2" />
                            <span>Fleet Selection</span>
                        </div>

                        {/* Leave Current Fleet Button */}
                        <button
                            className={`btn btn-danger ${leaveDisabled ? 'button-disabled' : ''}`}
                            disabled={leaveDisabled}
                            aria-label="Leave Current Fleet"
                            title={leaveTitle}
                            onClick={() => { confirmLeaveSelectedFleet(); }}
                        >
                            <i className="fa fa-sign-out mr-2" />
                            Leave Current
                        </button>
                    </h6>

                    {/* Card Body Area */}
                    <div className="form-group my-4 px-4">
                        <div className="flex flex-row flex-wrap items-center justify-start gap-4">
                            {fleetsWithAccess.map((fleetAccess) => (
                                <MultifleetSelectBadge
                                    key={fleetAccess.fleetName}
                                    fleetName={fleetAccess.fleetName}
                                    fleetId={fleetAccess.fleetId}
                                    accessType={fleetAccess.accessType}
                                    isSelected={fleetAccess.fleetId === fleetSelected}
                                    updateSelectedFleet={updateSelectedFleet}
                                />
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );

}

export default MultifleetSelect;