// ngafid-frontend/src/app/pages/protected/heatmap/heat_map.tsx
import PanelAlert from "@/components/panel_alert";
import { ALL_AIRFRAMES_ID, ALL_AIRFRAMES_NAME, useAirframes } from "@/components/providers/airframes_provider";
import { getLogger } from "@/components/providers/logger";
import { usePlatform } from "@/components/providers/platform_provider";
import TimeHeader from "@/components/providers/time_header/time_header";
import { useTimeHeader } from "@/components/providers/time_header/time_header_provider";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Slider } from "@/components/ui/slider";
import { fetchJson } from "@/fetchJson";
import {
    AlertCircle,
    Flame,
    Grid2X2,
    Move,
    Plane,
    X,
} from "lucide-react";
import Feature from "ol/Feature";
import OlMap from "ol/Map";
import View from "ol/View";
import { platformModifierKeyOnly } from "ol/events/condition";
import LineString from "ol/geom/LineString";
import Point from "ol/geom/Point";
import Polygon from "ol/geom/Polygon";
import DragBox from "ol/interaction/DragBox";
import Heatmap from "ol/layer/Heatmap";
import TileLayer from "ol/layer/Tile";
import VectorLayer from "ol/layer/Vector";
import "ol/ol.css";
import { fromLonLat, getTransform, toLonLat } from "ol/proj";
import VectorSource from "ol/source/Vector";
import XYZ from "ol/source/XYZ";
import CircleStyle from "ol/style/Circle";
import Fill from "ol/style/Fill";
import Stroke from "ol/style/Stroke";
import Style from "ol/style/Style";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import "./heat_map.css";
import { useModal } from "@/components/modals/modal_context";
import ErrorModal from "@/components/modals/error_modal";

const log = getLogger("HeatMap", "black", "Page");

const EVENT_ANY = "ANY Event";
const MARKER_VISIBILITY_ZOOM_THRESHOLD = 15;
const BATCH_SIZE = 1000;

const PROXIMITY_DEFINITION_IDS = new Set<number>([-1, -2, -3]);

type RgbColor = {
    r: number;
    g: number;
    b: number;
};

const ALL_EVENT_NAMES = [
    "ANY Event",
    "Airspeed",
    "Altitude",
    "Battery Not Charging",
    "CHT Divergence",
    "CHT Sensor Divergence",
    "Cylinder Head Temperature",
    "E1 EGT Divergence",
    "EGT Sensor Divergence",
    "EGT Sensor Divergence (200 degrees)",
    "Engine Shutdown Below 3000 Ft",
    "High Altitude LOC-I",
    "High Altitude Limit Exceeded",
    "High Altitude Spin",
    "High Altitude Stall",
    "High CHT",
    "High Lateral Acceleration",
    "High Pitch",
    "High Vertical Acceleration",
    "Low Airspeed on Approach",
    "Low Airspeed on Climbout",
    "Low Altitude LOC-I",
    "Low Altitude Spin",
    "Low Altitude Stall",
    "Low Battery Level",
    "Low Ending Fuel",
    "Low Fuel",
    "Low Lateral Acceleration",
    "Low Oil Pressure",
    "Low Pitch",
    "Low Positional Accuracy",
    "Low Vertical Acceleration",
    "Proximity",
    "Roll",
    "Tail Strike",
    "VSI on Final",
] as const;

const EVENT_NAME_TO_DEFINITION_IDS: Record<string, number[]> = {
    "ANY Event": [],
    Airspeed: [9, 11, 12, 13, 63],
    Altitude: [14, 15, 16, 17, 64],
    "Battery Not Charging": [57],
    "CHT Divergence": [65],
    "CHT Sensor Divergence": [39, 40, 41],
    "Cylinder Head Temperature": [21, 22, 23, 62],
    "E1 EGT Divergence": [67],
    "EGT Sensor Divergence": [42, 43, 44, 45],
    "EGT Sensor Divergence (200 degrees)": [72],
    "Engine Shutdown Below 3000 Ft": [46, 47, 48, 49],
    "High Altitude LOC-I": [50],
    "High Altitude Limit Exceeded": [59, 60, 61],
    "High Altitude Spin": [-3],
    "High Altitude Stall": [51],
    "High CHT": [66],
    "High Lateral Acceleration": [4],
    "High Pitch": [2],
    "High Vertical Acceleration": [6],
    "Low Airspeed on Approach": [31, 32, 33, 34, 69],
    "Low Airspeed on Climbout": [35, 36, 37, 38, 68],
    "Low Altitude LOC-I": [53],
    "Low Altitude Spin": [-2],
    "Low Altitude Stall": [52],
    "Low Battery Level": [56],
    "Low Ending Fuel": [-7, -6, -5, -4],
    "Low Fuel": [28, 29, 30],
    "Low Lateral Acceleration": [3],
    "Low Oil Pressure": [24, 25, 26, 27, 70],
    "Low Pitch": [1],
    "Low Positional Accuracy": [58],
    "Low Vertical Acceleration": [5],
    Proximity: [-1],
    Roll: [7],
    "Tail Strike": [55, 71],
    "VSI on Final": [8],
};

EVENT_NAME_TO_DEFINITION_IDS[EVENT_ANY] = Array.from(
    new Set(Object.values(EVENT_NAME_TO_DEFINITION_IDS).flat()),
);

type MapStyleName =
    | "Aerial"
    | "Road"
    | "RoadOnDemand"
    | "SectionalCharts"
    | "TerminalAreaCharts"
    | "IFREnrouteLowCharts"
    | "IFREnrouteHighCharts"
    | "HelicopterCharts";

type MapLayerOption = {
    value: MapStyleName;
    label: string;
    url: () => string | undefined;
};

type BoxCoords = {
    minLat: string;
    maxLat: string;
    minLon: string;
    maxLon: string;
};

type HeatPoint = {
    latitude: number;
    longitude: number;
    altitudeAgl: number;
    timestamp: string;
};

type EventRow = {
    id: number;
    eventDefinitionId: number;
    flightId: number;
    otherFlightId: number | null;
    severity: number;
    airframe: string;
    otherAirframe: string;
};

type EventPointGroup = {
    eventId: number;
    eventDefinitionId: number;
    mainFlightId: number;
    otherFlightId: number | null;
    mainFlightPoints: HeatPoint[];
    otherFlightPoints: HeatPoint[];
    severity: number;
    airframe: string;
    otherAirframe: string;
};

type MarkerEvent = {
    eventId: number;
    eventDefinitionId: number;
    flightId: number;
    otherFlightId: number | null;
    time: string;
    flightAirframe: string;
    otherFlightAirframe: string;
    severity: number;
    altitudeAgl: number;
    latitude: number;
    longitude: number;
};

type PopupEventSummary = {
    eventId: number;
    eventDefinitionId: number;
    eventType: string;
    time: string;
    flightId: number;
    otherFlightId: number | null;
    severity: number;
    flightAirframe: string;
    otherFlightAirframe: string;
};

type PopupData = {
    time: string;
    latitude: number;
    longitude: number;
    altitude: number;
    flightId: number;
    flightAirframe: string;
    otherFlightId: number | null;
    otherFlightAirframe: string;
    severity: number;
    eventId: number | null;
    eventType: string;
    eventTypes: string[];
    eventSummaries: PopupEventSummary[];
    columnValues?: Record<string, string | number | null>;
};

type PopupState = {
    id: string;
    coord: number[];
    position: {
        left: number;
        top: number;
    };
    data: PopupData;
};

type SelectedPoint = {
    id: string;
    latitude: number;
    longitude: number;
    altitude: number;
};

type Distances = {
    lateral: number | null;
    euclidean: number | null;
};

type EventStatistics = {
    totalEvents: number;
    eventsByType: Record<string, number>;
};

type BatchResultRow = {
    event_id?: unknown;
    flight_id?: unknown;
    points?: unknown;
};

type BatchResponse = {
    results?: BatchResultRow[];
};

const MARKER_STYLE_CACHE = new Map<number, Style>();
const CONNECTOR_SEGMENT_STYLE_CACHE = new Map<string, Style>();

function hslToRgb(hue: number, saturation: number, lightness: number): RgbColor {
    const h = ((hue % 360) + 360) % 360;
    const s = Math.max(0, Math.min(1, saturation));
    const l = Math.max(0, Math.min(1, lightness));

    const chroma = (1 - Math.abs((2 * l) - 1)) * s;
    const segment = h / 60;
    const x = chroma * (1 - Math.abs((segment % 2) - 1));

    let rPrime = 0;
    let gPrime = 0;
    let bPrime = 0;

    if (segment >= 0 && segment < 1) {
        rPrime = chroma;
        gPrime = x;
    } else if (segment >= 1 && segment < 2) {
        rPrime = x;
        gPrime = chroma;
    } else if (segment >= 2 && segment < 3) {
        gPrime = chroma;
        bPrime = x;
    } else if (segment >= 3 && segment < 4) {
        gPrime = x;
        bPrime = chroma;
    } else if (segment >= 4 && segment < 5) {
        rPrime = x;
        bPrime = chroma;
    } else {
        rPrime = chroma;
        bPrime = x;
    }

    const match = l - (chroma / 2);
    return {
        r: Math.round((rPrime + match) * 255),
        g: Math.round((gPrime + match) * 255),
        b: Math.round((bPrime + match) * 255),
    };
}

function getFlightColorRgb(flightId: number): RgbColor {
    const normalized = Math.abs(Math.trunc(flightId)) || 1;
    const hue = (normalized * 137.508) % 360;
    return hslToRgb(hue, 0.78, 0.48);
}

