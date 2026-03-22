// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_chart.tsx
"use client";

import { ChartsListModal } from "@/components/modals/charts_list_modal/charts_list_modal";
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import SuccessModal from "@/components/modals/success_modal";
import Ping from "@/components/pings/ping";
import { getLogger } from "@/components/providers/logger";
import { useTheme } from "@/components/providers/theme-provider";
import { useCartesianZoomPan } from "@/components/providers/useCartesianZoomPan";
import { Accordion, AccordionContent, AccordionTrigger } from "@/components/ui/accordion";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
    ChartConfig,
    ChartContainer,
    ChartTooltip,
    ChartTooltipContent
} from "@/components/ui/chart";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { fetchJson } from "@/fetchJson";
import { useEffectPrev } from "@/lib/useEffectPrev";
import { useFlightsChart } from "@/pages/protected/flights/_flights_context_chart";
import FlightsPanelChartLabelCard from "@/pages/protected/flights/_panels/_chart/flights_panel_chart_label_card";
import { Flight } from "@/pages/protected/flights/types";
import { TraceSeries } from "@/pages/protected/flights/types_charts";
import { AccordionItem } from "@radix-ui/react-accordion";
import { ArrowBigUp, ArrowLeftToLine, Expand, Info, List, Mouse, MousePointerClick, NavigationOff, SquareActivity, SquareChevronUp } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState } from "react";
import { CartesianGrid, Line, LineChart, ReferenceArea, ReferenceLine, ResponsiveContainer, XAxis, YAxis } from "recharts";

const log = getLogger("FlightsPanelChart", "blue", "Component");

type ActiveSeries = {
    seriesKey: string; //<-- unique key: f{flightId}_{sanitizedParamName}
    flight: Flight;
    flightIndex: number;
    paramName: string;
    paramIndex: number;
    series: TraceSeries;
};

const MAX_POINTS_PER_SERIES = 1000;

const LABELING_MARKER_COLORS = [
    "#e6194b", "#3cb44b", "#4363d8", "#f58231", "#911eb4", "#42d4f4", "#f032e6", "#bfef45",
    "#fabed4", "#469990", "#dcbeff", "#9a6324", "#fffac8", "#800000", "#aaffc3", "#808000", "#ffd8b1", "#000075",
];

const getLabelMarkerColor = (sectionIndex: number): string => (
    LABELING_MARKER_COLORS[Math.abs(sectionIndex) % LABELING_MARKER_COLORS.length] ?? "#e6194b"
);

const buildNonHighContrastColor = (flightSlotIndex: number, paramIndex: number): string => {

    /*
        Uses a set of predefined color palettes
        for up to 4 flights with 5 distinct
        colors per flight.

        Defaults to pink if out of range.
    */

    const COLORS = [

        // First Flight
        [
            "rgba(120, 60, 255)",
            "rgba(0, 60, 230)",
            "rgba(30, 120, 215)",
            "rgba(60, 180, 200)",
            "rgba(0, 255, 255)",
        ],

        // Second Flight
        [
            "rgba(255, 196, 0)",
            "rgba(255, 196, 128)",
            "rgba(230, 128, 0)",
            "rgba(215, 64, 0)",
            "rgba(180, 64, 32)",
        ],

        // Third Flight
        [
            "rgba(0, 255, 0)",
            "rgba(150, 255, 50)",
            "rgba(200, 255, 150)",
            "rgba(0, 128, 0)",
            "rgba(0, 64, 0)",
        ],

        // Fourth Flight
        [
            "rgba(196, 0, 0)",
            "rgba(255, 0, 0)",
            "rgba(255, 0, 128)",
            "rgba(255, 128, 128)",
            "rgba(255, 128, 255)",
        ],

    ];

    const COLORS_PER_FLIGHT = COLORS[0]!.length;
    const COLOR_DEFAULT = "rgba(255, 0, 255)";

    const colorOut =
        (COLORS[flightSlotIndex] && COLORS[flightSlotIndex]![paramIndex % COLORS_PER_FLIGHT]) ||
        COLOR_DEFAULT;

    return colorOut;

};

const buildHighContrastColor = (seriesIndex: number): string => {

    const MAX_HC_COLORS = 12;
    const INDEX_OFFSET = 1; //<-- --chart-hc-1..N

    const index = (seriesIndex % MAX_HC_COLORS) + INDEX_OFFSET;
    const colorOut = `var(--chart-hc-${index})`;

    return colorOut;

};

const makeSeriesKeyForChart = (flightId: number, paramName: string): string =>
    `f${flightId}_${paramName.replace(/[^a-zA-Z0-9]+/g, "_")}`;

type ChartModel = {
    loading: boolean;
    hasData: boolean;
    data: Array<Record<string, number | string>>;
    config: ChartConfig;
    seriesKeys: string[];
    seriesFlightIDByKey: Record<string, number>;
    eventOverlays: Array<{
        id: string;
        flightId: number;
        eventName: string;
        severity: number;
        x1: number;
        x2: number;
        color: string;
    }>;
    labelOverlays: Array<{
        id: string;
        flightId: number;
        x1: number;
        x2: number;
        color: string;
    }>;
    xMin: number;
    xMax: number;
    yMin: number;
    yMax: number;
};

const buildEmptyChartModel = (loading: boolean): ChartModel => ({
    loading,
    hasData: false,
    data: [],
    config: {} as ChartConfig,
    seriesKeys: [],
    seriesFlightIDByKey: {},
    eventOverlays: [],
    labelOverlays: [],
    xMin: 0,
    xMax: 0,
    yMin: 0,
    yMax: 0,
});

type ApiFlightLabel = {
    id: number;
    startIndex: number;
    endIndex: number;
    startTime: number;
    endTime: number;
    startValue: number;
    endValue: number;
    labelText?: string;
    parameterNames?: string[];
    visibleOnChart?: boolean;
};

type LabelSection = {
    id: number | null;
    startIndex: number;
    endIndex: number;
    startTime: number;
    endTime: number;
    startValue: number;
    endValue: number;
    labelText: string;
    parameterNames: string[];
    visibleOnChart?: boolean;
};

type FleetLabelDefinition = {
    id: number;
    labelText: string;
    displayOrder: number;
};

const mapApiLabelToSection = (entry: ApiFlightLabel): LabelSection => ({
    id: Number.isFinite(entry.id) ? entry.id : null,
    startIndex: Number.isFinite(entry.startIndex) ? entry.startIndex : 0,
    endIndex: Number.isFinite(entry.endIndex) ? entry.endIndex : 0,
    startTime: Number(entry.startTime),
    endTime: Number(entry.endTime),
    startValue: Number(entry.startValue),
    endValue: Number(entry.endValue),
    labelText: String(entry.labelText ?? ""),
    parameterNames: Array.isArray(entry.parameterNames) ? entry.parameterNames : [],
    visibleOnChart: entry.visibleOnChart,
});

const toRelativeSeconds = (rawTime: number, flightStartMs: number): number => {
    if (!Number.isFinite(rawTime))
        return 0;

    // Absolute milliseconds timestamp
    if (rawTime >= 1e12)
        return (rawTime - flightStartMs) / 1000;

    // Absolute seconds timestamp
    if (rawTime >= 1e9)
        return rawTime - (flightStartMs / 1000);

    return rawTime;
};

const normalizeTraceTimestampToRelativeSeconds = (rawTime: number, flightStartMs: number): number => {
    if (!Number.isFinite(rawTime) || !Number.isFinite(flightStartMs))
        return Number.NaN;

    // Milliseconds since epoch
    if (rawTime >= 1e12)
        return (rawTime - flightStartMs) / 1000;

    // Seconds since epoch
    if (rawTime >= 1e9)
        return rawTime - (flightStartMs / 1000);

    // Already relative seconds
    return rawTime;
};

const toAbsoluteSecondsFromTimestampCandidate = (timestampCandidate: number, flightStartMs: number): number => {
    if (!Number.isFinite(timestampCandidate) || !Number.isFinite(flightStartMs))
        return Number.NaN;

    // Absolute milliseconds timestamp
    if (timestampCandidate >= 1e12)
        return timestampCandidate / 1000;

    // Absolute seconds timestamp
    if (timestampCandidate >= 1e9)
        return timestampCandidate;

    // Relative seconds timestamp
    return (flightStartMs / 1000) + timestampCandidate;
};

const sortPair = (a: number, b: number): [number, number] => (
    a <= b ? [a, b] : [b, a]
);

const decimateSeries = (series: TraceSeries) => {

    const timestamps = series.timestamps ?? [];
    const values = series.values ?? [];
    const timestampsCount = timestamps.length;

    // No timestamps, return empty array
    if (timestampsCount === 0)
        return [] as { t: number; v: number }[];

    // Already beneath the maximum, return as-is
    if (timestampsCount <= MAX_POINTS_PER_SERIES) {

        const out: { t: number; v: number }[] = new Array(timestampsCount);
        for (let i = 0; i < timestampsCount; i++) {
            out[i] = { t: timestamps[i]!, v: values[i]! };
        }

        return out;

    }

    const step = Math.ceil(timestampsCount / MAX_POINTS_PER_SERIES);
    const out: { t: number; v: number }[] = [];

    for (let i = 0; i < timestampsCount; i += step) {

        const t = timestamps[i];
        const v = values[i];

        // Got invalid data, skip
        if (t === undefined || v === undefined)
            continue;

        out.push({ t, v });

    }

    // Always attempt to include the last point
    const lastIdx = (timestampsCount - 1);
    const lastT = timestamps[lastIdx];
    const lastV = values[lastIdx];

    if (lastT !== undefined && lastV !== undefined && (out.length === 0 || out[out.length - 1]!.t !== lastT))
        out.push({ t: lastT, v: lastV });

    return out;

};


type AxisMeta = { ticks: number[]; dayBoundaryTimes: number[]; domainSpanMinutes: number; };

type InteractiveChartProps = {
    chartModel: ChartModel;
    activeLabelingCaptureFlightId: number | null;
    useAlignedStartTimes: boolean;
    theme: string | undefined;
    shiftHeld: boolean;
    ctrlHeld: boolean;
    altHeld: boolean;
    onChartXClick?: (xValue: number) => void;
    pendingStartMarker?: { x: number; color: string } | null;
};

type InteractiveChartHandle = { resetView: () => void; };


