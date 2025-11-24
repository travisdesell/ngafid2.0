// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_chart.tsx

"use client";

import { ChartsListModal } from "@/components/modals/charts_list_modal/charts_list_modal";
import { useModal } from "@/components/modals/modal_context";
import Ping from "@/components/pings/ping";
import { getLogger } from "@/components/providers/logger";
import { useTheme } from "@/components/providers/theme-provider";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { useEffectPrev } from "@/lib/useEffectPrev";
import { useFlights } from "@/pages/protected/flights/_flights_context";
import { Flight } from "@/pages/protected/flights/types";
import { TraceSeries } from "@/pages/protected/flights/types_charts";
import { ArrowLeftToLine, ChartArea, Info, List } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { useEffect, useMemo, useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, XAxis, YAxis } from "recharts";

const log = getLogger("FlightsPanelChart", "blue", "Component");

type ActiveSeries = {
    seriesKey: string; //<-- unique key: f{flightId}_{sanitizedParamName}
    flight: Flight;
    flightIndex: number;
    paramName: string;
    paramIndex: number;
    series: TraceSeries;
};

// Flight chart color config
const BASE_FLIGHT_COLORS = [
    "var(--chart-hc-1)",
    "var(--chart-hc-2)",
    "var(--chart-hc-3)",
    "var(--chart-hc-4)",
    "var(--chart-hc-5)",
    "var(--chart-hc-6)",
    "var(--chart-hc-7)",
    "var(--chart-hc-8)",
    "var(--chart-hc-9)",
] as const;

const BRIGHTNESS_LEVELS_PER_FLIGHT = 8;

// Hard cap on number of points per series sent to Recharts.
const MAX_POINTS_PER_SERIES = 1000;

const makeSeriesKeyForChart = (flightId: number, paramName: string): string =>
    `f${flightId}_${paramName.replace(/[^a-zA-Z0-9]+/g, "_")}`;

const buildNonHighContrastColor = (flightIndex: number, paramIndex: number): string => {

    const base = BASE_FLIGHT_COLORS[flightIndex % BASE_FLIGHT_COLORS.length];
    const level = paramIndex % BRIGHTNESS_LEVELS_PER_FLIGHT;

    // 0 -> brightest, higher -> darker
    const WEIGHT_BASE = 100;
    const WEIGHT_MIN = 0;
    const WEIGHT_STEP = (WEIGHT_BASE - WEIGHT_MIN) / (BRIGHTNESS_LEVELS_PER_FLIGHT - 1);
    const weight = WEIGHT_BASE - level * WEIGHT_STEP;

    // Mix toward background to vary lightness while keeping hue
    const colorOut = `color-mix(in oklch, ${base} ${weight}%, var(--background))`;

    return colorOut;

};

const buildHighContrastColor = (seriesIndex: number): string => {

    const MAX_HC_COLORS = 12;
    const INDEX_OFFSET = 1; //<-- --chart-hc-1..N

    const index = (seriesIndex % MAX_HC_COLORS) + INDEX_OFFSET;
    const colorOut = `var(--chart-hc-${index})`;

    return colorOut;

};

type ChartModel = {
    loading: boolean;
    hasData: boolean;
    data: Array<Record<string, number | string>>;
    config: ChartConfig;
    seriesKeys: string[];
    ticks: number[];
    dayBoundaryTimes: number[];
    domainSpanMinutes: number;
};

const buildEmptyChartModel = (loading: boolean): ChartModel => ({
    loading,
    hasData: false,
    data: [],
    config: {} as ChartConfig,
    seriesKeys: [],
    ticks: [],
    dayBoundaryTimes: [],
    domainSpanMinutes: 0,
});

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