function toRgba(color: RgbColor, alpha: number): string {
    const a = Math.max(0, Math.min(1, alpha));
    return `rgba(${color.r}, ${color.g}, ${color.b}, ${a})`;
}

function interpolateRgb(start: RgbColor, end: RgbColor, ratio: number): RgbColor {
    const t = Math.max(0, Math.min(1, ratio));
    return {
        r: Math.round(start.r + ((end.r - start.r) * t)),
        g: Math.round(start.g + ((end.g - start.g) * t)),
        b: Math.round(start.b + ((end.b - start.b) * t)),
    };
}

function lerp(start: number, end: number, ratio: number): number {
    return start + ((end - start) * ratio);
}

function getMarkerStyleForFlight(flightId: number): Style {
    const normalized = Math.abs(Math.trunc(flightId)) || 1;
    const cached = MARKER_STYLE_CACHE.get(normalized);
    if (cached)
        return cached;

    const color = getFlightColorRgb(normalized);
    const style = new Style({
        image: new CircleStyle({
            radius: 4,
            fill: new Fill({ color: toRgba(color, 0.95) }),
            stroke: new Stroke({ color: "rgba(0, 0, 0, 0.65)", width: 1.2 }),
        }),
    });

    MARKER_STYLE_CACHE.set(normalized, style);
    return style;
}

function getConnectorSegmentStyle(color: string): Style {
    const cached = CONNECTOR_SEGMENT_STYLE_CACHE.get(color);
    if (cached)
        return cached;

    const style = new Style({
        stroke: new Stroke({
            color,
            width: 1.5,
        }),
    });

    CONNECTOR_SEGMENT_STYLE_CACHE.set(color, style);
    return style;
}

const PROXIMITY_CONNECTOR_STYLE = new Style({
    stroke: new Stroke({
        color: "rgba(34, 139, 230, 0.35)",
        width: 1.4,
    }),
});

function getAzureMapsKey(): string | undefined {
    const value = (window as unknown as { azureMapsKey?: unknown }).azureMapsKey;
    if (typeof value !== "string")
        return undefined;
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : undefined;
}

function getChartTileBase(): string {
    const value = (window as unknown as { chartTileBaseUrl?: unknown }).chartTileBaseUrl;
    if (typeof value !== "string")
        return "http://localhost:8187";
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : "http://localhost:8187";
}

function buildMapLayerOptions(): MapLayerOption[] {
    const azureMapsKey = getAzureMapsKey();
    const chartTileBase = getChartTileBase();
    const fallbackRoadUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
    const fallbackAerialUrl = "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}";

    console.log("Azure Maps Key:", azureMapsKey ? "Provided" : "Not provided");

    return [
        {
            value: "Aerial",
            label: "Aerial",
            url: () => (azureMapsKey
                ? `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.imagery&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}`
                : fallbackAerialUrl),
        },
        {
            value: "Road",
            label: "Road (static)",
            url: () => (azureMapsKey
                ? `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.road&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}`
                : fallbackRoadUrl),
        },
        {
            value: "RoadOnDemand",
            label: "Road (dynamic)",
            url: () => (azureMapsKey
                ? `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.hybrid.road&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}`
                : fallbackRoadUrl),
        },
        {
            value: "SectionalCharts",
            label: "Sectional Charts",
            url: () => `${chartTileBase}/sectional/{z}/{x}/{-y}.png`,
        },
        {
            value: "TerminalAreaCharts",
            label: "Terminal Area Charts",
            url: () => `${chartTileBase}/terminal-area/{z}/{x}/{-y}.png`,
        },
        {
            value: "IFREnrouteLowCharts",
            label: "IFR Enroute Low Charts",
            url: () => `${chartTileBase}/ifr-enroute-low/{z}/{x}/{-y}.png`,
        },
        {
            value: "IFREnrouteHighCharts",
            label: "IFR Enroute High Charts",
            url: () => `${chartTileBase}/ifr-enroute-high/{z}/{x}/{-y}.png`,
        },
        {
            value: "HelicopterCharts",
            label: "Helicopter Charts",
            url: () => `${chartTileBase}/helicopter/{z}/{x}/{-y}.png`,
        },
    ];
}

function interpolateColor(value: number): string {
    const clamped = Math.max(0, Math.min(1, value));
    const r = Math.round(255 * clamped);
    const g = Math.round(255 * (1 - clamped));
    return `rgba(${r},${g},0,0.55)`;
}

function toRadians(degrees: number): number {
    return degrees * (Math.PI / 180);
}

function createCoordinateKey(lat: number, lon: number, precision = 6): string {
    return `${lat.toFixed(precision)},${lon.toFixed(precision)}`;
}

function calculateDistanceBetweenPoints(
    lat1: number,
    lon1: number,
    alt1: number,
    lat2: number,
    lon2: number,
    alt2: number,
): { lateral: number; euclidean: number } {
    const earthRadiusMeters = 6371000;
    const lat1Rad = toRadians(lat1);
    const lat2Rad = toRadians(lat2);
    const deltaLatRad = toRadians(lat2 - lat1);
    const deltaLonRad = toRadians(lon2 - lon1);

    const a = Math.sin(deltaLatRad / 2) ** 2
        + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLonRad / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    const lateralFeet = (earthRadiusMeters * c) * 3.28084;
    const deltaAltitudeFeet = alt2 - alt1;

    return {
        lateral: lateralFeet,
        euclidean: Math.sqrt(lateralFeet ** 2 + deltaAltitudeFeet ** 2),
    };
}

function getEventTypeName(eventDefinitionId: number): string {
    for (const [eventName, ids] of Object.entries(EVENT_NAME_TO_DEFINITION_IDS)) {
        if (eventName === EVENT_ANY)
            continue;
        if (ids.includes(eventDefinitionId))
            return eventName;
    }
    return `Event ${eventDefinitionId}`;
}

function toNumber(value: unknown): number | undefined {
    const parsed = Number(value);
    if (!Number.isFinite(parsed))
        return undefined;
    return parsed;
}

function normalizePoint(value: unknown): HeatPoint | null {
    if (!value || typeof value !== "object")
        return null;

    const raw = value as Record<string, unknown>;
    const latitude = toNumber(raw.latitude);
    const longitude = toNumber(raw.longitude);
    const altitudeAgl = toNumber(raw.altitude_agl) ?? toNumber(raw.altitudeAgl) ?? 0;
    const timestamp = String(raw.timestamp ?? "");

    if (latitude === undefined || longitude === undefined)
        return null;

    return {
        latitude,
        longitude,
        altitudeAgl,
        timestamp,
    };
}

function normalizeEventRow(value: unknown): EventRow | null {
    if (!value || typeof value !== "object")
        return null;

    const raw = value as Record<string, unknown>;
    const id = toNumber(raw.id);
    const eventDefinitionId = toNumber(raw.event_definition_id) ?? toNumber(raw.eventDefinitionId);
    const flightId = toNumber(raw.flight_id) ?? toNumber(raw.flightId);
    const otherFlightIdMaybe = toNumber(raw.other_flight_id) ?? toNumber(raw.otherFlightId);
    const severity = toNumber(raw.severity) ?? 0;
    const airframe = String(raw.airframe ?? "").trim();
    const otherAirframe = String(raw.other_airframe ?? raw.otherAirframe ?? "").trim();

    if (id === undefined || eventDefinitionId === undefined || flightId === undefined)
        return null;

    return {
        id,
        eventDefinitionId,
        flightId,
        otherFlightId: (otherFlightIdMaybe !== undefined && otherFlightIdMaybe > 0) ? otherFlightIdMaybe : null,
        severity,
        airframe,
        otherAirframe,
    };
}

function isValidExtent(extent: number[]): boolean {
    return (
        Array.isArray(extent)
        && extent.length === 4
        && extent.every((n) => Number.isFinite(n))
        && extent[0] !== extent[2]
        && extent[1] !== extent[3]
    );
}

function normalizeTimestamp(text: string): string {
    if (!text)
        return "Unknown time";
    const parsed = new Date(text);
    if (Number.isNaN(parsed.getTime()))
        return text;
    return parsed.toLocaleString();
}

function parseBatchResults(response: BatchResponse): Array<{ eventId: number; flightId: number; points: HeatPoint[] }> {
    const rows = Array.isArray(response.results) ? response.results : [];
    return rows
        .map((row) => {
            const eventId = toNumber(row.event_id);
            const flightId = toNumber(row.flight_id);
            if (eventId === undefined || flightId === undefined)
                return null;

            const pointsRaw = Array.isArray(row.points) ? row.points : [];
            const points = pointsRaw
                .map((point) => normalizePoint(point))
                .filter((point): point is HeatPoint => point !== null);

            return { eventId, flightId, points };
        })
        .filter((row): row is { eventId: number; flightId: number; points: HeatPoint[] } => row !== null);
}

function toTimestampMillis(value: string): number | null {
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : null;
}