const InteractiveChart = forwardRef<InteractiveChartHandle, InteractiveChartProps>(function InteractiveChart(
    {
        chartModel,
        activeLabelingCaptureFlightId,
        useAlignedStartTimes,
        theme,
        shiftHeld,
        ctrlHeld,
        altHeld,
        onChartXClick,
        pendingStartMarker,
    },
    ref,
) {

    const extractChartXValue = (state: any): number | null => {

        const fromActiveLabel = Number(state?.activeLabel);
        if (Number.isFinite(fromActiveLabel))
            return fromActiveLabel;

        const fromActivePayload = Number(state?.activePayload?.[0]?.payload?.time);
        if (Number.isFinite(fromActivePayload))
            return fromActivePayload;

        const tooltipIndexRaw = Number(state?.activeTooltipIndex);
        if (Number.isFinite(tooltipIndexRaw)) {
            const tooltipIndex = Math.floor(tooltipIndexRaw);
            const point = chartModel.data[tooltipIndex] as Record<string, number | string> | undefined;
            const fromTooltipIndex = Number(point?.time ?? Number.NaN);

            if (Number.isFinite(fromTooltipIndex))
                return fromTooltipIndex;
        }

        return null;

    };

    // Early out if no data
    if (!chartModel.hasData) {
        return null;
    }

    const {
        chartRef,
        xDomainOverride,
        yDomainOverride,
        selection,
        isInteracting,
        handleChartMouseDown,
        handleChartMouseOperationCancel,
        handleWheel,
        resetView,
    } = useCartesianZoomPan({
        hasData: chartModel.hasData,
        baseXDomain: [chartModel.xMin, chartModel.xMax],
        baseYDomain: [chartModel.yMin, chartModel.yMax],
        // Reset interactions whenever alignment mode or base domains change
        resetDeps: [
            useAlignedStartTimes,
            chartModel.xMin,
            chartModel.xMax,
            chartModel.yMin,
            chartModel.yMax,
        ],
        zoomSpeed: 0.005,
        minSpanFactor: 0.001,
        animationDurationMs: 140,
    });

    useImperativeHandle(
        ref,
        () => ({ resetView }),
        [resetView],
    );

    const hasDomain =
        Number.isFinite(chartModel.xMin) &&
        Number.isFinite(chartModel.xMax) &&
        Number.isFinite(chartModel.yMin) &&
        Number.isFinite(chartModel.yMax);

    const activeXDomain: [number, number] | null = hasDomain
        ? [
            xDomainOverride?.[0] ?? chartModel.xMin,
            xDomainOverride?.[1] ?? chartModel.xMax,
        ]
        : null;

    const activeYDomain: [number, number] | null = hasDomain
        ? [
            yDomainOverride?.[0] ?? chartModel.yMin,
            yDomainOverride?.[1] ?? chartModel.yMax,
        ]
        : null;

        
        const axisMeta: AxisMeta = useMemo(() => {
        if (!activeXDomain) {
            return { ticks: [], dayBoundaryTimes: [], domainSpanMinutes: 0 };
        }

        const [domainMin, domainMax] = activeXDomain;
        let ticks: number[] = [];
        let dayBoundaryTimes: number[] = [];
        let domainSpanMinutes = 0;

        if (useAlignedStartTimes) {

            domainSpanMinutes = (domainMax - domainMin) / 60; // seconds -> minutes

            if (domainMax === domainMin) {
                ticks.push(domainMin);
            } else {
                const MAX_TICKS = 8;
                const tickCount = MAX_TICKS;
                const span = Math.max(domainMax - domainMin, 1);
                const step = span / (tickCount - 1);

                for (let i = 0; i < tickCount; i++) {
                    ticks.push(domainMin + step * i);
                }
            }

        } else {

            domainSpanMinutes = (domainMax - domainMin) / 60_000; // ms -> minutes

            if (domainMax === domainMin) {

                ticks.push(domainMin);
                dayBoundaryTimes.push(domainMin);

            } else {

                const MAX_TICKS = 8;
                const tickCount = MAX_TICKS;
                const span = Math.max(domainMax - domainMin, 1);
                const step = span / (tickCount - 1);

                let prevDayKey: string | null = null;

                for (let i = 0; i < tickCount; i++) {

                    const t = Math.round(domainMin + step * i);
                    ticks.push(t);

                    const date = new Date(t);
                    const key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;

                    if (key !== prevDayKey) {
                        dayBoundaryTimes.push(t);
                        prevDayKey = key;
                    }

                }

            }

        }

        const uniqueTicks = Array.from(new Set(ticks)).sort((a, b) => a - b);
        const uniqueDayBoundaries = Array.from(new Set(dayBoundaryTimes)).sort((a, b) => a - b);

        log("Updated axis meta", {
            domainMin,
            domainMax,
            tickCount: uniqueTicks.length,
            dayBoundaryCount: uniqueDayBoundaries.length,
            domainSpanMinutes,
        });

        return { ticks: uniqueTicks, dayBoundaryTimes: uniqueDayBoundaries, domainSpanMinutes };

    }, [activeXDomain?.[0], activeXDomain?.[1], useAlignedStartTimes]);

    /*
        Custom tick renderer for the time axis (x-axis)
    */
    const TimeAxisTick = (props: any) => {

        const { x, y, payload } = props;
        const value = payload.value as number;

        const showSeconds = axisMeta.domainSpanMinutes <= 10;

        if (useAlignedStartTimes) {

            const totalSeconds = value;
            const hours = Math.floor(totalSeconds / 3600);
            const minutes = Math.floor((totalSeconds % 3600) / 60);
            const seconds = Math.floor(totalSeconds % 60);

            const pad = (n: number) => n.toString().padStart(2, "0");

            const timeLabel = (showSeconds)
                ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
                : `${pad(hours)}:${pad(minutes)}`;

            return (
                <g transform={`translate(${x},${y})`}>
                    <text
                        dy={0}
                        textAnchor="middle"
                        fill="currentColor"
                        className="text-[10px]"
                    >
                        {timeLabel}
                    </text>
                </g>
            );
        }

        const date = new Date(value);
        const boundarySet = new Set(axisMeta.dayBoundaryTimes);
        const isDayBoundary = boundarySet.has(value);

        const timeLabel = date.toLocaleTimeString(undefined, {
            hour: "2-digit",
            minute: "2-digit",
            second: showSeconds ? "2-digit" : undefined,
            hour12: false,
        });

        const dateLabel = date.toLocaleDateString(undefined, {
            month: "short",
            day: "2-digit",
        });

        return (
            <g transform={`translate(${x},${y})`}>
                <text
                    dy={isDayBoundary ? -2 : 0}
                    textAnchor="middle"
                    fill="currentColor"
                    className="text-[10px]"
                >
                    {timeLabel}
                </text>
                {
                    (isDayBoundary)
                    && (
                        <text
                            dy={10}
                            textAnchor="middle"
                            fill="currentColor"
                            className="text-[9px] opacity-70"
                        >
                            {dateLabel}
                        </text>
                    )
                }
            </g>
        );
    };

    const timeLabelFormatter = (label: any) => {

        const numericLabel = Number(label);
        const overlayTolerance = (useAlignedStartTimes ? 0.001 : 1); // ~1ms for absolute mode

        const activeEventNames = Array.from(
            new Set(
                chartModel.eventOverlays
                    .filter((overlay) => (
                        numericLabel >= (overlay.x1 - overlayTolerance)
                        && numericLabel <= (overlay.x2 + overlayTolerance)
                    ))
                    .map((overlay) => overlay.eventName),
            ),
        ).sort((a, b) => a.localeCompare(b));

        const activeEventsSummary = (() => {
            if (activeEventNames.length === 0)
                return null;

            const maxNames = 3;
            const shown = activeEventNames.slice(0, maxNames).join(", ");
            const remaining = activeEventNames.length - maxNames;

            return remaining > 0
                ? `${shown} (+${remaining} more)`
                : shown;
        })();

        let timeLabelOut: string;

        if (useAlignedStartTimes) {

            const SEC_PER_HOUR = 3600;
            const SEC_PER_MIN = 60;
            const DIGITS_PER_FIELD = 2;

            const totalSeconds = numericLabel;
            const hours = Math.floor(totalSeconds / SEC_PER_HOUR);
            const minutes = Math.floor((totalSeconds % SEC_PER_HOUR) / SEC_PER_MIN);
            const seconds = Math.floor(totalSeconds % SEC_PER_MIN);
            const pad = (n: number) => n.toString().padStart(DIGITS_PER_FIELD, "0");

            const showSeconds = axisMeta.domainSpanMinutes <= 10;

            const timePart = showSeconds
                ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
                : `${pad(hours)}:${pad(minutes)}`;

            timeLabelOut = `Elapsed ${timePart}`;

        } else {

            const date = new Date(numericLabel);
            timeLabelOut = date.toLocaleTimeString(undefined, {
                day: "2-digit",
                month: "short",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
                hour12: false,
            });

        }

        if (!activeEventsSummary)
            return timeLabelOut;

        return (
            <div>{timeLabelOut}</div>
        );

    };

    const handleContextMenu = (e: React.MouseEvent) => {
        e.preventDefault(); // <-- Suppress the browser's context menu
        // handleChartMouseOperationCancel(e.nativeEvent as MouseEvent); // <-- Cancel any ongoing chart interaction
    };

    const cursorClass:string = (() => {

        // Default -> Default
        let cursorOut = "cursor-default!";

        // Shift Held...
        if (shiftHeld) {

            // Interacting -> Grabbing
            if (isInteracting)
                return "cursor-grabbing!";

            // Otherwise -> Grab
            else 
                return "cursor-grab!";

        }

        return cursorOut;

    })();

    return (
        <ResponsiveContainer width="100%" height="100%" className="h-full flex p-4 select-none">
            <ChartContainer
                config={chartModel.config}
                className="w-full h-full my-auto"
                onWheel={handleWheel}
                onMouseDown={(e) => handleChartMouseOperationCancel(e as unknown as MouseEvent)}
                onContextMenu={handleContextMenu}
            >
                <LineChart
                    id="flights-panel-chart-linechart"
                    ref={chartRef}
                    accessibilityLayer
                    data={chartModel.data}
                    margin={{
                        left: 12,
                        right: 12,
                    }}
                    onMouseDown={(state: any) => {
                        const mouseButton = Number(state?.nativeEvent?.button);
                        const altHeldFromEvent = !!state?.nativeEvent?.altKey;
                        const altCaptureActive = mouseButton === 0 && (altHeldFromEvent || altHeld);

                        // Alt+LMB is reserved for label range capture, so skip zoom/pan handling.
                        if (altCaptureActive) {
                            log("Alt label capture mousedown", {
                                altHeldFromEvent,
                                altHeldFromState: altHeld,
                                activeLabel: state?.activeLabel,
                                activeTooltipIndex: state?.activeTooltipIndex,
                            });

                            return;
                        }

                        handleChartMouseDown(state);
                    }}
                    onClick={(state: any) => {
                        const altHeldFromEvent = !!state?.nativeEvent?.altKey;
                        const altCaptureActive = (altHeldFromEvent || altHeld);

                        if (!altCaptureActive)
                            return;

                        const activePayload = Array.isArray(state?.activePayload) ? state.activePayload : [];
                        const hasActiveFlightPayload = (
                            activeLabelingCaptureFlightId !== null
                            && activePayload.some((payloadItem: any) => {
                                const dataKey = String(payloadItem?.dataKey ?? "");
                                const payloadFlightId = chartModel.seriesFlightIDByKey[dataKey];
                                const payloadValue = Number(payloadItem?.value);

                                return payloadFlightId === activeLabelingCaptureFlightId && Number.isFinite(payloadValue);
                            })
                        );

                        if (!hasActiveFlightPayload) {
                            log.warn("Ignoring Alt label capture click outside active flight series", {
                                activeLabelingCaptureFlightId,
                                activePayloadKeys: activePayload.map((payloadItem: any) => String(payloadItem?.dataKey ?? "")),
                            });
                            return;
                        }

                        const xValue = extractChartXValue(state);

                        log("Alt label capture click", {
                            altHeldFromEvent,
                            altHeldFromState: altHeld,
                            activeLabel: state?.activeLabel,
                            activeTooltipIndex: state?.activeTooltipIndex,
                            resolvedX: xValue,
                        });

                        if (xValue === null) {
                            log.warn("Alt label capture click had no resolvable x-value", {
                                activeLabel: state?.activeLabel,
                                activeTooltipIndex: state?.activeTooltipIndex,
                            });
                            return;
                        }

                        onChartXClick?.(xValue);
                    }}
                    className={`${cursorClass}`}
                >
                    <CartesianGrid />
                    <YAxis
                        type="number"
                        domain={
                            activeYDomain
                                ? [activeYDomain[0], activeYDomain[1]]
                                : ["auto", "auto"]
                        }
                        allowDataOverflow
                        tickLine={false}
                        axisLine={false}
                        tickMargin={8}
                        tick={{ fontSize: 10 }}
                        tickFormatter={(value) => {

                            if (typeof value === "number") {
                                return value.toFixed(1);
                            }
                            return String(value);
                        }}
                    />
                    <XAxis
                        dataKey="time"
                        type="number"
                        scale={useAlignedStartTimes ? "linear" : "time"}
                        domain={
                            activeXDomain
                                ? [activeXDomain[0], activeXDomain[1]]
                                : ["dataMin", "dataMax"]
                        }
                        allowDataOverflow
                        ticks={axisMeta.ticks}
                        tickLine={false}
                        axisLine={false}
                        tickMargin={8}
                        tick={TimeAxisTick}
                    />
                    <ChartTooltip
                        cursor={false}
                        content={
                            <ChartTooltipContent
                                className={isInteracting ? "opacity-0" : ""}
                                labelFormatter={timeLabelFormatter}
                                formatter={(value, name, item: any, index, payload) => {

                                    const row = item?.payload as Record<string, number | string> | undefined;
                                    const time = Number(row?.time ?? Number.NaN);
                                    const seriesKey = String(item?.dataKey ?? "");
                                    const flightId = chartModel.seriesFlightIDByKey[seriesKey];
                                    const displayName = String(chartModel.config[seriesKey]?.label ?? name ?? seriesKey);
                                    const seriesColor = String(item?.color ?? chartModel.config[seriesKey]?.color ?? "currentColor");

                                    const rowValue = Number(value ?? 0);
                                    const formattedValue = Number.isFinite(rowValue)
                                        ? rowValue.toLocaleString(undefined, { maximumFractionDigits: 2 })
                                        : String(value ?? "");

                                    const overlayTolerance = (useAlignedStartTimes ? 0.001 : 1);

                                    const activeEventsForFlight = (
                                        Number.isFinite(time) && Number.isFinite(flightId)
                                    )
                                        ? chartModel.eventOverlays
                                            .filter((overlay) => (
                                                overlay.flightId === flightId
                                                && time >= (overlay.x1 - overlayTolerance)
                                                && time <= (overlay.x2 + overlayTolerance)
                                            ))
                                        : [];

                                    const firstRowForFlight = (() => {
                                        if (!Number.isFinite(flightId))
                                            return true;

                                        const seenFlightIDs = new Set<number>();

                                        for (let i = 0; i < payload.length; i++) {

                                            const payloadItem = payload[i] as any;
                                            const payloadSeriesKey = String(payloadItem?.dataKey ?? "");
                                            const payloadFlightID = chartModel.seriesFlightIDByKey[payloadSeriesKey];

                                            if (Number.isFinite(payloadFlightID) && !seenFlightIDs.has(payloadFlightID)) {
                                                seenFlightIDs.add(payloadFlightID);

                                                if (payloadFlightID === flightId)
                                                    return i === index;
                                            }

                                        }

                                        return true;
                                    })();

                                    return (
                                        <div className="grid w-full gap-1">
                                            <div className="flex items-center justify-between gap-2">
                                                <div className="flex items-center gap-2">
                                                    <span
                                                        className="inline-block h-2 w-2 rounded-[2px]"
                                                        style={{ backgroundColor: seriesColor }}
                                                    />
                                                    <span className="text-muted-foreground">{displayName}</span>
                                                </div>
                                                <span className="text-foreground font-mono font-medium tabular-nums">{formattedValue}</span>
                                            </div>
                                            {
                                                firstRowForFlight && activeEventsForFlight.length > 0
                                                    ? (
                                                        <div className="ml-3 grid gap-0.5 text-[10px] text-muted-foreground/90">
                                                            {
                                                                activeEventsForFlight.map((overlay) => (
                                                                    <div key={overlay.id} className="flex items-center gap-1.5">
                                                                        <span
                                                                            className="inline-block h-1.5 w-1.5 rounded-full"
                                                                            style={{ backgroundColor: overlay.color }}
                                                                        />
                                                                        <span>{`${overlay.eventName} (${overlay.severity.toFixed(2)})`}</span>
                                                                    </div>
                                                                ))
                                                            }
                                                        </div>
                                                    )
                                                    : null
                                            }
                                        </div>
                                    );
                                }}
                            />
                        }
                    />
                    {
                        chartModel.eventOverlays.map((overlay) => (
                            <ReferenceArea
                                key={overlay.id}
                                x1={overlay.x1}
                                x2={overlay.x2}
                                y1={activeYDomain ? activeYDomain[0] : chartModel.yMin}
                                y2={activeYDomain ? activeYDomain[1] : chartModel.yMax}
                                ifOverflow="hidden"
                                stroke="none"
                                fill={overlay.color}
                                fillOpacity={0.22}
                            />
                        ))
                    }
                    {
                        chartModel.labelOverlays.map((overlay) => (
                            <ReferenceArea
                                key={overlay.id}
                                x1={overlay.x1}
                                x2={overlay.x2}
                                y1={activeYDomain ? activeYDomain[0] : chartModel.yMin}
                                y2={activeYDomain ? activeYDomain[1] : chartModel.yMax}
                                ifOverflow="hidden"
                                stroke={overlay.color}
                                strokeOpacity={0.7}
                                fill={overlay.color}
                                fillOpacity={0.12}
                            />
                        ))
                    }
                    {
                        pendingStartMarker && Number.isFinite(pendingStartMarker.x)
                            ? (
                                <ReferenceLine
                                    x={pendingStartMarker.x}
                                    stroke={pendingStartMarker.color}
                                    strokeWidth={2}
                                    strokeDasharray="6 6"
                                    ifOverflow="hidden"
                                />
                            )
                            : null
                    }
                    {
                        chartModel.seriesKeys.map((key) => (
                            <Line
                                key={key}
                                dataKey={key}
                                type="linear"
                                stroke={`var(--color-${key})`}
                                strokeWidth={2}
                                dot={false}
                                isAnimationActive={false}
                            />
                        ))
                    }
                    {
                        selection &&
                        selection.x1 !== null &&
                        selection.x2 !== null &&
                        selection.y1 !== null &&
                        selection.y2 !== null && (
                            <ReferenceArea
                                x1={selection.x1}
                                x2={selection.x2}
                                y1={selection.y1}
                                y2={selection.y2}
                                stroke={theme === "dark" ? "rgba(255,255,255,0.9)" : "rgba(0,0,0,0.9)"}
                                fill={theme === "dark" ? "rgba(255,255,255,0.2)" : "rgba(0,0,0,0.1)"}
                                fillOpacity={0.3}
                            />
                        )
                    }
                </LineChart>
            </ChartContainer>
        </ResponsiveContainer>
    );

});


