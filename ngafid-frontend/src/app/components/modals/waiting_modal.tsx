// ngafid-frontend/src/app/components/modals/waiting_modal.tsx

import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { motion } from "motion/react";
import type { ModalProps } from "./types";
import { useModal } from '@/components/modals/modal_context';
import { useAuth } from '@/components/providers/auth_provider';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { useEffect, useState } from "react";
import ErrorModal from "@/components/modals/error_modal";
import { openRoute } from "@/lib/route_utils";
import { fleetSelectable } from "@/components/navbars/multifleet_select";

export default function WaitingModal(_props: ModalProps) {

    const { setModal, renderModalHeader } = useModal();
    const { user, fleetLoading } = useAuth();
    const currentFleet = user?.fleet ?? null;
    const fleetAccess = user?.fleetAccess ?? [];

    const [selectedFleetId, setSelectedFleetId] = useState<string>("");

    useEffect(() => {
        setSelectedFleetId(currentFleet?.id ? String(currentFleet.id) : "");
    }, [currentFleet?.id]);


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
                                {fleetAccess.map((fleet) => (
                                    <SelectItem
                                        key={fleet.fleetId}
                                        value={String(fleet.fleetId)}
                                        disabled={!fleetSelectable(currentFleet?.id ?? -1, fleet.fleetId, fleet)}
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
