// ngafid-frontend/src/app/pages/protected/flights/flights.tsx
'use client';

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import SuccessModal from "@/components/modals/success_modal";
import { NavbarExtras } from "@/components/navbars/navbar_slot";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { base64urlToU8, fromWire } from "@/pages/protected/flights/_filters/flights_filter_copy_helpers";
import { Filter } from "@/pages/protected/flights/_filters/types";
import FlightsPanelMap from "@/pages/protected/flights/_panels/flights_panel_map";
import FlightsPanelResults from "@/pages/protected/flights/_panels/flights_panel_results";
import { ChartArea, Earth, Map, Search, Slash } from "lucide-react";
import { AnimatePresence, LayoutGroup, motion } from "motion/react";
import pako from "pako";
import { createContext, useContext, useEffect, useRef, useState } from "react";
import type { ImperativePanelHandle } from "react-resizable-panels";
import FlightsPanelSearch from "./_panels/flights_panel_search";


const log = getLogger("Flights", "black", "Page");


export const FILTER_RULE_NAME_NEW = "New Rule";

type FlightsState = {
    allowSearchSubmit: boolean;
    filter: Filter;
};
type FlightsContextValue = FlightsState & {
    setFilter: (updater: (prev: Filter) => Filter) => void;
    setFilterFromJSON: (json: string) => void;
    filterIsEmpty: (filter: Filter) => boolean;
    filterIsValid: (filter: Filter) => boolean;
    newID: () => string;
};


const FlightsContext = createContext<FlightsContextValue | null>(null);


export default function FlightsPage() {

    const { setModal } = useModal();

    // Animation Config
    const spring = { type: "spring" as const, stiffness: 420, damping: 34, mass: 0.6 };
    const panelInitial = { opacity: 0.00, scale: 0.00 };
    const panelAnimate = { opacity: 1.00, scale: 1.00 };
    const panelExit = { opacity: 0.00, scale: 0.00 };

    // Layout State
    const [isColumnalLayout, setIsColumnalLayout] = useState(true);
    const [searchPanelVisible, setSearchPanelVisible] = useState(true);

    // Analysis Panels
    const [chartPanelVisible, setChartPanelVisible] = useState(false);
    const [cesiumPanelVisible, setCesiumPanelVisible] = useState(false);
    const [mapPanelVisible, setMapPanelVisible] = useState(false);

    const anyAnalysisPanelVisible = (chartPanelVisible || cesiumPanelVisible || mapPanelVisible);
    const analysisPanelCount = (chartPanelVisible ? 1 : 0) + (cesiumPanelVisible ? 1 : 0) + (mapPanelVisible ? 1 : 0);
    const analysisSectionGridClasses = (isColumnalLayout) ? `grid-cols-1 grid-rows-${analysisPanelCount}` : `grid-rows-1 grid-cols-${analysisPanelCount}`;

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
                allowSearchSubmit: filterIsValid(parsed),
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



    // Flights State
    const [allowSearchSubmit, setAllowSearchSubmit] = useState(false);
    const [state, setState] = useState<FlightsState>({
        allowSearchSubmit: allowSearchSubmit,
        filter: makeEmpty(),
    });

    const setFilter: FlightsContextValue["setFilter"] = (updater) => {

        const updatedFilter = updater(state.filter);
        log("Setting new filter:", updatedFilter);

        setState((prev) => ({
            ...prev,
            filter: updatedFilter,
            allowSearchSubmit: filterIsValid(updatedFilter),
        }));

    };

    const setFilterFromJSON: FlightsContextValue["setFilterFromJSON"] = (json) => {

        const parsed = JSON.parse(json);
        setFilter(() => parsed);
        
    }

    const value: FlightsContextValue = {
        allowSearchSubmit: state.allowSearchSubmit,
        filter: state.filter,
        setFilter,
        setFilterFromJSON,
        filterIsEmpty: filterIsEmpty,
        filterIsValid: filterIsValid,
        newID,
    };


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

                        <ResizablePanelGroup direction={isColumnalLayout ? "horizontal" : "vertical"} className="gap-2">

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