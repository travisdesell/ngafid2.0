// ngafid-frontend/src/app/pages/protected/severities/severities.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import PanelAlert from "@/components/panel_alert";
import { ALL_AIRFRAMES_ID, ALL_AIRFRAMES_NAME, useAirframes } from "@/components/providers/airframes_provider";
import { getLogger } from "@/components/providers/logger";
import { useTags } from "@/components/providers/tags/tags_provider";
import { useTheme } from "@/components/providers/theme-provider";
import TimeHeader from "@/components/providers/time_header/time_header";
import { useTimeHeader } from "@/components/providers/time_header/time_header_provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartConfig, ChartContainer, ChartLegend, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { fetchJson } from "@/fetchJson";
import { CircleQuestionMark, Download } from "lucide-react";
import { useCallback, useEffect, useId, useMemo, useState } from "react";
import { CartesianGrid, Scatter, ScatterChart, XAxis, YAxis } from "recharts";

const log = getLogger("Severities", "black", "Page");

const EVENT_ANY = "ANY Event";
const TAG_ALL = "All Tags";
const GARMIN_FLIGHT_DISPLAY = "Garmin Flight Display";
const MARKER_SHAPES = ["circle", "square", "cross", "triangle", "diamond", "star", "wye"] as const;

type EventMetaDataItem = {
    name: string;
    value: number | string;
};

type EventCount = {
    id: number;
    severity: number;
    startTime: string;
    endTime: string;
    startLine?: number;
    endLine?: number;
    systemId: number | string;
    tail: string;
    eventDefinitionId: number;
    tagName: string;
    flightId: number | string;
    otherFlightId?: number | string;
};

type EventSeverityByAirframe = Record<string, EventCount[]>;
type EventSeverities = Record<string, EventSeverityByAirframe>;
type EventDescriptionResponse = Record<string, Record<string, string>>;

type SeverityPoint = {
    x: number;
    y: number;
    eventId: number;
    eventName: string;
    airframeName: string;
    flightId: string;
    otherFlightId?: string;
    systemId: string;
    tail: string;
    eventDefinitionId: number;
    tagName: string;
    severity: number;
    startTime: string;
    endTime: string;
    startLine?: number;
    endLine?: number;
};

type SeveritySeries = {
    seriesKey: string;
    eventName: string;
    airframeName: string;
    isAny: boolean;
    symbol: (typeof MARKER_SHAPES)[number];
    data: SeverityPoint[];
};

type LegendItem = {
    dataKey?: string;
    color?: string;
    value?: string;
};

const sanitizeKey = (value: string) => value.replace(/[^a-zA-Z0-9_]+/g, "_");

const csvEscape = (value: string | number | undefined | null) => {
    const text = String(value ?? "");
    if (!/[",\n]/.test(text))
        return text;
    return `"${text.replace(/"/g, '""')}"`;
};

const formatMonthTick = (value: number) => {
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime()))
        return "";
    return parsed.toLocaleDateString(undefined, { month: "short", year: "2-digit" });
};

const formatSeverityTick = (value: number) => {
    if (!Number.isFinite(value))
        return "";

    return Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 });
};

const toEpochMs = (value: string | number) => {
    if (typeof value === "number")
        return value;

    if (/^\d+$/.test(value))
        return value.length <= 10 ? Number(value) * 1000 : Number(value);

    const parsed = Date.parse(value);
    return Number.isNaN(parsed) ? undefined : parsed;
};

const normalizeEventMetaData = (value: unknown): EventMetaDataItem[] => {
    if (!Array.isArray(value))
        return [];

    const out: EventMetaDataItem[] = [];
    for (const row of value) {
        if (!row || typeof row !== "object")
            continue;

        const raw = row as Record<string, unknown>;
        const name = String(raw.name ?? "").trim();
        const rawValue = raw.value;

        if (!name)
            continue;

        if (typeof rawValue === "number" || typeof rawValue === "string") {
            out.push({ name, value: rawValue });
            continue;
        }

        out.push({ name, value: String(rawValue ?? "") });
    }

    return out;
};

const normalizeEventCount = (value: unknown): EventCount | null => {
    if (!value || typeof value !== "object")
        return null;

    const raw = value as Record<string, unknown>;
    const id = Number(raw.id);
    const severity = Number(raw.severity);
    const startTime = String(raw.startTime ?? "");
    const endTime = String(raw.endTime ?? "");

    if (!Number.isFinite(id) || !Number.isFinite(severity) || !startTime || !endTime)
        return null;

    return {
        id,
        severity,
        startTime,
        endTime,
        startLine: raw.startLine === undefined ? undefined : Number(raw.startLine),
        endLine: raw.endLine === undefined ? undefined : Number(raw.endLine),
        systemId: String(raw.systemId ?? ""),
        tail: String(raw.tail ?? ""),
        eventDefinitionId: Number(raw.eventDefinitionId ?? 0),
        tagName: String(raw.tagName ?? ""),
        flightId: String(raw.flightId ?? ""),
        otherFlightId: (raw.otherFlightId === undefined)
            ? undefined
            : String(raw.otherFlightId),
    };
};

