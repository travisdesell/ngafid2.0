import React, { useEffect } from "react";
import { AirframeNameID } from "src/types";
import ErrorModal from "./components/modals/error_modal";
import { useModal } from "./components/modals/modal_provider";
import { useAuth } from "./auth";
import { AIRFRAME_NAMES_IGNORED } from "./lib/airframe_names_ignored";

export const ALL_AIRFRAMES_ID = -1;
export const ALL_AIRFRAMES_NAME = "All Airframes";
export const ALL_AIRFRAMES_PAIR = {
    name: ALL_AIRFRAMES_NAME,
    id: ALL_AIRFRAMES_ID,
};

type AirframesContextValue = {
    airframes: AirframeNameID[],
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

    const [airframes, setAirframes] = React.useState<AirframeNameID[]>([ALL_AIRFRAMES_PAIR]);
    const [airframeIDSelected, setAirframeIDSelected] = React.useState<number | typeof ALL_AIRFRAMES_ID>(ALL_AIRFRAMES_ID);
    const [airframeNameSelected, setAirframeNameSelected] = React.useState<string | typeof ALL_AIRFRAMES_NAME>(ALL_AIRFRAMES_NAME);

    useEffect(() => {

        if (!user)
            return;

        console.log("Airframes Provider - User logged in, fetching data...");

        //Fetch airframe name/ID pairs
        fetchAirframeNameIDPairs();

    }, [user]);

    const fetchAirframeNameIDPairs = async () => {

        console.log("Airframes Provider - Fetching airframe name/ID pairs...");

        fetch("/api/aircraft/airframes")
            .then((response) => {
                if (!response.ok)
                    throw new Error(`Error fetching airframe name/ID pairs: ${response.status} ${response.statusText}`);
                return response.json();
            })
            .then((data: AirframeNameID[]) => {
                console.log("Airframes Provider - Fetched airframe name/ID pairs:", data);

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