function buildProximityPointPairs(mainPoints: HeatPoint[], otherPoints: HeatPoint[]): Array<{ mainPoint: HeatPoint; otherPoint: HeatPoint }> {
    if (mainPoints.length === 0 || otherPoints.length === 0)
        return [];

    const pairs: Array<{ mainPoint: HeatPoint; otherPoint: HeatPoint }> = [];
    const otherByTimestamp = new Map<string, HeatPoint[]>();
    const unmatchedOther = new Set<HeatPoint>(otherPoints);

    for (const otherPoint of otherPoints) {
        const queue = otherByTimestamp.get(otherPoint.timestamp) ?? [];
        queue.push(otherPoint);
        otherByTimestamp.set(otherPoint.timestamp, queue);
    }

    for (const mainPoint of mainPoints) {
        const exactTimestampQueue = otherByTimestamp.get(mainPoint.timestamp);

        let matchedOther: HeatPoint | undefined;

        if (exactTimestampQueue && exactTimestampQueue.length > 0) {
            matchedOther = exactTimestampQueue.shift();
        }

        if (!matchedOther && unmatchedOther.size > 0) {
            const mainMillis = toTimestampMillis(mainPoint.timestamp);

            if (mainMillis !== null) {
                let nearestOther: HeatPoint | null = null;
                let nearestDelta = Number.POSITIVE_INFINITY;

                for (const candidateOther of unmatchedOther) {
                    const otherMillis = toTimestampMillis(candidateOther.timestamp);
                    if (otherMillis === null)
                        continue;

                    const delta = Math.abs(mainMillis - otherMillis);
                    if (delta < nearestDelta) {
                        nearestDelta = delta;
                        nearestOther = candidateOther;
                    }
                }

                if (nearestOther)
                    matchedOther = nearestOther;
            }

            if (!matchedOther)
                matchedOther = unmatchedOther.values().next().value as HeatPoint | undefined;
        }

        if (!matchedOther)
            continue;

        unmatchedOther.delete(matchedOther);
        pairs.push({ mainPoint, otherPoint: matchedOther });
    }

    return pairs;
}

function hasAreaSelected(boxCoords: BoxCoords): boolean {
    const values = [boxCoords.minLat, boxCoords.maxLat, boxCoords.minLon, boxCoords.maxLon];
    if (values.some((v) => !v || Number.isNaN(Number(v))))
        return false;

    const allZero = values.every((v) => Number(v) === 0);
    if (allZero)
        return false;

    return true;
}