const normalizeEventSeverityByAirframe = (value: unknown): EventSeverityByAirframe => {
    if (!value || typeof value !== "object")
        return {};

    const out: EventSeverityByAirframe = {};

    for (const [airframeNameRaw, countsRaw] of Object.entries(value as Record<string, unknown>)) {
        const airframeName = String(airframeNameRaw ?? "").trim();
        if (!airframeName || airframeName === GARMIN_FLIGHT_DISPLAY)
            continue;

        if (!Array.isArray(countsRaw))
            continue;

        const normalized = countsRaw
            .map((row) => normalizeEventCount(row))
            .filter((row): row is EventCount => row !== null);

        out[airframeName] = normalized;
    }

    return out;
};

const buildAnyEvent = (source: EventSeverities): EventSeverityByAirframe => {
    const out: EventSeverityByAirframe = {};

    for (const [eventName, byAirframe] of Object.entries(source)) {
        if (eventName === EVENT_ANY)
            continue;

        for (const [airframeName, counts] of Object.entries(byAirframe)) {
            if (airframeName === GARMIN_FLIGHT_DISPLAY)
                continue;

            if (!out[airframeName])
                out[airframeName] = [];

            out[airframeName] = [...out[airframeName], ...counts];
        }
    }

    return out;
};

const MarkerShapeIcon = ({ shape, color }: { shape: (typeof MARKER_SHAPES)[number]; color: string }) => {
    const fillStyle = { fill: color, stroke: "rgba(0, 0, 0, 0.55)", strokeWidth: 0.8 } as const;

    switch (shape) {
    case "circle":
        return <svg viewBox="0 0 16 16" className="h-3 w-3 shrink-0"><circle cx="8" cy="8" r="4.4" {...fillStyle} /></svg>;
    case "square":
        return <svg viewBox="0 0 16 16" className="h-3 w-3 shrink-0"><rect x="3.2" y="3.2" width="9.6" height="9.6" {...fillStyle} /></svg>;
    case "cross":
        return (
            <svg viewBox="0 0 16 16" className="h-3 w-3 shrink-0">
                <path d="M6.2 2.5h3.6v3.7h3.7v3.6H9.8v3.7H6.2V9.8H2.5V6.2h3.7z" {...fillStyle} />
            </svg>
        );
    case "triangle":
        return <svg viewBox="0 0 16 16" className="h-3 w-3 shrink-0"><polygon points="8,2.2 13.6,13.2 2.4,13.2" {...fillStyle} /></svg>;
    case "diamond":
        return <svg viewBox="0 0 16 16" className="h-3 w-3 shrink-0"><polygon points="8,1.8 14.2,8 8,14.2 1.8,8" {...fillStyle} /></svg>;
    case "star":
        return (
            <svg viewBox="0 0 16 16" className="h-3 w-3 shrink-0">
                <polygon points="8,1.8 9.9,6 14.4,6 10.8,8.8 12.2,13.1 8,10.5 3.8,13.1 5.2,8.8 1.6,6 6.1,6" {...fillStyle} />
            </svg>
        );
    case "wye":
        return (
            <svg viewBox="0 0 16 16" className="h-3 w-3 shrink-0">
                <path d="M8 8 4 3.2M8 8 12 3.2M8 8v5.2" fill="none" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
            </svg>
        );
    default:
        return <svg viewBox="0 0 16 16" className="h-3 w-3 shrink-0"><circle cx="8" cy="8" r="4.4" {...fillStyle} /></svg>;
    }
};

const renderSeveritiesLegendContent = (
    config: ChartConfig,
    symbolsBySeries: Record<string, (typeof MARKER_SHAPES)[number]>,
    payload?: LegendItem[],
) => {
    if (!payload?.length)
        return null;

    const visiblePayload = payload.filter((item, index) => {
        const key = String(item.dataKey ?? item.value ?? `series-${index}`);
        return !key.endsWith("__glow");
    });

    if (!visiblePayload.length)
        return null;

    return (
        <div className="flex flex-col items-start gap-2 pt-3 ml-8">
            {visiblePayload.map((item, index) => {
                const key = String(item.dataKey ?? item.value ?? `series-${index}`);
                const label = String(config[key]?.label ?? item.value ?? key);
                const symbol = symbolsBySeries[key] ?? "circle";
                const color = String(item.color ?? config[key]?.color ?? "currentColor");

                return (
                    <div key={`${key}-${index}`} className="flex items-center gap-1.5">
                        <MarkerShapeIcon shape={symbol} color={color} />
                        <span>{label}</span>
                    </div>
                );
            })}
        </div>
    );
};

