// ngafid-frontend/src/app/pages/protected/flights/flights.tsx
'use client';

import { useModal } from "@/components/modals/modal_provider";
import ProtectedNavbar from "@/components/navbars/protected_navbar";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ChartArea, Earth, MoveHorizontal, Search, Map, MoveVertical, Slash } from "lucide-react";
import { AnimatePresence, LayoutGroup, motion, MotionConfig } from "motion/react";
import { useState } from "react";

export default function UploadsPage() {

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

    // const searchSectionGridClasses = (searchPanelVisible) ? 'grid-cols-1 grid-rows-2' : 'grid-cols-1 grid-rows-1';
    // const searchSectionGridClasses = (searchPanelVisible) ? 'flex-col' : 'flex-row';

    const anyAnalysisPanelVisible = (chartPanelVisible || cesiumPanelVisible || mapPanelVisible);
    const analysisPanelCount = (chartPanelVisible ? 1 : 0) + (cesiumPanelVisible ? 1 : 0) + (mapPanelVisible ? 1 : 0);
    const analysisSectionGridClasses = (isColumnalLayout) ? `grid-cols-1 grid-rows-${analysisPanelCount}` : `grid-rows-1 grid-cols-${analysisPanelCount}`;

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
        <div className="page-container">
            <AnimatePresence mode="wait">
                <ProtectedNavbar>

                    {/* Layout Toggle */}
                    <Button variant="outline" size="icon" onClick={() => setIsColumnalLayout(!isColumnalLayout)}>
                        {
                            (isColumnalLayout)
                                ? <MoveHorizontal />
                                : <MoveVertical />
                        }
                    </Button>

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

                <div className="page-content gap-4">

                    <ResizablePanelGroup direction={isColumnalLayout ? "horizontal" : "vertical"} className="gap-2">

                        {/* Search Section */}
                        <ResizablePanel>

                            <ResizablePanelGroup direction="vertical" className="gap-2 h-full w-full">

                                <AnimatePresence initial={false} mode="popLayout">
                                    {
                                        (searchPanelVisible)
                                        &&
                                        <ResizablePanel key="search">
                                            <motion.div
                                                layout
                                                initial={panelInitial}
                                                animate={panelAnimate}
                                                exit={panelExit}
                                                transition={spring}
                                                className="w-full h-full "
                                            >
                                                <Card className="w-full h-full">
                                                    <CardHeader>
                                                        <CardTitle>Search Panel</CardTitle>
                                                        <CardDescription>Search for flights to view</CardDescription>
                                                    </CardHeader>
                                                </Card>
                                            </motion.div>
                                        </ResizablePanel>
                                    }
                                </AnimatePresence>

                                <ResizablePanel>
                                    <motion.div
                                        layout
                                        initial={panelInitial}
                                        animate={panelAnimate}
                                        exit={panelExit}
                                        transition={spring}
                                        className="w-full h-full"
                                    >
                                        <Card className="w-full h-full">
                                            <CardHeader>
                                                <CardTitle>Results Panel</CardTitle>
                                                <CardDescription>Search for flights to view</CardDescription>
                                            </CardHeader>
                                        </Card>
                                    </motion.div>
                                </ResizablePanel>
                            </ResizablePanelGroup>
                        </ResizablePanel>


                        {
                            (anyAnalysisPanelVisible)
                            &&
                            <>
                                <ResizableHandle withHandle />

                                <ResizablePanel>
                                    <motion.div
                                        layout
                                        layoutRoot
                                        initial={false}
                                        animate={{}}
                                        transition={{ layout: spring, staggerChildren: 0.05 }}
                                        className={`grid ${analysisSectionGridClasses} gap-4 h-full`}
                                    >
                                        <LayoutGroup id="analysis-panels">
                                            <AnimatePresence initial={false} mode="popLayout">

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
                                                        <Card className="p-4 border rounded-lg w-full h-full">
                                                            Chart Panel
                                                        </Card>
                                                    </motion.div>
                                                }

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
                                                        <Card className="p-4 border rounded-lg w-full h-full">
                                                            Cesium Panel
                                                        </Card>
                                                    </motion.div>
                                                }

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
                                                        <Card className="p-4 border rounded-lg w-full h-full">
                                                            Map Panel
                                                        </Card>
                                                    </motion.div>
                                                }
                                            </AnimatePresence>
                                        </LayoutGroup>
                                    </motion.div>
                                </ResizablePanel>
                            </>
                        }


                    </ResizablePanelGroup>

                </div>
            </AnimatePresence>
        </div>
    );

    return render();

}