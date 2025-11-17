// ngafid-frontend/src/app/pages/protected/flights/flights.tsx
'use client';

import ConfirmModal from "@/components/modals/confirm_modal";
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import SuccessModal from "@/components/modals/success_modal";
import { NavbarExtras } from "@/components/navbars/navbar_slot";
import { getLogger } from "@/components/providers/logger";
import { type TagData } from "@/components/providers/tags/tags_provider";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { fetchJson } from "@/fetchJson";
import { base64urlToU8, fromWire } from "@/pages/protected/flights/_filters/flights_filter_copy_helpers";
import { BASE_RULE_DEFINITIONS, SORTABLE_COLUMN_VALUES, SORTABLE_COLUMNS } from "@/pages/protected/flights/_filters/flights_filter_rules";
import { Filter, FilterCondition, FilterGroup, FilterRule, FilterRuleDefinition, SPECIAL_FILTER_GROUP_ID } from "@/pages/protected/flights/_filters/types";
import { Flight } from "@/pages/protected/flights/_flight_row/flight_row";
import FlightsPanelMap from "@/pages/protected/flights/_panels/flights_panel_map";
import FlightsPanelResults from "@/pages/protected/flights/_panels/flights_panel_results";
import { ChartArea, Earth, Map, Search, Slash } from "lucide-react";
import { AnimatePresence, LayoutGroup, motion } from "motion/react";
import pako from "pako";
import { createContext, useContext, useEffect, useRef, useState, useTransition } from "react";
import type { ImperativePanelHandle } from "react-resizable-panels";
import FlightsPanelSearch from "./_panels/flights_panel_search";


const log = getLogger("Flights", "black", "Page");


export type SortingDirection = "Ascending" | "Descending";
export const isValidSortingDirection = (value: string): value is SortingDirection => {
    return (value === "Ascending" || value === "Descending");
}

export const FLIGHTS_PER_PAGE_OPTIONS = [
    10,
    25,
    50,
    100,
]


export const FILTER_RULE_NAME_NEW = "New Rule";

type FlightsResponse = {
    flights: Flight[];
    totalFlights: number;
    numberPages: number;
}

type FlightsState = {
    flights: Flight[];
    totalFlights: number;
    numberPages: number;

    filter: Filter;

    isFilterSearchLoading: boolean;
    isFilterSearchLoadingManual: boolean;

    filterSearched: Filter | null;
    sortingColumn?: string | null;
    sortingDirection?: SortingDirection;    
    pageSize?: number;
    currentPage: number;
};
type FlightsContextValue = FlightsState & {
    setFilter: (updater: (prev: Filter) => Filter) => void;
    setFilterFromJSON: (json: string) => void;
    filterIsEmpty: (filter: Filter) => boolean;
    filterIsValid: (filter: Filter) => boolean;
    revertFilter: () => void;

    addFlightIDToFilter: (flightID: string) => void;
    flightIDInSpecialGroup: (flightID: string) => boolean;

    newID: () => string;

    setSortingColumn: (column: string | null) => void;
    setSortingDirection: (direction: SortingDirection) => void;
    setPageSize: (size: number) => void;
    setCurrentPage: (page: number) => void;

    fetchFlightsWithFilter: (filter: FilterGroup, isTriggeredManually: boolean) => Promise<any>;

    updateFlightTags: (flightId: number, tags: TagData[] | null) => void;
};


const FlightsContext = createContext<FlightsContextValue | null>(null);


