// ngafid-frontend/src/app/components/providers/flight_filters_provider.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import { useAuth } from "@/components/providers/auth_provider";
import { fetchJson } from "@/fetchJson";
import { createContext, useContext, useEffect, useState } from "react"


export type FlightFilter = {
    id: number,
    name: string,
    color: string,
    criteria: string, //<-- JSON string representing filter criteria
};
type FlightFiltersContextValue = {
    filters: FlightFilter[],
}

const FlightFiltersContext = createContext<FlightFiltersContextValue | null>(null);

export function useFlightFilters() {
    const context = useContext(FlightFiltersContext);
    if (!context)
        throw new Error("useFlightFilters must be used within a FlightFiltersProvider");
    return context;
}

export function FlightFiltersProvider({ children }: { children: React.ReactNode }) {

    const { setModal } = useModal();
    const { user } = useAuth();
    
    const [filters, setFilters] = useState<FlightFilter[]>([]);

    useEffect(() => {

        if (!user)
            return;

        console.log("Flight Filters Provider - User logged in, fetching data...");

        // Fetch flight filters from API
        fetchFlightFilters();
    }, [user]);

    const fetchFlightFilters = async () => {

        console.log("Flight Filters Provider - Fetching flight filters...");

        // fetch("/api/filter")
        let filtersData = await fetchJson.get("api/filter")
            .catch((error) => {
                setModal(ErrorModal, {
                    title: "Error Fetching Flight Filters",
                    message: error.toString(),
                });
            });

        if (!filtersData)
            console.warn("No filters data received");
        else
            console.log("Flight Filters Provider - Fetched flight filters:", filtersData);

        setFilters(filtersData as FlightFilter[] || []);

    };

    return (
        <FlightFiltersContext.Provider value={{ filters }}>
            {children}
        </FlightFiltersContext.Provider>
    );
}