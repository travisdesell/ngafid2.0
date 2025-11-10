// ngafid-frontend/src/app/components/providers/flight_filters_provider.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import SuccessModal from "@/components/modals/success_modal";
import { useAuth } from "@/components/providers/auth_provider";
import { getLogger } from "@/components/providers/logger";
import { fetchJson } from "@/fetchJson";
import { createContext, useContext, useEffect, useState } from "react";


const log = getLogger("FlightFiltersProvider", "purple", "Provider");


export type FlightFilter = {
    name: string,
    color: string,
    filter: string, //<-- JSON string representing filter criteria
};

type FlightFiltersState = {
    filters: FlightFilter[],
}
type FlightFiltersContextValue = {
    filters: FlightFilter[],
} & {
    saveFilter(filter: FlightFilter): Promise<void>,
    deleteFilterByName(filterName: string): Promise<void>,
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
    
    // const [filters, setFilters] = useState<FlightFilter[]>([]);
    const [state, setState] = useState<FlightFiltersState>({
        filters: [],
    });
    const { filters } = state;
    const setFilters = (filters: FlightFilter[]) => {
        setState((prevState) => ({
            ...prevState,
            filters: filters,
        }));
    }



    useEffect(() => {

        if (!user)
            return;

        log("Flight Filters Provider - User logged in, fetching data...");

        // Fetch flight filters from API
        fetchFlightFilters();
        
    }, [user]);


    const fetchFlightFilters = async () => {

        log("Fetching flight filters...");

        let filtersData = await fetchJson.get("/api/filter")
            .catch((error) => {
                setModal(ErrorModal, {
                    title: "Error Fetching Flight Filters",
                    message: error.toString(),
                });
            });

        if (!filtersData)
            log("No filters data received!", {level: "warn"});
        else
            log(`Fetched flight filters: ${JSON.stringify(filtersData)}`);

        setFilters(filtersData as FlightFilter[] || []);

    };


    const saveFilter = async (filter: FlightFilter) => {

        log("Flight Filters Provider - Saving flight filter:", filter);

        const params = new URLSearchParams();
        params.set("name", filter.name.trim());
        params.set("filterJSON", filter.filter ?? "{}");
        params.set("color", filter.color ?? "#000000");

        await fetchJson.put("/api/filter", params)
            .then(() => {

                setModal(SuccessModal, { title: "Flight Filter Saved", message: `Successfully saved flight filter: ${filter.name}` });

                // Refresh filters after saving
                fetchFlightFilters();
            })
            .catch((error) => {
                setModal(ErrorModal, {
                    title: "Error Saving Flight Filter",
                    message: error.toString(),
                });
            });

    };

    const deleteFilterByName = async (name: string) => {

        log(`Deleting flight filter by name: ${name}...`);

        await fetchJson
            .delete(`/api/filter/${encodeURIComponent(name)}`)
            .then(() => {
                fetchFlightFilters()
                setModal(SuccessModal, { title: "Flight Filter Deleted", message: `Successfully deleted flight filter: ${name}` });
            })
            .catch((error) => {
                setModal(ErrorModal, { title: "Error Deleting Flight Filter", message: error.toString() });
            });

    };


    return (
        <FlightFiltersContext.Provider value={{ filters, saveFilter, deleteFilterByName }}>
            {children}
        </FlightFiltersContext.Provider>
    );
}