/* ============================================================================
   FlightsPanelChart (parent)
   - Builds chartModel and all non-interaction UI.
   - InteractiveChart handles all zoom/pan state so this component
     no longer re-renders on every mouse move.
============================================================================ */

export function FlightsPanelChart() {

    const {
        chartFlights,
        setChartFlights,
        chartSelection,
        eventSelection,
        chartData,
        ensureSeries,
        toggleUniversalParam,
        togglePerFlightParam,
        labelingEnabledFlightIds,
        selectedLabelingParamByFlight,
        pendingLabelStartXByFlight,
        labelSectionsByFlight,
        setLabelingEnabledForFlight,
        setSelectedLabelingParam,
        setPendingLabelStartX,
        setFlightLabelSections,
    } = useFlightsChart();

    const { useHighContrastCharts, theme } = useTheme();
    const { setModal } = useModal();

    const [gotChartFlightAdded, setGotChartFlightAdded] = useState(false);
    const [useAlignedStartTimes, setUseAlignedStartTimes] = useState(false);
    const [activeLabelingCaptureFlightId, setActiveLabelingCaptureFlightId] = useState<number | null>(null);
    const [labelingCardPositionByFlight, setLabelingCardPositionByFlight] = useState<Record<number, { left: number; top: number }>>({});
    const [labelDefinitions, setLabelDefinitions] = useState<FleetLabelDefinition[]>([]);
    const labelingEnabledFlightIdsPrevRef = useRef<number[]>([]);

    const interactiveChartRef = useRef<InteractiveChartHandle | null>(null);

    useEffectPrev(chartFlights, (prevChartFlights) => {

        // Had no flights before, now have some, show the ping
        if (prevChartFlights && chartFlights.length > prevChartFlights.length)
            setGotChartFlightAdded(true);

    });

    // Map flightId to selected parameter names (union of universal and per-flight parameters)
    const selectedParamsByFlight = useMemo(() => {

        const result: Record<number, string[]> = {};
        const universal = chartSelection.universalParams;
        const perFlight = chartSelection.perFlightParams;

        for (const flight of chartFlights) {

            const names = new Set<string>();

            universal.forEach((name) => names.add(name));

            const perSet = perFlight[flight.id];
            if (perSet)
                perSet.forEach((name) => names.add(name));

            if (names.size > 0)
                result[flight.id] = Array.from(names);

        }

        return result;

    }, [chartFlights, chartSelection]);

    // Map flightId to selected event names (union of universal and per-flight events)
    const selectedEventsByFlight = useMemo(() => {

        const result: Record<number, Set<string>> = {};
        const universal = eventSelection.universalEvents;
        const perFlight = eventSelection.perFlightEvents;

        for (const flight of chartFlights) {

            const names = new Set<string>();

            universal.forEach((name) => names.add(name));

            const perSet = perFlight[flight.id];
            if (perSet)
                perSet.forEach((name) => names.add(name));

            if (names.size > 0)
                result[flight.id] = names;

        }

        return result;

    }, [chartFlights, eventSelection]);

    const hasAnySelectedParams = useMemo(
        () => Object.values(selectedParamsByFlight).some((list) => list.length > 0),
        [selectedParamsByFlight],
    );

    // Precompute flight start times once per chartFlights change
    const flightStartMsById = useMemo(() => {

        const map = new Map<number, number>();

        for (const f of chartFlights) {

            const ms = new Date(f.startDateTime).getTime();
            if (!Number.isNaN(ms))
                map.set(f.id, ms);

        }

        return map;

    }, [chartFlights]);

    const flightEndMsById = useMemo(() => {

        const map = new Map<number, number>();

        for (const f of chartFlights) {

            const ms = new Date(f.endDateTime).getTime();
            if (!Number.isNaN(ms))
                map.set(f.id, ms);

        }

        return map;

    }, [chartFlights]);

    const labelingEnabledFlightIdSet = useMemo(
        () => new Set(labelingEnabledFlightIds),
        [labelingEnabledFlightIds],
    );

    const labelingEnabledFlights = useMemo(
        () => chartFlights.filter((flight) => labelingEnabledFlightIdSet.has(flight.id)),
        [chartFlights, labelingEnabledFlightIdSet],
    );

    useEffect(() => {

        setLabelingCardPositionByFlight((prev) => {
            const next = { ...prev };

            labelingEnabledFlights.forEach((flight, index) => {
                if (!next[flight.id]) {
                    next[flight.id] = {
                        left: 8 + (index * 28),
                        top: 8 + (index * 28),
                    };
                }
            });

            return next;
        });

    }, [labelingEnabledFlights]);

    const loadLabelsForFlight = useCallback(async (flightId: number) => {
        try {
            const result = await fetchJson.get<ApiFlightLabel[]>(`/api/flight/${flightId}/labels`);
            const mapped = (result ?? []).map(mapApiLabelToSection);
            setFlightLabelSections(flightId, mapped);
        } catch (error: any) {
            setModal(ErrorModal, {
                title: "Error Loading Labels",
                message: `Failed to load labels for flight ${flightId}.`,
                code: error?.toString?.() ?? String(error),
            });
        }
    }, [setFlightLabelSections, setModal]);

    const loadLabelDefinitions = useCallback(async () => {
        try {
            const result = await fetchJson.get<FleetLabelDefinition[]>("/api/fleet/labels");
            setLabelDefinitions(Array.isArray(result) ? result : []);
        } catch (error: any) {
            setModal(ErrorModal, {
                title: "Error Loading Label Definitions",
                message: "Failed to load fleet label definitions.",
                code: error?.toString?.() ?? String(error),
            });
        }
    }, [setModal]);

    useEffect(() => {

        labelingEnabledFlightIds.forEach((flightId) => {
            void loadLabelsForFlight(flightId);
        });

    }, [labelingEnabledFlightIds, loadLabelsForFlight]);

    useEffect(() => {
        if (labelingEnabledFlightIds.length === 0)
            return;

        void loadLabelDefinitions();
    }, [labelingEnabledFlightIds, loadLabelDefinitions]);

    useEffect(() => {
        const prevEnabled = labelingEnabledFlightIdsPrevRef.current;

        const newlyEnabled = labelingEnabledFlightIds.filter((flightId) => !prevEnabled.includes(flightId));
        if (newlyEnabled.length > 0) {
            setActiveLabelingCaptureFlightId(newlyEnabled[newlyEnabled.length - 1] ?? null);
            labelingEnabledFlightIdsPrevRef.current = labelingEnabledFlightIds;
            return;
        }

        setActiveLabelingCaptureFlightId((current) => {
            if (current !== null && labelingEnabledFlightIdSet.has(current))
                return current;

            if (labelingEnabledFlightIds.length === 0)
                return null;

            return labelingEnabledFlightIds[labelingEnabledFlightIds.length - 1] ?? null;
        });

        labelingEnabledFlightIdsPrevRef.current = labelingEnabledFlightIds;
    }, [labelingEnabledFlightIds, labelingEnabledFlightIdSet]);

    useEffect(() => {

        labelingEnabledFlightIds.forEach((flightId) => {
            const selectedParam = selectedLabelingParamByFlight[flightId] ?? "";
            const activeParams = selectedParamsByFlight[flightId] ?? [];

            if (activeParams.length === 0) {
                log("No active chart parameters for labeling-enabled flight", { flightId });
                return;
            }

            // Keep labeling param aligned with currently active chart parameters.
            if (!activeParams.includes(selectedParam)) {
                const nextParam = activeParams[0]!;
                log("Updated labeling parameter to first active chart parameter", {
                    flightId,
                    selectedParam,
                    nextParam,
                    activeParams,
                });
                setSelectedLabelingParam(flightId, nextParam);
                setPendingLabelStartX(flightId, null);
            }
        });

    }, [
        labelingEnabledFlightIds,
        selectedLabelingParamByFlight,
        selectedParamsByFlight,
        setSelectedLabelingParam,
        setPendingLabelStartX,
    ]);

    const handleLabelingParameterChange = (flightId: number, paramName: string) => {
        setSelectedLabelingParam(flightId, paramName);
        setPendingLabelStartX(flightId, null);
    };

    const handleToggleRangeCapture = (flightId: number) => {
        setActiveLabelingCaptureFlightId((prev) => {
            const next = (prev === flightId) ? null : flightId;

            if (next === null)
                setPendingLabelStartX(flightId, null);

            return next;
        });
    };

    const handleCloseLabeling = (flightId: number) => {
        setPendingLabelStartX(flightId, null);

        if (activeLabelingCaptureFlightId === flightId)
            setActiveLabelingCaptureFlightId(null);

        setLabelingEnabledForFlight(flightId, false);
    };

    const setActiveLabelingFlightFromCard = useCallback((flightId: number) => {
        setActiveLabelingCaptureFlightId((prev) => (prev === flightId ? prev : flightId));
    }, []);

    const handleImportCsv = (flightId: number, file: File) => {

        void (async () => {
            const formData = new FormData();
            formData.append("file", file);

            try {
                await fetchJson.post(`/api/flight/${flightId}/labels/import`, formData);
                await loadLabelsForFlight(flightId);
                setModal(SuccessModal, {
                    title: "Labels Imported",
                    message: `Successfully imported labels for flight ${flightId}.`,
                });
            } catch (error: any) {
                setModal(ErrorModal, {
                    title: "Import Failed",
                    message: `Could not import labels for flight ${flightId}.`,
                    code: error?.toString?.() ?? String(error),
                });
            }
        })();

    };

    const handleRemoveLabelSection = (flightId: number, sectionIndex: number) => {

        const currentSections = labelSectionsByFlight[flightId] ?? [];
        const targetSection = currentSections[sectionIndex];

        if (!targetSection)
            return;

        const nextSections = currentSections.filter((_, i) => i !== sectionIndex);
        setFlightLabelSections(flightId, nextSections);

        if (targetSection.id === null)
            return;

        const sectionId = targetSection.id;

        void (async () => {
            try {
                await fetchJson.delete(`/api/flight/${flightId}/labels/${encodeURIComponent(sectionId)}`);
                await loadLabelsForFlight(flightId);
            } catch (error: any) {
                await loadLabelsForFlight(flightId);
                setModal(ErrorModal, {
                    title: "Error Removing Label Section",
                    message: `Could not remove label section for flight ${flightId}.`,
                    code: error?.toString?.() ?? String(error),
                });
            }
        })();

    };

    const handleUpdateSectionLabel = (flightId: number, sectionIndex: number, labelText: string) => {

        const currentSections = labelSectionsByFlight[flightId] ?? [];
        const targetSection = currentSections[sectionIndex];
        if (!targetSection)
            return;

        const nextSections = currentSections.map((section, i) => (
            i === sectionIndex
                ? { ...section, labelText }
                : section
        ));
        setFlightLabelSections(flightId, nextSections);

        if (targetSection.id === null)
            return;

        const sectionId = targetSection.id;

        void (async () => {
            try {
                await fetchJson.put(`/api/flight/${flightId}/labels/${encodeURIComponent(sectionId)}`, {
                    labelText,
                    visibleOnChart: targetSection.visibleOnChart !== false,
                });
            } catch (error: any) {
                await loadLabelsForFlight(flightId);
                setModal(ErrorModal, {
                    title: "Error Updating Label",
                    message: `Could not update label for flight ${flightId}.`,
                    code: error?.toString?.() ?? String(error),
                });
            }
        })();

    };

    const handleToggleSectionVisibility = (flightId: number, sectionIndex: number, visibleOnChart: boolean) => {

        const currentSections = labelSectionsByFlight[flightId] ?? [];
        const targetSection = currentSections[sectionIndex];
        if (!targetSection)
            return;

        const nextSections = currentSections.map((section, i) => (
            i === sectionIndex
                ? { ...section, visibleOnChart }
                : section
        ));

        setFlightLabelSections(flightId, nextSections);

        if (targetSection.id === null)
            return;

        const sectionId = targetSection.id;

        void (async () => {
            try {
                await fetchJson.put(`/api/flight/${flightId}/labels/${encodeURIComponent(sectionId)}`, {
                    labelText: targetSection.labelText,
                    visibleOnChart,
                });
            } catch (error: any) {
                await loadLabelsForFlight(flightId);
                setModal(ErrorModal, {
                    title: "Error Updating Section Visibility",
                    message: `Could not update chart visibility for label section on flight ${flightId}.`,
                    code: error?.toString?.() ?? String(error),
                });
            }
        })();

    };

    const handleCreateLabelDefinition = async (labelText: string): Promise<boolean> => {
        try {
            await fetchJson.post("/api/fleet/labels", { labelText });
            await loadLabelDefinitions();
            return true;
        } catch (error: any) {
            setModal(ErrorModal, {
                title: "Error Adding Label",
                message: "Could not add the new label definition.",
                code: error?.toString?.() ?? String(error),
            });
            return false;
        }
    };

    const getSeriesForLabeling = async (flightId: number, paramName: string): Promise<TraceSeries | null> => {
        const existing = chartData.seriesByFlight[flightId]?.[paramName];
        if (existing)
            return existing;

        try {
            return await ensureSeries(flightId, paramName);
        } catch (error: any) {
            setModal(ErrorModal, {
                title: "Error Loading Labeling Parameter",
                message: `Could not load ${paramName} for flight ${flightId}.`,
                code: error?.toString?.() ?? String(error),
            });
            return null;
        }
    };

    const findNearestSeriesIndex = (timestamps: number[], targetSeconds: number): number | null => {
        if (timestamps.length === 0 || !Number.isFinite(targetSeconds))
            return null;

        let nearestIndex = 0;
        let bestDistance = Number.POSITIVE_INFINITY;

        for (let i = 0; i < timestamps.length; i++) {
            const distance = Math.abs(timestamps[i]! - targetSeconds);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    };

    const resolveSectionAbsoluteTimes = useCallback((flightId: number, section: LabelSection): [number, number] | null => {

        const flightStartMs = flightStartMsById.get(flightId);
        if (flightStartMs === undefined || Number.isNaN(flightStartMs))
            return null;

        const flightEndMs = flightEndMsById.get(flightId);
        const flightStartSec = flightStartMs / 1000;
        const flightEndSec = (flightEndMs !== undefined && !Number.isNaN(flightEndMs))
            ? (flightEndMs / 1000)
            : (flightStartSec + 8 * 3600);

        const plausibleMin = flightStartSec - 3600;
        const plausibleMax = flightEndSec + 3600;

        const isPlausible = (t: number) => Number.isFinite(t) && t >= plausibleMin && t <= plausibleMax;

        const sectionStartAbs = toAbsoluteSecondsFromTimestampCandidate(Number(section.startTime), flightStartMs);
        const sectionEndAbs = toAbsoluteSecondsFromTimestampCandidate(Number(section.endTime), flightStartMs);

        if (isPlausible(sectionStartAbs) && isPlausible(sectionEndAbs))
            return sortPair(sectionStartAbs, sectionEndAbs);

        const parameterCandidates = Array.isArray(section.parameterNames) ? section.parameterNames : [];

        for (const paramName of parameterCandidates) {

            const series = chartData.seriesByFlight[flightId]?.[paramName];
            if (!series || !Array.isArray(series.timestamps) || series.timestamps.length === 0)
                continue;

            const startIndex = Math.max(0, Math.min(series.timestamps.length - 1, Number(section.startIndex)));
            const endIndex = Math.max(0, Math.min(series.timestamps.length - 1, Number(section.endIndex)));

            const startTsRaw = Number(series.timestamps[startIndex]);
            const endTsRaw = Number(series.timestamps[endIndex]);

            const startAbs = toAbsoluteSecondsFromTimestampCandidate(startTsRaw, flightStartMs);
            const endAbs = toAbsoluteSecondsFromTimestampCandidate(endTsRaw, flightStartMs);

            if (isPlausible(startAbs) && isPlausible(endAbs)) {
                log("Corrected implausible section times using trace indices", {
                    flightId,
                    paramName,
                    sectionId: section.id,
                    startIndex,
                    endIndex,
                    correctedStartAbs: startAbs,
                    correctedEndAbs: endAbs,
                    originalStart: section.startTime,
                    originalEnd: section.endTime,
                });
                return sortPair(startAbs, endAbs);
            }

        }

        if (Number.isFinite(sectionStartAbs) && Number.isFinite(sectionEndAbs))
            return sortPair(sectionStartAbs, sectionEndAbs);

        return null;

    }, [chartData.seriesByFlight, flightStartMsById, flightEndMsById]);

    const labelSectionsForDisplayByFlight = useMemo(() => {
        const out: Record<number, LabelSection[]> = {};

        for (const flight of chartFlights) {
            const sections = labelSectionsByFlight[flight.id] ?? [];

            out[flight.id] = sections.map((section) => {
                const resolvedTimes = resolveSectionAbsoluteTimes(flight.id, section);
                if (!resolvedTimes)
                    return section;

                const [startAbs, endAbs] = resolvedTimes;

                return {
                    ...section,
                    startTime: startAbs,
                    endTime: endAbs,
                };
            });
        }

        return out;
    }, [chartFlights, labelSectionsByFlight, resolveSectionAbsoluteTimes]);

    const handleLabelingChartClick = (xValue: number) => {

        log("handleLabelingChartClick invoked", {
            xValue,
            activeLabelingCaptureFlightId,
            useAlignedStartTimes,
        });

        if (activeLabelingCaptureFlightId === null) {
            log.warn("Ignoring labeling click: no active labeling flight");
            return;
        }

        const selectedParam = selectedLabelingParamByFlight[activeLabelingCaptureFlightId] ?? "";
        const activeParams = selectedParamsByFlight[activeLabelingCaptureFlightId] ?? [];

        const effectiveParam = activeParams.includes(selectedParam)
            ? selectedParam
            : (activeParams[0] ?? "");

        if (!effectiveParam) {
            log.warn("Ignoring labeling click: no selected labeling parameter", {
                flightId: activeLabelingCaptureFlightId,
                selectedParam,
                activeParams,
            });
            return;
        }

        if (effectiveParam !== selectedParam) {
            log("Using fallback active chart parameter for labeling click", {
                flightId: activeLabelingCaptureFlightId,
                selectedParam,
                effectiveParam,
                activeParams,
            });
            setSelectedLabelingParam(activeLabelingCaptureFlightId, effectiveParam);
        }

        void (async () => {

            const series = await getSeriesForLabeling(activeLabelingCaptureFlightId, effectiveParam);
            if (!series) {
                log.warn("Ignoring labeling click: missing trace series", {
                    flightId: activeLabelingCaptureFlightId,
                    selectedParam: effectiveParam,
                });
                return;
            }

            const flightStartMsMaybe = flightStartMsById.get(activeLabelingCaptureFlightId);
            if (typeof flightStartMsMaybe !== "number" || Number.isNaN(flightStartMsMaybe)) {
                log.warn("Ignoring labeling click: missing/invalid flightStartMs", {
                    flightId: activeLabelingCaptureFlightId,
                    flightStartMsMaybe,
                });
                return;
            }
            const flightStartMs = flightStartMsMaybe;

            const relativeSeriesTimestamps = series.timestamps.map((timestampRaw) => (
                normalizeTraceTimestampToRelativeSeconds(timestampRaw, flightStartMs)
            ));

            if (relativeSeriesTimestamps.length === 0 || !relativeSeriesTimestamps.some((t) => Number.isFinite(t))) {
                log.warn("Ignoring labeling click: no valid normalized timestamps for active series", {
                    flightId: activeLabelingCaptureFlightId,
                    selectedParam: effectiveParam,
                });
                return;
            }

            const clickedRelativeSeconds = useAlignedStartTimes
                ? xValue
                : (xValue - flightStartMs) / 1000;

            const pendingStartX = pendingLabelStartXByFlight[activeLabelingCaptureFlightId] ?? null;

            log("Resolved labeling click values", {
                flightId: activeLabelingCaptureFlightId,
                selectedParam: effectiveParam,
                clickedRelativeSeconds,
                pendingStartX,
            });

            // First click marks the start of the new label section.
            if (pendingStartX === null) {
                setPendingLabelStartX(activeLabelingCaptureFlightId, clickedRelativeSeconds);
                log("Stored pending label range start", {
                    flightId: activeLabelingCaptureFlightId,
                    pendingStartX: clickedRelativeSeconds,
                });
                return;
            }

            const startIdxRaw = findNearestSeriesIndex(relativeSeriesTimestamps, pendingStartX);
            const endIdxRaw = findNearestSeriesIndex(relativeSeriesTimestamps, clickedRelativeSeconds);

            if (startIdxRaw === null || endIdxRaw === null) {
                log.warn("Could not resolve nearest series indices for label range", {
                    flightId: activeLabelingCaptureFlightId,
                    pendingStartX,
                    clickedRelativeSeconds,
                });
                return;
            }

            const startIndex = Math.min(startIdxRaw, endIdxRaw);
            const endIndex = Math.max(startIdxRaw, endIdxRaw);

            const startRelSeconds = relativeSeriesTimestamps[startIndex] ?? 0;
            const endRelSeconds = relativeSeriesTimestamps[endIndex] ?? startRelSeconds;

            const sectionParameterNames = Array.from(new Set(activeParams));
            const isSingleParameterSection = (sectionParameterNames.length === 1);

            const startValue = isSingleParameterSection ? (series.values[startIndex] ?? 0) : 0;
            const endValue = isSingleParameterSection ? (series.values[endIndex] ?? startValue) : 0;

            const startTimeSeconds = toAbsoluteSecondsFromTimestampCandidate(pendingStartX, flightStartMs);
            const endTimeSeconds = toAbsoluteSecondsFromTimestampCandidate(clickedRelativeSeconds, flightStartMs);

            if (!Number.isFinite(startTimeSeconds) || !Number.isFinite(endTimeSeconds)) {
                log.warn("Could not compute absolute label section times", {
                    flightId: activeLabelingCaptureFlightId,
                    pendingStartX,
                    clickedRelativeSeconds,
                    startRelSeconds,
                    endRelSeconds,
                    flightStartMs,
                });
                setPendingLabelStartX(activeLabelingCaptureFlightId, null);
                return;
            }

            const startTimeNormalized = Math.min(startTimeSeconds, endTimeSeconds);
            const endTimeNormalized = Math.max(startTimeSeconds, endTimeSeconds);

            const payload = {
                startIndex,
                endIndex,
                startTime: startTimeNormalized,
                endTime: endTimeNormalized,
                startValue,
                endValue,
                labelText: "",
                parameterNames: sectionParameterNames,
                visibleOnChart: true,
            };

            log("Submitting label range payload", {
                flightId: activeLabelingCaptureFlightId,
                selectedParam: effectiveParam,
                sectionParameterNames,
                payload,
            });

            try {
                await fetchJson.post(`/api/flight/${activeLabelingCaptureFlightId}/labels`, payload);
                setPendingLabelStartX(activeLabelingCaptureFlightId, null);
                await loadLabelsForFlight(activeLabelingCaptureFlightId);
                log("Label range saved successfully", {
                    flightId: activeLabelingCaptureFlightId,
                });
            } catch (error: any) {
                setPendingLabelStartX(activeLabelingCaptureFlightId, null);
                log.error("Error saving label range", {
                    flightId: activeLabelingCaptureFlightId,
                    error,
                });
                setModal(ErrorModal, {
                    title: "Error Saving Label Section",
                    message: `Could not create label section for flight ${activeLabelingCaptureFlightId}.`,
                    code: error?.toString?.() ?? String(error),
                });
            }

        })();

    };

    // Ensure series are loaded for all selected (flightId, paramName)
    useEffect(() => {

        Object.entries(selectedParamsByFlight).forEach(([flightIdStr, params]) => {

            const flightId = Number(flightIdStr);

            params.forEach((name) => {

                const existing = chartData.seriesByFlight[flightId]?.[name];

                // Already have this series, skip
                if (existing)
                    return;

                ensureSeries(flightId, name).catch((error) => {
                    log.error("Error ensuring series", { flightId, name, error });
                });

            });

        });

    }, [selectedParamsByFlight, chartData.seriesByFlight, ensureSeries]);

    const buildChartConfig = (activeSeries: ActiveSeries[]): ChartConfig => {

        const config: ChartConfig = {};

        // Build a stable 0..N-1 index for *active* flights
        const flightIdsInOrder = Array.from(
            new Set(activeSeries.map((entry) => entry.flight.id)),
        );

        const flightSlotIndexById = new Map<number, number>();
        flightIdsInOrder.forEach((id, idx) => {
            flightSlotIndexById.set(id, idx);
        });

        activeSeries.forEach((entry, seriesIndex) => {

            const { seriesKey, flight, paramName, paramIndex } = entry;

            const flightSlotIndex = flightSlotIndexById.get(flight.id) ?? 0;

            const color = (useHighContrastCharts)
                ? buildHighContrastColor(seriesIndex)
                : buildNonHighContrastColor(flightSlotIndex, paramIndex);

            config[seriesKey] = {
                label: `${flight.id} — ${paramName}`,
                color,
            };

        });

        return config;

    };

    // Main, expensive chart model (memoized)
    const chartModel = useMemo<ChartModel>(() => {

        // No flights, return empty model
        if (!hasAnySelectedParams)
            return buildEmptyChartModel(false);

        const activeSeries: ActiveSeries[] = [];

        chartFlights.forEach((flight, flightIndex) => {

            const paramNames = selectedParamsByFlight[flight.id];

            // No params selected for this flight, skip
            if (!paramNames || paramNames.length === 0)
                return;

            paramNames.forEach((paramName, paramIndex) => {

                const series = chartData.seriesByFlight[flight.id]?.[paramName];
                if (!series)
                    return;

                const seriesKey = makeSeriesKeyForChart(flight.id, paramName);

                activeSeries.push({
                    seriesKey,
                    flight,
                    flightIndex,
                    paramName,
                    paramIndex,
                    series,
                });

            });

        });

        // No series to show, return empty model
        if (activeSeries.length === 0)
            return buildEmptyChartModel(true);

        type PreparedSeries = {
            seriesKey: string;
            startMS: number;
            points: { t: number; v: number }[];
        };

        const preparedSeries: PreparedSeries[] = [];
        const timeSet = new Set<number>();

        // Decimate and collect union of times (absolute or relative)
        activeSeries.forEach(({ seriesKey, flight, series }) => {

            const pointsRaw = decimateSeries(series);

            const startMSForFlight = flightStartMsById.get(flight.id);

            // Invalid start time, skip this series
            if (startMSForFlight === undefined || Number.isNaN(startMSForFlight))
                return;

            const points = pointsRaw
                .map((point) => ({
                    t: normalizeTraceTimestampToRelativeSeconds(point.t, startMSForFlight),
                    v: point.v,
                }))
                .filter((point) => Number.isFinite(point.t) && Number.isFinite(point.v));

            // No points, skip
            if (points.length === 0)
                return;

            if (useAlignedStartTimes) {

                // Relative seconds from 0
                preparedSeries.push({ seriesKey, startMS: 0, points });
                for (const p of points)
                    timeSet.add(p.t);

            } else {

                const startMS = startMSForFlight;

                preparedSeries.push({ seriesKey, startMS, points });

                for (const p of points) {

                    const absMS = (startMS + p.t * 1_000);
                    timeSet.add(absMS);

                }

            }

        });

        if (preparedSeries.length === 0 || timeSet.size === 0)
            return buildEmptyChartModel(false);

        let allTimes = Array.from(timeSet).sort((a, b) => a - b);

        // X domain
        let xMin: number;
        let xMax: number;

        if (useAlignedStartTimes) {

            const longestDurationSec = preparedSeries.reduce((max, s) => {
                const last = s.points[s.points.length - 1]?.t ?? 0;
                return Math.max(max, last);
            }, 0);

            xMin = 0;
            xMax = longestDurationSec;

            if (xMax > 0 && (allTimes.length === 0 || allTimes[allTimes.length - 1] !== xMax)) {
                allTimes = [...allTimes, xMax].sort((a, b) => a - b);
            }

        } else {

            xMin = allTimes[0]!;
            xMax = allTimes[allTimes.length - 1]!;

        }

        /*
            Interpolate each series at every shared
            time so overlapping flights have values
            at the same x positions (fixes holes,
            creates combined tooltip).
        */
        const perSeriesValueMap = new Map<string, Map<number, number>>();

        for (const { seriesKey, startMS, points } of preparedSeries) {

            const valueMap = new Map<number, number>();
            const lastIndex = points.length - 1;

            if (useAlignedStartTimes) {

                const firstT = points[0].t;
                const lastT = points[lastIndex].t;
                let i = 0;

                for (const time of allTimes) {

                    if (time < firstT)
                        continue;
                    if (time > lastT)
                        break;

                    const tSec = time;

                    while (i < lastIndex && points[i + 1].t < tSec)
                        i++;

                    const p1 = points[i];
                    const p2 = points[Math.min(i + 1, lastIndex)];

                    let value: number;
                    if (!p2 || p1.t === p2.t || tSec <= p1.t) {
                        value = p1.v;
                    } else if (tSec >= p2.t) {
                        value = p2.v;
                    } else {
                        const ratio = (tSec - p1.t) / (p2.t - p1.t);
                        value = p1.v + ratio * (p2.v - p1.v);
                    }

                    valueMap.set(time, value);
                }

            } else {

                const firstAbs = (startMS + points[0].t * 1_000);
                const lastAbs = (startMS + points[lastIndex].t * 1_000);
                let i = 0;

                for (const time of allTimes) {

                    if (time < firstAbs)
                        continue;
                    if (time > lastAbs)
                        break;

                    const tSec = (time - startMS) / 1_000;

                    while (i < lastIndex && points[i + 1].t < tSec)
                        i++;

                    const p1 = points[i];
                    const p2 = points[Math.min(i + 1, lastIndex)];

                    let value: number;
                    if (!p2 || p1.t === p2.t || tSec <= p1.t) {
                        value = p1.v;
                    } else if (tSec >= p2.t) {
                        value = p2.v;
                    } else {
                        const ratio = (tSec - p1.t) / (p2.t - p1.t);
                        value = p1.v + ratio * (p2.v - p1.v);
                    }

                    valueMap.set(time, value);

                }

            }

            perSeriesValueMap.set(seriesKey, valueMap);

        }

        // Build the Recharts data array
        const data = allTimes.map((time) => {

            const point: Record<string, number | string> = { time };

            preparedSeries.forEach(({ seriesKey }) => {
                const v = perSeriesValueMap.get(seriesKey)?.get(time);
                if (v !== undefined)
                    point[seriesKey] = v;
            });

            return point;

        });

        const seriesKeys = preparedSeries.map((s) => s.seriesKey);
        const seriesFlightIDByKey = Object.fromEntries(
            activeSeries.map((entry) => [entry.seriesKey, entry.flight.id]),
        ) as Record<string, number>;
        const config = buildChartConfig(activeSeries);

        // Compute global Y domain across all series
        let yMin = Number.POSITIVE_INFINITY;
        let yMax = Number.NEGATIVE_INFINITY;

        for (const row of data) {
            for (const key of seriesKeys) {
                const v = row[key] as number | undefined;
                if (typeof v === "number") {
                    if (v < yMin) yMin = v;
                    if (v > yMax) yMax = v;
                }
            }
        }

        if (!Number.isFinite(yMin) || !Number.isFinite(yMax)) {
            yMin = 0;
            yMax = 0;
        }

        const selectedEventNames = Array.from(
            new Set(
                Object.values(selectedEventsByFlight)
                    .flatMap((set) => Array.from(set)),
            ),
        ).sort((a, b) => a.localeCompare(b));

        // Keep one representative timestamps array per flight for event line-index mapping.
        const representativeTimestampsByFlight = new Map<number, number[]>();
        for (const entry of activeSeries) {

            const flightStartMS = flightStartMsById.get(entry.flight.id);
            if (flightStartMS === undefined || Number.isNaN(flightStartMS))
                continue;

            const candidate = (entry.series.timestamps ?? [])
                .map((timestampRaw) => normalizeTraceTimestampToRelativeSeconds(timestampRaw, flightStartMS))
                .filter((t) => Number.isFinite(t));

            if (candidate.length === 0)
                continue;

            const current = representativeTimestampsByFlight.get(entry.flight.id);
            if (!current || candidate.length > current.length)
                representativeTimestampsByFlight.set(entry.flight.id, candidate);

        }

        const eventColorByName = new Map<string, string>();
        selectedEventNames.forEach((eventName, idx) => {

            if (useHighContrastCharts) {
                eventColorByName.set(eventName, `var(--chart-hc-${(idx % 12) + 1})`);
                return;
            }

            const denominator = Math.max(1, selectedEventNames.length - 1);
            const hue = Math.round((idx / denominator) * 360);
            eventColorByName.set(eventName, `hsl(${hue} 85% 56%)`);

        });

        const eventOverlays: ChartModel["eventOverlays"] = [];

        for (const flight of chartFlights) {

            const selectedNames = selectedEventsByFlight[flight.id];
            if (!selectedNames || selectedNames.size === 0)
                continue;

            if (!flight.events || !flight.eventDefinitions)
                continue;

            const definitionNameByID = new Map<number, string>();
            for (const definition of flight.eventDefinitions)
                definitionNameByID.set(definition.id, definition.name);

            const flightStartMS = flightStartMsById.get(flight.id);
            const representativeTimestamps = representativeTimestampsByFlight.get(flight.id) ?? [];

            for (const event of flight.events) {

                const eventName = definitionNameByID.get(event.eventDefinitionId);
                if (!eventName || !selectedNames.has(eventName))
                    continue;

                let x1: number;
                let x2: number;

                const startLine = Number(event.startLine);
                const endLine = Number(event.endLine);

                const lineIndicesUsable = (
                    representativeTimestamps.length > 0
                    && Number.isFinite(startLine)
                    && Number.isFinite(endLine)
                    && startLine >= 0
                    && endLine >= 0
                    && startLine < representativeTimestamps.length
                );

                if (lineIndicesUsable) {

                    const startIndex = Math.floor(startLine);
                    const endIndexRaw = Math.floor(endLine);

                    // Use one point beyond end index (when available) for a full event span.
                    const endIndex = Math.min(
                        Math.max(endIndexRaw + 1, startIndex),
                        representativeTimestamps.length - 1,
                    );

                    const tStart = representativeTimestamps[startIndex];
                    const tEnd = representativeTimestamps[endIndex];

                    if (!Number.isFinite(tStart) || !Number.isFinite(tEnd))
                        continue;

                    if (useAlignedStartTimes) {
                        x1 = tStart;
                        x2 = tEnd;
                    } else {
                        if (flightStartMS === undefined || Number.isNaN(flightStartMS))
                            continue;

                        x1 = flightStartMS + tStart * 1_000;
                        x2 = flightStartMS + tEnd * 1_000;
                    }

                } else {

                    // Fallback for unusual data where line indices are unavailable.
                    const startMS = Date.parse(String(event.startTime));
                    const endMS = Date.parse(String(event.endTime));

                    if (!Number.isFinite(startMS) || !Number.isFinite(endMS))
                        continue;

                    if (useAlignedStartTimes) {

                        if (flightStartMS === undefined || Number.isNaN(flightStartMS))
                            continue;

                        x1 = (startMS - flightStartMS) / 1000;
                        x2 = (endMS - flightStartMS) / 1000;

                    } else {

                        x1 = startMS;
                        x2 = endMS;

                    }

                }

                if (!Number.isFinite(x1) || !Number.isFinite(x2))
                    continue;

                if (x2 < x1) {
                    const temp = x1;
                    x1 = x2;
                    x2 = temp;
                }

                // Ensure zero-length events remain visible.
                if (x1 === x2)
                    x2 = x1 + (useAlignedStartTimes ? 0.25 : 250);

                eventOverlays.push({
                    id: `evt-${flight.id}-${event.id}-${event.eventDefinitionId}`,
                    flightId: flight.id,
                    eventName,
                    severity: Number.isFinite(event.severity) ? event.severity : 0,
                    x1,
                    x2,
                    color: eventColorByName.get(eventName) ?? "var(--warning)",
                });

            }

        }

        eventOverlays.sort((a, b) => (a.x1 - b.x1) || (a.x2 - b.x2));

        const labelOverlays: ChartModel["labelOverlays"] = [];

        for (const flight of chartFlights) {

            const sections = labelSectionsByFlight[flight.id] ?? [];
            if (sections.length === 0)
                continue;

            const flightStartMsMaybe = flightStartMsById.get(flight.id);
            if (typeof flightStartMsMaybe !== "number" || Number.isNaN(flightStartMsMaybe))
                continue;
            const flightStartMs = flightStartMsMaybe;

            sections.forEach((section, sectionIndex) => {

                if (section.visibleOnChart === false)
                    return;

                const resolvedTimes = resolveSectionAbsoluteTimes(flight.id, section);
                if (!resolvedTimes)
                    return;

                const [startAbs, endAbs] = resolvedTimes;

                const startRelSeconds = toRelativeSeconds(startAbs, flightStartMs);
                const endRelSeconds = toRelativeSeconds(endAbs, flightStartMs);

                let x1 = useAlignedStartTimes
                    ? startRelSeconds
                    : (flightStartMs + startRelSeconds * 1000);

                let x2 = useAlignedStartTimes
                    ? endRelSeconds
                    : (flightStartMs + endRelSeconds * 1000);

                if (!Number.isFinite(x1) || !Number.isFinite(x2))
                    return;

                if (x2 < x1) {
                    const tmp = x1;
                    x1 = x2;
                    x2 = tmp;
                }

                if (x1 === x2)
                    x2 = x1 + (useAlignedStartTimes ? 0.25 : 250);

                labelOverlays.push({
                    id: `label-${flight.id}-${section.id ?? `${section.startIndex}-${section.endIndex}`}`,
                    flightId: flight.id,
                    x1,
                    x2,
                    color: getLabelMarkerColor(sectionIndex),
                });

            });

        }

        labelOverlays.sort((a, b) => (a.x1 - b.x1) || (a.x2 - b.x2));

        log("Built chart config and base domains: ", {
            xMin,
            xMax,
            yMin,
            yMax,
            seriesCount: seriesKeys.length,
            pointCount: data.length,
            eventOverlayCount: eventOverlays.length,
            labelOverlayCount: labelOverlays.length,
        });

        return {
            loading: false,
            hasData: true,
            data,
            config,
            seriesKeys,
            seriesFlightIDByKey,
            eventOverlays,
            labelOverlays,
            xMin,
            xMax,
            yMin,
            yMax,
        };

    }, [
        hasAnySelectedParams,
        chartFlights,
        selectedParamsByFlight,
        selectedEventsByFlight,
        chartData.seriesByFlight,
        labelSectionsByFlight,
        useHighContrastCharts,
        flightStartMsById,
        useAlignedStartTimes,
        resolveSectionAbsoluteTimes,
    ]);

    const pendingStartMarker = useMemo(() => {
        if (activeLabelingCaptureFlightId === null)
            return null;

        const pendingStartX = pendingLabelStartXByFlight[activeLabelingCaptureFlightId] ?? null;
        if (pendingStartX === null || !Number.isFinite(pendingStartX))
            return null;

        const flightStartMsMaybe = flightStartMsById.get(activeLabelingCaptureFlightId);
        if (typeof flightStartMsMaybe !== "number" || Number.isNaN(flightStartMsMaybe))
            return null;

        const x = useAlignedStartTimes
            ? pendingStartX
            : (flightStartMsMaybe + pendingStartX * 1000);

        if (!Number.isFinite(x))
            return null;

        const nextSectionIndex = labelSectionsByFlight[activeLabelingCaptureFlightId]?.length ?? 0;

        return {
            x,
            color: getLabelMarkerColor(nextSectionIndex),
        };
    }, [
        activeLabelingCaptureFlightId,
        pendingLabelStartXByFlight,
        flightStartMsById,
        useAlignedStartTimes,
        labelSectionsByFlight,
    ]);

    const renderNoDataMessage = () => (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2"
        >
            <Info />

            <div className="flex flex-col">
                <AlertTitle>No Selected Flights!</AlertTitle>
                <AlertDescription>
                    Data for the selected flights will appear here.
                    <br />
                    <div className="flex">
                        Try using the{" "}
                        <div className="flex items-center font-bold gap-1 mx-2">
                            <List size={16} />
                            List
                        </div>{" "}
                        button in a flight result row.
                    </div>
                </AlertDescription>
            </div>
        </motion.div>
    );

    const renderNoParamsMessage = () => (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2"
        >
            <Info />

            <div className="flex flex-col">
                <AlertTitle>No Parameters Selected!</AlertTitle>
                <AlertDescription>
                    Use the chart data dialog to choose which parameters to show.
                    <br />
                    <span className="flex items-center">
                        <span>Open the</span>
                        <span className="font-bold mx-2 inline-flex items-center gap-1"><List size={16} />Chart Data</span>
                        <span>menu in the top-right of this panel.</span>
                    </span>
                </AlertDescription>
            </div>
        </motion.div>
    );

    const renderLoadingMessage = () => (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2"
        >
            <Info />

            <div className="flex flex-col">
                <AlertTitle>Loading Chart Data…</AlertTitle>
                <AlertDescription>Fetching series for the selected parameters.</AlertDescription>
            </div>
        </motion.div>
    );

    const renderExpandChartItemsButton = () => {

        const expandChartItemsModal = () => {

            setGotChartFlightAdded(false);

            setModal(ChartsListModal, {
                chartFlights,
                setChartFlights,
                chartSelection,
                toggleUniversalParam,
                togglePerFlightParam,
            });

        };

        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        id="expand-chart-items-button"
                        variant="ghost"
                        onClick={expandChartItemsModal}
                        className="p-2"
                    >
                        <List />
                        <span>
                            {chartFlights.length}&nbsp;
                            <span className="@max-3xl:hidden!">Selected</span>
                        </span>

                        {gotChartFlightAdded && <Ping />}
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Expand Chart Items</TooltipContent>
            </Tooltip>
        );

    };

    const renderAlignedStartTimesToggle = () => {

        const toggleAlignedStartTimes = () => {
            setUseAlignedStartTimes((prev) => {
                const next = !prev;
                log("Toggled useAlignedStartTimes.", { previous: prev, next });
                return next;
            });
        };

        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        variant="ghost"
                        className="p-2"
                        onClick={toggleAlignedStartTimes}
                    >
                        <ArrowLeftToLine size={16} />
                        <span className="text-xs @max-4xl:hidden!">Align Start Times</span>
                        <input
                            type="checkbox"
                            checked={useAlignedStartTimes}
                            onChange={toggleAlignedStartTimes}
                            className="ml-1 pointer-events-none"
                        />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Toggle Aligned Start Times</TooltipContent>
            </Tooltip>
        );

    };

    const renderResetViewButton = () => {

        const handleClick = () => {
            interactiveChartRef.current?.resetView();
        };

        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        variant="ghost"
                        className="p-2"
                        onClick={handleClick}
                    >
                        <Expand size={16} />
                        <span className="text-xs @max-4xl:hidden!">Reset View</span>
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Reset Zoom & Pan</TooltipContent>
            </Tooltip>
        )

    }


    const [shiftHeld, setShiftHeld] = useState(false);
    const [ctrlHeld, setCtrlHeld] = useState(false);
    const [altHeld, setAltHeld] = useState(false);
    useEffect(() => {

        const handleKeyDown = (e: KeyboardEvent) => {

            if (e.key === "Control")
                setCtrlHeld(true);

            else if (e.key === "Shift" && !ctrlHeld)
                setShiftHeld(true);

            else if (e.key === "Alt")
                setAltHeld(true);
            
        };

        const handleKeyUp = (e: KeyboardEvent) => {

            if (e.key === "Shift")
                setShiftHeld(false);
            
            if (e.key === "Control")
                setCtrlHeld(false);

            if (e.key === "Alt")
                setAltHeld(false);
            
        };

        const handleMouseWheel = (e: WheelEvent) => {

            const chartElement = document.getElementById("flights-panel-chart-linechart");

            // Chart element not found, exit
            if (!chartElement)
                return;

            const chartRegion = chartElement.getBoundingClientRect();
            const x = e.clientX;
            const y = e.clientY;

            const isInChartArea =
                (x >= chartRegion.left)
                && (x <= chartRegion.right) 
                && (y >= chartRegion.top)
                && (y <= chartRegion.bottom);

            // Not hovering the chart, scroll the page normally
            if (!isInChartArea)
                return;

            e.preventDefault();

        }

        window.addEventListener("keydown", handleKeyDown);
        window.addEventListener("keyup", handleKeyUp);
        window.addEventListener("wheel", handleMouseWheel, { passive: false });

        return () => {
            window.removeEventListener("keydown", handleKeyDown);
            window.removeEventListener("keyup", handleKeyUp);
            window.removeEventListener("wheel", handleMouseWheel);
        };

    }, []);

    
    const renderChartInteractionControlsGuide = () => {

        const renderControl = (name:string, description: string, icon: React.ReactNode, iconSecondary?: React.ReactNode) => (

            <div className="flex items-center gap-2 mb-2">
                <div className="flex relative">
                    {icon}
                    {
                        (iconSecondary)
                        &&
                        <div className="absolute right-6.5 top-1/2 -translate-y-1/2 scale-75 opacity-75">
                            {iconSecondary}
                        </div>
                    }
                </div>
                <div className="flex flex-col">
                    <div className="font-bold">{name}</div>
                    <div className="text-xs">{description}</div>
                </div>
            </div>

        );

        return <Card className="absolute left-24 top-2 w-80 overflow-y-auto max-h-[90%] bg-transparent backdrop-blur-xs">

            <Accordion type="single" collapsible defaultValue="" className="px-2">

                <AccordionItem value="controls-guide">
                    <AccordionTrigger className="mx-6">
                        Chart Controls
                    </AccordionTrigger>

                    <AccordionContent className="flex flex-col gap-2 mx-6">

                        {/* Pan */}
                        {renderControl(
                            "Pan",
                            "Shift + left-click and drag to pan around the chart.",
                            <MousePointerClick type="right-click-drag" className="w-6 h-6" />,
                            <ArrowBigUp className={`w-4 h-4 ${shiftHeld ? "opacity-100 scale-150!" : "opacity-50"} transition-transform duration-100`} />,
                        )}

                        {/* Zoom (Selection) */}
                        {renderControl(
                            "Zoom (Selection)",
                            "Left-click and drag to select a region to zoom in on.",
                            <MousePointerClick type="left-click-drag" className="w-6 h-6" />,
                        )}
                        
                        {/* Zoom (Wheel) */}
                        {renderControl(
                            "Zoom (Wheel)",
                            "Use the mouse wheel to zoom in and out centered on the cursor.",
                            <Mouse className="w-6 h-6" />,
                        )}

                        {/* Zoom (Wheel Horizontal) */}
                        {renderControl(
                            "Zoom (Wheel Horizontal)",
                            "Hold Shift and use the mouse wheel to zoom horizontally centered on the cursor.",
                            <Mouse className="w-6 h-6" />,
                            <ArrowBigUp className={`w-4 h-4 ${shiftHeld ? "opacity-100 scale-150!" : "opacity-50"} transition-transform duration-100`} />,
                        )}

                        {/* Zoom (Wheel Vertical) */}
                        {renderControl(
                            "Zoom (Wheel Vertical)",
                            "Hold Ctrl and use the mouse wheel to zoom vertically centered on the cursor.",
                            <Mouse className="w-6 h-6" />,
                            <SquareChevronUp className={`w-4 h-4 ${ctrlHeld ? "opacity-100 scale-150!" : "opacity-50"} transition-transform duration-100`} />,
                        )}

                        {/* Cancel */}
                        {renderControl(
                            "Cancel",
                            "Right-click to cancel the current interaction.",
                            <NavigationOff type="right-click" className="w-6 h-6" />,
                        )}

                        {/* Label Range Input */}
                        {renderControl(
                            "Label Range",
                            "Alt + left-click to select start/end points for the active labeling flight.",
                            <MousePointerClick type="left-click" className="w-6 h-6" />,
                            <SquareActivity className={`w-4 h-4 ${altHeld ? "opacity-100 scale-150!" : "opacity-50"} transition-transform duration-100`} />,
                        )}

                    </AccordionContent>
                </AccordionItem>
            </Accordion>

        </Card>

    }

    const noFlights = (chartFlights.length === 0);

    const legendItems = chartModel.seriesKeys.map((key) => ({
        id: key,
        value: chartModel.config[key]?.label ?? key,
        color: chartModel.config[key]?.color,
        type: "line" as const,
    }));

    let legendItemFlightIDPrevious = 0;

    log("Rendering ChartPanel: ", {
        noFlights,
        hasAnySelectedParams,
        chartModelLoading: chartModel.loading,
        chartModelHasData: chartModel.hasData,
        seriesCount: chartModel.seriesKeys.length,
        dataPointCount: chartModel.data.length,
        useAlignedStartTimes,
    });

    return (
        <Card className="border rounded-lg w-full h-full card-glossy relative @container">

            {/* Legend Overlay */}
            {
                (legendItems.length > 0)
                && (
                    <div
                        className="absolute z-10 flex flex-col gap-1 p-2 rounded bg-muted shadow-sm opacity-50 hover:opacity-100 transition-opacity"
                        style={{ top: 52, right: 8 }}
                    >
                        {
                            legendItems.map((item) => {

                                const labelFlightID = Number((item as any).value.split(" — ")[0]);
                                const showFlightSeparator = (labelFlightID !== legendItemFlightIDPrevious) && (legendItemFlightIDPrevious !== 0);
                                legendItemFlightIDPrevious = labelFlightID;

                                return (
                                    <div className="flex flex-col" key={item.id}>
                                        {
                                            (showFlightSeparator)
                                            &&
                                            <div className="border-t border-border my-1" />
                                        }
                                        <div className="flex items-center gap-2 text-xs">
                                            <span
                                                className="inline-block w-3 h-3 rounded-full"
                                                style={{ backgroundColor: item.color }}
                                            />
                                            <span>{item.value}</span>
                                        </div>
                                    </div>
                                );
                            })
                        }
                    </div>
                )
            }

            <AnimatePresence mode="wait" initial={false}>
                {
                    (noFlights) ? (
                        renderNoDataMessage()
                    ) : (!hasAnySelectedParams) ? (
                        renderNoParamsMessage()
                    ) : (chartModel.loading || !chartModel.hasData) ? (
                        renderLoadingMessage()
                    ) : (
                        <InteractiveChart
                            ref={interactiveChartRef}
                            chartModel={chartModel}
                            activeLabelingCaptureFlightId={activeLabelingCaptureFlightId}
                            useAlignedStartTimes={useAlignedStartTimes}
                            theme={theme}
                            shiftHeld={shiftHeld}
                            ctrlHeld={ctrlHeld}
                            altHeld={altHeld}
                            onChartXClick={handleLabelingChartClick}
                            pendingStartMarker={pendingStartMarker}
                        />
                    )}
            </AnimatePresence>


            {
                (!noFlights)
                &&
                <>
                    {/* Labeling Tool */}
                    {
                        labelingEnabledFlights.map((flight, index) => {

                            const defaultPosition = {
                                left: 8 + (index * 28),
                                top: 8 + (index * 28),
                            };
                            const position = labelingCardPositionByFlight[flight.id] ?? defaultPosition;

                            return (
                                <FlightsPanelChartLabelCard
                                    isActive={activeLabelingCaptureFlightId === flight.id}
                                    key={flight.id}
                                    flightId={flight.id}
                                    pendingStartX={pendingLabelStartXByFlight[flight.id] ?? null}
                                    flightLabelSections={labelSectionsForDisplayByFlight[flight.id] ?? []}
                                    onFlightCsvDownload={() => {
                                        window.location.href = `/api/flight/${flight.id}/labels/csv`;
                                    }}
                                    onFleetCsvDownload={() => {
                                        window.location.href = "/api/fleet/labels/csv";
                                    }}
                                    onImportCsv={(file) => {
                                        handleImportCsv(flight.id, file);
                                    }}
                                    onRemoveSection={(sectionIndex) => {
                                        handleRemoveLabelSection(flight.id, sectionIndex);
                                    }}
                                    labelDefinitions={labelDefinitions}
                                    onUpdateSectionLabel={(sectionIndex, labelText) => {
                                        handleUpdateSectionLabel(flight.id, sectionIndex, labelText);
                                    }}
                                    onToggleSectionVisibility={(sectionIndex, visibleOnChart) => {
                                        handleToggleSectionVisibility(flight.id, sectionIndex, visibleOnChart);
                                    }}
                                    onCreateLabelDefinition={handleCreateLabelDefinition}
                                    onClose={() => {
                                        handleCloseLabeling(flight.id);
                                    }}
                                    onActivate={() => {
                                        setActiveLabelingFlightFromCard(flight.id);
                                    }}
                                    position={position}
                                    onPositionChange={(nextPosition) => {
                                        setLabelingCardPositionByFlight((prev) => ({
                                            ...prev,
                                            [flight.id]: nextPosition,
                                        }));
                                    }}
                                />
                            );
                        })
                    }

                    {/* Controls Guide */}
                    {renderChartInteractionControlsGuide()}

                    {/* Chart Control Buttons */}
                    <div className="flex gap-2 absolute right-2 top-2 justify-end">
                        {renderResetViewButton()}
                        {renderAlignedStartTimesToggle()}
                        {renderExpandChartItemsButton()}
                    </div>
                </>

            }

        </Card>
    );
}
