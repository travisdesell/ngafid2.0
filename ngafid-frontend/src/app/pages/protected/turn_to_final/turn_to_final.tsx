import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import PanelAlert from "@/components/panel_alert";
import { useAirports } from "@/components/providers/airports_provider";
import { getLogger } from "@/components/providers/logger";
import TimeHeader from "@/components/providers/time_header/time_header";
import { useTimeHeader } from "@/components/providers/time_header/time_header_provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Slider } from "@/components/ui/slider";
import { fetchJson } from "@/fetchJson";
import Feature from "ol/Feature.js";
import OLMap from "ol/Map.js";
import Overlay from "ol/Overlay.js";
import View from "ol/View.js";
import { isEmpty as extentIsEmpty } from "ol/extent.js";
import LineString from "ol/geom/LineString.js";
import Point from "ol/geom/Point.js";
import TileLayer from "ol/layer/Tile.js";
import VectorLayer from "ol/layer/Vector.js";
import "ol/ol.css";
import { fromLonLat } from "ol/proj.js";
import OSM from "ol/source/OSM.js";
import VectorSource from "ol/source/Vector.js";
import XYZ from "ol/source/XYZ.js";
import { Circle as CircleStyle, Stroke, Style } from "ol/style.js";
import { useDeferredValue, useEffect, useMemo, useRef, useState } from "react";
import { Bar, BarChart, CartesianGrid, Scatter, ScatterChart, XAxis, YAxis } from "recharts";

const log = getLogger("TurnToFinal", "black", "Page");

const ROLL_THRESHOLDS = {
    Min: 0,
    Default: 0,
    Dangerous: 26,
    MaxSoft: 30,
    MaxHard: 45,
} as const;

const MAX_RENDERED_MAP_APPROACHES = 1000;
const MAX_RENDERED_CHART_APPROACHES = 100;
const MAX_POINTS_PER_SCATTER_SERIES = 100;

const RUNWAY_ANY = "Any Runway";

type AirportInfo = {
    iataCode?: string;
    latitude?: number;
    longitude?: number;
};

type RunwayInfo = {
    name?: string;
};

type TurnToFinalApi = {
    flightId?: string;
    flight_id?: string;
    airportIataCode?: string;
    runway?: RunwayInfo;
    flightStartDate?: string;
    maxRoll?: number;
    selfDefinedGlideAngle?: number;
    selfDefinedGlidePathDeviations?: number[];
    distanceFromRunway?: number[];
    AltAGL?: number[];
    latitude?: number[];
    longitude?: number[];
    lat?: number[];
    lon?: number[];
    approachn?: number;
};

type TurnToFinalNormalized = {
    id: string;
    flightId: string;
    approachn: number;
    airportIataCode: string;
    runwayName: string;
    flightStartDate: Date | null;
    maxRoll: number;
    selfDefinedGlideAngle: number;
    deviations: number[];
    distanceFromRunway: number[];
    altAgl: number[];
    pointsLonLat: Array<[number, number]>;
};

type TurnToFinalResponse = {
    airports?: Record<string, AirportInfo>;
    ttfs?: TurnToFinalApi[];
};

type CachedPayload = {
    airport: string;
    startDate: string;
    endDate: string;
    airports: Record<string, AirportInfo>;
    ttfs: TurnToFinalNormalized[];
};

type XYPoint = {
    x: number;
    y: number;
};

type LegacyWindow = Window & {
    airports?: unknown;
    runways?: unknown;
};

const toDate = (input: string | undefined | null): Date | null => {
    if (!input)
        return null;

    const parsed = new Date(input);
    if (!Number.isNaN(parsed.getTime()))
        return parsed;

    return null;
};

const toFiniteNumberArray = (arr: unknown): number[] => {
    if (!Array.isArray(arr))
        return [];

    return arr
        .map((value) => Number(value))
        .filter((value) => Number.isFinite(value));
};

