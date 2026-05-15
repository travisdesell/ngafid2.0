// ngafid-frontend/src/app/components/providers/airframes_provider.tsx
import { useModal } from "@/components/modals/modal_context";
import { getLogger } from "@/components/providers/logger";
import React, { useEffect } from "react";
import { AirframeNameID } from "src/types/types";
import { AIRFRAME_NAMES_IGNORED } from "../../lib/airframe_names_ignored";
import ErrorModal from "../modals/error_modal";
import { useAuth } from "./auth_provider";

const log = getLogger("AirframesProvider", "black", "Provider");

export const ALL_AIRFRAMES_ID = -1;
export const ALL_AIRFRAMES_NAME = "All Airframes";
export const ALL_AIRFRAMES_PAIR = {
    name: ALL_AIRFRAMES_NAME,
    id: ALL_AIRFRAMES_ID,
};

interface AirframesContextValue {
    airframes: Array<AirframeNameID>,
    airframeIDSelected: number | typeof ALL_AIRFRAMES_ID,
    setAirframeIDSelected: React.Dispatch<React.SetStateAction<number>>,
    airframeNameSelected: string | typeof ALL_AIRFRAMES_NAME,
    setAirframeNameSelected: React.Dispatch<React.SetStateAction<string>>,
}

const AirframesContext = React.createContext<AirframesContextValue | null>(null);

export function useAirframes() {
    const ctx = React.useContext(AirframesContext);
    if (!ctx)
        throw new Error("useAirframes must be used within <AirframesProvider>");
    return ctx;
}
export function AirframesProvider({ children }: { children: React.ReactNode }) {

    const { setModal } = useModal();
    const { user } = useAuth();

    const [airframes, setAirframes] = React.useState<Array<AirframeNameID>>([ALL_AIRFRAMES_PAIR]);
    const [airframeIDSelected, setAirframeIDSelected] = React.useState<number | typeof ALL_AIRFRAMES_ID>(ALL_AIRFRAMES_ID);
    const [airframeNameSelected, setAirframeNameSelected] = React.useState<string | typeof ALL_AIRFRAMES_NAME>(ALL_AIRFRAMES_NAME);

    useEffect(() => {

        // User missing / logged out, exit
        if (!user)
            return;

        // Outside of a protected route, exit
        if (!window.location.pathname.includes('protected'))
            return;

        log(`User logged in (${user.email}), fetching data...`);

        //Fetch airframe name/ID pairs
        fetchAirframeNameIDPairs();

    }, [user]);

    const fetchAirframeNameIDPairs = async () => {

        log("Fetching airframe name/ID pairs...");

        fetch("/api/aircraft/airframes")
            .then((response) => {
                if (!response.ok)
                    throw new Error(`Error fetching airframe name/ID pairs: ${response.status} ${response.statusText}`);
                return response.json();
            })
            .then((data: Array<AirframeNameID>) => {
                log(`Fetched airframe name/ID pairs: ${JSON.stringify(data)}`);

                //Exclude ignored names from AIRFRAME_NAMES_IGNORED
                data = data.filter(af => !AIRFRAME_NAMES_IGNORED.includes(af.name));

                //Sort alphabetically by name
                data.sort((a, b) => a.name.localeCompare(b.name));

                setAirframes([ALL_AIRFRAMES_PAIR, ...data]);
            })
            .catch((error) => {
                setModal(ErrorModal, { title: "Error fetching airframe name/ID pairs", message: error.toString() });
            });

    };

    const value = {
        airframes,
        airframeIDSelected,
        setAirframeIDSelected,
        airframeNameSelected,
        setAirframeNameSelected,
    };

    return (
        <AirframesContext.Provider value={value}>
            {children}
        </AirframesContext.Provider>
    );
}
