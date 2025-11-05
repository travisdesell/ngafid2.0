// ngafid-frontend/src/app/pages/protected/flights/flights.tsx
'use client';

import { useModal } from "@/components/modals/modal_provider";
import ProtectedNavbar from "@/components/navbars/protected_navbar";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Filter } from "@/pages/protected/flights/_filters/types";
import FlightsPanelMap from "@/pages/protected/flights/_panels/flights_panel_map";
import FlightsPanelResults from "@/pages/protected/flights/_panels/flights_panel_results";
import { ChartArea, Earth, Map, Search, Slash } from "lucide-react";
import { AnimatePresence, LayoutGroup, motion } from "motion/react";
import { createContext, useContext, useEffect, useRef, useState } from "react";
import type { ImperativePanelHandle } from "react-resizable-panels";
import FlightsPanelSearch from "./_panels/flights_panel_search";


const log = getLogger("Flights", "black", "Page");


type FlightsState = {
    allowSearchSubmit: boolean;
    filter: Filter;
};
type FlightsContextValue = FlightsState & {
    setFilter: (updater: (prev: Filter) => Filter) => void;
    filterIsEmpty: (filter: Filter) => boolean;
    newID: () => string;
};


const FlightsContext = createContext<FlightsContextValue|null>(null);


export default function FlightsPage() {

    const { setModal } = useModal();

    const spring = { type: "spring" as const, stiffness: 420, damping: 34, mass: 0.6 };
    const panelInitial = { opacity: 0.00, scale: 0.00 };
    const panelAnimate = { opacity: 1.00, scale: 1.00 };
    const panelExit    = { opacity: 0.00, scale: 0.00 };

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
        if (filter.rules && filter.rules.length > 0)
            return false;
        if (filter.groups && filter.groups.length > 0)
            return false;
        return true;
    }

    const newID = () => ((typeof crypto !== "undefined") && crypto.randomUUID)
        ? crypto.randomUUID()
        : Math.random().toString(36).slice(2);
        


    // Flights State
    const [allowSearchSubmit, setAllowSearchSubmit] = useState(false);
    const [state, setState] = useState<FlightsState>({
        allowSearchSubmit: allowSearchSubmit,
        filter: {
            id: newID(),
            operator: "AND",
            rules: [],
            groups: []
        }
    });

    const setFilter: FlightsContextValue["setFilter"] = (updater) => {
        setState((prev) => ({ ...prev, filter: updater(prev.filter) }));
    };

    const value: FlightsContextValue = {
        allowSearchSubmit: state.allowSearchSubmit,
        filter: state.filter,
        setFilter,
        filterIsEmpty: filterIsEmpty,
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

    const render = () => (
        <FlightsContext.Provider value={value}>
            <div className="page-container">
                <AnimatePresence mode="sync">

                    <motion.div key="flights-page-navbar" layout>
                        <ProtectedNavbar>

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

                        </ProtectedNavbar>
                    </motion.div>

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