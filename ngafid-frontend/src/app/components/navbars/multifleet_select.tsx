// ngafid-frontend/src/app/components/navbars/multifleet_select.tsx

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { useAuth } from "@/components/providers/auth_provider";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { ChevronDown, UsersRound } from "lucide-react";
import type { AccessType, FleetAccess } from "src/types";

const log = getLogger("MultifleetSelect", "black", "Component");


type FleetAccessInput = AccessType | Pick<FleetAccess, "accessType"> | undefined | null;

function normalizeAccessType(access: FleetAccessInput): AccessType | undefined {
    if (!access)
        return undefined;

    if (typeof access === "string")
        return access;

    return access.accessType;
}

export const fleetAccessAllowed = (access: FleetAccessInput) => {

    const accessType = normalizeAccessType(access);

    // Missing access info -> False
    if (!accessType)
        return false;

    const allowedTypes: AccessType[] = ["VIEW", "UPLOAD", "MANAGER"];
    return allowedTypes.includes(accessType);

}

export const fleetSelectable = (fleetIDCurrent: number, fleetIDTarget: number, access: FleetAccessInput) => {

    // Fleet access not allowed -> Not selectable
    if (!fleetAccessAllowed(access))
        return false;

    // Fleet already selected -> Not selectable
    if (fleetIDCurrent === fleetIDTarget)
        return false;

    return true;

}



export default function MultifleetSelect() {

    const { setModal } = useModal();
    const { user } = useAuth();

    const fleetAccess = user?.fleetAccess ?? [];
    const currentFleetId = user?.fleet?.id ?? -1;
    const hasAnyFleetsSelectable = fleetAccess.length > 0;

    async function switchToFleet(fleetId: number) {

        log("Switching to fleet with ID:", fleetId);

        await fetch("/api/user/select-fleet", {
            method: "PUT",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({ fleetIdSelected: `${fleetId}` }).toString(),
        })
            .then(response => {

                log("Received response from switch fleet request:", response);

                // Response not OK
                if (!response.ok) {

                    throw new Error(`Failed to switch fleets. Server responded with status ${response.status}: ${response.statusText}`);

                }

                log("Successfully switched fleet:", response);
                window.location.reload();
            })
            .catch(error => {
                setModal(ErrorModal, { title: "Error switching fleet", message: error.message || "An unknown error occurred while switching fleets." });
            });

    }

    function render() {

        log("Rendering MultifleetSelect...");

        return <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghostMono" className="-mr-3 -ml-3 w-full max-w-48 overflow-hidden **:text-ellipsis!">
                    <UsersRound />
                    <div className="w-full text-ellipsis overflow-hidden">
                        <span className="w-full">{user?.fleet?.name ?? "(No Fleet!)"}</span>
                    </div>
                    <ChevronDown />
                </Button>
            </DropdownMenuTrigger>
            {
                (hasAnyFleetsSelectable)
                &&
                <DropdownMenuContent>
                    
                    {/* Display All Available Fleets */}
                    {/* {userFleetAccess.map((fleet: FleetAccess) => (
                        <DropdownMenuItem
                            key={fleet.fleetId}
                            onSelect={() => switchToFleet(fleet.fleetId)}
                            disabled={!fleetSelectable(user?.fleet?.id ? parseInt(String(user.fleet?.id)) : -1, fleet.fleetId, fleet.accessType )}
                        >
                            <span>{fleet.fleetName}</span>
                        </DropdownMenuItem>
                    ))} */}
                    {
                        fleetAccess.map((fleet: FleetAccess) => (
                            <DropdownMenuItem
                                key={fleet.fleetId}
                                onSelect={() => switchToFleet(fleet.fleetId)}
                                disabled={!fleetSelectable(currentFleetId, fleet.fleetId, fleet)}
                            >
                                <span>{fleet.fleetName}</span>
                            </DropdownMenuItem>
                        ))
                    }

                </DropdownMenuContent>
            }
        </DropdownMenu>

    }

    return render();

}