const normalizeTurnToFinalRows = (rowsRaw: TurnToFinalApi[]): TurnToFinalNormalized[] => {
    const rows = rowsRaw ?? [];
    const approachCounts: Record<string, number> = {};

    return rows.map((row, rowIndex) => {
        const flightId = String(row.flightId ?? row.flight_id ?? `Approach-${rowIndex + 1}`);
        approachCounts[flightId] = (approachCounts[flightId] ?? 0) + 1;
        const approachn = Number.isFinite(row.approachn) ? Number(row.approachn) : approachCounts[flightId]!;

        const lat = toFiniteNumberArray(row.lat ?? row.latitude);
        const lon = toFiniteNumberArray(row.lon ?? row.longitude);
        const pointsLonLat: Array<[number, number]> = [];

        const pointsCount = Math.min(lat.length, lon.length);
        for (let pointIndex = 0; pointIndex < pointsCount; pointIndex++) {
            pointsLonLat.push([lon[pointIndex]!, lat[pointIndex]!]);
        }

        const distanceFromRunway = toFiniteNumberArray(row.distanceFromRunway);
        const deviations = toFiniteNumberArray(row.selfDefinedGlidePathDeviations);
        const altAgl = toFiniteNumberArray(row.AltAGL);

        return {
            id: `${flightId}::${approachn}::${rowIndex}`,
            flightId,
            approachn,
            airportIataCode: String(row.airportIataCode ?? ""),
            runwayName: String(row.runway?.name ?? "Unknown"),
            flightStartDate: toDate(row.flightStartDate),
            maxRoll: Number.isFinite(Number(row.maxRoll)) ? Number(row.maxRoll) : 0,
            selfDefinedGlideAngle: Number.isFinite(Number(row.selfDefinedGlideAngle)) ? Number(row.selfDefinedGlideAngle) : 0,
            deviations,
            distanceFromRunway,
            altAgl,
            pointsLonLat,
        };
    });
};

const dateWithinRange = (value: Date | null, startDate: Date, endDate: Date): boolean => {
    if (!value)
        return true;
    return startDate <= value && value <= endDate;
};

const toHex = (value: number): string => Math.max(0, Math.min(255, Math.round(value))).toString(16).padStart(2, "0");

const colorForRoll_OLD = (roll: number): string => {
    const tDanger = (Math.max(ROLL_THRESHOLDS.Min, Math.min(ROLL_THRESHOLDS.Dangerous, roll)) - ROLL_THRESHOLDS.Min)
        / Math.max(1, ROLL_THRESHOLDS.Dangerous - ROLL_THRESHOLDS.Min);
    const tSoft = (Math.max(ROLL_THRESHOLDS.Dangerous, Math.min(ROLL_THRESHOLDS.MaxSoft, roll)) - ROLL_THRESHOLDS.Dangerous)
        / Math.max(1, ROLL_THRESHOLDS.MaxSoft - ROLL_THRESHOLDS.Dangerous);

    let r = 0;
    let g = 255;
    if (roll <= ROLL_THRESHOLDS.Dangerous) {
        r = 255 * tDanger;
        g = 255;
    } else {
        r = 255;
        g = 255 * (1 - tSoft);
    }

    return `#${toHex(r)}${toHex(g)}00`;
};

const colorForRoll = (roll: number): string => {

    /*
        Interpolate Colors:

        0: #3182CE (Light Blue)
        25: #1D4ED8 (Blue)
        30: #ca8a04 (Yellow)
        45: #e17100 (Amber)
    */

    let r, g, b;

    // Non-dangerous range (0 to 25)
    if (roll <= ROLL_THRESHOLDS.Dangerous) {

        const col0 = { r: 49, g: 130, b: 206 }; // #3182CE
        const col25 = { r: 29, g: 78, b: 216 }; // #1D4ED8

        const localT = roll / ROLL_THRESHOLDS.Dangerous;

        r = Math.round(col0.r + (col25.r - col0.r) * localT);
        g = Math.round(col0.g + (col25.g - col0.g) * localT);
        b = Math.round(col0.b + (col25.b - col0.b) * localT);

    // Dangerous range (25 to 30)
    } else if (roll <= ROLL_THRESHOLDS.MaxSoft) {

        const col25 = { r: 29, g: 78, b: 216 }; // #1D4ED8
        const col30 = { r: 202, g: 138, b: 4 }; // #ca8a04

        const localT = (roll - ROLL_THRESHOLDS.Dangerous) / (ROLL_THRESHOLDS.MaxSoft - ROLL_THRESHOLDS.Dangerous);

        r = Math.round(col25.r + (col30.r - col25.r) * localT);
        g = Math.round(col25.g + (col30.g - col25.g) * localT);
        b = Math.round(col25.b + (col30.b - col25.b) * localT);

    // Highly dangerous range (30 to 45)
    } else {

        const col30 = { r: 202, g: 138, b: 4 }; // #ca8a04
        const col45 = { r: 225, g: 113, b: 0 }; // #e17100

        const localT = (roll - ROLL_THRESHOLDS.MaxSoft) / (ROLL_THRESHOLDS.MaxHard - ROLL_THRESHOLDS.MaxSoft);

        r = Math.round(col30.r + (col45.r - col30.r) * localT);
        g = Math.round(col30.g + (col45.g - col30.g) * localT);
        b = Math.round(col30.b + (col45.b - col30.b) * localT);

    }

    return `#${toHex(r)}${toHex(g)}${toHex(b)}`;

}

