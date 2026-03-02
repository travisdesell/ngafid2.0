// ngafid-frontend/src/app/components/providers/airports_provider.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { useAuth } from "@/components/providers/auth_provider";
import { getLogger } from "@/components/providers/logger";
import { fetchJson } from "@/fetchJson";
import React, { useEffect } from "react";

const log = getLogger("AirportsProvider", "black", "Provider");

type AirportsContextValue = {
    visitedAirports: string[];
    visitedRunways: string[];
    setVisitedAirports: React.Dispatch<React.SetStateAction<string[]>>;
    setVisitedRunways: React.Dispatch<React.SetStateAction<string[]>>;
    refreshVisitedAirportsAndRunways: () => Promise<void>;
}

const AirportsContext = React.createContext<AirportsContextValue | null>(null);

export function useAirports() {
    const context = React.useContext(AirportsContext);
    if (!context)
        throw new Error("useAirports must be used within <AirportsProvider>");
    return context;
}

export function AirportsProvider({ children }: { children: React.ReactNode }) {

    const { setModal } = useModal();
    const { user } = useAuth();

    const [visitedAirports, setVisitedAirports] = React.useState<string[]>([]);
    const [visitedRunways, setVisitedRunways] = React.useState<string[]>([]);

    useEffect(() => {

        if (!user)
            return;

        log("User logged in, fetching airport/runway data...");

        void refreshVisitedAirportsAndRunways();

    }, [user]);

    const refreshVisitedAirportsAndRunways = async () => {

        log("Fetching visited airports and runways...");

        try {

            const [airportsData, runwaysData] = await Promise.all([
                fetchJson.get<string[]>("/api/aircraft/airports"),
                fetchJson.get<string[]>("/api/aircraft/runways"),
            ]);

            const airports = (airportsData ?? [])
                .map(value => String(value).trim())
                .filter(value => value.length > 0)
                .sort((a, b) => a.localeCompare(b));

            const runways = (runwaysData ?? [])
                .map(value => String(value).trim())
                .filter(value => value.length > 0)
                .sort((a, b) => a.localeCompare(b));

            setVisitedAirports(Array.from(new Set(airports)));
            setVisitedRunways(Array.from(new Set(runways)));

            log("Fetched visited airports/runways", {
                airportsCount: airports.length,
                runwaysCount: runways.length,
            });

        } catch (error) {

            setModal(ErrorModal, {
                title: "Error Fetching Airports/Runways",
                message: error instanceof Error ? error.toString() : "Unknown error",
            });

        }

    };

    const value = {
        visitedAirports,
        visitedRunways,
        setVisitedAirports,
        setVisitedRunways,
        refreshVisitedAirportsAndRunways,
    };

    return (
        <AirportsContext.Provider value={value}>
            {children}
        </AirportsContext.Provider>
    );
}