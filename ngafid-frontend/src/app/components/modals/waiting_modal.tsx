// ngafid-frontend/src/app/components/modals/waiting_modal.tsx

import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { motion } from "motion/react";
import { getLogger } from "@/components/providers/logger";
import type { ModalProps } from "./types";
import { useModal } from '@/components/modals/modal_context';
import { Separator } from '@/components/ui/separator';
import { useAuth } from '@/components/providers/auth_provider';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { useEffect, useState } from "react";
import ErrorModal from "@/components/modals/error_modal";
import { fetchJson } from "@/fetchJson";
import { openRoute } from "@/lib/route_utils";
import { fleetAccessAllowed } from "@/components/navbars/multifleet_select";

const log = getLogger("WaitingModal", "black", "Modal");


/*
    TODO:

    Pull the fleet selection logic out of this.
    Has some duplicate code with:
    
    1. _profile_preferences_site_preferences_content.tsx
    2. multifleet_select.tsx

    Or, just export relevant stuff from multifleet_select
    and use it here if that's easier ¯\_(ツ)_/¯
*/

type Fleet = {
    id: number;
    name: string;
};
type FleetAccess = {
    fleetName: string;
    fleetId: number;
    accessType: string;
    userId: number;
};


export default function WaitingModal({ data }: ModalProps) {

    const { setModal, renderModalHeader } = useModal();
    const { user } = useAuth();

    const [currentFleet, setCurrentFleet] = useState<Fleet | null>(null);
    const [selectedFleetId, setSelectedFleetId] = useState<string>("");
    const [fleetAccess, setFleetAccess] = useState<FleetAccess[]>([]);
    const [fleetLoading, setFleetLoading] = useState(true);


    const switchToFleet = async (fleetId: number) => {

        // ...
        if (!fleetId || fleetId === currentFleet?.id)
            return;

        try {
            const response = await fetch("/api/user/select-fleet", {
                method: "PUT",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({ fleetIdSelected: String(fleetId) }).toString(),
            });

            if (!response.ok) {
                const message = await response.text().catch(() => "");
                throw new Error(message || "Failed to switch fleets.");
            }

            // window.location.reload();

            // Switched successfully, go to Summary page 
            openRoute("summary", true);

        } catch (error) {
            setModal(ErrorModal, { title: "Error switching fleets", message: (error as Error).message });
        }

    };

    useEffect(() => {
        const fetchFleets = async () => {
            setFleetLoading(true);
            const [fleetResponse, accessResponse] = await Promise.all([
                fetchJson.get<Fleet>("/api/fleet").catch((error: Error) => {
                    setModal(ErrorModal, { title: "Error fetching fleet", message: error.message });
                    return null;
                }),
                fetchJson.get<FleetAccess[]>("/api/user/fleet-access").catch((error: Error) => {
                    setModal(ErrorModal, { title: "Error fetching fleet access", message: error.message });
                    return null;
                }),
            ]);

            if (fleetResponse) {
                setCurrentFleet(fleetResponse);
                setSelectedFleetId(String(fleetResponse.id));
            }
            if (accessResponse)
                setFleetAccess(accessResponse);

            setFleetLoading(false);
        };

        fetchFleets();
    }, [setModal]);

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">

                {renderModalHeader(`Waiting for Confirmation`, 'Select a different fleet or close this to log out.')}

                {/* Fleet Selection */}
                <CardContent>

                    {/* Fleet Selection */}
                    <div className="flex flex-col gap-2">
                        <Label>Switch Fleet</Label>
                        <Select
                            value={selectedFleetId}
                            onValueChange={(value) => {
                                setSelectedFleetId(value);
                                switchToFleet(Number(value));
                            }}
                            disabled={fleetLoading || fleetAccess.length === 0}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder={fleetLoading ? "Loading..." : "Select fleet"} />
                            </SelectTrigger>
                            <SelectContent>
                                {fleetAccess.map((fleet, i) => (
                                    <SelectItem
                                        key={fleet.fleetId}
                                        value={String(fleet.fleetId)}
                                        disabled={!fleetAccessAllowed(fleetAccess[i])}
                                    >
                                        {fleet.fleetName}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                </CardContent>

                {/* Contact Message */}
                <CardFooter className="text-sm text-muted-foreground">
                    If you believe you should have access to an unavailable fleet, please contact the fleet owner.
                </CardFooter>
                
            </Card>
        </motion.div>
    );

}