export default function FlightsPage() {

    useEffect(() => {
        document.title = `NGAFID — Flights`;
    });

    const { setModal } = useModal();

    // Animation Config
    const spring = { type: "spring" as const, stiffness: 420, damping: 34, mass: 0.6 };
    const panelInitial = { opacity: 0.00, scale: 0.00 };
    const panelAnimate = { opacity: 1.00, scale: 1.00 };
    const panelExit = { opacity: 0.00, scale: 0.00 };

    // Flights
    const [flights, setFlights] = useState<Flight[]>([]);
    const [totalFlights, setTotalFlights] = useState<number>(0);
    const [numberPages, setNumberPages] = useState<number>(0);

    // Filter Search Helpers (Debounce, AbortController, loading flag...)
    const inflightCtrlRef = useRef<AbortController | null>(null);
    const reqIdRef = useRef(0);
    const [isFilterSearchLoading, startTransition] = useTransition();
    const [isFilterSearchLoadingManual, setIsFilterSearchLoadingManual] = useState(false);
    
    // Layout State
    const [searchPanelVisible, setSearchPanelVisible] = useState(true);

    // Analysis Panels
    const [chartPanelVisible, setChartPanelVisible] = useState(false);
    const [cesiumPanelVisible, setCesiumPanelVisible] = useState(false);
    const [mapPanelVisible, setMapPanelVisible] = useState(false);

    // Search Options State
    const [filterSearched, setFilterSearched] = useState<Filter | null>(null);
    const [sortingColumn, setSortingColumn] = useState<string | null>(SORTABLE_COLUMNS["Flight ID"]);
    const [sortingDirection, setSortingDirection] = useState<SortingDirection>("Ascending");
    const [pageSize, setPageSize] = useState<number>(FLIGHTS_PER_PAGE_OPTIONS[0]);
    const [currentPage, setCurrentPage] = useState<number>(0);

    const anyAnalysisPanelVisible = (chartPanelVisible || cesiumPanelVisible || mapPanelVisible);
    const analysisPanelCount = (chartPanelVisible ? 1 : 0) + (cesiumPanelVisible ? 1 : 0) + (mapPanelVisible ? 1 : 0);
    const analysisSectionGridClasses = `grid-cols-1 grid-rows-${analysisPanelCount}`;

    const searchPanelRef = useRef<ImperativePanelHandle | null>(null);
    const analysisPanelRef = useRef<ImperativePanelHandle | null>(null);

    const filterIsEmpty = (filter: Filter): boolean => {

        // Has rules -> Not Empty
        if (filter.rules && filter.rules.length > 0)
            return false;

        // Has Groups -> Not Empty
        if (filter.groups && filter.groups.length > 0)
            return false;

        // No rules or groups -> Empty
        return true;

    }

    const filterIsValid = (filter: Filter): boolean => {

        /*
            Valid filter conditions:

            - All groups have at least 1 rule
            - All rules have every field filled out

            Note: Groups don't necessarily have to have rules,
            since they can have sub-groups which contain rules.
        */

        // No rules or groups -> Invalid
        const hasNoRules = (!filter.rules || filter.rules.length === 0);
        const hasNoGroups = (!filter.groups || filter.groups.length === 0);
        if (hasNoRules && hasNoGroups) {
            log.warn(`Filter is invalid: Found group with no rules or sub-groups.`);
            return false;
        }

        // Check rules...
        for (const rule of filter.rules ?? []) {

            // ... Rule is a new/placeholder rule -> Invalid
            if (rule.name === FILTER_RULE_NAME_NEW) {
                log.warn(`Filter is invalid: Found rule ${rule.id} with placeholder name.`);
                return false;
            }

            // ...Check conditions of each rule...
            for (const condition of rule.conditions) {

                // ...Condition has no value -> Invalid
                if ((condition.value === undefined) || (condition.value === null) || (condition.value === "")) {
                    log.warn(`Filter is invalid: Rule ${rule.id} has condition ${condition.name} with no value.`);
                    return false;
                }

            }

        }

        // Check sub-groups...
        for (const group of filter.groups ?? []) {

            // ...Recursively validate sub-group
            if (!filterIsValid(group))
                return false;

        }

        // All checks passed -> Valid
        return true;

    }

    const newID = () => ((typeof crypto !== "undefined") && crypto.randomUUID)
        ? crypto.randomUUID()
        : Math.random().toString(36).slice(2);



    
    // Parse filter from URL / Set initial empty filter
    function parseFilterFromURL(): { filter?: Filter; outcome: "ok" | "error" | "none" } {

        try {
            const params = new URLSearchParams(window.location.search);

            const filterURLParam = params.get("f");

            // No filter param -> Nothing to do
            if (!filterURLParam)
                return { outcome: "none" };

            const u8 = base64urlToU8(filterURLParam);
            const inflated = pako.inflate(u8);
            const json = new TextDecoder().decode(inflated);
            const wire = JSON.parse(json);
            const parsed = fromWire(wire, newID);

            // Failed to parse filter -> Error
            if (!parsed)
                return { outcome: "error" };

            // Parsed successfully, return it
            return { filter: parsed, outcome: "ok" };

        } catch {
            return { outcome: "error" };
        }

    }

    const makeEmpty = (): Filter => ({
        id: newID(),
        operator: "AND",
        rules: [],
        groups: [],
    });

    const didInitRef = useRef(false);
    useEffect(() => {

        // Guard StrictMode double-invoke
        if (didInitRef.current)
            return;

        didInitRef.current = true;

        const { filter: parsed, outcome } = parseFilterFromURL();

        // Got parsed filter from URL, set it
        if (parsed) {
            setState(prev => ({
                ...prev,
                filter: parsed,
            }));
        }

        // Parsed successfully, show success modal
        if (outcome === "ok") {
            setModal(SuccessModal, {
                title: "Filter Loaded from URL",
                message: "Successfully loaded filter from URL parameter.",
            });

        // Otherwise, show error modal
        } else if (outcome === "error") {
            setModal(ErrorModal, {
                title: "Error Loading Filter from URL",
                message: "There was an error loading the filter from the URL. An empty filter has been loaded instead.",
            });
        }

    }, []);



    // Flights Search
    const fetchFlightsWithFilter = async (filter: FilterGroup, isTriggeredManually: boolean) => {

        log(`Attempting to fetch flights with filter: (Triggered Manually: ${isTriggeredManually})`, filter);

        /*
            Makes a GET request to fetch flights
            matching the given filter.

            /api/flight
        */

        // Invalid or empty filter -> Exit
        if (!filter || filterIsEmpty(filter) || !filterIsValid(filter)) {

            if (isTriggeredManually) {

                log.error("Cannot fetch flights: Filter is undefined, empty, or invalid.", filter);
                setModal(ErrorModal, { title: "Invalid Filter", message: "Cannot fetch flights: The current filter is undefined, empty, or invalid." });

            } else {

                log.warn("Not fetching flights: Filter is undefined, empty, or invalid.", filter);

            }

            return;

        }

        /*
            Triggered a manual submission,
            update the searched filter state.

            (This would be redundant for the
            automatic submissions, since that
            just uses the cached searched filter
            anyways.)
        */
        if (isTriggeredManually)
            setFilterSearched(filter);

        setIsFilterSearchLoadingManual(true);


        // Abort any previous request
        inflightCtrlRef.current?.abort();
        const abortController = new AbortController();
        inflightCtrlRef.current = abortController;

        // Mark the current request ID
        const reqId = (reqIdRef.current + 1);
        reqIdRef.current = reqId;

        try {

            // Invalid current page -> Error
            if (currentPage === undefined || currentPage < 0)
                throw new Error(`Current page is not defined or invalid: ${currentPage}`);

            // Invalid page size -> Error
            if (!pageSize || !FLIGHTS_PER_PAGE_OPTIONS.includes(pageSize))
                throw new Error(`Page size is not defined or invalid: ${pageSize}`);

            // Invalid sorting direction
            if (!sortingDirection || !isValidSortingDirection(sortingDirection))
                throw new Error(`Sorting direction is not defined or invalid: ${sortingDirection}`);

            // Invalid sorting column
            if (!sortingColumn || !SORTABLE_COLUMN_VALUES.includes(sortingColumn))
                throw new Error(`Sorting column is not defined or invalid: ${sortingColumn}`);

            const params = new URLSearchParams({
                filterQuery: JSON.stringify(filter),
                currentPage: currentPage.toString(),
                pageSize: pageSize.toString(),
                sortingColumn: sortingColumn,
                sortingOrder: sortingDirection,
            });
            const response = await fetchJson.get<FlightsResponse>("/api/flight", { params, signal: abortController.signal });

            // Got a newer request -> Discard this response
            if (reqIdRef.current !== reqId) {
                log.warn("Discarding flights response: A newer request has been made.");
                return;
            }

            // Got no flights -> Show modal
            if (!response || response.totalFlights === 0 || !response.flights || response.flights.length === 0) {
                log.warn("No flights found with current filter.");
                setModal(SuccessModal, { title: "No Flights Found", message: "No flights were found matching the current filter." });
                return response;
            }

            log("Fetched flights response: ", response);
            log.table(`Fetched flights (${response.flights.length}):`, response.flights);

            // Update flights state
            startTransition(() => {
                setFlights(response.flights);
                setTotalFlights(response.totalFlights);
                setNumberPages(response.numberPages ?? 0);
            });

            return response;

        } catch (error) {

            setModal(ErrorModal, { title: "Error Fetching Flights", message: "An error occurred while fetching flights with the current filter.", code: (error as Error).message });

        } finally {

            setIsFilterSearchLoadingManual(false);

        }

    }


    // Revert filter to last searched
    const revertFilter = () => {

        if (!filterSearched) {
            log.warn("No previously searched filter to revert to.");
            setModal(ErrorModal, { allowReport: false, title: "No Previous Filter", message: "There is no previously searched filter to revert to." });
            return;
        }

        const confirmRevert = () => {

            log("Reverting filter to last searched filter:", filterSearched);
            setState((prev) => ({
                ...prev,
                filter: filterSearched!,
            }));

        }

        setModal(ConfirmModal, {
            title: "Revert Filter",
            message: "Are you sure you want to revert to the last searched filter?",
            buttonVariant: "default",
            onConfirm: confirmRevert,
        });

    }

    // Add Flight ID to special filter group
    const addFlightIDToFilter = (flightID: string) => {

        /*
            Creates a new rule that checks if the
            flight ID is equal to the given flight
            ID.

            A special filter group is used for the
            rules added via this function.

            If the special filter group doesn't exist,
            it is created.
        */

        setFilter((prev) => {

            // Deep clone for immutability
            const updatedFilter: Filter = structuredClone(prev);

            // Use the existing "Flight ID" rule definition as a template
            const flightIdTemplateDef = BASE_RULE_DEFINITIONS.find((r) => r.name === "Flight ID");

            // Target ID already in filter -> Exit
            if (flightIdTemplateDef) {

                const existingRule = updatedFilter.groups
                    ?.find((g) => g.id === SPECIAL_FILTER_GROUP_ID)
                    ?.rules?.find(
                        (r) =>
                            r.name === "Flight ID" &&
                            r.conditions.some((c) => c.name === "number" && c.value === flightID),
                    );

                if (existingRule) {
                    log.warn(`Flight ID ${flightID} is already in the filter, skipping.`);
                    return prev;
                }
            }

            const fromTemplate = (template: FilterRuleDefinition): FilterCondition[] =>
                template.conditions.map((c) => {
                    const cloned = structuredClone(c) as FilterCondition;

                    // Default the comparison operator to "="
                    if (cloned.name === "condition" && cloned.type === "select")
                        cloned.value = "=";

                    // Set the Flight ID value
                    if (cloned.name === "number" && cloned.type === "number")
                        cloned.value = flightID;

                    return cloned;
                });

            const freshConditions = (): FilterCondition[] => ([
                {
                    type: "select",
                    name: "condition",
                    options: ["="],
                    value: "=",
                } as FilterCondition,
                {
                    type: "number",
                    name: "number",
                    value: flightID,
                } as FilterCondition,
            ]);

            const conditions: FilterCondition[] = flightIdTemplateDef
                ? fromTemplate(flightIdTemplateDef)
                : freshConditions();

            const newRule: FilterRule = {
                id: newID(),
                name: "Flight ID",
                conditions,
            };

            // Ensure groups array exists
            const groups = updatedFilter.groups ?? [];

            // Check if the special filter group exists
            let specialGroup = groups.find((g) => g.id === "special-flight-id-group");

            // Special group doesn't exist, create it with the new rule
            if (!specialGroup) {

                specialGroup = {
                    id: "special-flight-id-group",
                    operator: "OR",
                    rules: [newRule],
                    groups: [],
                };

                updatedFilter.groups = [...groups, specialGroup];

            // Otherwise, add the new rule to the existing group
            } else {
                specialGroup.rules = [...(specialGroup.rules ?? []), newRule];
            }

            return updatedFilter;

        });

    };

    const flightIDInSpecialGroup = (flightID: string): boolean => {

        const specialGroup = state.filter.groups?.find((g) => g.id === SPECIAL_FILTER_GROUP_ID);
        if (!specialGroup || !specialGroup.rules)
            return false;

        return specialGroup.rules.some((r) => r.name === "Flight ID" && r.conditions.some((c) => c.name === "number" && c.value === flightID));

    }


    // Flights State
    const [state, setState] = useState<FlightsState>({
        flights: flights,
        totalFlights: totalFlights,
        numberPages: numberPages,

        filter: makeEmpty(),

        isFilterSearchLoading: isFilterSearchLoading,
        isFilterSearchLoadingManual: isFilterSearchLoadingManual,

        filterSearched: null,
        sortingColumn: sortingColumn,
        sortingDirection: sortingDirection,
        pageSize: pageSize,
        currentPage
    });

    const setFilter: FlightsContextValue["setFilter"] = (updater) => {

        const updatedFilter = updater(state.filter);
        log("Setting new filter:", updatedFilter);

        setState((prev) => ({
            ...prev,
            filter: updatedFilter,
        }));

    };

    const setFilterFromJSON: FlightsContextValue["setFilterFromJSON"] = (json) => {

        log("Attempting to parse filter from JSON:", json);

        const parsed = JSON.parse(json);
        log("Setting filter from parsed JSON:", parsed);

        setFilter(() => parsed);
        
    }

    const updateFlightTags: FlightsContextValue["updateFlightTags"] = (flightId, tags) => {

        log(`Updating tags for flight ${flightId}:`, tags);

        setFlights((prev) =>
            prev.map((flight) =>
                flight.id === flightId ? { ...flight, tags } : flight,
            ),
        );

    };

    const value: FlightsContextValue = {
        flights: flights,
        totalFlights: totalFlights,
        numberPages: numberPages,

        filter: state.filter,

        isFilterSearchLoading: isFilterSearchLoading,
        isFilterSearchLoadingManual: isFilterSearchLoadingManual,

        filterSearched: filterSearched,
        sortingColumn,
        sortingDirection,
        pageSize,
        currentPage,

        setFilter,
        setFilterFromJSON,
        filterIsEmpty: filterIsEmpty,
        filterIsValid: filterIsValid,
        revertFilter,
        
        addFlightIDToFilter,
        flightIDInSpecialGroup,

        newID,

        setSortingColumn,
        setSortingDirection,
        setPageSize,
        setCurrentPage,

        fetchFlightsWithFilter,

        updateFlightTags,
    };

    useEffect(() => {

        /*
            Reset the current page to 0
            when the sorting options or
            total number of pages change.
        */

        setCurrentPage(0);

    }, [sortingColumn, sortingDirection, pageSize, numberPages]);

    useEffect(() => {

        /*
            Trigger a re-fetch whenever the
            the sorting options or current
            page change.

            Ensure that this does not get
            triggered on initial render.
        */

        if (didInitRef.current === false)
            return;

        if (!filterSearched) {
            log.warn("Not automatically fetching flights: No previously searched filter.");
            return;
        }

        // fetchFlightsWithFilter(filterSearched, false);
        const id = setTimeout(() => fetchFlightsWithFilter(filterSearched, false), 150);
        return () => clearTimeout(id);

    }, [sortingColumn, sortingDirection, pageSize, currentPage, filterSearched]);


    useEffect(() => {

        const searchPanelAPI = searchPanelRef.current;

        // Panel instance doesn't exist, exit
        if (!searchPanelAPI)
            return;

        const isCollapsed = searchPanelAPI.isCollapsed?.() ?? false;

        // Panel should be visible but is collapsed, expand it
        if (searchPanelVisible && isCollapsed)
            searchPanelAPI.expand();

        // Panel should not be visible but is expanded, collapse it
        if (!searchPanelVisible && !isCollapsed)
            searchPanelAPI.collapse();

    }, [searchPanelVisible]);


    useEffect(() => {

        if (anyAnalysisPanelVisible)
            analysisPanelRef.current?.expand();
        else
            analysisPanelRef.current?.collapse();

    }, [anyAnalysisPanelVisible]);


    const renderSectionToggleButton = (Icon: React.ElementType, isActive: boolean, toggleMethod: (isActive: boolean) => void, buttonClass = "") => (

        <Button variant="outline" size="icon" onClick={() => toggleMethod(!isActive)} className={`relative ${buttonClass}`}>
            <Icon className={`${isActive ? 'opacity-100' : 'opacity-25'}`} />
            {
                (!isActive)
                &&
                <Slash className="absolute" />
            }
        </Button>

    );

    const navbarExtras = (
        <>
            {/* Search Area Toggle */}
            {renderSectionToggleButton(Search, searchPanelVisible, setSearchPanelVisible)}

            {/* Chart Toggle */}
            {renderSectionToggleButton(ChartArea, chartPanelVisible, setChartPanelVisible)}

            {/* Cesium Toggle */}
            {renderSectionToggleButton(Earth, cesiumPanelVisible, setCesiumPanelVisible)}

            {/* Map Toggle & Select */}
            <div className="flex">

                {renderSectionToggleButton(Map, mapPanelVisible, setMapPanelVisible, "rounded-r-none")}

                <Select>
                    <SelectTrigger className="w-[180px] rounded-l-none">
                        <SelectValue placeholder="Select Map Type" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="none">None</SelectItem>
                        <SelectItem value="cesium">Cesium</SelectItem>
                        <SelectItem value="leaflet">Leaflet</SelectItem>
                    </SelectContent>
                </Select>
            </div>
        </>
    );

    const render = () => (
        <FlightsContext.Provider value={value}>
            <div className="page-container">
                <AnimatePresence mode="sync">

                    <NavbarExtras>
                        {navbarExtras}
                    </NavbarExtras>

                    <motion.div key="flights-page-content" layout className="page-content gap-4">

                        <ResizablePanelGroup direction={"horizontal"} className="gap-2">

                            {/* Search Section */}
                            <ResizablePanel
                                defaultSize={60}
                                minSize={20}
                            >

                                <ResizablePanelGroup direction="vertical" className="gap-2 h-full w-full">

                                    <LayoutGroup id="search-and-results-panels">

                                        {/* Search Panel */}
                                        {
                                            <ResizablePanel
                                                ref={searchPanelRef}
                                                collapsible
                                                key="search"
                                                onCollapse={() => setSearchPanelVisible(false)}
                                                onExpand={() => setSearchPanelVisible(true)}
                                            >
                                                <motion.div
                                                    layout
                                                    initial={panelInitial}
                                                    animate={panelAnimate}
                                                    exit={panelExit}
                                                    transition={spring}
                                                    className="w-full h-full min-h-0"
                                                >
                                                    <FlightsPanelSearch />
                                                </motion.div>
                                            </ResizablePanel>
                                        }

                                        <ResizableHandle withHandle />

                                        {/* Results Panel */}
                                        <ResizablePanel>
                                            <motion.div
                                                layout
                                                initial={panelInitial}
                                                animate={panelAnimate}
                                                exit={panelExit}
                                                transition={spring}
                                                className="w-full h-full"
                                            >
                                                <FlightsPanelResults />
                                            </motion.div>

                                        </ResizablePanel>

                                    </LayoutGroup>

                                </ResizablePanelGroup>
                            </ResizablePanel>

                            {/* Analysis Section */}
                            <>
                                <ResizableHandle withHandle className={`${anyAnalysisPanelVisible ? "visible" : "hidden"}`} />

                                <ResizablePanel
                                    ref={analysisPanelRef}
                                    collapsible
                                    defaultSize={40}
                                    className={`${anyAnalysisPanelVisible ? "visible" : "hidden"}`}
                                >
                                    <motion.div
                                        layout
                                        layoutRoot
                                        initial={false}
                                        className={`grid ${analysisSectionGridClasses} gap-4 h-full`}
                                    >
                                        <LayoutGroup id="analysis-panels">

                                            {/* Chart Panel */}
                                            {
                                                (chartPanelVisible)
                                                &&
                                                <motion.div
                                                    key="chart"
                                                    layout
                                                    initial={panelInitial}
                                                    animate={panelAnimate}
                                                    exit={panelExit}
                                                    transition={spring}
                                                >
                                                    <Card className="p-4 border rounded-lg w-full h-full card-glossy">
                                                        Chart Panel
                                                    </Card>
                                                </motion.div>
                                            }

                                            {/* Cesium Panel */}
                                            {
                                                (cesiumPanelVisible)
                                                &&
                                                <motion.div
                                                    key="cesium"
                                                    layout
                                                    initial={panelInitial}
                                                    animate={panelAnimate}
                                                    exit={panelExit}
                                                    transition={spring}
                                                >
                                                    <Card className="p-4 border rounded-lg w-full h-full card-glossy">
                                                        Cesium Panel
                                                    </Card>
                                                </motion.div>
                                            }

                                            {/* Map Panel */}
                                            {
                                                (mapPanelVisible)
                                                &&
                                                <motion.div
                                                    key="map"
                                                    layout
                                                    initial={panelInitial}
                                                    animate={panelAnimate}
                                                    exit={panelExit}
                                                    transition={spring}
                                                >
                                                    <FlightsPanelMap />
                                                </motion.div>
                                            }

                                        </LayoutGroup>
                                    </motion.div>
                                </ResizablePanel>
                            </>


                        </ResizablePanelGroup>

                    </motion.div>
                </AnimatePresence>
            </div>
        </FlightsContext.Provider>
    );


    log("Rendering Flights Page");
    return render();

}


export function useFlights() {

    const context = useContext(FlightsContext);
    if (!context)
        throw new Error("useFlights must be used within a FlightsContext.Provider");

    return context;

}