// ngafid-frontend/src/multifleet/multifleet_invites.tsx
import React from "react";
import type { MultifleetInvite } from "../types";
import { showConfirmModal } from "../confirm_modal";
import { showErrorModal } from "../error_modal";


interface MultifleetInviteDecline extends MultifleetInvite {
    removeMultifleetInviteLocally: (fleetName: string) => void;
}
const declineInvite = ({ fleetName, inviteEmail, removeMultifleetInviteLocally }: MultifleetInviteDecline) => {

    const onConfirmDecline = () => {

        $.ajax({
            type: 'POST',
            url: `/api/user/multifleet-invites/decline`,
            async: true,
            data: {
                fleetName,
            },
            success: () => {
                removeMultifleetInviteLocally(fleetName);
                console.log('Successfully declined invite to fleet:', fleetName);
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log('Error declining invite:', errorThrown);
                showErrorModal("Error accepting invite:", errorThrown);
            }
        });

    };

    showConfirmModal(
        "Decline Invite",
        `Are you sure you want to decline the invite to join the fleet "${fleetName}"?`,
        () => {
            console.log("Attempting to decline invite for:", fleetName, inviteEmail);
            onConfirmDecline();
        }
    );

};


interface MultifleetInviteAccept extends MultifleetInvite {
    removeMultifleetInviteLocally: (fleetName: string) => void;
}
const acceptInvite = ({ fleetName, inviteEmail, removeMultifleetInviteLocally }: MultifleetInviteAccept) => {

    const onConfirmAccept = () => {

        $.ajax({
            type: 'POST',
            url: `/api/user/multifleet-invites/accept`,
            async: true,
            data: {
                fleetName,
            },
            success: () => {
                removeMultifleetInviteLocally(fleetName);
                console.log('Successfully accepted invite to fleet:', fleetName);
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log('Error accepting invite:', errorThrown);
                showErrorModal("Error accepting invite:", errorThrown);
            }
        });

    };

    showConfirmModal(
        "Accept Invite",
        `Are you sure you want to accept the invite to join the fleet "${fleetName}"?`,
        () => {
            console.log("Attempting to accept invite for:", fleetName, inviteEmail);
            onConfirmAccept();
        }
    );

};


interface MultifleetInviteBadgeProps {
    fleetName: string;
    inviteEmail: string;
    removeMultifleetInviteLocally: (fleetName: string) => void;
}
function MultifleetInviteBadge({ fleetName, inviteEmail, removeMultifleetInviteLocally }: MultifleetInviteBadgeProps) {

    return (
        <div key={fleetName} className="flex flex-row bg-[var(--c_tag_badge)] rounded-lg items-center p-2 pr-3">

            <div className="mr-3 ml-2">
                <div className="flex flex-row gap-2 font-bold items-center">
                    <i className="fa fa-envelope" />
                    {fleetName}
                </div>
                <span className="opacity-50">From {inviteEmail}</span>
            </div>

            {/* Buttons */}
            <div className="ml-1 flex flex-row gap-2">

                {/* Accept Button */}
                <button
                    className="btn btn-primary"
                    onClick={() => acceptInvite({ fleetName, inviteEmail, removeMultifleetInviteLocally })}
                >
                    <i className="fa fa-check mr-2" />
                    Accept
                </button>

                {/* Decline Button */}
                <button
                    className="btn btn-danger"
                    onClick={() => declineInvite({ fleetName, inviteEmail, removeMultifleetInviteLocally })}
                >
                    <i className="fa fa-times mr-2" />
                    Decline
                </button>

            </div>

        </div>
    );

}


type MultifleetInviteProps = {
    invites: MultifleetInvite[];
    removeMultifleetInviteLocally: (fleetName: string) => void;
};
export function MultifleetInvites({ invites, removeMultifleetInviteLocally }: MultifleetInviteProps) {

    console.log("Rendering Multifleet Invite Badges: ", invites);

    //No invites, show nothing
    if (invites.length === 0)
        return null;

    //Otherwise, show invite badges
    return (
        <div className="card-body">
            <div className="col" style={{ padding: "0 0 0 0" }}>
                <div className="card-alt card">
                    <h6 className="card-header">
                        <i className="fa fa-envelope mr-2" />
                        Fleet Invites
                    </h6>
                    <div className="form-group my-4 px-4">
                        <div className="flex flex-row flex-wrap items-center justify-start gap-4">
                            {invites.map((invite) => (
                                <MultifleetInviteBadge
                                    key={invite.fleetName}
                                    fleetName={invite.fleetName}
                                    inviteEmail={invite.inviteEmail}
                                    removeMultifleetInviteLocally={removeMultifleetInviteLocally}
                                />
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );

};

export default MultifleetInvites;