export function FlightsPanelChart() {

    const { chartFlights, setChartFlights, chartSelection, chartData, ensureSeries, toggleUniversalParam, togglePerFlightParam } = useFlights();
    const { useHighContrastCharts } = useTheme();
    const { setModal } = useModal();

    const [gotChartFlightAdded, setGotChartFlightAdded] = useState(false);
    const [useAlignedStartTimes, setUseAlignedStartTimes] = useState(false);

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

    const hasAnySelectedParams = useMemo(
        () => Object.values(selectedParamsByFlight).some((list) => list.length > 0),
        [selectedParamsByFlight]
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

        activeSeries.forEach((entry, seriesIndex) => {

            const { seriesKey, flight, flightIndex, paramName, paramIndex } = entry;

            const color = (useHighContrastCharts)
                ? buildHighContrastColor(seriesIndex)
                : buildNonHighContrastColor(flightIndex, paramIndex);

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

        // Decimate and collect union of absolute times
        activeSeries.forEach(({ seriesKey, flight, series }) => {

            // Decimate the series to reduce points for Recharts
            const points = decimateSeries(series);

            // No points, skip
            if (points.length === 0)
                return;

            // Using aligned start times...
            if (useAlignedStartTimes) {

                preparedSeries.push({ seriesKey, startMS: 0, points });
                for (const p of points)
                    timeSet.add(p.t);

            // Otherwise...
            } else { 

                const startMS = flightStartMsById.get(flight.id);

                // Invalid start time, skip this series
                if (startMS === undefined || Number.isNaN(startMS))
                    return;

                preparedSeries.push({ seriesKey, startMS, points });

                for (const p of points) {

                    const absMS = (startMS + p.t * 1_000);
                    timeSet.add(absMS);

                }

            }

        });

        let allTimes = Array.from(timeSet).sort((a, b) => a - b);

        // For aligned mode, force domain to start at 0 and end at the longest flight duration.
        let domainMin: number;
        let domainMax: number;

        if (useAlignedStartTimes) {

            const longestDurationSec = preparedSeries.reduce((max, s) => {
                const last = s.points[s.points.length - 1]?.t ?? 0;
                return Math.max(max, last);
            }, 0);

            domainMin = 0;
            domainMax = longestDurationSec;

            // Ensure last duration is represented in allTimes (for domain/ticks) even if no sample exactly there
            if (domainMax > 0 && (allTimes.length === 0 || allTimes[allTimes.length - 1] !== domainMax)) {
                allTimes = [...allTimes, domainMax].sort((a, b) => a - b);
            }

        } else {

            domainMin = allTimes[0];
            domainMax = allTimes[allTimes.length - 1];

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

                    // Time outside this series' relative range, skip
                    if (time < firstT)
                        continue;
                    if (time > lastT)
                        break;

                    const tSec = time;

                    // Advance i until points[i] is just before tSec
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

                    // Time outside this series' absolute range, skip
                    if (time < firstAbs)
                        continue;
                    if (time > lastAbs)
                        break;

                    const tSec = (time - startMS) / 1_000;

                    // Advance i until points[i] is just before tSec
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
        const config = buildChartConfig(activeSeries);

        /*
            Span in 'minutes' for label granularity:
                - Absolute mode: time is MS
                - Aligned mode: time is seconds
        */
        let domainSpanMinutes: number;
        if (useAlignedStartTimes) {
            domainSpanMinutes = (domainMax - domainMin) / 60;      // Seconds -> Minutes
        } else {
            domainSpanMinutes = (domainMax - domainMin) / 60_000;  // MS -> Minutes
        }

        /*
            Build ticks:
                - Aligned mode: evenly spaced between [0, longestDuration]
                - Absolute mode: evenly spaced between [dataMin, dataMax] with day boundaries
        */
        const ticks: number[] = [];
        const dayBoundaryTimes: number[] = [];

        if (domainMax === domainMin) {

            ticks.push(domainMin);
            if (!useAlignedStartTimes)
                dayBoundaryTimes.push(domainMin);

        } else if (useAlignedStartTimes) {

            const MAX_TICKS = 8;
            const tickCount = MAX_TICKS;
            const span = Math.max(domainMax - domainMin, 1);
            const step = span / (tickCount - 1);

            for (let i = 0; i < tickCount; i++) {
                ticks.push(domainMin + step * i);
            }

        } else {

            const MAX_TICKS = 8;
            const tickCount = Math.min(MAX_TICKS, allTimes.length);

            const min = domainMin;
            const max = domainMax;
            const span = (max - min || 1);
            const step = span / (tickCount - 1);

            let prevDayKey: string | null = null;

            for (let i = 0; i < tickCount; i++) {

                const t = Math.round(min + step * i);
                ticks.push(t);

                const date = new Date(t);
                const key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;

                if (key !== prevDayKey) {
                    dayBoundaryTimes.push(t);
                    prevDayKey = key;
                }

            }

        }

        log("Built chart config: ", config);

        return {
            loading: false,
            hasData: true,
            data,
            config,
            seriesKeys,
            ticks,
            dayBoundaryTimes,
            domainSpanMinutes,
        };
    }, [hasAnySelectedParams, chartFlights, selectedParamsByFlight, chartData.seriesByFlight, useHighContrastCharts, flightStartMsById, useAlignedStartTimes]);

    /*
        Custom tick renderer for the time axis (x-axis)

        Absolute mode:
            - 24-hour times (no AM/PM)
            - Optional seconds
            - Date label under first tick of each day

        Aligned mode:
            - 00:00:00 style
            - Elapsed time from 0
            - No date labels
    */
    const TimeAxisTick = (props: any) => {

        const { x, y, payload } = props;
        const value = payload.value as number;

        const showSeconds = chartModel.domainSpanMinutes <= 10;

        if (useAlignedStartTimes) {
            // value is seconds since start
            const totalSeconds = value;
            const hours = Math.floor(totalSeconds / 3600);
            const minutes = Math.floor((totalSeconds % 3600) / 60);
            const seconds = Math.floor(totalSeconds % 60);

            const pad = (n: number) => n.toString().padStart(2, "0");

            const timeLabel = showSeconds
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

        // Absolute-time mode
        const date = new Date(value);
        const boundarySet = new Set(chartModel.dayBoundaryTimes);
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

        // Using aligned start times, format as elapsed time
        if (useAlignedStartTimes) {

            const SEC_PER_HOUR = 3600;
            const SEC_PER_MIN = 60;
            const DIGITS_PER_FIELD = 2;

            const totalSeconds = numericLabel;
            const hours = Math.floor(totalSeconds / SEC_PER_HOUR);
            const minutes = Math.floor((totalSeconds % SEC_PER_HOUR) / SEC_PER_MIN);
            const seconds = Math.floor(totalSeconds % SEC_PER_MIN);
            const pad = (n: number) => n.toString().padStart(DIGITS_PER_FIELD, "0");

            const timePart = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`

            return `Elapsed ${timePart}`;

        }

        // Otherwise, format as absolute time
        const date = new Date(numericLabel);
        return date.toLocaleTimeString(undefined, {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false,
        });

    }

    const renderNoDataMessage = () => {

        log("No flights data found, rendering empty chart panel.");

        return (
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2"
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
                                <ChartArea size={16} />
                                Chart
                            </div>{" "}
                            button in a flight result row.
                        </div>
                    </AlertDescription>
                </div>
            </motion.div>
        );
    };

    const renderNoParamsMessage = () => {

        log("Flights selected but no parameters chosen.");

        return (
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2"
            >
                <Info />

                <div className="flex flex-col">
                    <AlertTitle>No Parameters Selected</AlertTitle>
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
    };

    const renderLoadingMessage = () => {

        log("Selected parameters, waiting for series data.");

        return (
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
    };

    const renderExpandChartItemsButton = () => {

        log("Rendering expand chart items button.");

        const expandChartItemsModal = () => {

            log("Opening expand chart items modal.");

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
                        <span>{chartFlights.length} Selected</span>

                        {gotChartFlightAdded && <Ping />}
                    </Button>
                </TooltipTrigger>
                <TooltipContent>Expand Chart Items</TooltipContent>
            </Tooltip>
        );

    };

    const renderAlignedStartTimesToggle = () => {

        const toggleAlignedStartTimes = () => {
            setUseAlignedStartTimes((prev) => !prev);
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
                        <span className="text-xs">Align Start Times</span>
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

    const noFlights = (chartFlights.length === 0);

    const legendItems = chartModel.seriesKeys.map((key) => ({
        id: key,
        value: chartModel.config[key]?.label ?? key,
        color: chartModel.config[key]?.color,
        type: "line" as const,
    }));

    return (
        <Card className="border rounded-lg w-full h-full card-glossy relative">

            {/* Legend Overlay */}
            {
                (legendItems.length > 0)
                && (
                    <div
                        className="absolute z-10 flex flex-col gap-1 p-2 rounded bg-muted shadow-sm opacity-50 hover:opacity-100 transition-opacity"
                        style={{ top: 52, right: 8 }}
                    >
                        {
                            legendItems.map((item) => (
                                <div key={item.id} className="flex items-center gap-2 text-xs">
                                    <span
                                        className="inline-block w-3 h-3 rounded-full"
                                        style={{ backgroundColor: item.color }}
                                    />
                                    <span>{item.value}</span>
                                </div>
                            ))
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
                    <>
                        {/* Chart */}
                        <ResponsiveContainer width="100%" height="100%" className="h-full flex p-4">
                            <ChartContainer
                                config={chartModel.config}
                                className="w-full h-full my-auto"
                            >
                                <LineChart
                                    accessibilityLayer
                                    data={chartModel.data}
                                    margin={{
                                        left: 12,
                                        right: 12,
                                    }}
                                >
                                    <CartesianGrid vertical={false} />
                                    <YAxis
                                        type="number"
                                        domain={["auto", "auto"]}
                                        tickLine={false}
                                        axisLine={false}
                                        tickMargin={8}
                                        tick={{ fontSize: 10 }}
                                    />
                                    <XAxis
                                        dataKey="time"
                                        type="number"
                                        scale="time"
                                        domain={["dataMin", "dataMax"]}
                                        ticks={chartModel.ticks}
                                        tickLine={false}
                                        axisLine={false}
                                        tickMargin={8}
                                        tick={TimeAxisTick}
                                    />
                                    <ChartTooltip
                                        cursor={false}
                                        content={<ChartTooltipContent labelFormatter={timeLabelFormatter} />}
                                    />
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
                                </LineChart>
                            </ChartContainer>
                        </ResponsiveContainer>
                    </>
                )}
            </AnimatePresence>

            {
                (!noFlights)
                &&
                <div className="flex gap-2  absolute right-2 top-2 justify-end">
                    {renderAlignedStartTimesToggle()}
                    {renderExpandChartItemsButton()}
                </div>
            }

        </Card>
    );
}