const styleForRoll = (roll: number): Style => new Style({
    stroke: new Stroke({
        color: colorForRoll(roll),
        width: 2.5,
    }),
    image: new CircleStyle({
        radius: 4,
        stroke: new Stroke({
            color: "rgba(0,0,0,0)",
            width: 1,
        }),
    }),
});

const sampleEvenly = <T,>(items: T[], maxItems: number): T[] => {
    if (maxItems <= 0)
        return [];

    if (items.length <= maxItems)
        return items;

    const sampled: T[] = [];
    const step = items.length / maxItems;

    for (let i = 0; i < maxItems; i++) {
        const index = Math.min(items.length - 1, Math.floor(i * step));
        sampled.push(items[index]!);
    }

    return sampled;
};

const decimatePairSeries = (xValues: number[], yValues: number[], maxPoints: number): XYPoint[] => {
    const pointCount = Math.min(xValues.length, yValues.length);

    if (pointCount <= 0)
        return [];

    if (pointCount <= maxPoints) {
        const out: XYPoint[] = new Array(pointCount);

        for (let index = 0; index < pointCount; index++) {
            out[index] = {
                x: xValues[index]!,
                y: yValues[index]!,
            };
        }

        return out;
    }

    const step = Math.ceil(pointCount / maxPoints);
    const out: XYPoint[] = [];

    for (let index = 0; index < pointCount; index += step) {
        out.push({
            x: xValues[index]!,
            y: yValues[index]!,
        });
    }

    const lastIndex = pointCount - 1;
    const lastX = xValues[lastIndex];
    const lastY = yValues[lastIndex];
    const lastPoint = out[out.length - 1];

    if (lastX !== undefined && lastY !== undefined && (!lastPoint || lastPoint.x !== lastX || lastPoint.y !== lastY)) {
        out.push({
            x: lastX,
            y: lastY,
        });
    }

    return out;
};