export default function SeveritiesPage() {
    const glowFilterId = `severity-glow-${useId().replace(/:/g, "")}`;
    const { setModal } = useModal();
    const { useHighContrastCharts } = useTheme();
    const { fleetTags } = useTags();
    const { endpointStartDate, endpointEndDate, reapplyTrigger, renderDateRangeMonthly } = useTimeHeader();
    const { airframes, airframeIDSelected, setAirframeIDSelected, airframeNameSelected, setAirframeNameSelected } = useAirframes();

    const [hasApplied, setHasApplied] = useState(false);
    const [isCheckingAvailability, setIsCheckingAvailability] = useState(false);
    const [isFetchingEvents, setIsFetchingEvents] = useState(false);
    const [tagName, setTagName] = useState(TAG_ALL);
    const [glowMode, setGlowMode] = useState<"off" | "subtle" | "strong">("off");

    const [eventDescriptions, setEventDescriptions] = useState<Record<string, string>>({});
    const [eventChecked, setEventChecked] = useState<Record<string, boolean>>({ [EVENT_ANY]: false });
    const [eventsEmpty, setEventsEmpty] = useState<Record<string, boolean>>({ [EVENT_ANY]: false });
    const [eventCounts, setEventCounts] = useState<Record<string, number>>({});
    const [eventSeverities, setEventSeverities] = useState<EventSeverities>({});
    const [eventMetaDataById, setEventMetaDataById] = useState<Record<number, EventMetaDataItem[]>>({});

    const loading = isCheckingAvailability || isFetchingEvents;

    useEffect(() => {
        document.title = "NGAFID — Severities";
    }, []);

    const eventNames = useMemo(() => {
        const names = Object.keys(eventDescriptions).sort((a, b) => a.localeCompare(b));
        return [EVENT_ANY, ...names];
    }, [eventDescriptions]);

    const nonAnyEventNames = useMemo(
        () => eventNames.filter((name) => name !== EVENT_ANY),
        [eventNames],
    );

    useEffect(() => {
        setEventChecked((prev) => {
            const next: Record<string, boolean> = {};
            for (const eventName of eventNames)
                next[eventName] = prev[eventName] ?? false;
            return next;
        });

        setEventsEmpty((prev) => {
            const next: Record<string, boolean> = { [EVENT_ANY]: false };
            for (const eventName of nonAnyEventNames)
                next[eventName] = prev[eventName] ?? true;
            return next;
        });
    }, [eventNames, nonAnyEventNames]);

    const tagNames = useMemo(() => {
        const names = fleetTags
            .map((tag) => String(tag.name ?? "").trim())
            .filter((name) => name.length > 0 && name !== GARMIN_FLIGHT_DISPLAY)
            .sort((a, b) => a.localeCompare(b));

        return [TAG_ALL, ...names];
    }, [fleetTags]);

    useEffect(() => {
        if (!tagNames.includes(tagName))
            setTagName(TAG_ALL);
    }, [tagNames, tagName]);

    const selectableAirframes = useMemo(
        () => airframes.filter((airframe) => airframe.name !== GARMIN_FLIGHT_DISPLAY),
        [airframes],
    );

    const selectedAirframeNames = useMemo(() => {
        if (airframeIDSelected === ALL_AIRFRAMES_ID)
            return new Set<string>(selectableAirframes.map((af) => af.name));
        return new Set<string>([airframeNameSelected]);
    }, [airframeIDSelected, airframeNameSelected, selectableAirframes]);

    const fetchEventDescriptions = useCallback(async () => {
        const descriptions = await fetchJson.get<EventDescriptionResponse>("/api/event/definition/description").catch((error) => {
            setModal(ErrorModal, {
                title: "Error Fetching Event Descriptions",
                message: String(error),
            });
            return {} as EventDescriptionResponse;
        });

        const flattened: Record<string, string> = {};
        for (const [eventName, byAirframe] of Object.entries(descriptions ?? {})) {
            const preferred = byAirframe?.Any || byAirframe?.["Any Airframe"];
            const fallback = Object.values(byAirframe ?? {}).find((d) => (d ?? "").trim().length > 0);
            flattened[eventName] = preferred ?? fallback ?? "No description available.";
        }

        setEventDescriptions(flattened);
    }, [setModal]);

    useEffect(() => {
        fetchEventDescriptions();
    }, [fetchEventDescriptions]);

    const fetchAvailability = useCallback(async () => {
        if (nonAnyEventNames.length === 0)
            return;

        setIsCheckingAvailability(true);

        const params = new URLSearchParams({
            startDate: endpointStartDate,
            endDate: endpointEndDate,
            eventNames: JSON.stringify(nonAnyEventNames),
            tagName,
        });

        const response = await fetchJson.get<Record<string, unknown>>("/api/event/severities/available", { params }).catch((error) => {
            setModal(ErrorModal, {
                title: "Error Checking Event Availability",
                message: String(error),
            });
            return null;
        });

        if (!response) {
            setIsCheckingAvailability(false);
            return;
        }

        const nextCounts: Record<string, number> = {};
        for (const eventName of nonAnyEventNames) {
            const count = Number(response[eventName] ?? 0);
            nextCounts[eventName] = Number.isFinite(count) && count > 0 ? count : 0;
        }

        setEventCounts(nextCounts);
        setEventsEmpty((prev) => {
            const next: Record<string, boolean> = { ...prev, [EVENT_ANY]: false };
            for (const eventName of nonAnyEventNames)
                next[eventName] = (nextCounts[eventName] ?? 0) === 0;
            return next;
        });

        const clearedChecked: Record<string, boolean> = { [EVENT_ANY]: false };
        for (const eventName of nonAnyEventNames)
            clearedChecked[eventName] = false;
        setEventChecked(clearedChecked);

        setEventSeverities({});
        setEventMetaDataById({});
        setIsCheckingAvailability(false);
    }, [endpointEndDate, endpointStartDate, nonAnyEventNames, setModal, tagName]);

    useEffect(() => {
        if (!hasApplied)
            return;
        fetchAvailability();
    }, [fetchAvailability, hasApplied, reapplyTrigger]);

    const fetchEventSeveritiesByName = useCallback(async (eventName: string) => {
        setIsFetchingEvents(true);

        const params = new URLSearchParams({
            startDate: endpointStartDate,
            endDate: endpointEndDate,
            tagName,
        });

        const response = await fetchJson.get<unknown>(`/api/event/severities/${encodeURIComponent(eventName)}`, { params }).catch((error) => {
            setModal(ErrorModal, {
                title: `Error Fetching ${eventName} Severities`,
                message: String(error),
            });
            return null;
        });

        if (!response) {
            setIsFetchingEvents(false);
            return;
        }

        const normalized = normalizeEventSeverityByAirframe(response);
        const hasAnyData = Object.values(normalized).some((counts) => counts.length > 0);

        setEventsEmpty((prev) => ({ ...prev, [eventName]: !hasAnyData }));
        setEventSeverities((prev) => ({
            ...prev,
            [eventName]: hasAnyData ? normalized : {},
        }));

        setIsFetchingEvents(false);
    }, [endpointEndDate, endpointStartDate, setModal, tagName]);

    const fetchAllEventSeverities = useCallback(async () => {
        if (nonAnyEventNames.length === 0)
            return;

        setIsFetchingEvents(true);

        const params = new URLSearchParams({
            startDate: endpointStartDate,
            endDate: endpointEndDate,
            eventNames: JSON.stringify(nonAnyEventNames),
            tagName,
        });

        const response = await fetchJson.get<unknown>("/api/event/severities", { params }).catch((error) => {
            setModal(ErrorModal, {
                title: "Error Fetching Event Severities",
                message: String(error),
            });
            return null;
        });

        if (!response || typeof response !== "object") {
            setIsFetchingEvents(false);
            return;
        }

        const next: EventSeverities = {};
        const nextEmpties: Record<string, boolean> = {};

        for (const [eventName, byAirframe] of Object.entries(response as Record<string, unknown>)) {
            const normalized = normalizeEventSeverityByAirframe(byAirframe);
            const hasAnyData = Object.values(normalized).some((counts) => counts.length > 0);

            next[eventName] = hasAnyData ? normalized : {};
            nextEmpties[eventName] = !hasAnyData;
        }

        setEventSeverities((prev) => ({ ...prev, ...next }));
        setEventsEmpty((prev) => ({ ...prev, ...nextEmpties }));
        setIsFetchingEvents(false);
    }, [endpointEndDate, endpointStartDate, nonAnyEventNames, setModal, tagName]);

    const toggleEvent = async (eventName: string) => {
        const nextChecked = !(eventChecked[eventName] ?? false);
        setEventChecked((prev) => ({ ...prev, [eventName]: nextChecked }));

        if (!nextChecked)
            return;

        if (eventName === EVENT_ANY) {
            await fetchAllEventSeverities();
            return;
        }

        if (!eventSeverities[eventName])
            await fetchEventSeveritiesByName(eventName);
    };

    const chartModel = useMemo(() => {
        const chartConfig: ChartConfig = {};
        const series: SeveritySeries[] = [];
        const anyEventData = buildAnyEvent(eventSeverities);
        const eventNamesByColor = nonAnyEventNames;

        const colorForEvent = (eventName: string) => {
            if (eventName === EVENT_ANY)
                return "var(--muted-foreground)";

            if (useHighContrastCharts) {
                const palettePrefix = "--chart-hc";
                const index = Math.max(0, eventNamesByColor.indexOf(eventName));
                return `var(${palettePrefix}-${(index % 12) + 1})`;
            }

            const index = Math.max(0, eventNamesByColor.indexOf(eventName));
            const denominator = Math.max(1, eventNamesByColor.length - 1);
            const hue = Math.round((index / denominator) * 360);

            // Non-high-contrast mode uses full-hue mapping across event types.
            return `hsl(${hue} 88% 54%)`;
        };

        const airframeSymbolIndex = new Map<string, number>();
        let seriesCounter = 0;

        for (const eventName of eventNames) {
            if (!eventChecked[eventName])
                continue;

            const isAny = eventName === EVENT_ANY;
            const byAirframe = isAny ? anyEventData : (eventSeverities[eventName] ?? {});

            for (const [airframeName, counts] of Object.entries(byAirframe)) {
                if (!selectedAirframeNames.has(airframeName))
                    continue;

                if (counts.length === 0)
                    continue;

                if (!airframeSymbolIndex.has(airframeName))
                    airframeSymbolIndex.set(airframeName, airframeSymbolIndex.size);

                const symbolIndex = airframeSymbolIndex.get(airframeName) ?? 0;
                const symbol = MARKER_SHAPES[symbolIndex % MARKER_SHAPES.length]!;

                const data: SeverityPoint[] = [];
                for (const count of counts) {
                    const x = toEpochMs(count.startTime);
                    if (x === undefined)
                        continue;

                    data.push({
                        x,
                        y: count.severity,
                        eventId: count.id,
                        eventName,
                        airframeName,
                        flightId: String(count.flightId),
                        otherFlightId: count.otherFlightId === undefined ? undefined : String(count.otherFlightId),
                        systemId: String(count.systemId),
                        tail: String(count.tail ?? ""),
                        eventDefinitionId: Number(count.eventDefinitionId ?? 0),
                        tagName: String(count.tagName ?? ""),
                        severity: Number(count.severity ?? 0),
                        startTime: String(count.startTime),
                        endTime: String(count.endTime),
                        startLine: count.startLine,
                        endLine: count.endLine,
                    });
                }

                if (data.length === 0)
                    continue;

                const seriesKey = `${sanitizeKey(`severity_${eventName}_${airframeName}`)}_${seriesCounter}`;
                seriesCounter += 1;

                chartConfig[seriesKey] = {
                    label: `${eventName} - ${airframeName}`,
                    color: colorForEvent(eventName),
                };

                series.push({
                    seriesKey,
                    eventName,
                    airframeName,
                    isAny,
                    symbol,
                    data,
                });
            }
        }

        series.sort((a, b) => {
            if (a.isAny !== b.isAny)
                return a.isAny ? -1 : 1;
            return a.eventName.localeCompare(b.eventName) || a.airframeName.localeCompare(b.airframeName);
        });

        const points = series.flatMap((entry) => entry.data);
        const xValues = points.map((point) => point.x);
        const yValues = points.map((point) => point.y);

        const xMin = xValues.length > 0 ? Math.min(...xValues) : Date.now();
        const xMax = xValues.length > 0 ? Math.max(...xValues) : (xMin + 1000);
        const yMin = yValues.length > 0 ? Math.min(...yValues) : 0;
        const yMax = yValues.length > 0 ? Math.max(...yValues) : 1;

        const xPadding = (xMax === xMin) ? 60_000 : 0;
        const yPadding = (yMax === yMin) ? Math.max(1, Math.abs(yMax) * 0.05) : 0;

        return {
            chartConfig,
            series,
            symbolsBySeries: Object.fromEntries(series.map((entry) => [entry.seriesKey, entry.symbol])) as Record<string, (typeof MARKER_SHAPES)[number]>,
            hasData: series.length > 0,
            xDomain: [(xMin - xPadding), (xMax + xPadding)] as [number, number],
            yDomain: [(yMin - yPadding), (yMax + yPadding)] as [number, number],
        };
    }, [eventChecked, eventNames, eventSeverities, nonAnyEventNames, selectedAirframeNames, useHighContrastCharts]);

    const exportCSV = async () => {
        const selectedEvents = eventNames.filter((name) => eventChecked[name]);
        if (selectedEvents.length === 0)
            return;

        const anyEventData = buildAnyEvent(eventSeverities);
        const rows: Array<{ eventName: string; airframeName: string; count: EventCount }> = [];

        for (const eventName of selectedEvents) {
            const byAirframe = eventName === EVENT_ANY ? anyEventData : (eventSeverities[eventName] ?? {});

            for (const [airframeName, counts] of Object.entries(byAirframe)) {
                if (!selectedAirframeNames.has(airframeName))
                    continue;

                for (const count of counts)
                    rows.push({ eventName, airframeName, count });
            }
        }

        if (rows.length === 0)
            return;

        const metadataCache: Record<number, EventMetaDataItem[]> = { ...eventMetaDataById };
        const idsMissingMetadata = [...new Set(rows.map((row) => row.count.id).filter((id) => !(id in metadataCache)))];

        if (idsMissingMetadata.length > 0) {
            await Promise.all(idsMissingMetadata.map(async (eventId) => {
                const metadata = await fetchJson.get<unknown>(`/api/event/${eventId}/meta`).catch(() => null);
                metadataCache[eventId] = normalizeEventMetaData(metadata);
            }));

            setEventMetaDataById((prev) => ({ ...prev, ...metadataCache }));
        }

        const metadataNames = [...new Set(
            rows.flatMap((row) => (metadataCache[row.count.id] ?? []).map((meta) => meta.name)),
        )].sort((a, b) => a.localeCompare(b));

        const header = [
            "Event Name",
            "Airframe",
            "Flight ID",
            "Start Time",
            "End Time",
            "Start Line",
            "End Line",
            "Severity",
            ...metadataNames,
        ];

        const lines: string[] = [header.map(csvEscape).join(",")];

        for (const row of rows) {
            const metadataByName: Record<string, string> = {};
            for (const metadata of metadataCache[row.count.id] ?? []) {
                const formattedValue = typeof metadata.value === "number"
                    ? (Math.round(metadata.value * 100) / 100).toFixed(2)
                    : String(metadata.value ?? "");
                metadataByName[metadata.name] = formattedValue;
            }

            const values = [
                row.eventName,
                row.airframeName,
                row.count.flightId,
                row.count.startTime,
                row.count.endTime,
                row.count.startLine ?? "",
                row.count.endLine ?? "",
                row.count.severity.toFixed(2),
                ...metadataNames.map((name) => metadataByName[name] ?? ""),
            ];

            lines.push(values.map(csvEscape).join(","));
        }

        const anchor = document.createElement("a");
        anchor.setAttribute("href", `data:text/plain;charset=utf-8,${encodeURIComponent(lines.join("\n"))}`);
        anchor.setAttribute("download", "event_severities.csv");
        anchor.style.display = "none";
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
    };

    const handleChartClick = (state: unknown) => {
        const chartState = state as { activePayload?: Array<{ payload?: SeverityPoint }> };
        const point = chartState?.activePayload?.[0]?.payload;

        if (!point)
            return;

        const primaryFlightId = String(point.flightId);
        const secondaryFlightId = (point.eventDefinitionId === -1) && point.otherFlightId && (String(point.otherFlightId).length > 0)
                ? String(point.otherFlightId)
                : null;

        const params = new URLSearchParams();
        if (secondaryFlightId)
            params.append("flight_id", secondaryFlightId);
        params.append("flight_id", primaryFlightId);

        window.open(`/protected/flight?${params.toString()}`, "_blank", "noopener");
    };

    const anyEventDisabled = !hasApplied || nonAnyEventNames.every((name) => eventsEmpty[name] ?? true);
    const hasSelectedData = chartModel.hasData;
    const enableGlowLayer = !useHighContrastCharts && glowMode !== "off";
    const totalVisiblePoints = chartModel.series.reduce((sum, series) => sum + series.data.length, 0);

    // Adaptive glow tuning keeps dense datasets readable while preserving hotspot emphasis.
    const glowDensityScale = totalVisiblePoints > 5000
        ? 0.55
        : totalVisiblePoints > 2000
            ? 0.72
            : totalVisiblePoints > 800
                ? 0.86
                : 1;

    const glowIntensityScale = ({
        subtle: 0.9,
        strong: 1.4,
        off: 0,
    } as const)[glowMode] * glowDensityScale;

    // Radius is intentionally much larger for clear hotspot envelopes.
    const glowRadiusScale = ({
        subtle: 3.2,
        strong: 6.4,
        off: 0,
    } as const)[glowMode] * glowDensityScale;

    const glowBlurStdDev = 4.8 * glowIntensityScale * 3.0;
    const glowRadiusPrimary = 8 * glowRadiusScale;
    const glowRadiusAny = 6.4 * glowRadiusScale;
    const glowOpacityPrimary = 0.16 * glowIntensityScale;
    const glowOpacityAny = 0.1 * glowIntensityScale;
    const glowAlphaBoost = 1.12;

    return (
        <div className="page-container">
            <div className="page-content gap-4 overflow-hidden">

                <TimeHeader
                    onApply={() => {
                        log("Applying Severities filters...");
                        setHasApplied(true);
                    }}
                    dependencies={[tagName]}
                    requireManualInitialApply
                >
                    <div className="flex items-end gap-3">
                        <Button variant="outline" onClick={exportCSV} disabled={!hasApplied}>
                            <Download />
                            Export CSV
                        </Button>
                    </div>
                </TimeHeader>

                <div className="flex w-full flex-1 min-h-0 gap-2">
                    <Card className="card-glossy *:text-nowrap flex flex-col min-w-lg min-h-0">
                        <CardHeader className="w-full">
                            <CardTitle className="flex items-center gap-2 justify-between">
                                Event Selection
                                {renderDateRangeMonthly()}
                            </CardTitle>
                            <CardDescription>Select events to display in the severity chart.</CardDescription>
                        </CardHeader>

                        <CardContent className="flex flex-col gap-2 overflow-y-auto min-h-0 flex-1 relative">
                            {/* {
                                (!hasApplied)
                                &&
                                <PanelAlert title="Apply Filters" description="Choose date range and click Apply to load event availability." />
                            } */}

                            {
                                (loading && !hasSelectedData)
                                &&
                                <PanelAlert title="Loading Severities..." description="Fetching event severity data." />
                            }

                            {
                                eventNames.map((eventName) => {
                                    const checked = eventChecked[eventName] ?? false;
                                    const disabled = eventName === EVENT_ANY
                                        ? anyEventDisabled
                                        : (!hasApplied || (eventsEmpty[eventName] ?? true));
                                    const description = eventDescriptions[eventName] ?? "No description available.";

                                    return (
                                        <div key={eventName} className={`flex items-center gap-2 ${disabled ? "opacity-50" : ""}`}>
                                            <Checkbox
                                                checked={checked}
                                                disabled={disabled}
                                                onCheckedChange={() => {
                                                    void toggleEvent(eventName);
                                                }}
                                            />

                                            {
                                                eventName === EVENT_ANY
                                                    ? <Label className="font-normal">{eventName}</Label>
                                                    : (
                                                        <div className="text-sm flex gap-1 items-center">
                                                            <span>{eventName}{eventCounts[eventName] ? ` (${eventCounts[eventName]})` : ""}</span>
                                                            <Tooltip disableHoverableContent>
                                                                <TooltipTrigger asChild>
                                                                    <Label className="font-normal cursor-help inline-flex items-center gap-1">
                                                                        <CircleQuestionMark className="size-3.5" />
                                                                    </Label>
                                                                </TooltipTrigger>
                                                                <TooltipContent className="max-w-sm">{description}</TooltipContent>
                                                            </Tooltip>
                                                        </div>
                                                    )
                                            }
                                        </div>
                                    );
                                })
                            }
                        </CardContent>

                        <CardFooter className="flex flex-col justify-start items-start gap-4 mt-auto">
                            <Separator />

                            <div className="flex gap-2">

                                {/* Airframe Type Selection */}
                                <div className="flex flex-col gap-2">
                                    <Label>Airframe Type</Label>
                                    <Select
                                        value={airframeIDSelected.toString()}
                                        onValueChange={(value) => {
                                            const id = parseInt(value, 10);
                                            setAirframeIDSelected(id);

                                            const selected = selectableAirframes.find((airframe) => airframe.id === id);
                                            setAirframeNameSelected(selected?.name ?? ALL_AIRFRAMES_NAME);
                                        }}
                                    >
                                        <Button asChild variant="outline">
                                            <SelectTrigger className="w-55">
                                                <SelectValue placeholder="Select Airframe" />
                                            </SelectTrigger>
                                        </Button>
                                        <SelectContent>
                                            {selectableAirframes.map((airframe) => (
                                                <SelectItem key={airframe.id} value={airframe.id.toString()}>
                                                    {airframe.name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>

                                {/* Flight Tag Selection */}
                                <div className="flex flex-col gap-2">
                                    <Label className="px-1">Flight Tag</Label>
                                    <Select value={tagName} onValueChange={setTagName}>
                                        <Button asChild variant="outline">
                                            <SelectTrigger className="w-60">
                                                <SelectValue placeholder="Select Tag" />
                                            </SelectTrigger>
                                        </Button>
                                        <SelectContent>
                                            {tagNames.map((name) => (
                                                <SelectItem key={name} value={name}>
                                                    {name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>

                            </div>
                        </CardFooter>
                    </Card>

                    <Card className="card-glossy w-full min-h-0 flex-1 flex flex-col overflow-hidden">
                        <CardHeader>
                            <CardTitle>Severity of Events</CardTitle>
                            <CardDescription>Event severity over time for the selected event and airframe filters.</CardDescription>
                        </CardHeader>
                        <CardContent className="min-h-0 flex-1 w-full">
                            {
                                (!hasApplied)
                                    ? <PanelAlert title="No Data Available!" description="Apply a date range to load available event severities." />
                                    : (
                                        !hasSelectedData
                                            ? <PanelAlert title="No Data Available!" description="Select one or more available events to render severity points." />
                                            : (
                                                <div className="relative h-full w-full min-h-0">

                                                    {/* Glow Mode Selector (DEPRECATING THIS, TURNED OUT TO BE A STUPID IDEA) */}
                                                    {/* <div className="absolute right-3 bottom-3 z-10">
                                                        <Select value={glowMode} onValueChange={(value) => setGlowMode(value as "off" | "subtle" | "strong")}>
                                                            <Button asChild variant="outline">
                                                                <SelectTrigger className="w-36 bg-card/90 backdrop-blur-sm">
                                                                    <SelectValue placeholder="Glow" />
                                                                </SelectTrigger>
                                                            </Button>
                                                            <SelectContent>
                                                                <SelectItem value="off">Glow: Off</SelectItem>
                                                                <SelectItem value="subtle">Glow: Subtle</SelectItem>
                                                                <SelectItem value="strong">Glow: Strong</SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                    </div> */}

                                                    <ChartContainer config={chartModel.chartConfig} className="h-full w-full min-h-0 aspect-auto!">
                                                        <ScatterChart
                                                            margin={{ left: 8, right: 8, top: 8, bottom: 8 }}
                                                            accessibilityLayer
                                                            onClick={handleChartClick}
                                                        >
                                                        {
                                                            enableGlowLayer
                                                                ? (
                                                                    <defs>
                                                                        <filter id={glowFilterId} x="-60%" y="-60%" width="220%" height="220%">
                                                                            <feGaussianBlur stdDeviation={glowBlurStdDev} result="blur" />
                                                                            <feColorMatrix in="blur" type="matrix" values={`1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 ${glowAlphaBoost} 0`} />
                                                                        </filter>
                                                                    </defs>
                                                                )
                                                                : null
                                                        }
                                                        <CartesianGrid vertical={false} />
                                                        <XAxis
                                                            type="number"
                                                            dataKey="x"
                                                            domain={chartModel.xDomain}
                                                            tickLine={false}
                                                            axisLine={false}
                                                            tickMargin={8}
                                                            tickFormatter={(value) => formatMonthTick(Number(value))}
                                                        />
                                                        <YAxis
                                                            type="number"
                                                            dataKey="y"
                                                            domain={chartModel.yDomain}
                                                            tickLine={false}
                                                            axisLine={false}
                                                            tickMargin={8}
                                                            tickFormatter={(value) => formatSeverityTick(Number(value))}
                                                        />
                                                        <ChartTooltip
                                                            content={(
                                                                <ChartTooltipContent
                                                                    hideLabel
                                                                    formatter={(_, name, item) => {
                                                                        const seriesName = String(item?.name ?? "");

                                                                        // Scatter payload includes both x and y rows; only render once for y.
                                                                        if (item?.dataKey !== "y" || seriesName.endsWith("__glow"))
                                                                            return null;

                                                                        const point = item.payload as SeverityPoint;
                                                                        const seriesMatch = chartModel.series.find(
                                                                            (series) => series.eventName === point.eventName && series.airframeName === point.airframeName,
                                                                        );

                                                                        const symbol = seriesMatch?.symbol ?? "circle";
                                                                        const symbolColor = seriesMatch
                                                                            ? String(chartModel.chartConfig[seriesMatch.seriesKey]?.color ?? item?.color ?? "currentColor")
                                                                            : String(item?.color ?? "currentColor");

                                                                        const otherFlightID = (point.otherFlightId?.length ?? 0) > 0 && (point.otherFlightId !== "null")
                                                                            ? point.otherFlightId
                                                                            : null;

                                                                        return (
                                                                            <div className="grid w-full gap-2 text-xs text-muted-foreground">
                                                                                <div className="flex items-center gap-1.5 font-mono font-medium tabular-nums text-foreground">
                                                                                    <MarkerShapeIcon shape={symbol} color={symbolColor} />
                                                                                    <span>{`Severity: ${point.severity.toFixed(2)}`}</span>
                                                                                </div>
                                                                                <hr />
                                                                                <div>{`Flight ID: ${point.flightId} ${otherFlightID ? `(Other ID: ${otherFlightID})` : ""}`}</div>
                                                                                <div>{`System ID: ${point.systemId}`}</div>
                                                                                <div>{`Tail: ${point.tail?.length > 0 ? point.tail : "N/A"}`}</div>
                                                                                <hr />
                                                                                <div>{`Tag: ${point.tagName || "N/A"}`}</div>
                                                                                <hr />
                                                                                <div>{`Event Start: ${point.startTime}`}</div>
                                                                                <div>{`Event End: ${point.endTime}`}</div>
                                                                            </div>
                                                                        );
                                                                    }}
                                                                />
                                                            )}
                                                        />
                                                        <ChartLegend
                                                            className="flex-col items-start ml-8"
                                                            content={(props) => renderSeveritiesLegendContent(chartModel.chartConfig, chartModel.symbolsBySeries, props.payload as LegendItem[])}
                                                            layout="vertical"
                                                            verticalAlign="top"
                                                            align="right"
                                                        />
                                                        {
                                                            enableGlowLayer
                                                                ? chartModel.series.map((series) => (
                                                                    <Scatter
                                                                        key={`${series.seriesKey}-glow`}
                                                                        data={series.data}
                                                                        name={`${series.seriesKey}__glow`}
                                                                        legendType="none"
                                                                        fill={`var(--color-${series.seriesKey})`}
                                                                        stroke="none"
                                                                        isAnimationActive={false}
                                                                        shape={(props: any) => (
                                                                            <circle
                                                                                cx={props.cx}
                                                                                cy={props.cy}
                                                                                r={series.isAny ? glowRadiusAny : glowRadiusPrimary}
                                                                                fill={`var(--color-${series.seriesKey})`}
                                                                                fillOpacity={series.isAny ? glowOpacityAny : glowOpacityPrimary}
                                                                                filter={`url(#${glowFilterId})`}
                                                                                style={{ mixBlendMode: "screen", pointerEvents: "none" }}
                                                                            />
                                                                        )}
                                                                    />
                                                                ))
                                                                : null
                                                        }
                                                        {
                                                            chartModel.series.map((series) => (
                                                                <Scatter
                                                                    key={series.seriesKey}
                                                                    data={series.data}
                                                                    name={series.seriesKey}
                                                                    shape={series.symbol as never}
                                                                    fill={`var(--color-${series.seriesKey})`}
                                                                    stroke={`rgb(0.0,0.0,0.0)`}
                                                                    fillOpacity={series.isAny ? 0 : 0.92}
                                                                    strokeOpacity={series.isAny ? 0.9 : 0.5}
                                                                    strokeWidth={series.isAny ? 1.8 : 1.2}
                                                                />
                                                            ))
                                                        }
                                                        </ScatterChart>
                                                    </ChartContainer>
                                                </div>
                                            )
                                    )
                            }
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    );
}