export default function HeatMapPage() {
    const { userOS } = usePlatform();
    const { endpointStartDate, endpointEndDate, renderDateRangeMonthly} = useTimeHeader();
    const {
        airframes,
        airframeIDSelected,
        setAirframeIDSelected,
        airframeNameSelected,
        setAirframeNameSelected,
    } = useAirframes();
    const { setModal } = useModal();
    const mapLayerOptions = useMemo(() => buildMapLayerOptions(), []);

    const [mapStyle, setMapStyle] = useState<MapStyleName>("Road");
    const [loading, setLoading] = useState(false);

    const [eventChecked, setEventChecked] = useState<Record<string, boolean>>(() => {
        const initial: Record<string, boolean> = {};
        for (const eventName of ALL_EVENT_NAMES)
            initial[eventName] = false;
        return initial;
    });

    const [boxCoords, setBoxCoords] = useState<BoxCoords>({ minLat: "", maxLat: "", minLon: "", maxLon: "" });

    const [displayMinSeverity, setDisplayMinSeverity] = useState(0);
    const [displayMaxSeverity, setDisplayMaxSeverity] = useState(1000);
    const [backendMinSeverity, setBackendMinSeverity] = useState(-10000);
    const [backendMaxSeverity, setBackendMaxSeverity] = useState(1000);

    const [showGrid, setShowGrid] = useState(false);
    const [navigationTipsExpanded, setNavigationTipsExpanded] = useState(true);

    const [eventPointGroups, setEventPointGroups] = useState<EventPointGroup[]>([]);
    const [eventStatistics, setEventStatistics] = useState<EventStatistics>({ totalEvents: 0, eventsByType: {} });

    const [openPopups, setOpenPopups] = useState<PopupState[]>([]);
    const [selectedPoints, setSelectedPoints] = useState<SelectedPoint[]>([]);
    const [distances, setDistances] = useState<Distances>({ lateral: null, euclidean: null });
    const [draggedPopupId, setDraggedPopupId] = useState<string | null>(null);
    const [recentPopupId, setRecentPopupId] = useState<string | null>(null);

    const mapContainerRef = useRef<HTMLDivElement | null>(null);
    const mapRef = useRef<OlMap | null>(null);
    const tileLayersByStyleRef = useRef<Map<string, TileLayer<XYZ>>>(new Map());
    const selectionLayerRef = useRef<VectorLayer<VectorSource> | null>(null);
    const selectionSourceRef = useRef<VectorSource | null>(null);
    const selectionFeatureRef = useRef<Feature<Polygon> | null>(null);

    const heatmapLayerMainRef = useRef<Heatmap | null>(null);
    const heatmapLayerOtherRef = useRef<Heatmap | null>(null);
    const markerLayerRef = useRef<VectorLayer<VectorSource> | null>(null);
    const markerSourceRef = useRef<VectorSource | null>(null);
    const connectorLayerRef = useRef<VectorLayer<VectorSource> | null>(null);
    const connectorSourceRef = useRef<VectorSource | null>(null);
    const gridLayerRef = useRef<VectorLayer<VectorSource> | null>(null);
    const gridSourceRef = useRef<VectorSource | null>(null);

    const openPopupsRef = useRef<PopupState[]>([]);
    const selectedPointsRef = useRef<SelectedPoint[]>([]);

    const draggedPopupIdRef = useRef<string | null>(null);
    const dragStartRef = useRef({ x: 0, y: 0 });
    const popupStartRef = useRef({ left: 0, top: 0 });

    useEffect(() => {
        openPopupsRef.current = openPopups;
    }, [openPopups]);

    useEffect(() => {
        selectedPointsRef.current = selectedPoints;
    }, [selectedPoints]);

    useEffect(() => {
        document.title = "NGAFID — Heat Map";
    }, []);

    const availableAirframes = useMemo(() => (
        airframes.filter((airframe) => airframe.name !== "Garmin Flight Display")
    ), [airframes]);

    const mapStyleDropdown = (
        <div className="flex flex-col gap-2">
            {/* <Label>Map Layer</Label> */}
            <Select
                value={mapStyle}
                onValueChange={(value) => setMapStyle(value as MapStyleName)}
            >
                <Button asChild variant="outline">
                    <SelectTrigger className="w-55">
                        <SelectValue placeholder="Select Map" />
                    </SelectTrigger>
                </Button>
                <SelectContent>
                    {
                        mapLayerOptions.map((mapLayer) => (
                            <SelectItem key={mapLayer.value} value={mapLayer.value}>
                                {mapLayer.label}
                            </SelectItem>
                        ))
                    }
                </SelectContent>
            </Select>
        </div>
    );

    const getSelectedEventNames = useCallback((): string[] => {
        return ALL_EVENT_NAMES.filter((eventName) => eventChecked[eventName]);
    }, [eventChecked]);

    const clearMapLayers = useCallback(() => {
        heatmapLayerMainRef.current?.getSource()?.clear();
        heatmapLayerOtherRef.current?.getSource()?.clear();
        markerSourceRef.current?.clear();
        connectorSourceRef.current?.clear();

        const gridSource = gridSourceRef.current;
        if (gridSource) {
            const existing = gridSource.getFeatures();
            for (const feature of existing)
                gridSource.removeFeature(feature);
        }

        heatmapLayerMainRef.current?.setVisible(false);
        heatmapLayerOtherRef.current?.setVisible(false);
        connectorLayerRef.current?.setVisible(false);
        gridLayerRef.current?.setVisible(false);

        setOpenPopups([]);
        setSelectedPoints([]);
        setDistances({ lateral: null, euclidean: null });
        setEventStatistics({ totalEvents: 0, eventsByType: {} });
    }, []);

    const calculateEventStatistics = useCallback((groups: EventPointGroup[]): EventStatistics => {
        const eventsByType: Record<string, number> = {};
        let totalEvents = 0;

        for (const event of groups) {
            const eventType = getEventTypeName(event.eventDefinitionId);
            eventsByType[eventType] = (eventsByType[eventType] ?? 0) + 1;
            totalEvents += 1;
        }

        return { totalEvents, eventsByType };
    }, []);

    const applyMapStyle = useCallback((styleName: MapStyleName) => {
        for (const [key, layer] of tileLayersByStyleRef.current.entries())
            layer.setVisible(key === styleName);
    }, []);

    const renderEventGroups = useCallback((groups: EventPointGroup[], useGrid: boolean) => {
        const map = mapRef.current;
        const heatmapLayerMain = heatmapLayerMainRef.current;
        const heatmapLayerOther = heatmapLayerOtherRef.current;
        const markerSource = markerSourceRef.current;
        const connectorSource = connectorSourceRef.current;
        const connectorLayer = connectorLayerRef.current;
        const gridSource = gridSourceRef.current;

        if (!map || !heatmapLayerMain || !heatmapLayerOther || !markerSource || !connectorSource || !connectorLayer || !gridSource)
            return;

        const sourceMain = heatmapLayerMain.getSource();
        const sourceOther = heatmapLayerOther.getSource();
        if (!sourceMain || !sourceOther)
            return;

        sourceMain.clear();
        sourceOther.clear();
        markerSource.clear();
        connectorSource.clear();
        gridSource.clear();

        const coordinateRegistry = new Map<string, { coord: number[]; events: MarkerEvent[] }>();
        const allPointsForGrid: Array<{ latitude: number; longitude: number }> = [];
        const connectorFeatures: Feature<LineString>[] = [];

        let hasSecondaryPoints = false;

        for (const eventGroup of groups) {
            for (const point of eventGroup.mainFlightPoints) {
                const coord = fromLonLat([point.longitude + 0.0001, point.latitude + 0.0001]);
                const feature = new Feature({ geometry: new Point(coord) });
                feature.set("weight", 0.7);
                sourceMain.addFeature(feature);

                const coordKey = createCoordinateKey(point.latitude, point.longitude);
                const groupAtCoord = coordinateRegistry.get(coordKey) ?? { coord, events: [] };
                groupAtCoord.events.push({
                    eventId: eventGroup.eventId,
                    eventDefinitionId: eventGroup.eventDefinitionId,
                    flightId: eventGroup.mainFlightId,
                    otherFlightId: eventGroup.otherFlightId,
                    time: point.timestamp,
                    flightAirframe: eventGroup.airframe,
                    otherFlightAirframe: eventGroup.otherAirframe,
                    severity: eventGroup.severity,
                    altitudeAgl: point.altitudeAgl,
                    latitude: point.latitude,
                    longitude: point.longitude,
                });
                coordinateRegistry.set(coordKey, groupAtCoord);
                allPointsForGrid.push({ latitude: point.latitude, longitude: point.longitude });
            }

            for (const point of eventGroup.otherFlightPoints) {
                hasSecondaryPoints = true;
                const coord = fromLonLat([point.longitude + 0.0001, point.latitude + 0.0001]);
                const feature = new Feature({ geometry: new Point(coord) });
                feature.set("weight", 0.7);
                sourceOther.addFeature(feature);

                const coordKey = createCoordinateKey(point.latitude, point.longitude);
                const groupAtCoord = coordinateRegistry.get(coordKey) ?? { coord, events: [] };
                groupAtCoord.events.push({
                    eventId: eventGroup.eventId,
                    eventDefinitionId: eventGroup.eventDefinitionId,
                    flightId: eventGroup.otherFlightId ?? 0,
                    otherFlightId: eventGroup.mainFlightId,
                    time: point.timestamp,
                    flightAirframe: eventGroup.otherAirframe,
                    otherFlightAirframe: eventGroup.airframe,
                    severity: eventGroup.severity,
                    altitudeAgl: point.altitudeAgl,
                    latitude: point.latitude,
                    longitude: point.longitude,
                });
                coordinateRegistry.set(coordKey, groupAtCoord);
                allPointsForGrid.push({ latitude: point.latitude, longitude: point.longitude });
            }

            // Draw connector lines for proximity pairs shown on map.
            if (PROXIMITY_DEFINITION_IDS.has(eventGroup.eventDefinitionId) && eventGroup.otherFlightPoints.length > 0) {
                const pointPairs = buildProximityPointPairs(eventGroup.mainFlightPoints, eventGroup.otherFlightPoints);
                const mainFlightColor = getFlightColorRgb(eventGroup.mainFlightId);
                const otherFlightColor = getFlightColorRgb(eventGroup.otherFlightId ?? eventGroup.mainFlightId);
                const gradientSegments = 8;

                for (const pointPair of pointPairs) {
                    const mainCoord = fromLonLat([pointPair.mainPoint.longitude + 0.0001, pointPair.mainPoint.latitude + 0.0001]);
                    const otherCoord = fromLonLat([pointPair.otherPoint.longitude + 0.0001, pointPair.otherPoint.latitude + 0.0001]);

                    for (let segmentIndex = 0; segmentIndex < gradientSegments; segmentIndex += 1) {
                        const t0 = segmentIndex / gradientSegments;
                        const t1 = (segmentIndex + 1) / gradientSegments;
                        const tm = (t0 + t1) / 2;

                        const segmentStart: [number, number] = [
                            lerp(mainCoord[0], otherCoord[0], t0),
                            lerp(mainCoord[1], otherCoord[1], t0),
                        ];
                        const segmentEnd: [number, number] = [
                            lerp(mainCoord[0], otherCoord[0], t1),
                            lerp(mainCoord[1], otherCoord[1], t1),
                        ];

                        const mixedColor = interpolateRgb(mainFlightColor, otherFlightColor, tm);
                        const connectorColor = toRgba(mixedColor, 0.62);

                        const connectorSegment = new Feature({
                            geometry: new LineString([segmentStart, segmentEnd]),
                        });
                        connectorSegment.setStyle(getConnectorSegmentStyle(connectorColor));
                        connectorFeatures.push(connectorSegment);
                    }
                }
            }
        }

        connectorSource.addFeatures(connectorFeatures);

        const zoom = map.getView().getZoom() ?? 0;
        const detailsVisible = zoom >= MARKER_VISIBILITY_ZOOM_THRESHOLD;
        connectorLayer.setVisible(detailsVisible && connectorFeatures.length > 0);

        for (const [coordKey, grouped] of coordinateRegistry.entries()) {
            const first = grouped.events[0];

            const flightCounts = new Map<number, number>();
            for (const markerEvent of grouped.events) {
                const flightId = markerEvent.flightId;
                flightCounts.set(flightId, (flightCounts.get(flightId) ?? 0) + 1);
            }

            let markerFlightId = first.flightId;
            let markerFlightCount = -1;
            for (const [flightId, count] of flightCounts.entries()) {
                if (count > markerFlightCount) {
                    markerFlightId = flightId;
                    markerFlightCount = count;
                }
            }

            const marker = new Feature({ geometry: new Point(grouped.coord) });
            marker.setProperties({
                isMarker: true,
                coordKey,
                markerFlightId,
                events: grouped.events,
                eventId: first.eventId,
                eventDefinitionId: first.eventDefinitionId,
                flightId: first.flightId,
                otherFlightId: first.otherFlightId,
                time: first.time,
                flightAirframe: first.flightAirframe,
                otherFlightAirframe: first.otherFlightAirframe,
                severity: first.severity,
                altitudeAgl: first.altitudeAgl,
                latitude: first.latitude,
                longitude: first.longitude,
            });
            markerSource.addFeature(marker);
        }

        const extentMainRaw = sourceMain.getExtent();
        const extentOtherRaw = sourceOther.getExtent();
        const extentMain = extentMainRaw ? [...extentMainRaw] : null;
        const extentOther = extentOtherRaw ? [...extentOtherRaw] : null;

        const mainHasExtent = !!extentMain && isValidExtent(extentMain);
        const otherHasExtent = !!extentOther && isValidExtent(extentOther);

        if (mainHasExtent || otherHasExtent) {
            let extentToFit: number[] = extentMain ?? extentOther ?? [0, 0, 0, 0];
            if (mainHasExtent && otherHasExtent) {
                extentToFit = [
                    Math.min(extentMain[0], extentOther[0]),
                    Math.min(extentMain[1], extentOther[1]),
                    Math.max(extentMain[2], extentOther[2]),
                    Math.max(extentMain[3], extentOther[3]),
                ];
            } else if (!mainHasExtent && extentOther) {
                extentToFit = extentOther;
            }

            map.getView().fit(extentToFit, { padding: [50, 50, 50, 50], maxZoom: 15 });
        }

        if (useGrid) {
            const gridSize = 0.05;
            const gridCounts: Record<string, number> = {};

            for (const point of allPointsForGrid) {
                const latKey = (Math.floor(point.latitude / gridSize) * gridSize).toFixed(4);
                const lonKey = (Math.floor(point.longitude / gridSize) * gridSize).toFixed(4);
                const key = `${latKey},${lonKey}`;
                gridCounts[key] = (gridCounts[key] ?? 0) + 1;
            }

            let latStart = Number(boxCoords.minLat);
            let latEnd = Number(boxCoords.maxLat);
            let lonStart = Number(boxCoords.minLon);
            let lonEnd = Number(boxCoords.maxLon);

            if (!Number.isFinite(latStart) || !Number.isFinite(latEnd) || !Number.isFinite(lonStart) || !Number.isFinite(lonEnd)) {
                if (allPointsForGrid.length > 0) {
                    latStart = Math.min(...allPointsForGrid.map((point) => point.latitude));
                    latEnd = Math.max(...allPointsForGrid.map((point) => point.latitude));
                    lonStart = Math.min(...allPointsForGrid.map((point) => point.longitude));
                    lonEnd = Math.max(...allPointsForGrid.map((point) => point.longitude));
                }
            }

            if (latStart > latEnd)
                [latStart, latEnd] = [latEnd, latStart];
            if (lonStart > lonEnd)
                [lonStart, lonEnd] = [lonEnd, lonStart];

            const maxCount = Math.max(1, ...Object.values(gridCounts));
            const toWebMercator = getTransform("EPSG:4326", "EPSG:3857");

            const xs: number[] = [];
            const ys: number[] = [];

            for (let lon = Math.floor(lonStart / gridSize) * gridSize; lon <= lonEnd + 1e-9; lon += gridSize)
                xs.push(toWebMercator([lon, 0])[0]);

            for (let lat = Math.floor(latStart / gridSize) * gridSize; lat <= latEnd + 1e-9; lat += gridSize)
                ys.push(toWebMercator([0, lat])[1]);

            const rows = ys.length - 1;
            const cols = xs.length - 1;

            const densityFeatures: Feature<Polygon>[] = [];

            for (let row = 0; row < rows; row++) {
                const y0 = ys[row];
                const y1 = ys[row + 1];
                const latVal = (Math.floor(latStart / gridSize) + row) * gridSize;

                for (let col = 0; col < cols; col++) {
                    const x0 = xs[col];
                    const x1 = xs[col + 1];
                    const lonVal = (Math.floor(lonStart / gridSize) + col) * gridSize;
                    const count = gridCounts[`${latVal.toFixed(4)},${lonVal.toFixed(4)}`] ?? 0;
                    const intensity = Math.sqrt(count / maxCount);

                    const polygon = new Polygon([[
                        [x0, y0],
                        [x1, y0],
                        [x1, y1],
                        [x0, y1],
                        [x0, y0],
                    ]]);
                    const cell = new Feature(polygon);
                    cell.set("kind", "density");
                    cell.set("fillColor", interpolateColor(intensity));

                    densityFeatures.push(cell);
                }
            }

            gridSource.addFeatures(densityFeatures);

            heatmapLayerMain.setVisible(false);
            heatmapLayerOther.setVisible(false);
            gridLayerRef.current?.setVisible(true);
        } else {
            heatmapLayerMain.setVisible(true);
            heatmapLayerOther.setVisible(hasSecondaryPoints);
            gridLayerRef.current?.setVisible(false);
        }
    }, [boxCoords.maxLat, boxCoords.maxLon, boxCoords.minLat, boxCoords.minLon]);

    const recalculateDistances = useCallback((points: SelectedPoint[]) => {
        if (points.length !== 2) {
            setDistances({ lateral: null, euclidean: null });
            return;
        }

        const [pointA, pointB] = points;
        const computed = calculateDistanceBetweenPoints(
            pointA.latitude,
            pointA.longitude,
            pointA.altitude,
            pointB.latitude,
            pointB.longitude,
            pointB.altitude,
        );
        setDistances({ lateral: computed.lateral, euclidean: computed.euclidean });
    }, []);

    const fetchEventColumnsValues = useCallback(async (
        eventId: number,
        flightId: number,
        timestamp: string,
    ): Promise<Record<string, string | number | null> | null> => {
        const params = new URLSearchParams({
            event_id: String(eventId),
            flight_id: String(flightId),
            timestamp,
        });

        const response = await fetchJson.get<unknown>(`/api/protected/event_columns_values?${params.toString()}`).catch(() => null);
        if (!response || typeof response !== "object")
            return null;

        const raw = (response as Record<string, unknown>).column_values;
        if (!raw || typeof raw !== "object")
            return null;

        const excluded = new Set(["AltAGL", "AltMSL", "Latitude", "Longitude", "Lcl Date", "Lcl Time", "UTCOfst"]);
        const values: Record<string, string | number | null> = {};
        for (const [column, value] of Object.entries(raw as Record<string, unknown>)) {
            if (excluded.has(column))
                continue;

            if (value === null || value === undefined) {
                values[column] = null;
                continue;
            }

            if (typeof value === "number" || typeof value === "string") {
                values[column] = value;
                continue;
            }

            values[column] = String(value);
        }

        return values;
    }, []);

    const fetchAndAttachPopupColumns = useCallback(async (
        popupId: string,
        markerEvents: MarkerEvent[],
    ) => {
        const merged: Record<string, string | number | null> = {};

        for (const markerEvent of markerEvents) {
            if (!markerEvent.eventId || !markerEvent.flightId || !markerEvent.time)
                continue;

            const values = await fetchEventColumnsValues(markerEvent.eventId, markerEvent.flightId, markerEvent.time);
            if (!values)
                continue;

            for (const [key, value] of Object.entries(values))
                merged[key] = value;
        }

        if (Object.keys(merged).length === 0)
            return;

        setOpenPopups((previous) => (
            previous.map((popup) => (
                popup.id === popupId
                    ? { ...popup, data: { ...popup.data, columnValues: merged } }
                    : popup
            ))
        ));
    }, [fetchEventColumnsValues]);

    const handlePopupMouseMove = useCallback((event: MouseEvent) => {
        if (!draggedPopupIdRef.current || !mapContainerRef.current)
            return;

        const dx = event.clientX - dragStartRef.current.x;
        const dy = event.clientY - dragStartRef.current.y;
        const mapBounds = mapContainerRef.current.getBoundingClientRect();

        const nextLeft = Math.max(0, Math.min(mapBounds.width - 240, popupStartRef.current.left + dx));
        const nextTop = Math.max(0, Math.min(mapBounds.height - 130, popupStartRef.current.top + dy));

        setOpenPopups((previous) => (
            previous.map((popup) => (
                popup.id === draggedPopupIdRef.current
                    ? {
                        ...popup,
                        position: {
                            left: nextLeft,
                            top: nextTop,
                        },
                    }
                    : popup
            ))
        ));
    }, []);

    const handlePopupMouseUp = useCallback(() => {
        setDraggedPopupId(null);
        draggedPopupIdRef.current = null;
        window.removeEventListener("mousemove", handlePopupMouseMove);
        window.removeEventListener("mouseup", handlePopupMouseUp);
    }, [handlePopupMouseMove]);

    const handlePopupMouseDown = useCallback((event: React.MouseEvent<HTMLButtonElement>, popupId: string) => {
        event.preventDefault();

        const popupElement = event.currentTarget.parentElement;
        if (!popupElement)
            return;

        dragStartRef.current = { x: event.clientX, y: event.clientY };
        popupStartRef.current = {
            left: Number.parseFloat(popupElement.style.left) || 0,
            top: Number.parseFloat(popupElement.style.top) || 0,
        };

        setDraggedPopupId(popupId);
        setRecentPopupId(popupId);
        draggedPopupIdRef.current = popupId;

        window.addEventListener("mousemove", handlePopupMouseMove);
        window.addEventListener("mouseup", handlePopupMouseUp);
    }, [handlePopupMouseMove, handlePopupMouseUp]);

    useEffect(() => {
        return () => {
            window.removeEventListener("mousemove", handlePopupMouseMove);
            window.removeEventListener("mouseup", handlePopupMouseUp);
        };
    }, [handlePopupMouseMove, handlePopupMouseUp]);

    useEffect(() => {
        if (!mapContainerRef.current || mapRef.current)
            return;

        const tileLayers: TileLayer<XYZ>[] = [];
        const tileLayersByStyle = new Map<string, TileLayer<XYZ>>();

        for (const option of mapLayerOptions) {
            const url = option.url();
            if (!url)
                continue;

            const layer = new TileLayer({
                visible: false,
                source: new XYZ({ url }),
            });

            layer.set("mapStyle", option.value);
            tileLayers.push(layer);
            tileLayersByStyle.set(option.value, layer);
        }

        tileLayersByStyleRef.current = tileLayersByStyle;

        const heatSourceMain = new VectorSource();
        const heatSourceOther = new VectorSource();
        const markerSource = new VectorSource();
        const connectorSource = new VectorSource();
        const gridSource = new VectorSource({ useSpatialIndex: false });
        const selectionSource = new VectorSource();

        const heatmapMain = new Heatmap({
            source: heatSourceMain,
            blur: 3,
            radius: 4,
            opacity: 0.8,
            gradient: [
                "rgba(0,255,0,0)",
                "rgba(0,255,0,1)",
                "rgba(255,255,0,1)",
                "rgba(255,165,0,1)",
                "rgba(255,0,0,1)",
            ],
        });
        heatmapMain.setVisible(false);

        const heatmapOther = new Heatmap({
            source: heatSourceOther,
            blur: 3,
            radius: 4,
            opacity: 0.8,
            gradient: [
                "rgba(0,255,0,0)",
                "rgba(0,255,0,1)",
                "rgba(255,255,0,1)",
                "rgba(255,165,0,1)",
                "rgba(255,0,0,1)",
            ],
        });
        heatmapOther.setVisible(false);

        const markerLayer = new VectorLayer({
            source: markerSource,
            style: (feature) => (
                feature.get("isMarker") ? getMarkerStyleForFlight(toNumber(feature.get("markerFlightId")) ?? 1) : undefined
            ),
            zIndex: 1001,
        });
        markerLayer.set("interactive", true);
        markerLayer.setVisible(false);

        const connectorLayer = new VectorLayer({
            source: connectorSource,
            style: PROXIMITY_CONNECTOR_STYLE,
            zIndex: 1000,
        });
        connectorLayer.setVisible(false);

        const gridLayer = new VectorLayer({
            source: gridSource,
            style: (feature) => new Style({
                fill: new Fill({ color: String(feature.get("fillColor") ?? "rgba(0,0,0,0)") }),
                stroke: new Stroke({ color: "rgba(0,0,0,0.12)", width: 1 }),
            }),
            zIndex: 1000,
            opacity: 0.75,
        });
        gridLayer.setVisible(false);

        const selectionLayer = new VectorLayer({
            source: selectionSource,
            style: new Style({
                fill: new Fill({ color: "rgba(128,128,128,0.30)" }),
                stroke: new Stroke({ color: "rgba(128,128,128,0.75)", width: 1.5 }),
            }),
            zIndex: 1100,
        });

        const map = new OlMap({
            target: mapContainerRef.current,
            layers: [...tileLayers, heatmapMain, heatmapOther, connectorLayer, markerLayer, gridLayer, selectionLayer],
            view: new View({
                center: fromLonLat([-95, 40]),
                zoom: 4,
            }),
        });

        const dragBox = new DragBox({ condition: platformModifierKeyOnly });
        map.addInteraction(dragBox);

        dragBox.on("boxend", () => {
            const extent = dragBox.getGeometry().getExtent();
            const bottomLeft = toLonLat([extent[0], extent[1]]);
            const topRight = toLonLat([extent[2], extent[3]]);

            setBoxCoords({
                minLat: Math.min(bottomLeft[1], topRight[1]).toFixed(6),
                maxLat: Math.max(bottomLeft[1], topRight[1]).toFixed(6),
                minLon: Math.min(bottomLeft[0], topRight[0]).toFixed(6),
                maxLon: Math.max(bottomLeft[0], topRight[0]).toFixed(6),
            });

            const polygon = new Polygon([[
                [extent[0], extent[1]],
                [extent[0], extent[3]],
                [extent[2], extent[3]],
                [extent[2], extent[1]],
                [extent[0], extent[1]],
            ]]);

            const feature = new Feature(polygon);
            selectionSource.clear();
            selectionSource.addFeature(feature);
            selectionFeatureRef.current = feature;
        });

        map.on("singleclick", (event) => {
            map.forEachFeatureAtPixel(
                event.pixel,
                (feature) => {
                    const isMarker = Boolean(feature.get("isMarker"));
                    if (!isMarker)
                        return;

                    if (openPopupsRef.current.length >= 2 || selectedPointsRef.current.length >= 2)
                        return;

                    const geometry = feature.getGeometry();
                    if (!(geometry instanceof Point))
                        return;

                    const coord = geometry.getCoordinates();
                    const latitude = toNumber(feature.get("latitude"));
                    const longitude = toNumber(feature.get("longitude"));
                    const altitude = toNumber(feature.get("altitudeAgl")) ?? 0;
                    const timeRaw = String(feature.get("time") ?? "");

                    if (latitude === undefined || longitude === undefined)
                        return;

                    const coordKey = String(feature.get("coordKey") ?? createCoordinateKey(latitude, longitude));
                    const popupId = `coord-${coordKey}`;
                    if (openPopupsRef.current.some((popup) => popup.id === popupId))
                        return;

                    const markerEvents = Array.isArray(feature.get("events"))
                        ? (feature.get("events") as MarkerEvent[])
                        : [];

                    const eventTypes = markerEvents.length > 0
                        ? Array.from(new Set(markerEvents.map((eventItem) => getEventTypeName(eventItem.eventDefinitionId))))
                        : [getEventTypeName(toNumber(feature.get("eventDefinitionId")) ?? 0)];

                    const eventSummaries: PopupEventSummary[] = markerEvents.map((eventItem) => ({
                        eventId: eventItem.eventId,
                        eventDefinitionId: eventItem.eventDefinitionId,
                        eventType: getEventTypeName(eventItem.eventDefinitionId),
                        time: normalizeTimestamp(eventItem.time),
                        flightId: eventItem.flightId,
                        otherFlightId: eventItem.otherFlightId,
                        severity: eventItem.severity,
                        flightAirframe: eventItem.flightAirframe,
                        otherFlightAirframe: eventItem.otherFlightAirframe,
                    }));

                    const popupData: PopupData = {
                        time: normalizeTimestamp(timeRaw),
                        latitude,
                        longitude,
                        altitude,
                        flightId: toNumber(feature.get("flightId")) ?? 0,
                        flightAirframe: String(feature.get("flightAirframe") ?? "N/A"),
                        otherFlightId: toNumber(feature.get("otherFlightId")) ?? null,
                        otherFlightAirframe: String(feature.get("otherFlightAirframe") ?? "N/A"),
                        severity: toNumber(feature.get("severity")) ?? 0,
                        eventId: toNumber(feature.get("eventId")) ?? null,
                        eventType: eventTypes.length === 1 ? eventTypes[0] : `${eventTypes.length} Events`,
                        eventTypes,
                        eventSummaries,
                    };

                    const pixel = map.getPixelFromCoordinate(coord);
                    const position = {
                        left: pixel ? pixel[0] : 100,
                        top: pixel ? pixel[1] : 100,
                    };

                    setOpenPopups((previous) => {
                        if (previous.length >= 2)
                            return previous;
                        return [...previous, { id: popupId, coord, position, data: popupData }];
                    });

                    setSelectedPoints((previous) => {
                        if (previous.length >= 2)
                            return previous;

                        const next = [...previous, {
                            id: popupId,
                            latitude,
                            longitude,
                            altitude,
                        }];
                        recalculateDistances(next);
                        return next;
                    });

                    setRecentPopupId(popupId);

                    if (markerEvents.length > 0)
                        void fetchAndAttachPopupColumns(popupId, markerEvents);
                },
                {
                    layerFilter: (layer) => layer === markerLayerRef.current,
                },
            );
        });

        map.on("moveend", () => {
            const zoom = map.getView().getZoom();
            if (zoom === undefined)
                return;

            const detailsVisible = zoom >= MARKER_VISIBILITY_ZOOM_THRESHOLD;
            markerLayer.setVisible(detailsVisible);
            connectorLayer.setVisible(detailsVisible && connectorSource.getFeatures().length > 0);
        });

        mapRef.current = map;
        heatmapLayerMainRef.current = heatmapMain;
        heatmapLayerOtherRef.current = heatmapOther;
        markerLayerRef.current = markerLayer;
        markerSourceRef.current = markerSource;
        connectorLayerRef.current = connectorLayer;
        connectorSourceRef.current = connectorSource;
        gridLayerRef.current = gridLayer;
        gridSourceRef.current = gridSource;
        selectionLayerRef.current = selectionLayer;
        selectionSourceRef.current = selectionSource;

        applyMapStyle(mapStyle);

        return () => {
            map.setTarget(undefined);
            mapRef.current = null;
            heatmapLayerMainRef.current = null;
            heatmapLayerOtherRef.current = null;
            markerLayerRef.current = null;
            markerSourceRef.current = null;
            connectorLayerRef.current = null;
            connectorSourceRef.current = null;
            gridLayerRef.current = null;
            gridSourceRef.current = null;
            selectionLayerRef.current = null;
            selectionSourceRef.current = null;
            selectionFeatureRef.current = null;
            tileLayersByStyleRef.current.clear();
        };
    }, [applyMapStyle, fetchAndAttachPopupColumns, mapLayerOptions, mapStyle, recalculateDistances]);

    useEffect(() => {
        applyMapStyle(mapStyle);
    }, [applyMapStyle, mapStyle]);

    useEffect(() => {
        if (eventPointGroups.length === 0)
            return;
        renderEventGroups(eventPointGroups, showGrid);
    }, [eventPointGroups, renderEventGroups, showGrid]);

    const updateMinSeverity = useCallback((nextValue: number) => {
        const clamped = Math.min(Math.max(0, nextValue), displayMaxSeverity);
        setDisplayMinSeverity(clamped);
        setBackendMinSeverity(clamped === 0 ? -10000 : clamped);
    }, [displayMaxSeverity]);

    const updateMaxSeverity = useCallback((nextValue: number) => {
        const clamped = Math.max(Math.min(1000, nextValue), displayMinSeverity);
        setDisplayMaxSeverity(clamped);
        setBackendMaxSeverity(clamped);
    }, [displayMinSeverity]);

    const handleEventToggle = useCallback((eventName: string) => {
        let shouldClear = false;
        setEventChecked((previous) => {
            const wasChecked = Boolean(previous[eventName]);
            const next = { ...previous, [eventName]: !wasChecked };

            if ((eventName === "Proximity" || eventName === EVENT_ANY) && wasChecked)
                shouldClear = true;

            return next;
        });

        if (shouldClear)
            clearMapLayers();
    }, [clearMapLayers]);

    const fetchEvents = useCallback(async (definitionIds: number[]): Promise<EventRow[]> => {
        const params = new URLSearchParams({
            event_definition_ids: definitionIds.join(","),
            start_date: endpointStartDate,
            end_date: endpointEndDate,
            area_min_lat: String(Number(boxCoords.minLat)),
            area_max_lat: String(Number(boxCoords.maxLat)),
            area_min_lon: String(Number(boxCoords.minLon)),
            area_max_lon: String(Number(boxCoords.maxLon)),
            min_severity: String(backendMinSeverity),
            max_severity: String(backendMaxSeverity),
        });

        if (airframeIDSelected !== ALL_AIRFRAMES_ID)
            params.set("airframe", airframeNameSelected);

        const response = await fetchJson.get<unknown>(`/api/protected/proximity_events_in_box?${params.toString()}`);
        const rows = Array.isArray(response) ? response : [];

        return rows
            .map((row) => normalizeEventRow(row))
            .filter((row): row is EventRow => row !== null);
    }, [airframeIDSelected, airframeNameSelected, backendMaxSeverity, backendMinSeverity, boxCoords.maxLat, boxCoords.maxLon, boxCoords.minLat, boxCoords.minLon, endpointEndDate, endpointStartDate]);

    const fetchPointsByEventId = useCallback(async (eventIds: number[]) => {
        const batches: number[][] = [];
        for (let index = 0; index < eventIds.length; index += BATCH_SIZE)
            batches.push(eventIds.slice(index, index + BATCH_SIZE));

        const responses = await Promise.all(
            batches.map((batch) => fetchJson.post<BatchResponse>("/api/protected/heatmap_points_batch", { event_ids: batch })),
        );

        const rows: Array<{ eventId: number; flightId: number; points: HeatPoint[] }> = [];
        for (const response of responses)
            rows.push(...parseBatchResults(response));

        return rows;
    }, []);

    const convertRowsToGroups = useCallback((events: EventRow[], pointRows: Array<{ eventId: number; flightId: number; points: HeatPoint[] }>): EventPointGroup[] => {
        const pointsByEventAndFlight = new Map<number, Map<number, HeatPoint[]>>();

        for (const row of pointRows) {
            const byFlight = pointsByEventAndFlight.get(row.eventId) ?? new Map<number, HeatPoint[]>();
            byFlight.set(row.flightId, row.points);
            pointsByEventAndFlight.set(row.eventId, byFlight);
        }

        const groups: EventPointGroup[] = [];
        const seenProximityPairs = new Set<string>();

        for (const event of events) {
            const byFlight = pointsByEventAndFlight.get(event.id);
            const mainPoints = byFlight?.get(event.flightId) ?? [];

            const isProximityEvent = PROXIMITY_DEFINITION_IDS.has(event.eventDefinitionId);

            if (isProximityEvent && event.otherFlightId && event.otherFlightId > 0) {
                const pairKey = `${event.id}-${Math.min(event.flightId, event.otherFlightId)}-${Math.max(event.flightId, event.otherFlightId)}`;
                if (seenProximityPairs.has(pairKey))
                    continue;
                seenProximityPairs.add(pairKey);

                const otherPoints = byFlight?.get(event.otherFlightId) ?? [];

                groups.push({
                    eventId: event.id,
                    eventDefinitionId: event.eventDefinitionId,
                    mainFlightId: event.flightId,
                    otherFlightId: event.otherFlightId,
                    mainFlightPoints: mainPoints,
                    otherFlightPoints: otherPoints,
                    severity: event.severity,
                    airframe: event.airframe,
                    otherAirframe: event.otherAirframe,
                });
                continue;
            }

            groups.push({
                eventId: event.id,
                eventDefinitionId: event.eventDefinitionId,
                mainFlightId: event.flightId,
                otherFlightId: null,
                mainFlightPoints: mainPoints,
                otherFlightPoints: [],
                severity: event.severity,
                airframe: event.airframe,
                otherAirframe: "",
            });
        }

        return groups;
    }, []);

    const handleApply = useCallback(async () => {

        setLoading(true);

        clearMapLayers();
        setEventPointGroups([]);

        try {

            const selectedEventNames = getSelectedEventNames();

            // No event names selected -> Error
            if (selectedEventNames.length === 0) {
                setModal(ErrorModal, {
                    title: "No Events Selected",
                    message: "Please select at least one event type to display on the heat map.",
                });
                setLoading(false);
                return;

            }

            // No region selected -> Error
            if (!hasAreaSelected(boxCoords)) {
                
                setModal(ErrorModal, {
                    title: "No Area Selected",
                    message: "Please select an area on the map by holding Ctrl (or Cmd on Mac) and dragging to create a selection box.",
                });
                setLoading(false);
                return;

            }

            const definitionIds = selectedEventNames
                .flatMap((eventName) => EVENT_NAME_TO_DEFINITION_IDS[eventName] ?? [])
                .filter((id): id is number => Number.isFinite(id));

            // No valid event definition IDs found for selected event names -> Error
            if (definitionIds.length === 0) {
                setModal(ErrorModal, {
                    title: "No Valid Events Selected",
                    message: "Please select at least one valid event type to display on the heat map.",
                });
                setLoading(false);
                return;
            }

            const events = await fetchEvents(definitionIds);

            // No events found for selected constraints -> Error
            if (events.length === 0) {
                setModal(ErrorModal, {
                    title: "No Events Found",
                    message: "No events found with the selected constraints.",
                });
                setLoading(false);
                return;
            }

            const points = await fetchPointsByEventId(events.map((event) => event.id));
            const groups = convertRowsToGroups(events, points);

            setEventPointGroups(groups);
            setEventStatistics(calculateEventStatistics(groups));
            renderEventGroups(groups, showGrid);

            if (selectionSourceRef.current && selectionFeatureRef.current) {
                selectionSourceRef.current.removeFeature(selectionFeatureRef.current);
                selectionFeatureRef.current = null;
            }
        } catch (applyError) {

            log.error("Failed to apply Heat Map filters:", applyError);
            setModal(ErrorModal, {
                title: "Error Applying Filters",
                message: "An error occurred while applying the heat map filters.",
            });
            setEventPointGroups([]);
            setEventStatistics({ totalEvents: 0, eventsByType: {} });

        } finally {
            setLoading(false);
        }

    }, [boxCoords, calculateEventStatistics, clearMapLayers, convertRowsToGroups, fetchEvents, fetchPointsByEventId, getSelectedEventNames, renderEventGroups, showGrid]);

    const closePopup = useCallback((popupId: string) => {
        setOpenPopups((previous) => previous.filter((popup) => popup.id !== popupId));
        setSelectedPoints((previous) => {
            const next = previous.filter((point) => point.id !== popupId);
            recalculateDistances(next);
            return next;
        });
    }, [recalculateDistances]);

    const commandDragText = userOS === "Mac OS" ? "Cmd + Drag" : "Ctrl + Drag";

    const heatmapDataLoaded = eventPointGroups.length > 0;

    const dependencies = useMemo(
        () => [
            airframeIDSelected,
            airframeNameSelected,
            boxCoords.minLat,
            boxCoords.maxLat,
            boxCoords.minLon,
            boxCoords.maxLon,
            displayMinSeverity,
            displayMaxSeverity,
            JSON.stringify(eventChecked),
        ],
        [airframeIDSelected, airframeNameSelected, boxCoords.maxLat, boxCoords.maxLon, boxCoords.minLat, boxCoords.minLon, displayMaxSeverity, displayMinSeverity, eventChecked],
    );

    return (
        <div className="h-full min-h-0 overflow-y-auto">

            <div className="h-full min-h-0 p-4 space-y-4 flex flex-col">

                {/* Time Header */}
                <TimeHeader onApply={handleApply} dependencies={dependencies}>

                    {/* Airframe Type Selection */}
                    <div className="flex flex-col gap-2">
                        <Label>Airframe Type</Label>
                        <Select
                            value={airframeIDSelected.toString()}
                            onValueChange={(value) => {
                                const id = parseInt(value, 10);
                                setAirframeIDSelected(id);

                                const selected = availableAirframes.find((airframe) => airframe.id === id);
                                setAirframeNameSelected(selected?.name ?? ALL_AIRFRAMES_NAME);
                            }}
                        >
                            <Button asChild variant="outline">
                                <SelectTrigger className="w-55">
                                    <SelectValue placeholder="Select Airframe" />
                                </SelectTrigger>
                            </Button>
                            <SelectContent>
                                {availableAirframes.map((airframe) => (
                                    <SelectItem key={airframe.id} value={airframe.id.toString()}>
                                        {airframe.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Selected Bounds */}
                    <div className="flex flex-col gap-2 min-w-65">
                        <Label className="text-sm">Selected Bounds</Label>
                        <div className="text-xs grid grid-cols-2 gap-x-2 gap-y-1">
                            <span>Min Lat: {boxCoords.minLat || "--"}</span>
                            <span>Max Lat: {boxCoords.maxLat || "--"}</span>
                            <span>Min Lon: {boxCoords.minLon || "--"}</span>
                            <span>Max Lon: {boxCoords.maxLon || "--"}</span>
                        </div>
                    </div>

                    {/* Severity Range */}
                    <div className="flex flex-col gap-2 w-70">
                        <Label className="text-sm">Severity Range</Label>
                        <div className="flex items-center justify-between gap-2 text-xs w-full">
                            <span>{displayMinSeverity}</span>
                            <hr className="bg-muted w-full"/>
                            <span>{displayMaxSeverity}</span>
                        </div>
                        <Slider
                            min={0}
                            max={1000}
                            step={1}
                            value={[displayMinSeverity, displayMaxSeverity]}
                            onValueChange={(value) => {
                                if (!Array.isArray(value) || value.length < 2)
                                    return;

                                updateMinSeverity(value[0]);
                                updateMaxSeverity(value[1]);
                            }}
                        />
                    </div>
                </TimeHeader>

                {/* Main Content */}
                <div className="flex w-full flex-1 min-h-0 gap-2">

                    {/* Event Selection */}
                    <Card className="card-glossy *:text-nowrap flex flex-col min-w-lg! w-lg! max-w-lg! min-h-0">
                        
                        <CardHeader className="w-full">
                            <CardTitle className="flex items-center gap-2 justify-between">
                                Event Selection
                                {renderDateRangeMonthly()}
                            </CardTitle>
                            <CardDescription>Select events to display in both trends charts.</CardDescription>
                        </CardHeader>

                        <CardContent className="flex flex-col gap-2 overflow-y-auto min-h-0 flex-1 relative">
                            {/* {
                                ALL_EVENT_NAMES.map((eventName) => {
                                    const checked = Boolean(eventChecked[eventName]);
                                    const id = `heat-map-event-${eventName.replace(/[^a-zA-Z0-9_-]+/g, "-")}`;

                                    return (
                                        <div key={eventName} className="flex items-center gap-2">
                                            <Checkbox
                                                id={id}
                                                checked={checked}
                                                onCheckedChange={() => handleEventToggle(eventName)}
                                            />
                                            <Label htmlFor={id} className="cursor-pointer text-sm ">
                                                {eventName}
                                            </Label>
                                        </div>
                                    );
                                })
                            } */}

                            {/* Event Checklist */}
                            {
                                ALL_EVENT_NAMES.map((eventName) => {

                                    const checked = (eventChecked[eventName] ?? false);
                                    return (
                                        <div key={eventName} className={`flex items-center gap-2`}>
                                            <Checkbox
                                                checked={checked}
                                                onCheckedChange={() => {
                                                    setEventChecked((prev) => ({
                                                        ...prev,
                                                        [eventName]: !prev[eventName],
                                                    }));
                                                }}
                                            />
                                            {
                                                // ANY Event (no tooltip)
                                                (eventName === EVENT_ANY)
                                                ?
                                                <Label className="font-normal">{eventName}</Label>

                                                // Other Events (with tooltip)
                                                : 
                                                <div className="text-sm flex gap-1">
                                                    {eventName}
                                                    {/* TODO: Add tooltip (maybe) */}
                                                </div>
                                            }
                                        </div>
                                    );
                                })
                            }

                        </CardContent>
                    </Card>

                    {/* Map */}
                    <Card className="min-h-0 flex flex-col card-glossy overflow-clip w-full">
                        <CardContent className="relative p-0 flex-1 min-h-0 overflow-hidden">

                            {/* Map Controls */}
                            <div className="absolute top-3 right-3 z-20 flex gap-2">

                                {/* Map Style Dropdown */}
                                {mapStyleDropdown}

                                {/* Grid/Heatmap Toggle */}
                                <div className="rounded-md border bg-background p-1.5 flex items-center gap-2 shadow">

                                    <button
                                        type="button"
                                        title="Heatmap"
                                        onClick={() => setShowGrid(false)}
                                        className={`mode-toggle-button ${!showGrid ? "bg-red-100 text-red-700" : "text-muted-foreground hover:bg-muted"}`}
                                    >
                                        <Flame size={16} />
                                    </button>
                                    <button
                                        type="button"
                                        title="Grid"
                                        onClick={() => setShowGrid(true)}
                                        className={`mode-toggle-button ${showGrid ? "bg-blue-100 text-blue-700" : "text-muted-foreground hover:bg-muted"}`}
                                    >
                                        <Grid2X2 size={16} />
                                    </button>
                                </div>
                            </div>

                            <div className="absolute top-3 left-3 z-20 w-75 rounded-lg border bg-background/95 p-3 shadow">
                                <div className="text-sm font-semibold border-b pb-2 mb-2">
                                    Events Found: {eventStatistics.totalEvents}
                                </div>

                                {
                                    eventStatistics.totalEvents > 0
                                    && (
                                        <div className="max-h-40 overflow-y-auto text-xs mb-3 pr-1 space-y-1">
                                            {
                                                Object.entries(eventStatistics.eventsByType)
                                                    .sort(([, countA], [, countB]) => countB - countA)
                                                    .map(([eventType, count]) => (
                                                        <div key={eventType} className="flex items-center justify-between border-b pb-1">
                                                            <span className="mr-2">{eventType}</span>
                                                            <span className="font-semibold text-blue-600">{count}</span>
                                                        </div>
                                                    ))
                                            }
                                        </div>
                                    )
                                }

                                <button
                                    type="button"
                                    className="text-xs font-medium text-muted-foreground hover:text-foreground transition"
                                    onClick={() => setNavigationTipsExpanded((current) => !current)}
                                >
                                    {navigationTipsExpanded ? "Hide" : "Show"} map controls
                                </button>

                                {
                                    navigationTipsExpanded
                                    && (
                                        <ul className="list-disc pl-5 mt-2 text-xs text-muted-foreground space-y-1">
                                            <li>Use {commandDragText} to select a map area.</li>
                                            <li>Toggle heatmap and grid views in the top-right corner.</li>
                                            <li>Zoom to level 15 or above to view clickable markers.</li>
                                            <li>Click up to two markers to compare distances.</li>
                                        </ul>
                                    )
                                }
                            </div>

                            <div ref={mapContainerRef} className="h-full w-full min-h-105" />

                            {
                                loading
                                && (
                                    <PanelAlert
                                        title="Loading Heat Map"
                                        description={["Fetching matching events and map points..."]}
                                    />
                                )
                            }

                            {
                                (!loading && !heatmapDataLoaded)
                                && (
                                    <PanelAlert
                                        isMap
                                        title="No Data Loaded"
                                        description={[
                                            "Select events, draw an area, then click Apply.",
                                            "Use the selected date range shown in the time header.",
                                        ]}
                                    />
                                )
                            }

                            <div className="absolute inset-0 pointer-events-none z-30">
                                {
                                    openPopups.map((popup) => {
                                        const isRecent = recentPopupId === popup.id;
                                        const isDragging = draggedPopupId === popup.id;
                                        const verticalSeparation = selectedPoints.length === 2
                                            ? Math.abs(selectedPoints[0].altitude - selectedPoints[1].altitude)
                                            : null;

                                        return (
                                            <div
                                                key={popup.id}
                                                className="absolute pointer-events-auto w-60 rounded-md border bg-background/95 shadow"
                                                style={{
                                                    left: popup.position.left,
                                                    top: popup.position.top,
                                                    zIndex: isRecent ? 60 : 50,
                                                }}
                                            >
                                                <div className="p-2 border-b flex items-center justify-between">
                                                    <button
                                                        type="button"
                                                        className={`rounded p-1 ${isDragging ? "cursor-grabbing" : "cursor-grab"}`}
                                                        onMouseDown={(event) => handlePopupMouseDown(event, popup.id)}
                                                        aria-label="Drag popup"
                                                    >
                                                        <Move size={14} />
                                                    </button>

                                                    <div className="text-xs font-semibold text-right ml-2 line-clamp-1">{popup.data.eventType}</div>

                                                    <button
                                                        type="button"
                                                        className="rounded p-1 hover:bg-muted"
                                                        onClick={() => closePopup(popup.id)}
                                                        aria-label="Close popup"
                                                    >
                                                        <X size={14} />
                                                    </button>
                                                </div>

                                                <div className="p-2 text-xs space-y-1 max-h-75 overflow-y-auto">
                                                    {
                                                        popup.data.eventTypes.length > 1
                                                        && <div className="italic text-muted-foreground">{popup.data.eventTypes.join(", ")}</div>
                                                    }

                                                    {
                                                        popup.data.eventSummaries.length > 1
                                                        && (
                                                            <>
                                                                <div className="rounded border bg-muted/40 p-2 space-y-1">
                                                                    <div><strong>Stacked Events At This Point:</strong> {popup.data.eventSummaries.length}</div>
                                                                    <div className="text-muted-foreground">This marker contains multiple co-located events.</div>
                                                                </div>
                                                                <Separator className="my-2" />
                                                                <div className="font-semibold">Events In This Marker</div>
                                                                {
                                                                    popup.data.eventSummaries.map((summary, index) => (
                                                                        <div
                                                                            key={`${popup.id}-summary-${summary.eventId}-${summary.flightId}-${index}`}
                                                                            className="rounded border p-2 space-y-0.5"
                                                                        >
                                                                            <div><strong>Type:</strong> {summary.eventType}</div>
                                                                            <div><strong>Event ID:</strong> {summary.eventId}</div>
                                                                            <div><strong>Time:</strong> {summary.time}</div>
                                                                            <div><strong>Flight:</strong> {summary.flightId} ({summary.flightAirframe || "N/A"})</div>
                                                                            {
                                                                                summary.otherFlightId
                                                                                && <div><strong>Other Flight:</strong> {summary.otherFlightId} ({summary.otherFlightAirframe || "N/A"})</div>
                                                                            }
                                                                            <div><strong>Severity:</strong> {summary.severity.toFixed(2)}</div>
                                                                        </div>
                                                                    ))
                                                                }
                                                            </>
                                                        )
                                                    }

                                                    <div className="text-muted-foreground">{popup.data.time}</div>
                                                    <Separator className="my-2" />
                                                    <div><strong>Event ID:</strong> {popup.data.eventId ?? "N/A"}</div>
                                                    <div><strong>Latitude:</strong> {popup.data.latitude.toFixed(5)}deg</div>
                                                    <div><strong>Longitude:</strong> {popup.data.longitude.toFixed(5)}deg</div>
                                                    <div><strong>Altitude (AGL):</strong> {popup.data.altitude.toFixed(0)} ft</div>
                                                    <Separator className="my-2" />
                                                    <div className="flex items-center gap-1"><Plane size={12} /><strong>Flight:</strong> {popup.data.flightId}</div>
                                                    <div><strong>Airframe:</strong> {popup.data.flightAirframe || "N/A"}</div>

                                                    {
                                                        popup.data.otherFlightId
                                                        && (
                                                            <>
                                                                <Separator className="my-2" />
                                                                <div><strong>Other Flight:</strong> {popup.data.otherFlightId}</div>
                                                                <div><strong>Other Airframe:</strong> {popup.data.otherFlightAirframe || "N/A"}</div>
                                                            </>
                                                        )
                                                    }

                                                    <Separator className="my-2" />
                                                    <div><strong>Severity:</strong> {popup.data.severity.toFixed(2)}</div>

                                                    {
                                                        popup.data.columnValues
                                                        && Object.keys(popup.data.columnValues).length > 0
                                                        && (
                                                            <>
                                                                <Separator className="my-2" />
                                                                <div className="font-semibold">Event Data</div>
                                                                {
                                                                    Object.entries(popup.data.columnValues).map(([column, value]) => {
                                                                        const displayValue = (typeof value === "number")
                                                                            ? value.toFixed(2)
                                                                            : (value ?? "N/A");
                                                                        return (
                                                                            <div key={`${popup.id}-${column}`}>
                                                                                <strong>{column}:</strong> {displayValue}
                                                                            </div>
                                                                        );
                                                                    })
                                                                }
                                                            </>
                                                        )
                                                    }

                                                    {
                                                        distances.lateral !== null
                                                        && distances.euclidean !== null
                                                        && selectedPoints.length === 2
                                                        && (
                                                            <>
                                                                <Separator className="my-2" />
                                                                <div><strong>Lateral Distance:</strong> {distances.lateral.toFixed(0)} ft</div>
                                                                <div><strong>Euclidean Distance:</strong> {distances.euclidean.toFixed(0)} ft</div>
                                                                <div><strong>Vertical Separation:</strong> {(verticalSeparation ?? 0).toFixed(0)} ft</div>
                                                            </>
                                                        )
                                                    }
                                                </div>
                                            </div>
                                        );
                                    })
                                }
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    );
}