export default function TurnToFinalPage() {
    useEffect(() => {
        document.title = "NGAFID — Turn to Final";
    }, []);

    const { visitedAirports } = useAirports();
    const { setModal } = useModal();
    const { endpointStartDate, endpointEndDate, reapplyTrigger, appliedStartDate, appliedEndDate, renderDateRangeMonthly } = useTimeHeader();

    const [disableFetching, setDisableFetching] = useState(false);
    const [minRoll, setMinRoll] = useState<number>(ROLL_THRESHOLDS.Default);
    const [selectedAirport, setSelectedAirport] = useState("");
    const [selectedRunway, setSelectedRunway] = useState(RUNWAY_ANY);
    const [mapStyle, setMapStyle] = useState<"Road" | "Aerial">("Road");
    const [payload, setPayload] = useState<CachedPayload | null>(null);

    const mapContainerRef = useRef<HTMLDivElement | null>(null);
    const popupRef = useRef<HTMLDivElement | null>(null);
    const popupContentRef = useRef<HTMLDivElement | null>(null);
    const popupTitleRef = useRef<HTMLDivElement | null>(null);
    const popupLinkRef = useRef<HTMLAnchorElement | null>(null);
    const popupCloseRef = useRef<HTMLAnchorElement | null>(null);
    const mapRef = useRef<OLMap | null>(null);
    const overlayRef = useRef<Overlay | null>(null);
    const vectorLayerRef = useRef<VectorLayer<VectorSource> | null>(null);
    const vectorSourceRef = useRef<VectorSource | null>(null);
    const roadLayerRef = useRef<TileLayer<OSM> | null>(null);
    const aerialLayerRef = useRef<TileLayer<XYZ> | null>(null);
    const cacheRef = useRef<CachedPayload | null>(null);
    const styleCacheRef = useRef<Map<number, Style>>(new Map());

    const legacyGlobals = (window as LegacyWindow);

    const fallbackAirportList = useMemo<string[]>(() => {

        const value = legacyGlobals.airports;

        if (Array.isArray(value))
            return value.map((airportCode) => String(airportCode)).filter((airportCode) => airportCode.length > 0);

        if (value && typeof value === "object")
            return Object.keys(value as Record<string, unknown>);

        return [];
    }, [legacyGlobals.airports]);

    const fallbackRunwaysByAirport = useMemo<Record<string, string[]>>(() => {
        const value = legacyGlobals.runways;
        if (!value || typeof value !== "object")
            return {};

        const out: Record<string, string[]> = {};
        for (const [airportCode, runwaysRaw] of Object.entries(value as Record<string, unknown>)) {
            if (!Array.isArray(runwaysRaw))
                continue;

            out[airportCode] = runwaysRaw
                .map((runwayRaw) => {
                    if (runwayRaw && typeof runwayRaw === "object" && "name" in runwayRaw)
                        return String((runwayRaw as RunwayInfo).name ?? "");
                    return String(runwayRaw ?? "");
                })
                .filter((runwayName) => runwayName.length > 0)
                .sort((a, b) => a.localeCompare(b));
        }

        return out;
    }, [legacyGlobals.runways]);

    useEffect(() => {
        if (selectedAirport)
            return;
        if (visitedAirports.length > 0) {
            setSelectedAirport(visitedAirports[0]!);
            return;
        }
        if (fallbackAirportList.length === 0)
            return;

        setSelectedAirport(fallbackAirportList[0]!);
    }, [fallbackAirportList, selectedAirport, visitedAirports]);

    const fetchTurnToFinalData = async () => {

        // No airport selected, exit
        if (!selectedAirport) {
            setModal(ErrorModal, {
                title: "Turn to Final Airport Not Selected",
                message: "Select an airport first. If no airports are listed, the fleet may not have any visited-airport records yet.",
            });
            return;
        }

        const requestStartDate = endpointStartDate;
        const requestEndDate = endpointEndDate;

        const requestStartDateObject = toDate(requestStartDate);
        const requestEndDateObject = toDate(requestEndDate);
        const cached = cacheRef.current;

        if (
            cached &&
            cached.airport === selectedAirport &&
            requestStartDateObject &&
            requestEndDateObject &&
            toDate(cached.startDate) &&
            toDate(cached.endDate) &&
            dateWithinRange(requestStartDateObject, toDate(cached.startDate)!, toDate(cached.endDate)!) &&
            dateWithinRange(requestEndDateObject, toDate(cached.startDate)!, toDate(cached.endDate)!)
        ) {
            log("Using cached Turn-to-Final payload: ", cached);
            setPayload(cached);
            return;
        }

        setDisableFetching(true);

        const params = new URLSearchParams({
            startDate: requestStartDate,
            endDate: requestEndDate,
            airport: selectedAirport,
        });

        log("Fetching Turn-to-Final data with params:", params.toString());

        const response = await fetchJson.get<TurnToFinalResponse>("/api/flight/turn-to-final", { params })
            .catch((error) => {
                setModal(ErrorModal, {
                    title: "Error fetching Turn to Final data",
                    message: String(error),
                });
                return null;
            });

        // No response, exit
        if (!response) {
            setModal(ErrorModal, {
                title: "Error fetching Turn to Final data",
                message: "An unknown error occurred while fetching Turn to Final data.",
            });
            setDisableFetching(false);
            return;
        }

        log("Fetched Turn-to-Final data:", response);

        const normalizedRows = normalizeTurnToFinalRows(response.ttfs ?? []);
        const nextPayload: CachedPayload = {
            airport: selectedAirport,
            startDate: requestStartDate,
            endDate: requestEndDate,
            airports: response.airports ?? {},
            ttfs: normalizedRows,
        };

        cacheRef.current = nextPayload;
        setPayload(nextPayload);
        setDisableFetching(false);
    };

    useEffect(() => {

        // No airport selected, exit
        if (!selectedAirport)
            return;

        // Fetch data after TimeHeader dependency change
        fetchTurnToFinalData();

    }, [reapplyTrigger]);

    const airportOptions = useMemo(() => {
        const set = new Set<string>();

        for (const airportCode of visitedAirports)
            set.add(airportCode);

        for (const airportCode of fallbackAirportList)
            set.add(airportCode);

        for (const airportCode of Object.keys(payload?.airports ?? {}))
            set.add(airportCode);

        for (const row of payload?.ttfs ?? []) {
            if (row.airportIataCode)
                set.add(row.airportIataCode);
        }

        return Array.from(set).sort((a, b) => a.localeCompare(b));
    }, [fallbackAirportList, payload, visitedAirports]);

    const runwayOptions = useMemo(() => {
        const set = new Set<string>();

        for (const runway of (fallbackRunwaysByAirport[selectedAirport] ?? []))
            set.add(runway);

        for (const row of payload?.ttfs ?? []) {
            if (row.airportIataCode === selectedAirport && row.runwayName)
                set.add(row.runwayName);
        }

        return [RUNWAY_ANY, ...Array.from(set).sort((a, b) => a.localeCompare(b))];
    }, [fallbackRunwaysByAirport, payload, selectedAirport]);

    const filteredRows = useMemo(() => {
        const rows = (payload?.ttfs ?? []);

        return rows.filter((row) => {
            if (row.airportIataCode !== selectedAirport)
                return false;

            if (!dateWithinRange(row.flightStartDate, appliedStartDate, appliedEndDate))
                return false;

            if (selectedRunway !== RUNWAY_ANY && row.runwayName !== selectedRunway)
                return false;

            if (row.maxRoll < minRoll)
                return false;

            return true;
        });
    }, [appliedEndDate, appliedStartDate, minRoll, payload, selectedAirport, selectedRunway]);

    const deferredFilteredRows = useDeferredValue(filteredRows);

    const mapRows = useMemo(() => {
        return sampleEvenly(deferredFilteredRows, MAX_RENDERED_MAP_APPROACHES);
    }, [deferredFilteredRows]);

    const chartRows = useMemo(() => {
        return sampleEvenly(deferredFilteredRows, MAX_RENDERED_CHART_APPROACHES);
    }, [deferredFilteredRows]);

    const mapIsSampled = mapRows.length < deferredFilteredRows.length;
    const chartsAreSampled = chartRows.length < deferredFilteredRows.length;

    useEffect(() => {

        // Map container not ready, exit
        if (!mapContainerRef.current)
            return;

        const roadLayer = new TileLayer({
            source: new OSM(),
            visible: true,
        });
        const aerialLayer = new TileLayer({
            source: new XYZ({
                url: "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
            }),
            visible: false,
        });

        roadLayerRef.current = roadLayer;
        aerialLayerRef.current = aerialLayer;

        const vectorSource = new VectorSource();
        const vectorLayer = new VectorLayer({ source: vectorSource });
        vectorSourceRef.current = vectorSource;
        vectorLayerRef.current = vectorLayer;

        const overlay = popupRef.current
            ? new Overlay({
                element: popupRef.current,
                                autoPan: false,
              })
            : null;

        overlayRef.current = overlay;

        const map = new OLMap({
            target: mapContainerRef.current,
            layers: [roadLayer, aerialLayer, vectorLayer],
            overlays: overlay ? [overlay] : [],
            view: new View({
                center: fromLonLat([-97.0329, 47.9253]),
                zoom: 4,
                minZoom: 0,
                maxZoom: 18,
            }),
            controls: [],
        });

        mapRef.current = map;

        const hidePopup = () => {
            overlay?.setPosition(undefined);

            if (popupContentRef.current)
                popupContentRef.current.classList.add("hidden");
        };

        const mapClickHandler = (event: any) => {

            // No overlay, exit
            if (!overlay)
                return;

            event.preventDefault();
            hidePopup();

        }

        if (popupCloseRef.current && overlay)
            popupCloseRef.current.onclick = mapClickHandler;

        map.on("singleclick", (event) => {

            // No overlay, exit
            if (!overlay)
                return;

            const feature = map.forEachFeatureAtPixel(event.pixel, (featureAtPixel) => featureAtPixel as Feature);
            const row = feature?.get("ttf") as TurnToFinalNormalized | undefined;

            if (!row) {
                hidePopup();
                return;
            }

            if (popupTitleRef.current)
                popupTitleRef.current.textContent = `Flight ${row.flightId} — Approach ${row.approachn}`;

            if (popupLinkRef.current)
                popupLinkRef.current.href = `/protected/flights?flight_id=${encodeURIComponent(row.flightId)}`;

            if (popupContentRef.current)
                popupContentRef.current.classList.remove("hidden");

            overlay.setPosition(event.coordinate);
        });

        map.on("movestart", () => {
            hidePopup();
        });

        return () => {
            try {
                hidePopup();
                map.setTarget(undefined);
            } catch {
                /* ... */
            }

            mapRef.current = null;
            overlayRef.current = null;
            vectorLayerRef.current = null;
            vectorSourceRef.current = null;
            roadLayerRef.current = null;
            aerialLayerRef.current = null;
        };
    }, []);

    useEffect(() => {
        if (!roadLayerRef.current || !aerialLayerRef.current)
            return;

        roadLayerRef.current.setVisible(mapStyle === "Road");
        aerialLayerRef.current.setVisible(mapStyle === "Aerial");
    }, [mapStyle]);

    useEffect(() => {
        const vectorSource = vectorSourceRef.current;
        const map = mapRef.current;
        if (!vectorSource || !map)
            return;

        vectorSource.clear();

        const features: Feature[] = [];

        for (const row of mapRows) {
            if (row.pointsLonLat.length === 0)
                continue;

            const points = row.pointsLonLat.map((point) => fromLonLat(point));
            const pathFeature = new Feature({
                geometry: new LineString(points),
                ttf: row,
            });

            const styleKey = Math.round(row.maxRoll);
            let style = styleCacheRef.current.get(styleKey);

            if (!style) {
                style = styleForRoll(styleKey);
                styleCacheRef.current.set(styleKey, style);
            }

            pathFeature.setStyle(style);
            features.push(pathFeature);

            const markerFeature = new Feature({
                geometry: new Point(points[0]!),
                ttf: row,
            });
            markerFeature.setStyle(style);
            features.push(markerFeature);
        }

        vectorSource.addFeatures(features);

        const view = map.getView();
        const extent = vectorSource.getExtent();

        if (extent && !extentIsEmpty(extent)) {
            view.fit(extent, {
                padding: [40, 40, 40, 40],
                maxZoom: 15,
            });
            return;
        }

        const airportCenter = payload?.airports?.[selectedAirport];
        if (airportCenter && Number.isFinite(airportCenter.longitude) && Number.isFinite(airportCenter.latitude)) {
            view.setCenter(fromLonLat([airportCenter.longitude!, airportCenter.latitude!]));
            view.setZoom(11);
        }
    }, [mapRows, payload, selectedAirport]);

    const deviationsSeries = useMemo(() => {
        return chartRows.map((row) => {
            const data = decimatePairSeries(
                row.distanceFromRunway,
                row.deviations,
                MAX_POINTS_PER_SCATTER_SERIES,
            );

            return {
                id: row.id,
                name: `${row.flightId} (App ${row.approachn})`,
                color: colorForRoll(row.maxRoll),
                data,
            };
        }).filter((series) => series.data.length > 0);
    }, [chartRows]);

    const altitudeSeries = useMemo(() => {
        return chartRows.map((row) => {
            const data = decimatePairSeries(
                row.distanceFromRunway,
                row.altAgl,
                MAX_POINTS_PER_SCATTER_SERIES,
            );

            return {
                id: row.id,
                name: `${row.flightId} (App ${row.approachn})`,
                color: colorForRoll(row.maxRoll),
                data,
            };
        }).filter((series) => series.data.length > 0);
    }, [chartRows]);

    const histogramData = useMemo(() => {
        const binMin = ROLL_THRESHOLDS.Min;
        const binMax = ROLL_THRESHOLDS.MaxHard;
        const bins = Array.from({ length: binMax - binMin + 1 }, (_, index) => ({
            angle: binMin + index,
            count: 0,
        }));

        for (const row of deferredFilteredRows) {
            const value = Math.round(row.selfDefinedGlideAngle);
            if (value < binMin || value > binMax)
                continue;
            bins[value - binMin]!.count += 1;
        }

        return bins;
    }, [deferredFilteredRows]);


    const renderNoDataMessage = () => (
        <PanelAlert
            title="No Turn-to-Final Data"
            description={[
                "No matching approaches were found for the selected filters.",
                "Try reducing the roll threshold or changing runway/date filters.",
            ]}
        />
    );

    function renderTTFMap() {

        return <Card className="card-glossy overflow-clip relative min-h-0 **:text-black! **:opacity-100">
            <CardHeader className="absolute pointer-events-none z-10 w-full">
                <CardTitle className="flex justify-between items-center">
                    Turn-to-Final Map
                    {renderDateRangeMonthly()}
                </CardTitle>
                {
                    mapIsSampled
                    &&
                    <CardDescription>
                        Showing {mapRows.length.toLocaleString()} sampled approaches of {deferredFilteredRows.length.toLocaleString()} for responsive map rendering.
                    </CardDescription>
                }
            </CardHeader>
            <CardContent className="relative p-0 h-full min-h-0">
                <div ref={mapContainerRef} className="h-full w-full rounded-md border p-0 m-0" />
                <div ref={popupRef} className="ol-popup rounded-lg border bg-background/95 p-3 shadow-xl max-w-72 **:text-foreground!">
                    <a ref={popupCloseRef} href="#" className="ol-popup-closer text-muted-foreground absolute right-2 top-1 text-lg" aria-label="Close popup">×</a>
                    <div ref={popupContentRef} className="hidden flex-col gap-2 pr-4">
                        <div ref={popupTitleRef} className="text-sm font-semibold" />
                        <a ref={popupLinkRef} className="text-sm underline decoration-dashed text-primary" href="#" target="_blank" rel="noreferrer">Open flight in new tab</a>
                    </div>
                </div>
                {
                    (selectedAirport && !disableFetching && filteredRows.length === 0)
                    &&
                    renderNoDataMessage()
                }
            </CardContent>
        </Card>

    }

    function renderGlidePathDeviationsChart() {

        return <Card className="card-glossy overflow-clip min-h-0 flex flex-col">
            <CardHeader>
                <CardTitle className="flex justify-between">Glide Path Deviations {renderDateRangeMonthly()}</CardTitle>
                <CardDescription>
                    Distance above/below glide path vs distance from runway.
                    {chartsAreSampled && ` Showing ${chartRows.length.toLocaleString()} sampled approaches of ${deferredFilteredRows.length.toLocaleString()} for responsiveness.`}
                </CardDescription>
            </CardHeader>
            <CardContent className="h-full min-h-0 flex-1">
                {
                    (deviationsSeries.length === 0)
                    ?
                    renderNoDataMessage()
                    :
                    <ChartContainer config={{ }} className="w-full h-full">
                        <ScatterChart margin={{ top: 8, right: 20, left: 0, bottom: 8 }}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis type="number" dataKey="x" name="Distance from runway" reversed domain={["auto", "auto"]} tickFormatter={(value) => Number(value).toFixed(0)} />
                            <YAxis type="number" dataKey="y" name="Glide path deviation" domain={["auto", "auto"]} allowDecimals={false}/>
                            <ChartTooltip
                                content={<ChartTooltipContent />}
                            />
                            {
                                deviationsSeries.map((series) =>
                                    <Scatter key={series.id} data={series.data} line fill={series.color} stroke={series.color} name={series.name} isAnimationActive={false} />
                                )
                            }
                        </ScatterChart>
                    </ChartContainer>
                }
            </CardContent>
        </Card>

    }

    function renderAltitudeVsDistanceChart() {

        return <Card className="card-glossy overflow-clip min-h-0 flex flex-col">
            <CardHeader>
                <CardTitle className="flex justify-between">Altitude vs Distance {renderDateRangeMonthly()}</CardTitle>
                <CardDescription>
                    AGL altitude profile for each visible approach.
                    {chartsAreSampled && ` Showing ${chartRows.length.toLocaleString()} sampled approaches of ${deferredFilteredRows.length.toLocaleString()} for responsiveness.`}
                </CardDescription>
            </CardHeader>
            <CardContent className="h-full min-h-0 flex-1">
                {
                    (altitudeSeries.length === 0)
                    ?
                    renderNoDataMessage()
                    :
                    <ChartContainer config={{ }} className="w-full h-full">
                        <ScatterChart margin={{ top: 8, right: 20, left: 0, bottom: 8 }}>
                            <CartesianGrid strokeDasharray="3 3" />
                                <XAxis type="number" dataKey="x" name="Distance from runway" reversed domain={["auto", "auto"]} tickFormatter={(value) => Number(value).toFixed(0)} />
                                <YAxis type="number" dataKey="y" name="Altitude AGL" domain={["auto", "auto"]} />
                                <ChartTooltip
                                    content={<ChartTooltipContent />}
                                />
                                {
                                    altitudeSeries.map((series) =>
                                        <Scatter
                                            key={series.id}
                                            data={series.data}
                                            line
                                            fill={series.color}
                                            stroke={series.color}
                                            name={series.name}
                                            isAnimationActive={false}
                                        />
                                    )
                                }
                        </ScatterChart>
                    </ChartContainer>
                }
            </CardContent>
        </Card>

    }

    function renderGlidePathAngleHistogram() {

        return <Card className="card-glossy overflow-clip min-h-0 flex flex-col">
            <CardHeader>
                <CardTitle className="flex justify-between">Histogram of Glide Path Angles {renderDateRangeMonthly()}</CardTitle>
                <CardDescription>Distribution of self-defined glide path angles for filtered approaches.</CardDescription>
            </CardHeader>
            <CardContent className="h-full min-h-0 flex-1">
                {
                    filteredRows.length === 0
                    ? renderNoDataMessage()
                    : (
                        <ChartContainer config={{ }} className="w-full h-full">
                            <BarChart data={histogramData} margin={{ top: 8, right: 20, left: 0, bottom: 8 }}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="angle" tickFormatter={(value) => (value%5 === 0 ? `${value}°` : "")} />
                                <YAxis allowDecimals={false} />
                                <ChartTooltip
                                    content={<ChartTooltipContent
                                        labelFormatter={(value) => `Glide Angle: ${value}°`}
                                    />}
                                />
                                <Bar dataKey="count" fill="var(--chart-2)" maxBarSize={20} isAnimationActive={false} />
                            </BarChart>
                        </ChartContainer>
                    )
                }
            </CardContent>
        </Card>

    }


    return (
        <div className="page-container">
            <div className="page-content gap-4 overflow-hidden">

                <TimeHeader
                    onApply={() => {
                        log("Applying Turn-to-Final date range");
                    }}
                    dependencies={[selectedAirport]}
                >

                    {/* Airport Selection */}
                    <div className="flex flex-col gap-2 justify-start items-start min-w-32">
                        <Label>Airport</Label>
                        <Select
                            value={selectedAirport}
                            onValueChange={(airportCode) => {
                                setSelectedAirport(airportCode);
                                setSelectedRunway(RUNWAY_ANY);
                            }}
                            disabled={disableFetching || airportOptions.length === 0}
                        >
                            <Button asChild variant="outline" disabled={disableFetching || airportOptions.length === 0}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select airport" />
                                </SelectTrigger>
                            </Button>
                            <SelectContent className="max-h-[40dvh]">
                                {airportOptions.map((airportCode) => (
                                    <SelectItem key={airportCode} value={airportCode}>{airportCode}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Runway Selection */}
                    <div className="flex flex-col gap-2 justify-start items-start min-w-48">
                        <Label>Runway</Label>
                        <Select
                            value={selectedRunway}
                            onValueChange={(runwayName) => setSelectedRunway(runwayName)}
                            disabled={disableFetching || runwayOptions.length === 0}
                        >
                            <Button asChild variant="outline" disabled={disableFetching || runwayOptions.length === 0}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select runway" />
                                </SelectTrigger>
                            </Button>
                            <SelectContent className="max-h-[40dvh]">
                                {runwayOptions.map((runwayName) => (
                                    <SelectItem key={runwayName} value={runwayName}>
                                        {runwayName === RUNWAY_ANY ? RUNWAY_ANY : `${selectedAirport} - ${runwayName}`}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Roll Threshold */}
                    <div className="flex flex-col gap-4 min-w-48 text-nowrap">
                        <Label className="px-0 flex w-full justify-between">
                            <span>Minimum Roll Threshold:</span>
                            <span className="w-8">{minRoll.toFixed(0)}°</span>
                        </Label>
                        <Slider
                            min={ROLL_THRESHOLDS.Min}
                            max={ROLL_THRESHOLDS.MaxHard}
                            step={1}
                            value={[minRoll]}
                            onValueChange={(value) => setMinRoll(value[0] ?? ROLL_THRESHOLDS.Min)}
                        />
                    </div>
                </TimeHeader>

                <div className="grid grid-cols-2 grid-rows-2 gap-2 flex-1 min-h-0">

                    {/* Turn-to-Final Map */}
                    {renderTTFMap()}

                    {/* Altitude VS Distance Chart */}
                    {renderAltitudeVsDistanceChart()}

                    {/* Glide Path Angle Histogram */}
                    {renderGlidePathAngleHistogram()}

                    {/* Glide Path Deviation VS Distance Chart */}
                    {renderGlidePathDeviationsChart()}

                </div>

            </div>
        </div>
    );
}