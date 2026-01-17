// ngafid-frontend/src/app/pages/protected/flights/flights.tsx
'use client';

import ConfirmModal from "@/components/modals/confirm_modal";
import ErrorModal from "@/components/modals/error_modal";
import { FlightsSelectedModal } from "@/components/modals/flights_selected_modal/flights_selected_modal";
import { useModal } from "@/components/modals/modal_context";
import SuccessModal from "@/components/modals/success_modal";
import { NavbarExtras } from "@/components/navbars/navbar_slot";
import { CommandData, useRegisterCommands } from "@/components/providers/commands_provider";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { fetchJson } from "@/fetchJson";
import { base64urlToU8, fromWire, toWire, u8ToBase64url } from "@/pages/protected/flights/_filters/flights_filter_copy_helpers";
import { BASE_RULE_DEFINITIONS, SORTABLE_COLUMN_VALUES, SORTABLE_COLUMNS } from "@/pages/protected/flights/_filters/flights_filter_rules";
import { Filter, FilterCondition, FilterGroup, FilterRule, FilterRuleDefinition, SPECIAL_FILTER_GROUP_ID } from "@/pages/protected/flights/_filters/types";
import { EnsureSeriesFn, FlightsChartContext, FlightsChartContextValue, FlightsChartState } from "@/pages/protected/flights/_flights_context_chart";
import { FlightsResponse, FlightsResultsContext, FlightsResultsContextValue } from "@/pages/protected/flights/_flights_context_results";
import { FlightsFilterContext, FlightsFilterContextValue, FlightsFilterState, FlightsSearchFilterContext, FlightsSearchFilterContextValue, FlightsSearchFilterState } from "@/pages/protected/flights/_flights_context_search_filter";
import { FlightsPanelChart } from "@/pages/protected/flights/_panels/flights_panel_chart";
import FlightsPanelMap from "@/pages/protected/flights/_panels/flights_panel_map";
import { FlightsPanelResults } from "@/pages/protected/flights/_panels/flights_panel_results";
import { fetchSeries } from "@/pages/protected/flights/chart_data";
import { FILTER_RULE_NAME_NEW, FLIGHTS_PER_PAGE_OPTIONS, isValidSortingDirection, SortingDirection, type Flight } from "@/pages/protected/flights/types";
import { SeriesKey, TraceSeries } from "@/pages/protected/flights/types_charts";
import { EventSelectionState } from "@/pages/protected/flights/types_events";
import { ChartArea, Earth, List, Map as MapIcon, Search, Slash } from "lucide-react";
import { AnimatePresence, LayoutGroup, motion } from "motion/react";
import pako from "pako";
import { startTransition as startConcurrentTransition, useCallback, useEffect, useMemo, useRef, useState, useTransition } from "react";
import type { ImperativePanelHandle } from "react-resizable-panels";
import FlightsPanelCesium from "./_panels/_cesium/flights_panel_cesium";
import FlightsPanelSearch from "./_panels/_search/flights_panel_search";


const log = getLogger("Flights", "black", "Page");


type FlightsModalChartStoreSnapshot = {
    chartFlights: Flight[];
    eventSelection: EventSelectionState;
};

type FlightsModalChartStore = {
    subscribe: (listener: () => void) => () => void;
    getSnapshot: () => FlightsModalChartStoreSnapshot;
};



/* Chart & Time-Series Functions */
const makeSeriesKey = (flightId: number, name: string): SeriesKey =>
    `${flightId}:${name}` as SeriesKey;

const getSeriesFromState = (state: FlightsChartState, flightId: number, name: string): TraceSeries | undefined =>
    state.chartData.seriesByFlight[flightId]?.[name];

const setSeriesInState = (state: FlightsChartState, series: TraceSeries): FlightsChartState => {

    const byFlight = { ...state.chartData.seriesByFlight };
    const forFlight = { ...(byFlight[series.flightId] ?? {}) };

    forFlight[series.name] = series;
    byFlight[series.flightId] = forFlight;

    return {
        ...state,
        chartData: {
            ...state.chartData,
            seriesByFlight: byFlight,
        },
    };

};

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

