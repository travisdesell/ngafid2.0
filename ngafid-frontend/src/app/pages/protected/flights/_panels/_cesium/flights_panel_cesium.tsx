// ngafid-frontend/src/app/pages/protected/flights/_panels/_cesium/flights_panel_cesium.tsx
"use client";

import { useEffect, useMemo, useRef, useState } from "react";

import { getLogger } from "@/components/providers/logger";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { fetchJson } from "@/fetchJson";
import type { Flight } from "@/pages/protected/flights/types";

import { AnimatePresence, motion } from "motion/react";

import {
    Eye,
    EyeOff,
    Gauge,
    Globe2,
    Home,
    Info,
    List,
    Loader,
    Plane,
    Satellite
} from "lucide-react";


import { Clock, Entity, Globe, Scene, SkyAtmosphere, Viewer } from "resium";

import { CesiumListModal } from "@/components/modals/cesium_list_modal/cesium_list_modal";
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import Ping from "@/components/pings/ping";
import { Select, SelectContent, SelectItem, SelectTrigger } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import {
    Cartesian3,
    Color,
    Ion,
    IonResource,
    JulianDate,
    ModelGraphics,
    PathGraphics,
    PolylineOutlineMaterialProperty,
    SampledPositionProperty,
    TimeInterval,
    TimeIntervalCollection,
    VelocityOrientationProperty,
} from "cesium";
import { useFlightsChart } from "../../_flights_context_chart";

const log = getLogger("FlightsPanelCesium", "red", "Component");

/* ============================================================================
   Cesium data types
============================================================================ */

type CesiumFlightData = {
    startTime: string;                // ISO 8601
    endTime: string;                  // ISO 8601
    airframeType?: string;
    flightGeoInfoAgl: number[];       // [lon, lat, alt, ...]
    flightAglTimes: string[];         // ISO 8601 timestamps aligned with positions
    flightGeoAglTaxiing?: number[];
    flightGeoAglTakeOff?: number[];
    flightGeoAglClimb?: number[];
    flightGeoAglCruise?: number[];
};

type CesiumDataResponse = Record<number, CesiumFlightData>;

// Internal, per-flight cesium state
type CesiumFlightState = {
    data?: CesiumFlightData;
    error?: string;
    loading: boolean;
};

type AirframeModels = {
    Airplane?: ModelGraphics;
    Drone?: ModelGraphics;
};

/* ============================================================================
   Helpers
============================================================================ */

const CESIUM_ACCESS_TOKEN =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI3OTU3MDVjYS04ZGRiLTRkZmYtOWE5ZC1lNTEzMTZlNjE5NWEiLCJpZCI6OTYyNjMsImlhdCI6MTY1NDMxNTk2N30.c_n2k_FWWisRoXnAFVGs6Nbxk0NVFmrIpqL12kjE7sA";

const CESIUM_ASSET_AIRPLANE = 1084423;
const CESIUM_ASSET_DRONE = 1117220;

const CESIUM_DATA_ENDPOINT = "/api/cesium/data";

const RESOLUTION_OPTIONS: Array<{ label: string; value: "default" | number }> = [
    { label: "Browser Default", value: "default" },
    { label: "50%", value: 0.5 },
    { label: "100%", value: 1.0 },
    { label: "200%", value: 2.0 },
];

const colorForFlightIndex = (index: number): string => {
    const palette = [
        "#783CFF",
        "#0040E6",
        "#1E78D7",
        "#3CB8C8",
        "#00FFFF",
        "#FFC400",
        "#FFB000",
        "#E68000",
        "#C04000",
        "#9C0000",
        "#FF0000",
        "#FF4081",
    ];
    return palette[index % palette.length]!;
};

const buildPositionProperty = (flightData: CesiumFlightData): SampledPositionProperty | null => {
    const { flightGeoInfoAgl, flightAglTimes } = flightData;

    if (!flightGeoInfoAgl || !flightAglTimes || flightAglTimes.length === 0)
        return null;

    const positions = Cartesian3.fromDegreesArrayHeights(flightGeoInfoAgl);

    const positionProperty = new SampledPositionProperty();
    const sampleCount = Math.min(positions.length, flightAglTimes.length);

    for (let i = 0; i < sampleCount; i++) {
        const timeIso = flightAglTimes[i];
        const pos = positions[i];

        if (!timeIso || !pos)
            continue;

        positionProperty.addSample(JulianDate.fromIso8601(timeIso), pos);
    }

    return positionProperty;
};

