// ngafid-frontend/src/app/components/navbars/multifleet_select.tsx

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { fetchJson } from "@/fetchJson";
import { ChevronDown, UsersRound } from "lucide-react";
import { useEffect, useState } from "react";

const log = getLogger("MultifleetSelect", "black", "Component");

type Fleet = {
    name: string;
    id: string;
}

type FleetAccess = {
    fleetName: string;
    userId: number;
    fleetId: number;
    accessType: string;
}

export default function MultifleetSelect() {

    const { setModal } = useModal();

    let [userFleetCurrent, setUserFleetCurrent] = useState<Fleet|null>(null);
    let [userFleetAccess, setUserFleetAccess] = useState<FleetAccess[]>([]);

    useEffect(() => {

        const fetchCurrentFleet = async () => {

            log("Fetching User Current Fleet...");
            const newUserFleetCurrent = await fetchJson.get("/api/fleet");
            log("Fetched User Current Fleet:", newUserFleetCurrent);
            setUserFleetCurrent(newUserFleetCurrent);

        };

        const fetchFleetAccess = async () => {

            // NEW REQUESTS:
            log("Fetching User Fleet Access...");
            const newUserFleetAccess = await fetchJson.get("/api/user/fleet-access");
            log("Fetched User Fleet Access:", newUserFleetAccess);
            setUserFleetAccess(newUserFleetAccess);
            
        }

        fetchCurrentFleet();
        fetchFleetAccess();
        
    }, []);

    const hasAnyFleetsSelectable = (userFleetAccess.length > 0);

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
                <Button variant="ghost" className="-mr-3 -ml-3">
                    <UsersRound />
                    <span>{userFleetCurrent?.name ?? "(No Fleet!)"}</span>
                    <ChevronDown />
                </Button>
            </DropdownMenuTrigger>
            {
                (hasAnyFleetsSelectable)
                &&
                <DropdownMenuContent>
                    
                    {/* Display All Available Fleets */}
                    {userFleetAccess.map((fleet: FleetAccess) => (
                        <DropdownMenuItem key={fleet.fleetId} onSelect={() => switchToFleet(fleet.fleetId)}>
                            <span>{fleet.fleetName}</span>
                        </DropdownMenuItem>
                    ))}

                </DropdownMenuContent>
            }
        </DropdownMenu>

    }

    return render();

}