export default function FlightsPage() {

    useEffect(() => {
        document.title = `NGAFID — Flights`;
    });

    const { setModal } = useModal();


    // Animation Config
    const spring = { type: "spring" as const, stiffness: 350, damping: 34, mass: 0.6, bounce: 0 };
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
    const [isFilterSearchLoading, startFilterSearchLoadingTransition] = useTransition();
    const [isFilterSearchLoadingManual, setIsFilterSearchLoadingManual] = useState(false);

    // Chart Data State
    const [chartFlights, setChartFlightsRaw] = useState<Flight[]>([]);

    // Layout State
    const [searchPanelVisible, setSearchPanelVisible] = useState(true);

    // Analysis Panels
    const [chartPanelVisible, setChartPanelVisible] = useState(false);
    const [cesiumPanelVisible, setCesiumPanelVisible] = useState(false);
    const [mapPanelVisible, setMapPanelVisible] = useState(false);
    const [analysisAreaOpen, setAnalysisAreaOpen] = useState(false);

    // Search Options State
    const [filterSearched, setFilterSearched] = useState<Filter | null>(null);
    const [sortingColumn, setSortingColumn] = useState<string | null>(SORTABLE_COLUMNS["Flight ID"]);
    const [sortingDirection, setSortingDirection] = useState<SortingDirection>("Ascending");
    const [pageSize, setPageSize] = useState<number>(FLIGHTS_PER_PAGE_OPTIONS[0]);
    const [currentPage, setCurrentPage] = useState<number>(0);

    const anyAnalysisPanelVisible = (chartPanelVisible || cesiumPanelVisible || mapPanelVisible);
    const analysisPanelGridRowCount = [chartPanelVisible, cesiumPanelVisible, mapPanelVisible].filter(v => v).length;

    const searchPanelRef = useRef<ImperativePanelHandle | null>(null);
    const analysisPanelRef = useRef<ImperativePanelHandle | null>(null);

    const anyAnalysisPanelVisibleRef = useRef(false);
    useEffect(() => {
        anyAnalysisPanelVisibleRef.current = anyAnalysisPanelVisible;
    }, [anyAnalysisPanelVisible]);
    useEffect(() => {
        if (anyAnalysisPanelVisible) {
            setAnalysisAreaOpen(true);
            analysisPanelRef.current?.expand();
        }
    }, [anyAnalysisPanelVisible]);



    const newID = useCallback(() => (
        (typeof crypto !== "undefined") && crypto.randomUUID)
            ? crypto.randomUUID()
            : Math.random().toString(36).slice(2)
    , []);



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

        // Got parsed filter from URL, set it on the real filter state
        if (parsed) {
            setFilterState(prev => ({
                ...prev,
                filter: parsed,
            }));
        }

        if (outcome === "ok") {
            setModal(SuccessModal, {
                title: "Filter Loaded from URL",
                message: "Successfully loaded filter from URL parameter.",
            });
        } else if (outcome === "error") {
            setModal(ErrorModal, {
                title: "Error Loading Filter from URL",
                message: "There was an error loading the filter from the URL. An empty filter has been loaded instead.",
            });
        }
        
    }, []);




    // Flights Search
    const fetchFlightsWithFilter = useCallback(async (filter: FilterGroup, isTriggeredManually: boolean) => {

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
            update the searched filter state
            and reset the current page to 0.

            (This would be redundant for the
            automatic submissions, since that
            just uses the cached searched filter
            anyways.)
        */
        let currentPageTarget = currentPage;
        if (isTriggeredManually) {

            setFilterSearched(filter);

            setCurrentPage(0);
            currentPageTarget = 0;
        }

        setIsFilterSearchLoadingManual(true);


        // Abort any previous request
        inflightCtrlRef.current?.abort("New filter search initiated, aborting previous request.");

        // Create a new AbortController for this request
        const abortController = new AbortController();
        inflightCtrlRef.current = abortController;

        // Mark the current request ID
        const reqId = (reqIdRef.current + 1);
        reqIdRef.current = reqId;

        try {

            // Invalid current page -> Error
            if (currentPageTarget === undefined || currentPageTarget < 0)
                throw new Error(`Current page is not defined or invalid: ${currentPageTarget}`);

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
                currentPage: currentPageTarget.toString(),
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
            startFilterSearchLoadingTransition(() => {
                setFlights(response.flights);
                setTotalFlights(response.totalFlights);
                setNumberPages(response.numberPages ?? 0);
            });

            return response;

        } catch (error: any) {

            // Ignore errors from stale requests
            if (reqIdRef.current !== reqId) {
                log.warn("Ignoring error from stale request:", error);
                return;
            }

            // Ignore abort / cancel errors
            const name = (error?.name ?? "");
            const code = (error?.code ?? "");
            const message = (error?.message ?? "");

            const isAbort = (
                name === "AbortError"
                || code === "ERR_CANCELED"
                || code === 20
                || message?.toLowerCase?.().includes("abort")
                || error?.isCanceled === true
            );
            if (isAbort) {
                log.info("Flights request aborted by a newer request, ignoring.");
                return;
            }

            // Handle other errors
            const errorCode = (error instanceof Error)
                ? error.message
                : "An unknown error occurred.";

            setModal(ErrorModal, { title: "Error Fetching Flights", message: "An error occurred while fetching flights with the current filter.", code: errorCode });

        } finally {

            // This is the current request, end loading state
            if (reqIdRef.current === reqId)
                setIsFilterSearchLoadingManual(false);

        }

    }, [currentPage,
        pageSize,
        sortingColumn,
        sortingDirection,
        setModal,
        startFilterSearchLoadingTransition,
        setFlights,
        setTotalFlights,
        setNumberPages,
        setIsFilterSearchLoadingManual,
        setFilterSearched]
    );

    // Revert filter to last searched
    const revertFilter = useCallback(() => {

        if (!filterSearched) {

            log.warn("No previously searched filter to revert to.");
            setModal(ErrorModal, {
                allowReport: false,
                title: "No Previous Filter",
                message: "There is no previously searched filter to revert to.",
            });

            return;

        }

        const confirmRevert = () => {

            log("Reverting filter to last searched filter:", filterSearched);
            setFilterState((prev) => ({
                ...prev,
                filter: filterSearched!, //<-- Re-attach the last searched filter
            }));

        };

        setModal(ConfirmModal, {
            title: "Revert Filter",
            message: "Are you sure you want to revert to the last searched filter?",
            buttonVariant: "default",
            onConfirm: confirmRevert,
        });

    }, [filterSearched, setModal]);


    // Filter URL Copying
    const copyFilterURL = useCallback((filterTarget: Filter) => {

        /*
            Generates a URL encoding for the
            current filter from a JSON string.

            The URL is then copied to the user's
            clipboard.

            TODO: Compress the URL so it doesn't
            exceed the maximum URL length after
            just a few rules...
        */

        const wire = toWire(filterTarget);
        if (!wire) {
            setModal(ErrorModal, { title: "Filter Empty", message: "Add at least one complete rule." });
            return;
        }

        // JSON -> deflate -> base64url
        const json = JSON.stringify(wire);
        const deflated = pako.deflate(json);
        const payload = u8ToBase64url(deflated);

        const fullURL = `${location.origin}${location.pathname}?f=${payload}`;

        log.info("Generated Filter URL: ", fullURL);

        // Clipboard unavailable
        if (!navigator.clipboard) {
            setModal(ErrorModal, { title: "Clipboard Unavailable", message: "Your browser does not support clipboard operations. You can try manually copying the URL below:", code: fullURL });
            return;
        }

        // Attempt to the URL to the clipboard
        navigator.clipboard.writeText(fullURL)
            .then(
                () => setModal(SuccessModal, { title: "Filter URL Copied", message: "The URL linking to this filter has been copied to your clipboard." })
            )
            .catch(
                () => setModal(ErrorModal, { title: "Error Copying Filter URL", message: "An error occurred while trying to copy the filter URL to your clipboard.", code: "kek!" })
            );

    }, [setModal]);

    // Add Flight ID to special filter group
    const addFlightIDToFilter = useCallback((flightID: string) => {

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

    }, []);


    const [filterState, setFilterState] = useState<FlightsFilterState>({
        filter: makeEmpty(),
    });

    const [searchFilterState, setSearchFilterState] = useState<FlightsSearchFilterState>({
        isFilterSearchLoading: isFilterSearchLoading,
        isFilterSearchLoadingManual: isFilterSearchLoadingManual,

        sortingColumn: sortingColumn,
        sortingDirection: sortingDirection,
        pageSize: pageSize,
        currentPage,
    });
    const searchFilterStateRef = useRef(searchFilterState);
    useEffect(() => {
        searchFilterStateRef.current = searchFilterState;
    }, [searchFilterState]);

    const [chartState, setChartState] = useState<FlightsChartState>({
        chartFlights: chartFlights,
        chartData: {
            seriesByFlight: {},
            loadingSeries: new Set<SeriesKey>(),
        },
        chartSelection: {
            universalParams: new Set<string>(),
            perFlightParams: {},
        },

        eventSelection: {
            universalEvents: new Set<string>(),
            perFlightEvents: {},
        } satisfies EventSelectionState,

        selectedIds: new Set<number>(),
    });
    const chartStateRef = useRef(chartState);
    useEffect(() => {
        chartStateRef.current = chartState;
    }, [chartState]);


    const flightIDInSpecialGroup = useCallback((flightID: string): boolean => {

        const currentFilter = filterState.filter;

        const specialGroup = currentFilter.groups?.find((g) => g.id === SPECIAL_FILTER_GROUP_ID);
        if (!specialGroup || !specialGroup.rules)
            return false;

        return specialGroup.rules.some(
            (r) => r.name === "Flight ID"
                && r.conditions.some((c) =>
                    c.name === "number" && c.value === flightID
                )
        );

    }, [filterState]);


    const toggleUniversalParam = useCallback((name: string) => {

        setChartState(prev => {

            log("Toggling universal parameter:", name);

            const universal = new Set(prev.chartSelection.universalParams);

            // Already selected, deselect
            if (universal.has(name)) {

                log("Deselecting universal parameter:", name);
                universal.delete(name);

            // Otherwise, select
            } else {

                log("Selecting universal parameter:", name);
                universal.add(name);

            }

            return {
                ...prev,
                chartSelection: {
                    ...prev.chartSelection,
                    universalParams: universal,
                },
            };

        });

    }, [setChartState]);


    const togglePerFlightParam = useCallback((flightId: number, name: string) => {

        setChartState(prev => {

            log("Toggling per-flight parameter:", { flightId, name });

            const perFlight = { ...prev.chartSelection.perFlightParams };
            const forFlight = new Set(perFlight[flightId] ?? new Set<string>());

            // Already selected, deselect
            if (forFlight.has(name)) {

                log("Deselecting per-flight parameter for flight:", flightId, name);
                forFlight.delete(name);

            // Otherwise, select
            } else {

                log("Selecting per-flight parameter for flight:", flightId, name);
                forFlight.add(name);

            }

            perFlight[flightId] = forFlight;

            return {
                ...prev,
                chartSelection: {
                    ...prev.chartSelection,
                    perFlightParams: perFlight,
                },
            };

        });

    }, [setChartState]);


    const toggleUniversalEvent = useCallback((name: string) => {

        setChartState(prev => {

            log("Toggling universal event:", name);

            const universal = new Set(prev.eventSelection.universalEvents);

            // Already selected, deselect
            if (universal.has(name)) {

                log("Deselecting universal event:", name);
                universal.delete(name);

            // Otherwise, select
            } else {

                log("Selecting universal event:", name);
                universal.add(name);

            }

            return {
                ...prev,
                eventSelection: {
                    ...prev.eventSelection,
                    universalEvents: universal,
                },
            };

        });

    }, [setChartState]);

    const togglePerFlightEvent = useCallback((flightId: number, name: string) => {

        setChartState(prev => {

            log("Toggling per-flight event:", { flightId, name });

            const perFlight = { ...prev.eventSelection.perFlightEvents };
            const forFlight = new Set(perFlight[flightId] ?? new Set<string>());

            // Already selected, deselect
            if (forFlight.has(name)) {

                log("Deselecting per-flight event for flight:", flightId, name);
                forFlight.delete(name);

            // Otherwise, select
            } else {

                log("Selecting per-flight event for flight:", flightId, name);
                forFlight.add(name);

            }

            perFlight[flightId] = forFlight;

            return {
                ...prev,
                eventSelection: {
                    ...prev.eventSelection,
                    perFlightEvents: perFlight,
                },
            };

        });
        
    }, [setChartState]);

    const inflightSeriesRef = useRef<Map<SeriesKey, Promise<TraceSeries>>>(new Map());

    const ensureSeries = useCallback<EnsureSeriesFn>(
        async (flightId: number, paramName: string) => {
            const key = makeSeriesKey(flightId, paramName);

            // Use latest chart snapshot
            const snapshot = chartStateRef.current;

            const cached = getSeriesFromState(snapshot, flightId, paramName);
            if (cached)
                return cached;

            const existingPromise = inflightSeriesRef.current.get(key);
            if (existingPromise)
                return existingPromise;

            const promise = (async () => {
                // Mark as loading
                setChartState(prev => ({
                    ...prev,
                    chartData: {
                        ...prev.chartData,
                        loadingSeries: new Set(prev.chartData.loadingSeries).add(key),
                    },
                }));

                const series = await fetchSeries(flightId, paramName);

                setChartState(prev => {
                    const next = setSeriesInState(prev, series);
                    const loading = new Set(next.chartData.loadingSeries);
                    loading.delete(key);

                    return {
                        ...next,
                        chartData: {
                            ...next.chartData,
                            loadingSeries: loading,
                        },
                    };
                });

                inflightSeriesRef.current.delete(key);

                return series;
            })();

            inflightSeriesRef.current.set(key, promise);
            return promise;
        },
        [],
    );

        

    const setFilter: FlightsFilterContextValue["setFilter"] = useCallback(

        (updater) => {
            startConcurrentTransition(() => {
                setFilterState((prevState) => {
                    const nextFilter = updater(prevState.filter);

                    if (nextFilter === prevState.filter)
                        return prevState;

                    return {
                        ...prevState,
                        filter: nextFilter,
                    };
                });
            });
        },

    []);


    const setFilterFromJSON: FlightsFilterContextValue["setFilterFromJSON"] = useCallback((json) => {

        log("Attempting to parse filter from JSON:", json);

        const parsed = JSON.parse(json);
        log("Setting filter from parsed JSON:", parsed);

        setFilter(() => parsed);

    }, [setFilter]);

    const updateFlightTags: FlightsSearchFilterContextValue["updateFlightTags"] = useCallback(
        (flightId, tags) => {
            log(`Updating tags for flight ${flightId}:`, tags);

            // Local optimistic update
            setFlights(prev =>
                prev.map(flight =>
                    flight.id === flightId ? { ...flight, tags } : flight,
                ),
            );

            // Only re-fetch using the last searched filter
            if (!filterSearched)
                return;

            // filterSearched was only ever set from a valid, non-empty filter
            void fetchFlightsWithFilter(filterSearched, true);
        },
        [setFlights, filterSearched, fetchFlightsWithFilter],
    );



    const setChartFlights = useCallback<FlightsChartContextValue["setChartFlights"]>(

        (updater) => {

            startConcurrentTransition(() => {

                setChartFlightsRaw(prev => {

                    const next = (typeof updater === "function")
                        ? (updater as (prevFlights: Flight[]) => Flight[])(prev)
                        : updater;

                    setChartState(prevState => {

                        const remainingIDs = new Set(next.map(f => f.id));

                        // Prune per-flight param selections for removed flights
                        const newPerFlightParams: Record<number, Set<string>> = {};
                        for (const [flightIDStr, params] of Object.entries(prevState.chartSelection.perFlightParams)) {

                            const flightID = Number(flightIDStr);
                            if (remainingIDs.has(flightID))
                                newPerFlightParams[flightID] = params;

                        }

                        // Prune per-flight event selections for removed flights
                        const newPerFlightEvents: Record<number, Set<string>> = {};
                        for (const [flightIDStr, events] of Object.entries(prevState.eventSelection.perFlightEvents)) {
                            const flightID = Number(flightIDStr);
                            if (remainingIDs.has(flightID))
                                newPerFlightEvents[flightID] = events;
                        }

                        return {
                            ...prevState,
                            chartFlights: next,
                            chartSelection: {
                                ...prevState.chartSelection,
                                perFlightParams: newPerFlightParams,
                            },
                            eventSelection: {
                                ...prevState.eventSelection,
                                perFlightEvents: newPerFlightEvents,
                            },
                        };

                    });

                    return next;

                });

            });

        }, [setChartFlightsRaw, setChartState],

    );


    const flightsResultsContextValue: FlightsResultsContextValue = useMemo(() => ({
        flights,
        totalFlights,
        numberPages,
    }), [flights, totalFlights, numberPages]);

            
    const flightsSearchContextValue: FlightsSearchFilterContextValue = useMemo(() => ({
            isFilterSearchLoading,
            isFilterSearchLoadingManual,
            sortingColumn,
            sortingDirection,
            pageSize,
            currentPage,

            setSortingColumn,
            setSortingDirection,
            setPageSize,
            setCurrentPage,

            fetchFlightsWithFilter,
            updateFlightTags,
        }), [
            isFilterSearchLoading,
            isFilterSearchLoadingManual,
            sortingColumn,
            sortingDirection,
            pageSize,
            currentPage,
            fetchFlightsWithFilter,
            updateFlightTags,
        ]
    );

    const flightsFilterSearchContextValue: FlightsSearchFilterContextValue = {
        isFilterSearchLoading,
        isFilterSearchLoadingManual,
        sortingColumn,
        sortingDirection,
        pageSize,
        currentPage,

        setSortingColumn,
        setSortingDirection,
        setPageSize,
        setCurrentPage,

        fetchFlightsWithFilter,
        updateFlightTags,
    };

    const flightsFilterContextValue: FlightsFilterContextValue = {
        filter: filterState.filter,
        filterSearched: filterSearched,

        setFilter,
        setFilterFromJSON,
        revertFilter,
        copyFilterURL,
        addFlightIDToFilter,
        flightIDInSpecialGroup,
        newID,
        filterIsEmpty,
        filterIsValid,
    };

    const selectedIds = useMemo(
        () => new Set(chartState.chartFlights.map(f => f.id)),
        [chartState.chartFlights],
    );

    const flightsChartContextValue = useMemo<FlightsChartContextValue>(() => ({
        chartFlights,
        chartData: chartState.chartData,
        chartSelection: chartState.chartSelection,
        eventSelection: chartState.eventSelection,
        selectedIds,
        setChartFlights,
        ensureSeries,
        toggleUniversalParam,
        togglePerFlightParam,
        toggleUniversalEvent,
        togglePerFlightEvent,
    }), [
        chartFlights,
        chartState.chartData,
        chartState.chartSelection,
        chartState.eventSelection,
        selectedIds,
        setChartFlights,
        ensureSeries,
        toggleUniversalParam,
        togglePerFlightParam,
        toggleUniversalEvent,
        togglePerFlightEvent,
    ]);


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

        void fetchFlightsWithFilter(filterSearched, false);

    }, [sortingColumn, sortingDirection, pageSize, currentPage, filterSearched, fetchFlightsWithFilter]);


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


    const renderSectionToggleButton = (Icon: React.ElementType, isActive: boolean, tooltipName:string,  toggleMethod: (isActive: boolean) => void, buttonClass = "") => (

        <Tooltip>
            <TooltipTrigger asChild>
                <Button variant="outline" onClick={() => toggleMethod(!isActive)} className={`relative ${buttonClass} w-9`}>
                    <Icon className={`${isActive ? 'opacity-100' : 'opacity-25'}`} />
                    {
                        (!isActive)
                        &&
                        <Slash className="absolute" />
                    }
                </Button>
            </TooltipTrigger>
            <TooltipContent>
                Toggle {tooltipName} Panel
            </TooltipContent>
        </Tooltip>

    );



    const modalChartListenersRef = useRef(new Set<() => void>());
    const modalChartSnapshotRef = useRef<FlightsModalChartStoreSnapshot>({
        chartFlights: [],
        eventSelection: { universalEvents: new Set(), perFlightEvents: {} },
    });

    const modalChartStore = useMemo<FlightsModalChartStore>(() => ({

        subscribe: (listener) => {
            modalChartListenersRef.current.add(listener);
            return () => modalChartListenersRef.current.delete(listener);
        },
        getSnapshot: () => modalChartSnapshotRef.current,

    }), []);

    // Update snapshot and notify whenever the relevant state changes
    useEffect(() => {

        // Use a new object so useSyncExternalStore detects a changed snapshot reference
        modalChartSnapshotRef.current = {
            chartFlights: chartState.chartFlights,
            eventSelection: chartState.eventSelection,
        };

        for (const listener of modalChartListenersRef.current)
            listener();

    }, [chartState.chartFlights, chartState.eventSelection]);

    const expandSelectedFlightsModal = () => {

        setModal(FlightsSelectedModal, {
            chartStore: modalChartStore,
            setChartFlights,
            toggleUniversalEvent,
            togglePerFlightEvent,
        });

    }

    const noChartFlightsSelected = (chartFlights.length === 0);
    const navbarExtras = (
        <>
            {/* Search Area Toggle */}
            {renderSectionToggleButton(Search, searchPanelVisible, "Search", setSearchPanelVisible)}

            {/* Chart Toggle */}
            {renderSectionToggleButton(ChartArea, chartPanelVisible, "Chart", setChartPanelVisible)}

            {/* Cesium Toggle */}
            {renderSectionToggleButton(Earth, cesiumPanelVisible, "Cesium", setCesiumPanelVisible)}

            {/* Map Toggle & Select */}
            {renderSectionToggleButton(MapIcon, mapPanelVisible, "Map", setMapPanelVisible)}

            {/* Selected Flights */}
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        id="expand-chart-items-button"
                        variant="ghost"
                        onClick={expandSelectedFlightsModal}
                        className={`p-2 ${noChartFlightsSelected ? "opacity-0 pointer-events-none" : "opacity-100"} transition-opacity`}
                        inert={noChartFlightsSelected ? true : false}
                    >
                        <List />
                        <span>
                            {chartFlights.length}&nbsp;
                            <span className="@max-3xl:hidden!">Selected</span>
                        </span>

                        {/* {gotChartFlightAdded && <Ping />} */}
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Expand Selected Flights & Events</TooltipContent>
            </Tooltip>

        </>
    );

    const commands = useMemo<CommandData[]>(() => ([

        /* -- Panel Toggle Commands -- */
        {
            id: "flights.toggleSearchPanel",
            name: "Toggle Search Panel",
            Icon: Search,
            command: () => {
                log.info("Toggling Search Panel via command...");
                setSearchPanelVisible((prev) => !prev);
            },
            hotkey: "Ctrl+Shift+F",

        },
        {
            id: "flights.toggleChartPanel",
            name: "Toggle Chart Panel",
            Icon: ChartArea,
            command: () => {
                log.info("Toggling Chart Panel via command...");
                setChartPanelVisible((prev) => !prev);
            },
            hotkey: "Ctrl+Shift+H",
        },
        {
            id: "flights.toggleCesiumPanel",
            name: "Toggle Cesium Panel",
            Icon: Earth,
            command: () => {
                log.info("Toggling Cesium Panel via command...");
                setCesiumPanelVisible((prev) => !prev);
            },
            hotkey: "Ctrl+Shift+E",
        },
        {
            id: "flights.toggleMapPanel",
            name: "Toggle Map Panel",
            Icon: MapIcon,
            command: () => {
                log.info("Toggling Map Panel via command...");
                setMapPanelVisible((prev) => !prev);
            },
            hotkey: "Ctrl+Shift+M",
        },
        
    ]), [setSearchPanelVisible, setChartPanelVisible, setCesiumPanelVisible, setMapPanelVisible]);
    useRegisterCommands(commands);

    const render = () => (
        <FlightsSearchFilterContext.Provider value={ flightsSearchContextValue }>
            <FlightsFilterContext.Provider value={flightsFilterContextValue}>
                <FlightsResultsContext.Provider value={ flightsResultsContextValue }>
                    <FlightsChartContext.Provider value={ flightsChartContextValue }>
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
                                                            initial={panelInitial}
                                                            animate={panelAnimate}
                                                            exit={panelExit}
                                                            transition={spring}
                                                            className="w-full h-full min-h-0"
                                                        >
                                                            <FlightsPanelResults />
                                                        </motion.div>

                                                    </ResizablePanel>

                                                </LayoutGroup>

                                            </ResizablePanelGroup>
                                        </ResizablePanel>

                                        {/* Analysis Section */}
                                        <>
                                            <ResizableHandle
                                                withHandle
                                                className={`${analysisAreaOpen ? "opacity-100" : "opacity-0 pointer-events-none"} transition-opacity`}
                                            />

                                            <ResizablePanel
                                                ref={analysisPanelRef}
                                                collapsible
                                                collapsedSize={0}
                                                minSize={0}
                                                defaultSize={40}
                                                className={`min-w-0 overflow-hidden ${analysisAreaOpen ? "opacity-100" : "opacity-0 pointer-events-none"} transition-opacity`}
                                            >
                                                <motion.div
                                                    layout
                                                    layoutRoot
                                                    initial={false}
                                                    className={`relative grid grid-cols-1 gap-4 h-full min-h-0`}
                                                    style={{gridTemplateRows: `repeat(${analysisPanelGridRowCount}, 1fr)`}}
                                                >
                                                    <LayoutGroup id="analysis-panels">
                                                        <AnimatePresence
                                                            initial={false}
                                                            mode="popLayout"
                                                            onExitComplete={() => {
                                                                if (!anyAnalysisPanelVisibleRef.current) {
                                                                    analysisPanelRef.current?.collapse();
                                                                    setAnalysisAreaOpen(false);
                                                                }
                                                            }}
                                                        >

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
                                                                    style={{transformOrigin: "top"}}
                                                                    className="w-full h-full min-h-0"
                                                                >
                                                                    <FlightsPanelChart />
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
                                                                    style={{transformOrigin: "top"}}
                                                                    className="w-full h-full min-h-0"
                                                                >
                                                                    <FlightsPanelCesium />
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
                                                                    style={{transformOrigin: "top"}}
                                                                    className="w-full h-full min-h-0"
                                                                >
                                                                    <FlightsPanelMap />
                                                                </motion.div>
                                                            }

                                                        </AnimatePresence>
                                                    </LayoutGroup>
                                                </motion.div>
                                            </ResizablePanel>
                                        </>


                                    </ResizablePanelGroup>

                                </motion.div>
                            </AnimatePresence>
                        </div>
                    </FlightsChartContext.Provider>
                </FlightsResultsContext.Provider>
            </FlightsFilterContext.Provider>
        </FlightsSearchFilterContext.Provider>
    );


    log("Rendering Flights Page");
    return render();

}