const buildAirframeModel = (airframeType: string | undefined, models: AirframeModels): ModelGraphics | undefined => {
    if (!airframeType)
        return models.Airplane;

    if (airframeType === "Fixed Wing" || airframeType === "UAS Fixed Wing")
        return models.Airplane;

    if (airframeType === "UAS Rotorcraft")
        return models.Drone;

    return models.Airplane;
};

/* ============================================================================
   Per-flight entity group
============================================================================ */

type FlightEntitiesProps = {
    flight: Flight;
    cesiumData: CesiumFlightData;
    colorHex: string;
    models: AirframeModels;
};

function FlightEntities({ cesiumData, colorHex, models }: FlightEntitiesProps) {

    const { startTime, endTime, airframeType, flightGeoInfoAgl } = cesiumData;

    const startJulian = JulianDate.fromIso8601(startTime);
    const endJulian = JulianDate.fromIso8601(endTime);

    const availability = new TimeIntervalCollection([
        new TimeInterval({ start: startJulian, stop: endJulian }),
    ]);

    const position = buildPositionProperty(cesiumData);

    const pathColor = Color.fromCssColorString(colorHex).withAlpha(0.8);
    const outlinePathColor = pathColor;

    const pathGraphics = new PathGraphics({
        width: 5,
        material: new PolylineOutlineMaterialProperty({
            color: pathColor,
            outlineColor: outlinePathColor,
            outlineWidth: 5,
        }),
    });

    const groundPositions = Cartesian3.fromDegreesArrayHeights(flightGeoInfoAgl);
    const groundMaterial = new PolylineOutlineMaterialProperty({
        color: pathColor.withAlpha(0.6),
        outlineColor: Color.BLACK.withAlpha(0.9),
        outlineWidth: 2,
    });

    const modelGraphics = buildAirframeModel(airframeType, models);

    if (!position) {
        return (
            <Entity
                polyline={{
                    positions: groundPositions,
                    width: 3,
                    material: groundMaterial,
                    clampToGround: true,
                }}
            />
        );
    }

    return (
        <>
            <Entity
                availability={availability}
                position={position}
                orientation={new VelocityOrientationProperty(position)}
                model={modelGraphics}
                path={pathGraphics}
            />
            <Entity
                polyline={{
                    positions: groundPositions,
                    width: 3,
                    material: groundMaterial,
                    clampToGround: true,
                }}
            />
        </>
    );
}

/* ============================================================================
   Main Panel
============================================================================ */

export default function FlightsPanelCesium() {

    const { setModal } = useModal();
    const { chartFlights, setChartFlights } = useFlightsChart();

    const viewerRef = useRef<any>(null);

    const [airframeModels, setAirframeModels] = useState<AirframeModels>({});
    const [perFlightState, setPerFlightState] = useState<Record<number, CesiumFlightState>>({});
    const [visibleFlightIds, setVisibleFlightIds] = useState<Set<number>>(new Set());

    const [useDefaultResolution, setUseDefaultResolution] = useState(true);
    const [resolutionScale, setResolutionScale] = useState(1.0);

    const [globalError, setGlobalError] = useState<string | null>(null);

    const hasEverZoomedRef = useRef(false);

    /* ------------------------------------------------------------------------
       Cesium initialization (token + models)
    ------------------------------------------------------------------------ */

    useEffect(() => {

        Ion.defaultAccessToken = CESIUM_ACCESS_TOKEN;

        let cancelled = false;

        const loadModels = async () => {

            log("Attempting to load Cesium models...");

            try {

                const airplaneUri = await IonResource.fromAssetId(CESIUM_ASSET_AIRPLANE);
                const droneUri = await IonResource.fromAssetId(CESIUM_ASSET_DRONE);

                if (cancelled) {
                    log("Loading Cesium models cancelled.");
                    return;
                }

                setAirframeModels({
                    Airplane: new ModelGraphics({
                        uri: airplaneUri,
                        minimumPixelSize: 64,
                        maximumScale: 20_000,
                        scale: 0.5,
                    }),
                    Drone: new ModelGraphics({
                        uri: droneUri,
                        minimumPixelSize: 64,
                        maximumScale: 20_000,
                        scale: 0.5,
                    }),
                });

                log("Cesium models loaded.");

            } catch (error) {

                setModal(ErrorModal, {
                    title: "Error Loading Cesium Models",
                    code: (error as Error)?.message,
                    message:
                        "An error occurred while loading 3D models for the Cesium globe. " +
                        "The globe will still render flight paths, but without 3D aircraft models.",
                });

            }

        };

        loadModels();

        return () => {
            cancelled = true;
        };
    }, []);

    /* ------------------------------------------------------------------------
       Sync visible flight IDs with chartFlights selection
    ------------------------------------------------------------------------ */

    useEffect(() => {

        setVisibleFlightIds((prev) => {

            const next = new Set(prev);

            // // Add new chart flights
            // for (const f of chartFlights)
            //     next.add(f.id);

            // // Remove ids no longer present in chartFlights
            // for (const id of Array.from(next)) {
            //     if (!chartFlights.some((f) => f.id === id))
            //         next.delete(id);
            // }

            return next;

        });

    }, [chartFlights]);

    /* ------------------------------------------------------------------------
       Fetch Cesium data for visible flights
    ------------------------------------------------------------------------ */

    const fetchCesiumDataForFlight = async (flightId: number) => {

        log("Attempting to fetch Cesium data for flight:", flightId);

        setPerFlightState((prev) => ({
            ...prev,
            [flightId]: {
                ...prev[flightId],
                loading: true,
                error: undefined,
            },
        }));

        try {

            const form = new URLSearchParams();
            form.set("flightId", String(flightId));

            const response = await fetchJson.post<CesiumDataResponse>(CESIUM_DATA_ENDPOINT, form);
            const data = response?.[flightId];

            if (!data) {
                throw new Error("No Cesium data returned for this flight.");
            }

            setPerFlightState((prev) => ({
                ...prev,
                [flightId]: {
                    data,
                    loading: false,
                    error: undefined,
                },
            }));

            log("Loaded Cesium data for flight:", flightId);

        } catch (error: any) {

            log.error("Error fetching Cesium data:", { flightId, error });

            const message =
                error?.message ??
                "An unknown error occurred while loading Cesium data.";

            setPerFlightState((prev) => ({
                ...prev,
                [flightId]: {
                    ...prev[flightId],
                    loading: false,
                    error: message,
                },
            }));

            // setGlobalError("One or more flights could not be loaded in the Cesium viewer.");

            setModal(ErrorModal, {
                title: "Error Loading Cesium Data",
                code: (error as Error)?.message,
                message:
                    "An error occurred while loading Cesium data for one or more flights. " +
                    "Some flights may not be visible in the Cesium viewer.",
            });

        }

    };

    useEffect(() => {

        // for (const flight of chartFlights) {

        //     const id = flight.id;

        //     if (!visibleFlightIds.has(id))
        //         continue;

        //     const state = perFlightState[id];

        //     const alreadyLoaded = !!state?.data;
        //     const alreadyLoading = state?.loading;

        //     if (!alreadyLoaded && !alreadyLoading)
        //         fetchCesiumDataForFlight(id);

        // }

    }, [chartFlights, visibleFlightIds, perFlightState]);

    /* ------------------------------------------------------------------------
       Derived data
    ------------------------------------------------------------------------ */

    const visibleFlightsWithData: Array<{
        flight: Flight;
        data: CesiumFlightData;
        colorHex: string;
        index: number;
    }> = useMemo(() => {

        const out: Array<{
            flight: Flight;
            data: CesiumFlightData;
            colorHex: string;
            index: number;
        }> = [];

        // chartFlights.forEach((flight, idx) => {

        //     if (!visibleFlightIds.has(flight.id))
        //         return;

        //     const state = perFlightState[flight.id];
        //     if (!state?.data)
        //         return;

        //     out.push({
        //         flight,
        //         data: state.data,
        //         colorHex: colorForFlightIndex(idx),
        //         index: idx,
        //     });

        // });

        return out;

    }, [chartFlights, visibleFlightIds, perFlightState]);

    const hasAnyVisibleFlights = visibleFlightIds.size > 0;
    const hasAnyFlightsWithData = visibleFlightsWithData.length > 0;

    /* ------------------------------------------------------------------------
       Auto zoom on first data
    ------------------------------------------------------------------------ */

    useEffect(() => {

        if (hasEverZoomedRef.current)
            return;

        if (!hasAnyFlightsWithData)
            return;

        const viewer = viewerRef.current?.cesiumElement;
        if (!viewer)
            return;

        hasEverZoomedRef.current = true;

        try {
            viewer.zoomTo(viewer.entities);
        } catch (error) {
            log.warn("ZoomTo viewer.entities failed:", error);
        }

    }, [hasAnyFlightsWithData]);

    /* ------------------------------------------------------------------------
       Controls
    ------------------------------------------------------------------------ */

    const handleToggleFlightVisibility = (flightId: number) => {

        setVisibleFlightIds((prev) => {

            const next = new Set(prev);
            if (next.has(flightId))
                next.delete(flightId);
            else
                next.add(flightId);
            return next;

        });

    };

    const handleResetView = () => {

        const viewer = viewerRef.current?.cesiumElement;
        if (!viewer) {
            log.warn("Cannot reset view, missing viewer.");
            return;
        }

        try{

            // Viewer has entities, zoom to them
            if (viewer.entities && viewer.entities.values.length > 0)
                viewer.zoomTo(viewer.entities);

            // Otherwise, fly home
            else {

                const FLY_HOME_DURATION_S = 2;
                
                viewer.trackedEntity = undefined;           // stop tracking if any
                viewer.camera.cancelFlight();               // cancel any in-progress flight
                viewer.camera.flyHome(FLY_HOME_DURATION_S); // duration in seconds (tweak as desired)

            }

        } catch (error) {

            setModal(ErrorModal, {
                title: "Reset View Error",
                message: "An error occurred while resetting the Cesium viewer's camera.",
            });

        }

    };

    const handleJumpToFlightStart = (flightId: number) => {

        log("Attempting to jump to start of flight:", flightId);

        const flightState = perFlightState[flightId];
        const viewer = viewerRef.current?.cesiumElement;

        if (!flightState?.data || !viewer) {
            // log.warn("Cannot jump to flight start, missing data or viewer:", { flightId, flightState, viewer });
            setModal(ErrorModal, {
                title: "Jump to Start Error",
                message: "Cannot jump to the start of the selected flight. Data may still be loading.",
                code: {
                    flightId,
                    hasFlightData: !!flightState?.data,
                    hasViewer: !!viewer,
                },
            });
            return;
        }

        const flightData = flightState.data;
        const start = JulianDate.fromIso8601(flightData.startTime);
        const end = JulianDate.fromIso8601(flightData.endTime);

        viewer.clock.startTime = start.clone();
        viewer.clock.currentTime = start.clone();
        viewer.clock.stopTime = end.clone();
        viewer.clock.shouldAnimate = true;

        try {
            
            viewer.timeline?.zoomTo(start.clone(), end.clone());

        } catch (error) {
            
            setModal(ErrorModal, {
                title: "Timeline Zoom Error",
                message: "An error occurred while zooming the timeline to the selected flight.",
            });

        }

    };

    const handleResolutionChange = (value: "default" | number) => {

        log("Changing Viewer resolution...")

        if (value === "default") {
            setUseDefaultResolution(true);
            setResolutionScale(1.0);
        } else {
            setUseDefaultResolution(false);
            setResolutionScale(value);
        }

    };

    /* ------------------------------------------------------------------------
       Overlays
    ------------------------------------------------------------------------ */

    const renderStatusOverlay = () => {

        const noFlights = chartFlights.length === 0;
        const noVisible = !noFlights && !hasAnyVisibleFlights;
        const visibleButNoData = hasAnyVisibleFlights && !hasAnyFlightsWithData;

        const baseClass = `
            w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap
            absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2
            ${hasAnyFlights ? 'text-white' : '' /* <-- Always use white text here if the Cesium panel is interactible */}
        `;

        if (noFlights) {
            return (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className={baseClass}
                >
                    <Info />

                    <div className="flex flex-col">
                        <AlertTitle>No Flights Selected</AlertTitle>
                        <AlertDescription>
                            Add flights to the analysis area to view them on the globe.
                            <br />
                            Use the chart selection controls in the results panel.
                        </AlertDescription>
                    </div>
                </motion.div>
            );
        }

        if (noVisible) {
            return (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className={baseClass}
                >
                    <Info />
                    <div className="flex flex-col">
                        <AlertTitle>All Flights Hidden</AlertTitle>
                        <AlertDescription>
                            Use the flight list controls below to toggle flights back on.
                        </AlertDescription>
                    </div>
                </motion.div>
            );
        }

        if (visibleButNoData) {
            return (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className={baseClass}
                >
                    <Info />
                    <div className="flex flex-col">
                        <AlertTitle>Loading Cesium Data…</AlertTitle>
                        <AlertDescription>
                            Fetching globe data for the selected flights.
                        </AlertDescription>
                    </div>
                </motion.div>
            );
        }

        return null;

    };

    const renderLegendOverlay = () => {

        if (!hasAnyFlightsWithData)
            return null;

        return (
            <div
                className="absolute z-10 flex flex-col gap-1 p-2 rounded bg-muted shadow-sm opacity-50 hover:opacity-100 transition-opacity"
                style={{ top: 52, right: 8 }}
            >
                {
                    visibleFlightsWithData.map(({ flight, colorHex }) => (
                        <div key={flight.id} className="flex items-center gap-2 text-xs">
                            <span
                                className="inline-block w-3 h-3 rounded-full"
                                style={{ backgroundColor: colorHex }}
                            />
                            <span>Flight {flight.id}</span>
                        </div>
                    ))
                }
            </div>
        );
    };

    const renderInteractionGuide = () => {

        if (!hasAnyFlightsWithData)
            return null;

        return (
            <Card className="absolute left-24 top-2 w-72 max-h-[90%] overflow-y-auto text-xs">
                <div className="px-4 py-2 font-semibold flex items-center gap-2">
                    <Satellite size={14} />
                    Globe Controls
                </div>
                <div className="px-4 pb-3 space-y-2">
                    <div>
                        <div className="font-bold">Rotate</div>
                        <div>Left-click and drag to rotate the globe.</div>
                    </div>
                    <div>
                        <div className="font-bold">Pan</div>
                        <div>Right-click and drag to pan the camera.</div>
                    </div>
                    <div>
                        <div className="font-bold">Zoom</div>
                        <div>Use the mouse wheel to zoom in and out.</div>
                    </div>
                    <div>
                        <div className="font-bold">Timeline</div>
                        <div>
                            Use the timeline at the bottom of the viewer to scrub through the
                            replay.
                        </div>
                    </div>
                </div>
            </Card>
        );
    };

    const renderNoSelectedFlightsMessage = () => (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="w-fit mx-auto space-x-8 drop-shadow-2xl flex items-center *:text-nowrap absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-white"
        >
            <Info />

            <div className="flex flex-col">
                <AlertTitle>No Data!</AlertTitle>
                <AlertDescription>
                    Data for the selected flights will appear here.
                    <br />
                    <div className="flex">
                        Try using the{" "}
                        <div className="flex items-center font-bold gap-1 mx-2">
                            <Globe2 size={16} />
                            Cesium
                        </div>{" "}
                        button in a flight result row.
                    </div>
                </AlertDescription>
            </div>
        </motion.div>
    );

    const renderFlightListControls = () => {

        const noFlightsSelected = (chartFlights.length === 0);

        return (
            <div className="absolute bottom-9 right-2 flex flex-col gap-2 rounded bg-background/80 backdrop-blur p-2 shadow-sm max-h-[40%] overflow-y-auto w-fit min-w-64">

                <div className="flex items-center justify-between text-xs px-1">
                    <span className="font-semibold flex items-center gap-1">
                        <Plane size={14} />
                        Flights
                    </span>
                    <span className="opacity-70">
                        {
                            (noFlightsSelected)
                            ?
                            "None Selected"
                            :
                            `${visibleFlightIds.size}/${chartFlights.length} visible`
                        }
                    </span>
                </div>

                {
                    chartFlights.map((flight, idx) => {

                        const visible = visibleFlightIds.has(flight.id);
                        const state = perFlightState[flight.id];
                        const hasError = !!state?.error;
                        const loading = !!state?.loading;

                        const colorHex = colorForFlightIndex(idx);

                        return (
                            <div
                                key={flight.id}
                                className="flex items-center justify-between gap-1 px-1 py-1 rounded hover:bg-muted/80 text-xs"
                            >
                                <div className="flex items-center gap-1">

                                    {
                                        // Loading -> Spinner
                                        (loading)
                                        ?
                                        <Loader size={12} className="animate-spin w-5 h-5 opacity-50" />
                                        // Otherwise -> Eye / EyeOff
                                        :
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <Button
                                                    variant="ghost"
                                                    className="inline-flex items-center justify-center w-8 h-8 aspect-square"
                                                    onClick={() => handleToggleFlightVisibility(flight.id)}
                                                    aria-label={visible ? "Hide Flight" : "Show Flight"}
                                                >
                                                    {
                                                        (visible)
                                                            ? <Eye size={16} />
                                                            : <EyeOff size={16} />
                                                    }
                                                </Button>
                                            </TooltipTrigger>
                                            <TooltipContent>
                                                {(visible) ? "Hide Flight" : "Show Flight"}
                                            </TooltipContent>
                                        </Tooltip>
                                    }

                                    {/* Jump to Start Button */}
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <Button
                                                size="icon"
                                                variant="ghost"
                                                className="h-8 w-8 p-0"
                                                onClick={() => handleJumpToFlightStart(flight.id)}
                                                // disabled={!state?.data}
                                            >
                                                <Gauge size={16} />
                                            </Button>
                                        </TooltipTrigger>
                                        <TooltipContent>
                                            Jump to Start
                                        </TooltipContent>
                                    </Tooltip>


                                    {/* Color Indicator */}
                                    {/* <span
                                        className="inline-block w-4 h-4 rounded-full"
                                        style={{ backgroundColor: colorHex }}
                                    /> */}

                                    {/* Flight ID */}
                                    {/* <span>
                                        #{flight.id}
                                    </span> */}

                                    {/* Flight Start */}
                                    <span className="opacity-70 hidden sm:inline">
                                        {new Date(flight.startDateTime).toLocaleString()}
                                    </span>
                                </div>

                                <div className="flex items-center gap-2">

                                    {/* Error Indicator */}
                                    {/* {
                                        (hasError)
                                        &&
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <span className="text-[10px] text-destructive cursor-help">
                                                    Load error
                                                </span>
                                            </TooltipTrigger>
                                            <TooltipContent className="max-w-xs">
                                                {state?.error}
                                            </TooltipContent>
                                        </Tooltip>
                                    } */}
                                    
                                </div>
                            </div>
                        );

                    })
                }

            </div>
        );

    };

    const renderTopRightControls = () => {

        // No flights selected, don't render controls
        if (chartFlights.length === 0)
            return null;

        const renderExpandCesiumItemsButton = () => {

            const expandCesiumItemsModal = () => {

                // setGotChartFlightAdded(false);

                setModal(CesiumListModal, {
                    chartFlights,
                    setChartFlights,
                    // chartSelection,
                    // toggleUniversalParam,
                    // togglePerFlightParam,
                });

            };

            return (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            id="expand-cesium-items-button"
                            variant="ghost"
                            onClick={expandCesiumItemsModal}
                            className="p-2"
                        >
                            <List />
                            <span>
                                {chartFlights.length}&nbsp;
                                <span className="@max-3xl:hidden!">Selected</span>
                            </span>

                            {(false) && <Ping />}
                        </Button>
                    </TooltipTrigger>
                    <TooltipContent>Expand Cesium Items</TooltipContent>
                </Tooltip>
            );

        };


        return (
            <div className="flex gap-2 absolute right-2 top-2 justify-end text-white">

                {/* Reset View */}
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            variant="ghost"
                            className="p-2"
                            onClick={handleResetView}
                        >
                            <Home size={16} />
                            <span className="text-xs ml-1 hidden sm:inline">Reset View</span>
                        </Button>
                    </TooltipTrigger>
                    <TooltipContent>Zoom to all visible flights</TooltipContent>
                </Tooltip>

                {/* Resolution */}
                {/* <Select >
                    <SelectTrigger asChild>
                        <Button variant="ghost" className="text-white">
                            <span>Resolution</span>
                        </Button>
                    </SelectTrigger>

                    <SelectContent>
                    {
                        RESOLUTION_OPTIONS.map((opt) => {

                            // const isActive = (
                            //     (opt.value === "default" && useDefaultResolution)
                            //     || (typeof opt.value === "number" && !useDefaultResolution && resolutionScale === opt.value)
                            // );

                            return (
                                <SelectItem
                                    value={opt.value.toString()}
                                    key={opt.label}
                                    onClick={() => handleResolutionChange(opt.value)}
                                >
                                    {opt.label}
                                </SelectItem>
                            );

                        })
                    }
                    </SelectContent>
                </Select> */}
                <Select
                    value={useDefaultResolution ? "default" : String(resolutionScale)}
                    onValueChange={(v) => handleResolutionChange(v === "default" ? "default" : Number(v))}
                >
                    <SelectTrigger
                        className={cn(buttonVariants({ variant: "ghost" }), "text-white w-auto border-none")}
                    >
                        {/* <SelectValue placeholder="Resolution" /> */}
                        <span>Resolution</span>
                    </SelectTrigger>

                    <SelectContent>
                        {
                            RESOLUTION_OPTIONS.map((opt) => (
                                <SelectItem key={opt.label} value={String(opt.value)}>
                                    {opt.label}
                                </SelectItem>
                            ))
                        }
                    </SelectContent>
                </Select>

                {/* Selected Flights Modal Button */}
                {renderExpandCesiumItemsButton()}


            </div>
        );

    };
    
    /* ------------------------------------------------------------------------
       Render
    ------------------------------------------------------------------------ */

    const hasAnyFlights = (chartFlights.length > 0);

    log("Rendering CesiumPanel.");

    const render = () => {

        return (
            <Card className="border rounded-lg w-full h-full card-glossy relative overflow-hidden [&_.cesium-viewer-fullscreenContainer]:hidden!">

                {/* Legend overlay */}
                {renderLegendOverlay()}

                {/* Cesium Viewer */}
                <div
                    className={`w-full h-full ${hasAnyFlights ? "" : "blur-xs opacity-25 pointer-events-none select-none"} duration-500`}
                    id="cesium-viewer-wrapper-outer"
                >
                    <div
                        className={`w-full h-full`}
                        id="cesium-viewer-wrapper-inner"
                    >
                        <Viewer
                            full
                            homeButton={false}
                            navigationHelpButton={false}
                            geocoder={false}
                            animation={true}
                            ref={viewerRef}
                            timeline={true}
                            scene3DOnly={true}
                            selectionIndicator={true}
                            baseLayerPicker={false}
                            orderIndependentTranslucency={false}
                            useBrowserRecommendedResolution={useDefaultResolution}
                            resolutionScale={resolutionScale}
                        >
                            <Clock />
                            <Scene />
                            <SkyAtmosphere
                                hueShift={0.0}
                                saturationShift={0.0}
                                brightnessShift={0.0}
                            />
                            <Globe
                                depthTestAgainstTerrain={true}
                                atmosphereHueShift={0.0}
                                atmosphereSaturationShift={0.0}
                                atmosphereBrightnessShift={0.0}
                            />

                            {
                                visibleFlightsWithData.map(({ flight, data, colorHex }) => (
                                    <FlightEntities
                                        key={flight.id}
                                        flight={flight}
                                        cesiumData={data}
                                        colorHex={colorHex}
                                        models={airframeModels}
                                    />
                                ))
                            }

                        </Viewer>
                    </div>
                </div>

                {/* Status overlays (no flights / loading / etc.) */}
                <AnimatePresence mode="wait" initial={false}>
                    {!hasAnyFlights || !hasAnyFlightsWithData ? renderStatusOverlay() : null}
                </AnimatePresence>

                {/* Interaction guide */}
                {renderInteractionGuide()}

                {/* Top-right controls */}
                {renderTopRightControls()}

                {/* Flight list controls (bottom overlay) */}
                {renderFlightListControls()}

                {/* Global error indicator */}
                {
                    globalError && (
                        <div className="absolute left-2 top-2 text-[10px] text-destructive bg-background/80 px-2 py-1 rounded shadow-sm max-w-xs">
                            {globalError}
                        </div>
                    )
                }

                {/* No Flights Message [EX] */}
                {/* {(!hasAnyFlights) && renderNoSelectedFlightsMessage()} */}

            </Card>
        );

    }

    return render();

}
