// ngafid-frontend/src/app/components/ui/chart-interactions.tsx
"use client";

import * as React from "react";

import { getLogger } from "@/components/providers/logger";
import { ChartConfig, ChartContainer } from "@/components/ui/chart";
import { cn } from "@/lib/utils";
import { useTheme } from "@/components/providers/theme-provider";

const log = getLogger("ChartInteractions", "blue", "Component");

export type ChartInteractionZoomMode = "x" | "y" | "xy";

export interface ChartInteractionConfig {
    kind: "cartesian";
    zoom?: ChartInteractionZoomMode;
    pan?: boolean;
    panStrategy?: "live" | "deferred";
    wheelZoom?: boolean;
    selectionZoom?: boolean;
    constrainXDomain?: boolean;
    constrainYDomain?: boolean;
}

export interface ChartInteractionModifierState {
    shiftHeld: boolean;
    ctrlHeld: boolean;
    altHeld: boolean;
    metaHeld: boolean;
}

export interface ChartInteractionPointerContext {
    state: any;
    nativeEvent: MouseEvent | null;
    xValue: number | null;
    yValue: number | null;
    chartX: number | null;
    chartY: number | null;
    plotWidth: number | null;
    plotHeight: number | null;
    activeXDomain: [number, number] | null;
    activeYDomain: [number, number] | null;
    activePayload: Array<any>;
    modifiers: ChartInteractionModifierState;
}

export interface ChartInteractionPointAction {
    id?: string;
    preventDefaultInteraction?: boolean;
    shouldHandle: (context: ChartInteractionPointerContext) => boolean;
    onPoint: (context: ChartInteractionPointerContext) => void;
}

export interface ChartInteractionRegion {
    x1: number;
    x2: number;
    y1: number;
    y2: number;
    mode: ChartInteractionZoomMode;
}

export interface ChartInteractionSelectionState {
    x1: number | null;
    x2: number | null;
    y1: number | null;
    y2: number | null;
}

interface PanState {
    baseXDomain: [number, number];
    baseYDomain: [number, number];
    startClientX: number;
    startClientY: number;
    chartWidth: number;
    chartHeight: number;
}

interface PanPreviewState {
    container: HTMLElement;
    target: HTMLElement;
    overlay: SVGSVGElement;
    items: Array<{
        element: SVGElement;
        previousTransform: string | null;
    }>;
    axisTicks: Array<PanPreviewAxisTick>;
    previousPosition: string;
    previousDeferredPanning: string | null;
}

interface PanPreviewAxisTick {
    axis: "x" | "y";
    coordinate: number;
    index: number;
    axisConfig: any;
    plotStart: number;
    plotSpan: number;
    texts: Array<{
        element: SVGTextElement;
        previousText: string;
    }>;
}

interface PendingPanDomain {
    xDomain: [number, number] | null;
    yDomain: [number, number] | null;
}

interface SelectionMeta {
    startClientX: number;
    startClientY: number;
    startChartX: number;
    startChartY: number;
    startContainerX: number;
    startContainerY: number;
    plotLeft: number;
    plotTop: number;
    chartWidth: number;
    chartHeight: number;
}

interface PointerDownInfo {
    clientX: number;
    clientY: number;
}

interface ZoomAnimationState {
    fromX: [number, number] | null;
    toX: [number, number] | null;
    fromY: [number, number] | null;
    toY: [number, number] | null;
    startTime: number;
    duration: number;
}

interface PendingPointerMove {
    clientX: number;
    clientY: number;
}

export interface UseInteractiveCartesianChartOptions {
    hasData: boolean;
    interaction: ChartInteractionConfig;
    baseXDomain: [number, number] | null;
    baseYDomain: [number, number] | null;
    resetDeps?: React.DependencyList;
    zoomSpeed?: number;
    minSpanFactor?: number;
    animationDurationMs?: number;
    clickThresholdPx?: number;
    pointActions?: Array<ChartInteractionPointAction>;
    resolvePoint?: (
        state: any,
        fallback: { xValue: number | null; yValue: number | null },
    ) => Partial<{ xValue: number | null; yValue: number | null }> | null;
    onPointIntent?: (context: ChartInteractionPointerContext) => void;
    onRegionSelect?: (region: ChartInteractionRegion) => void;
    onInteractionStart?: () => void;
    onInteractionEnd?: () => void;
}

export interface InteractiveCartesianChartResult {
    chartRef: React.RefObject<any>;
    xDomainOverride: [number, number] | null;
    yDomainOverride: [number, number] | null;
    activeXDomain: [number, number] | null;
    activeYDomain: [number, number] | null;
    selection: ChartInteractionSelectionState | null;
    selectionMode: ChartInteractionZoomMode;
    selectionOverlayRef: React.RefObject<HTMLDivElement | null>;
    isInteracting: boolean;
    isDeferredPanning: boolean;
    modifiers: ChartInteractionModifierState;
    cursorClassName: string;
    handleChartMouseDown: (state: any) => void;
    handleChartClick: (state: any) => void;
    handleContainerMouseDown: (event: React.MouseEvent<HTMLElement>) => void;
    handleContainerContextMenu: (event: React.MouseEvent<HTMLElement>) => void;
    handleWheel: (event: React.WheelEvent<HTMLElement>) => void;
    resetView: () => void;
}

export interface InteractiveChartContainerProps extends Omit<React.ComponentProps<"div">, "children">, UseInteractiveCartesianChartOptions {
    config: ChartConfig;
    children: (chart: InteractiveCartesianChartResult) => React.ReactElement;
}

export interface InteractiveChartSelectionOverlayProps {
    previewRef: React.RefObject<HTMLDivElement | null>;
    stroke?: string;
    fill?: string;
    className?: string;
}

const normalizeScaleValueToNumber = (value: any): number => {
    if (value == null)
        return Number.NaN;

    if (value instanceof Date)
        return value.getTime();

    if (typeof value === "number")
        return value;

    const numeric = Number(value);
    return Number.isNaN(numeric) ? Number.NaN : numeric;
};

const zoomModeIncludesX = (mode: ChartInteractionZoomMode) => mode === "x" || mode === "xy";
const zoomModeIncludesY = (mode: ChartInteractionZoomMode) => mode === "y" || mode === "xy";

const getNativeMouseEvent = (state: any): MouseEvent | null => {
    const nativeEvent = state?.nativeEvent;
    return nativeEvent instanceof MouseEvent ? nativeEvent : (nativeEvent ?? null);
};

const clampDomainToBase = (
    domain: [number, number],
    baseDomain: [number, number] | null,
): [number, number] => {
    if (!baseDomain)
        return domain;

    const [baseMin, baseMax] = baseDomain;
    const baseSpan = baseMax - baseMin;
    const domainSpan = domain[1] - domain[0];

    if (!Number.isFinite(baseSpan) || baseSpan <= 0 || !Number.isFinite(domainSpan) || domainSpan <= 0)
        return domain;

    if (domainSpan >= baseSpan)
        return [baseMin, baseMax];

    let [min, max] = domain;

    if (min < baseMin) {
        max += baseMin - min;
        min = baseMin;
    }

    if (max > baseMax) {
        min -= max - baseMax;
        max = baseMax;
    }

    return [min, max];
};

const getFirstAxisConfig = (axisMap: any) => {
    if (!axisMap || typeof axisMap !== "object")
        return null;

    const firstKey = Object.keys(axisMap)[0];
    return firstKey ? axisMap[firstKey] : null;
};

const parseSvgTranslate = (transform: string | null): [number, number] | null => {
    if (!transform)
        return null;

    const match = /translate\(\s*([-+.\d]+)(?:[,\s]+([-+.\d]+))?\s*\)/.exec(transform);
    if (!match)
        return null;

    const x = Number(match[1]);
    const y = Number(match[2] ?? 0);
    return Number.isFinite(x) && Number.isFinite(y) ? [x, y] : null;
};

const getTickCoordinate = (tick: Element, axis: "x" | "y") => {
    const translated = parseSvgTranslate(tick.getAttribute("transform"));
    if (translated)
        return axis === "x" ? translated[0] : translated[1];

    const text = tick.querySelector("text");
    const value = Number(text?.getAttribute(axis));
    return Number.isFinite(value) ? value : null;
};

const formatDurationTick = (value: number, domainSpan: number) => {
    const sign = value < 0 ? "-" : "";
    const absolute = Math.abs(value);
    const hours = Math.floor(absolute / 3600);
    const minutes = Math.floor((absolute % 3600) / 60);
    const seconds = Math.floor(absolute % 60);
    const pad = (n: number) => n.toString().padStart(2, "0");

    if (domainSpan <= 600)
        return `${sign}${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;

    return `${sign}${pad(hours)}:${pad(minutes)}`;
};

const formatDateTimeTick = (value: number, domainSpan: number) => {
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return "";

    return date.toLocaleTimeString(undefined, {
        hour: "2-digit",
        minute: "2-digit",
        second: domainSpan <= 10 * 60 * 1000 ? "2-digit" : undefined,
        hour12: false,
    });
};

const formatDateSecondaryTick = (value: number) => {
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return "";

    return date.toLocaleDateString(undefined, {
        month: "short",
        day: "2-digit",
    });
};

const formatAxisPreviewTick = (
    axis: "x" | "y",
    value: number,
    index: number,
    axisConfig: any,
    domain: [number, number],
) => {
    if (!Number.isFinite(value))
        return "";

    const formatter = axisConfig?.tickFormatter;
    const unit = axisConfig?.unit ?? "";

    if (typeof formatter === "function")
        return `${formatter(value, index)}${unit}`;

    const domainSpan = Math.abs(domain[1] - domain[0]);
    const looksLikeEpochMs = Math.max(Math.abs(domain[0]), Math.abs(domain[1]), Math.abs(value)) > 10_000_000_000;

    if (axis === "x" && looksLikeEpochMs)
        return formatDateTimeTick(value, domainSpan);

    if (axis === "x" && axisConfig?.dataKey === "time" && domainSpan <= 7 * 24 * 3600)
        return formatDurationTick(value, domainSpan);

    return value.toLocaleString(undefined, { maximumFractionDigits: 2 });
};

export function useChartInteractionModifiers(): ChartInteractionModifierState {
    const [modifiers, setModifiers] = React.useState<ChartInteractionModifierState>({
        shiftHeld: false,
        ctrlHeld: false,
        altHeld: false,
        metaHeld: false,
    });

    React.useEffect(() => {
        const updateFromEvent = (event: KeyboardEvent) => {
            setModifiers({
                shiftHeld: event.shiftKey,
                ctrlHeld: event.ctrlKey,
                altHeld: event.altKey,
                metaHeld: event.metaKey,
            });
        };

        const clearModifiers = () => {
            setModifiers({
                shiftHeld: false,
                ctrlHeld: false,
                altHeld: false,
                metaHeld: false,
            });
        };

        window.addEventListener("keydown", updateFromEvent);
        window.addEventListener("keyup", updateFromEvent);
        window.addEventListener("blur", clearModifiers);

        return () => {
            window.removeEventListener("keydown", updateFromEvent);
            window.removeEventListener("keyup", updateFromEvent);
            window.removeEventListener("blur", clearModifiers);
        };
    }, []);

    return modifiers;
}

export function InteractiveChartSelectionOverlay({
    previewRef,
    stroke,
    fill,
    className,
}: InteractiveChartSelectionOverlayProps) {

    const { theme } = useTheme();
    const isDark = (theme === "dark");

    let lineStroke;

    // No stroke color assigned, default to theme color
    if (!stroke)
        lineStroke = (isDark) ? "rgba(255,255,255,0.9)" : "rgba(0,0,0,0.9)";

    // Otherwise, use the provided stroke color
    else
        lineStroke = stroke;

    let areaFill;

    // No fill color assigned, default to theme color
    if (!fill)
        areaFill = isDark ? "rgba(255,255,255,0.2)" : "rgba(0,0,0,0.1)";

    // Otherwise, use the provided fill color
    else
        areaFill = fill;

    return (
        <div
            ref={previewRef}
            aria-hidden="true"
            className={cn("pointer-events-none absolute z-[5] rounded-[2px]", className)}
            style={{
                display: "none",
                left: 0,
                top: 0,
                width: 0,
                height: 0,
                border: `1.5px solid ${lineStroke}`,
                backgroundColor: areaFill,
                boxShadow: `0 0 0 1px ${areaFill}`,
            }}
        />
    );
}

export function InteractiveChartContainer({
    config,
    className,
    children,
    hasData,
    interaction,
    baseXDomain,
    baseYDomain,
    resetDeps,
    zoomSpeed,
    minSpanFactor,
    animationDurationMs,
    clickThresholdPx,
    pointActions,
    resolvePoint,
    onPointIntent,
    onRegionSelect,
    onInteractionStart,
    onInteractionEnd,
    onWheel,
    onMouseDown,
    onContextMenu,
    ...props
}: InteractiveChartContainerProps) {
    const chart = useInteractiveCartesianChart({
        hasData,
        interaction,
        baseXDomain,
        baseYDomain,
        resetDeps,
        zoomSpeed,
        minSpanFactor,
        animationDurationMs,
        clickThresholdPx,
        pointActions,
        resolvePoint,
        onPointIntent,
        onRegionSelect,
        onInteractionStart,
        onInteractionEnd,
    });

    return (
        <ChartContainer
            config={config}
            className={className}
            onWheel={(event) => {
                onWheel?.(event);
                chart.handleWheel(event);
            }}
            onMouseDown={(event) => {
                onMouseDown?.(event);
                chart.handleContainerMouseDown(event);
            }}
            onContextMenu={(event) => {
                onContextMenu?.(event);
                chart.handleContainerContextMenu(event);
            }}
            {...props}
        >
            {children(chart)}
        </ChartContainer>
    );
}

export function useInteractiveCartesianChart({
    hasData,
    interaction,
    baseXDomain,
    baseYDomain,
    resetDeps,
    zoomSpeed = 0.005,
    minSpanFactor = 0.001,
    animationDurationMs = 140,
    clickThresholdPx = 4,
    pointActions,
    resolvePoint,
    onPointIntent,
    onRegionSelect,
    onInteractionStart,
    onInteractionEnd,
}: UseInteractiveCartesianChartOptions): InteractiveCartesianChartResult {
    const chartRef = React.useRef<any>(null);
    const modifiers = useChartInteractionModifiers();
    const modifiersRef = React.useRef(modifiers);
    const callbackRef = React.useRef({
        onPointIntent,
        onRegionSelect,
        onInteractionStart,
        onInteractionEnd,
        resolvePoint,
        pointActions,
    });

    const [xDomainOverride, setXDomainOverride] = React.useState<[number, number] | null>(null);
    const [yDomainOverride, setYDomainOverride] = React.useState<[number, number] | null>(null);
    const [selection, setSelection] = React.useState<ChartInteractionSelectionState | null>(null);
    const [isInteracting, setIsInteractingState] = React.useState(false);
    const [isDeferredPanning, setIsDeferredPanning] = React.useState(false);

    const isInteractingRef = React.useRef(false);
    const deferredPanActiveRef = React.useRef(false);
    const selectionOverlayRef = React.useRef<HTMLDivElement | null>(null);
    const selectionRef = React.useRef<ChartInteractionSelectionState | null>(null);
    const isDraggingRef = React.useRef(false);
    const panStateRef = React.useRef<PanState | null>(null);
    const panPreviewStateRef = React.useRef<PanPreviewState | null>(null);
    const pendingPanDomainRef = React.useRef<PendingPanDomain | null>(null);
    const selectionMetaRef = React.useRef<SelectionMeta | null>(null);
    const pointerDownInfoRef = React.useRef<PointerDownInfo | null>(null);
    const dragExceededThresholdRef = React.useRef(false);
    const ignoreNextClickRef = React.useRef(false);
    const wheelInteractingTimeoutRef = React.useRef<number | null>(null);
    const pointerMoveRafRef = React.useRef<number | null>(null);
    const pendingPointerMoveRef = React.useRef<PendingPointerMove | null>(null);
    const zoomAnimRafRef = React.useRef<number | null>(null);
    const zoomAnimStateRef = React.useRef<ZoomAnimationState | null>(null);
    const previousBodyUserSelectRef = React.useRef<string | null>(null);

    const zoomMode = interaction.zoom ?? "xy";
    const panEnabled = interaction.pan ?? true;
    const panStrategy = interaction.panStrategy ?? "live";
    const wheelZoomEnabled = interaction.wheelZoom ?? true;
    const selectionZoomEnabled = interaction.selectionZoom ?? true;
    const constrainXDomain = interaction.constrainXDomain ?? false;
    const constrainYDomain = interaction.constrainYDomain ?? false;
    const allowedX = zoomModeIncludesX(zoomMode);
    const allowedY = zoomModeIncludesY(zoomMode);

    const hasBaseDomains =
        hasData
        && baseXDomain != null
        && baseYDomain != null
        && Number.isFinite(baseXDomain[0])
        && Number.isFinite(baseXDomain[1])
        && Number.isFinite(baseYDomain[0])
        && Number.isFinite(baseYDomain[1]);

    const activeXDomain = hasBaseDomains ? (xDomainOverride ?? baseXDomain) : null;
    const activeYDomain = hasBaseDomains ? (yDomainOverride ?? baseYDomain) : null;

    const updateSelection = React.useCallback((
        next:
            | ChartInteractionSelectionState
            | null
            | ((current: ChartInteractionSelectionState | null) => ChartInteractionSelectionState | null),
    ) => {
        const resolved = typeof next === "function" ? next(selectionRef.current) : next;
        selectionRef.current = resolved;
        setSelection(resolved);
    }, []);

    const updateSelectionRef = React.useCallback((
        next:
            | ChartInteractionSelectionState
            | null
            | ((current: ChartInteractionSelectionState | null) => ChartInteractionSelectionState | null),
    ) => {
        const resolved = typeof next === "function" ? next(selectionRef.current) : next;
        selectionRef.current = resolved;
    }, []);

    const hideSelectionPreview = React.useCallback(() => {
        const overlay = selectionOverlayRef.current;
        if (!overlay)
            return;

        overlay.style.display = "none";
        overlay.style.width = "0px";
        overlay.style.height = "0px";
    }, []);

    const updateSelectionPreview = React.useCallback((selectionMeta: SelectionMeta, clientX: number, clientY: number) => {
        const overlay = selectionOverlayRef.current;
        if (!overlay)
            return;

        const {
            startClientX,
            startClientY,
            startContainerX,
            startContainerY,
            plotLeft,
            plotTop,
            chartWidth,
            chartHeight,
        } = selectionMeta;

        if (chartWidth <= 0 || chartHeight <= 0)
            return;

        const plotRight = plotLeft + chartWidth;
        const plotBottom = plotTop + chartHeight;
        const currentContainerX = Math.max(
            plotLeft,
            Math.min(plotRight, startContainerX + (clientX - startClientX)),
        );
        const currentContainerY = Math.max(
            plotTop,
            Math.min(plotBottom, startContainerY + (clientY - startClientY)),
        );

        let left = Math.min(startContainerX, currentContainerX);
        let top = Math.min(startContainerY, currentContainerY);
        let width = Math.abs(currentContainerX - startContainerX);
        let height = Math.abs(currentContainerY - startContainerY);

        if (zoomMode === "x") {
            top = plotTop;
            height = chartHeight;
        } else if (zoomMode === "y") {
            left = plotLeft;
            width = chartWidth;
        }

        overlay.style.display = "block";
        overlay.style.left = `${left}px`;
        overlay.style.top = `${top}px`;
        overlay.style.width = `${Math.max(1, width)}px`;
        overlay.style.height = `${Math.max(1, height)}px`;
    }, [zoomMode]);

    const getPanPreviewElements = React.useCallback(() => {
        const chartAny = chartRef.current as any;
        const root = chartAny?.container as HTMLElement | undefined;

        if (!root)
            return null;

        const target = root.classList.contains("recharts-wrapper")
            ? root
            : (
                root.querySelector(".recharts-wrapper")
                ?? root.closest(".recharts-wrapper")
            ) as HTMLElement | null;

        if (!target)
            return null;

        const container = (target.closest("[data-chart]") as HTMLElement | null) ?? target.parentElement ?? target;
        return { container, target };
    }, []);

    const getPanPreviewAxisTicks = React.useCallback((target: HTMLElement): Array<PanPreviewAxisTick> => {
        const chartAny = chartRef.current as any;
        const offset = chartAny?.state?.offset;

        if (
            !offset
            || typeof offset.left !== "number"
            || typeof offset.top !== "number"
            || typeof offset.width !== "number"
            || typeof offset.height !== "number"
        )
            return [];

        const xAxisConfig = getFirstAxisConfig(chartAny?.state?.xAxisMap);
        const yAxisConfig = getFirstAxisConfig(chartAny?.state?.yAxisMap);

        const collect = (
            axis: "x" | "y",
            selector: string,
            axisConfig: any,
            plotStart: number,
            plotSpan: number,
        ) => Array.from(target.querySelectorAll(selector))
            .map((tick, index): PanPreviewAxisTick | null => {
                const coordinate = getTickCoordinate(tick, axis);
                if (coordinate === null)
                    return null;

                const texts = Array.from(tick.querySelectorAll("text")) as Array<SVGTextElement>;
                if (texts.length === 0)
                    return null;

                return {
                    axis,
                    coordinate,
                    index,
                    axisConfig,
                    plotStart,
                    plotSpan,
                    texts: texts.map((element) => ({
                        element,
                        previousText: element.textContent ?? "",
                    })),
                };
            })
            .filter((tick): tick is PanPreviewAxisTick => tick !== null);

        return [
            ...collect(
                "x",
                ".recharts-xAxis .recharts-cartesian-axis-tick, .xAxis .recharts-cartesian-axis-tick",
                xAxisConfig,
                offset.left,
                offset.width,
            ),
            ...collect(
                "y",
                ".recharts-yAxis .recharts-cartesian-axis-tick, .yAxis .recharts-cartesian-axis-tick",
                yAxisConfig,
                offset.top,
                offset.height,
            ),
        ];
    }, []);

    const beginPanPreview = React.useCallback(() => {
        if (panStrategy !== "deferred" || panPreviewStateRef.current)
            return false;

        const elements = getPanPreviewElements();
        if (!elements)
            return false;

        const { container, target } = elements;
        const containerRect = container.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();
        const surface = target.querySelector("svg.recharts-surface") as SVGSVGElement | null;
        if (!surface)
            return false;

        const contentSelector = [
            ".recharts-line",
            ".recharts-area",
            ".recharts-bar",
            ".recharts-scatter",
            ".recharts-reference-area",
            ".recharts-reference-line",
            ".recharts-reference-dot",
        ].join(",");
        const overlay = surface.cloneNode(true) as SVGSVGElement;
        overlay.querySelectorAll("defs, .recharts-cartesian-grid, .recharts-cartesian-axis, .recharts-tooltip-cursor").forEach((element) => {
            element.remove();
        });

        const contentElements = Array.from(overlay.querySelectorAll(contentSelector)) as Array<SVGElement>;
        const contentSet = new Set(contentElements);
        const items = contentElements
            .filter((element) => {
                let parent = element.parentElement;
                while (parent) {
                    if (contentSet.has(parent as SVGElement))
                        return false;

                    parent = parent.parentElement;
                }

                return true;
            })
            .map((element) => ({
                element,
                previousTransform: element.getAttribute("transform"),
            }));

        if (items.length === 0)
            return false;

        overlay.setAttribute("aria-hidden", "true");
        overlay.style.position = "absolute";
        overlay.style.pointerEvents = "none";
        overlay.style.zIndex = "2";

        panPreviewStateRef.current = {
            container,
            target,
            overlay,
            items,
            axisTicks: getPanPreviewAxisTicks(target),
            previousPosition: container.style.position,
            previousDeferredPanning: target.getAttribute("data-chart-deferred-panning"),
        };

        if (window.getComputedStyle(container).position === "static")
            container.style.position = "relative";

        target.setAttribute("data-chart-deferred-panning", "true");

        overlay.style.left = `${targetRect.left - containerRect.left}px`;
        overlay.style.top = `${targetRect.top - containerRect.top}px`;
        overlay.style.width = `${targetRect.width}px`;
        overlay.style.height = `${targetRect.height}px`;
        overlay.style.overflow = "hidden";
        container.appendChild(overlay);
        return true;
    }, [getPanPreviewAxisTicks, getPanPreviewElements, panStrategy]);

    const updatePanPreview = React.useCallback((
        dx: number,
        dy: number,
        xDomain: [number, number] | null,
        yDomain: [number, number] | null,
    ) => {
        const preview = panPreviewStateRef.current;
        if (!preview)
            return;

        const translate = `translate(${dx} ${dy})`;
        preview.items.forEach(({ element, previousTransform }) => {
            element.setAttribute("transform", previousTransform ? `${previousTransform} ${translate}` : translate);
        });

        preview.axisTicks.forEach((tick) => {
            const domain = tick.axis === "x" ? xDomain : yDomain;
            if (!domain || tick.plotSpan <= 0)
                return;

            const span = domain[1] - domain[0];
            const ratio = (tick.coordinate - tick.plotStart) / tick.plotSpan;
            const value = tick.axis === "x"
                ? domain[0] + (ratio * span)
                : domain[1] - (ratio * span);

            tick.texts.forEach(({ element }, textIndex) => {
                element.textContent = textIndex === 0
                    ? formatAxisPreviewTick(tick.axis, value, tick.index, tick.axisConfig, domain)
                    : formatDateSecondaryTick(value);
            });
        });
    }, []);

    const clearPanPreview = React.useCallback((restoreAxisTicks = true) => {
        const preview = panPreviewStateRef.current;
        if (!preview)
            return;

        if (restoreAxisTicks) {
            preview.axisTicks.forEach((tick) => {
                tick.texts.forEach(({ element, previousText }) => {
                    element.textContent = previousText;
                });
            });
        }

        preview.overlay.remove();
        preview.container.style.position = preview.previousPosition;
        if (preview.previousDeferredPanning === null)
            preview.target.removeAttribute("data-chart-deferred-panning");
        else
            preview.target.setAttribute("data-chart-deferred-panning", preview.previousDeferredPanning);

        panPreviewStateRef.current = null;
    }, []);

    const schedulePanPreviewClear = React.useCallback((restoreAxisTicks = false) => {
        if (typeof window === "undefined") {
            clearPanPreview(restoreAxisTicks);
            return;
        }

        window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => {
                clearPanPreview(restoreAxisTicks);
            });
        });
    }, [clearPanPreview]);

    const setDocumentSelectionDisabled = React.useCallback((disabled: boolean) => {
        if (typeof document === "undefined")
            return;

        if (disabled) {
            if (previousBodyUserSelectRef.current === null)
                previousBodyUserSelectRef.current = document.body.style.userSelect;

            document.body.style.userSelect = "none";
            return;
        }

        if (previousBodyUserSelectRef.current !== null) {
            document.body.style.userSelect = previousBodyUserSelectRef.current;
            previousBodyUserSelectRef.current = null;
        }
    }, []);

    React.useEffect(() => {
        modifiersRef.current = modifiers;
    }, [modifiers]);

    React.useEffect(() => {
        callbackRef.current = {
            onPointIntent,
            onRegionSelect,
            onInteractionStart,
            onInteractionEnd,
            resolvePoint,
            pointActions,
        };
    }, [onPointIntent, onRegionSelect, onInteractionStart, onInteractionEnd, resolvePoint, pointActions]);

    const setInteracting = React.useCallback((next: boolean) => {
        if (isInteractingRef.current === next)
            return;

        isInteractingRef.current = next;
        setIsInteractingState(next);
        setDocumentSelectionDisabled(next);

        if (next)
            callbackRef.current.onInteractionStart?.();
        else
            callbackRef.current.onInteractionEnd?.();
    }, [setDocumentSelectionDisabled]);

    const cancelZoomAnimation = React.useCallback(() => {
        if (zoomAnimRafRef.current !== null) {
            window.cancelAnimationFrame(zoomAnimRafRef.current);
            zoomAnimRafRef.current = null;
        }

        zoomAnimStateRef.current = null;
    }, []);

    const cleanupInteraction = React.useCallback((cancelSelection = true) => {
        isDraggingRef.current = false;
        panStateRef.current = null;
        deferredPanActiveRef.current = false;
        setIsDeferredPanning(false);
        pendingPanDomainRef.current = null;
        selectionMetaRef.current = null;
        pointerDownInfoRef.current = null;
        dragExceededThresholdRef.current = false;
        hideSelectionPreview();
        clearPanPreview();

        if (cancelSelection)
            updateSelection(null);

        setInteracting(false);
    }, [clearPanPreview, hideSelectionPreview, setInteracting, updateSelection]);

    const resetView = React.useCallback(() => {
        cancelZoomAnimation();
        setXDomainOverride(null);
        setYDomainOverride(null);
        cleanupInteraction(true);
    }, [cancelZoomAnimation, cleanupInteraction]);

    React.useEffect(
        resetView,
        [hasBaseDomains, baseXDomain?.[0], baseXDomain?.[1], baseYDomain?.[0], baseYDomain?.[1], ...(resetDeps ?? [])],
    );

    React.useEffect(() => {
        return () => {
            if (wheelInteractingTimeoutRef.current !== null)
                window.clearTimeout(wheelInteractingTimeoutRef.current);

            if (pointerMoveRafRef.current !== null)
                window.cancelAnimationFrame(pointerMoveRafRef.current);

            setDocumentSelectionDisabled(false);
            hideSelectionPreview();
            clearPanPreview();
            cancelZoomAnimation();
        };
    }, [cancelZoomAnimation, clearPanPreview, hideSelectionPreview, setDocumentSelectionDisabled]);

    const startZoomAnimation = React.useCallback((
        nextXDomain: [number, number] | null,
        nextYDomain: [number, number] | null,
        currentXDomain: [number, number] | null,
        currentYDomain: [number, number] | null,
    ) => {
        const hasX = nextXDomain !== null && currentXDomain !== null;
        const hasY = nextYDomain !== null && currentYDomain !== null;

        if (!hasX && !hasY)
            return;

        cancelZoomAnimation();

        if (animationDurationMs <= 0) {
            if (nextXDomain)
                setXDomainOverride(nextXDomain);

            if (nextYDomain)
                setYDomainOverride(nextYDomain);

            return;
        }

        zoomAnimStateRef.current = {
            fromX: hasX ? currentXDomain : null,
            toX: hasX ? nextXDomain : null,
            fromY: hasY ? currentYDomain : null,
            toY: hasY ? nextYDomain : null,
            startTime: performance.now(),
            duration: animationDurationMs,
        };

        const step = () => {
            const zoomAnimStateCurrent = zoomAnimStateRef.current;
            if (!zoomAnimStateCurrent)
                return;

            const now = performance.now();
            const tRaw = (now - zoomAnimStateCurrent.startTime) / zoomAnimStateCurrent.duration;
            const t = Math.max(0.0, Math.min(1.0, tRaw));
            const eased = 1.0 - Math.pow(1.0 - t, 3.0);

            if (zoomAnimStateCurrent.fromX && zoomAnimStateCurrent.toX) {
                const [fx0, fx1] = zoomAnimStateCurrent.fromX;
                const [tx0, tx1] = zoomAnimStateCurrent.toX;
                setXDomainOverride([
                    fx0 + ((tx0 - fx0) * eased),
                    fx1 + ((tx1 - fx1) * eased),
                ]);
            }

            if (zoomAnimStateCurrent.fromY && zoomAnimStateCurrent.toY) {
                const [fy0, fy1] = zoomAnimStateCurrent.fromY;
                const [ty0, ty1] = zoomAnimStateCurrent.toY;
                setYDomainOverride([
                    fy0 + ((ty0 - fy0) * eased),
                    fy1 + ((ty1 - fy1) * eased),
                ]);
            }

            if (t < 1.0) {
                zoomAnimRafRef.current = window.requestAnimationFrame(step);
                return;
            }

            if (zoomAnimStateCurrent.toX)
                setXDomainOverride(zoomAnimStateCurrent.toX);

            if (zoomAnimStateCurrent.toY)
                setYDomainOverride(zoomAnimStateCurrent.toY);

            zoomAnimStateRef.current = null;
            zoomAnimRafRef.current = null;
        };

        zoomAnimRafRef.current = window.requestAnimationFrame(step);
    }, [animationDurationMs, cancelZoomAnimation]);

    const getChartScales = React.useCallback(() => {
        const chartAny = chartRef.current as any;

        if (
            !chartAny
            || typeof chartAny.getXScales !== "function"
            || typeof chartAny.getYScales !== "function"
        )
            return null;

        const firstScale = (obj: any) => {
            if (!obj)
                return null;

            const keys = Object.keys(obj);
            if (keys.length === 0)
                return null;

            return obj[keys[0]!];
        };

        const xScale = firstScale(chartAny.getXScales());
        const yScale = firstScale(chartAny.getYScales());

        if (
            !xScale
            || !yScale
            || typeof xScale.invert !== "function"
            || typeof yScale.invert !== "function"
        )
            return null;

        return { xScale, yScale };
    }, []);

    const getChartOffset = React.useCallback(() => {
        const chartAny = chartRef.current as any;
        const offset = chartAny?.state?.offset;

        if (
            !offset
            || typeof offset.width !== "number"
            || typeof offset.height !== "number"
        )
            return null;

        return offset as {
            left: number;
            top: number;
            width: number;
            height: number;
        };
    }, []);

    const getChartContainerRect = React.useCallback((): DOMRect | null => {
        const chartAny = chartRef.current as any;
        const container: HTMLElement | undefined = chartAny?.container;

        if (!container)
            return null;

        return container.getBoundingClientRect();
    }, []);

    const buildPointerContext = React.useCallback((state: any): ChartInteractionPointerContext => {
        const nativeEvent = getNativeMouseEvent(state);
        const scales = getChartScales();
        const chartOffset = getChartOffset();
        const chartX = typeof state?.chartX === "number" ? state.chartX : null;
        const chartY = typeof state?.chartY === "number" ? state.chartY : null;
        let xValue: number | null = null;
        let yValue: number | null = null;

        if (scales && chartX !== null) {
            const scaleValue = normalizeScaleValueToNumber(scales.xScale.invert(chartX));
            xValue = Number.isFinite(scaleValue) ? scaleValue : null;
        }

        if (scales && chartY !== null) {
            const scaleValue = normalizeScaleValueToNumber(scales.yScale.invert(chartY));
            yValue = Number.isFinite(scaleValue) ? scaleValue : null;
        }

        const resolved = callbackRef.current.resolvePoint?.(state, { xValue, yValue });
        if (resolved && "xValue" in resolved)
            xValue = resolved.xValue ?? null;

        if (resolved && "yValue" in resolved)
            yValue = resolved.yValue ?? null;

        const eventModifiers = {
            shiftHeld: !!nativeEvent?.shiftKey || modifiersRef.current.shiftHeld,
            ctrlHeld: !!nativeEvent?.ctrlKey || modifiersRef.current.ctrlHeld,
            altHeld: !!nativeEvent?.altKey || modifiersRef.current.altHeld,
            metaHeld: !!nativeEvent?.metaKey || modifiersRef.current.metaHeld,
        };

        return {
            state,
            nativeEvent,
            xValue,
            yValue,
            chartX,
            chartY,
            plotWidth: chartOffset?.width ?? null,
            plotHeight: chartOffset?.height ?? null,
            activeXDomain,
            activeYDomain,
            activePayload: Array.isArray(state?.activePayload) ? state.activePayload : [],
            modifiers: eventModifiers,
        };
    }, [activeXDomain, activeYDomain, getChartOffset, getChartScales]);

    const cancelCurrentOperation = React.useCallback((event?: { preventDefault?: () => void }) => {
        event?.preventDefault?.();
        log("Cancelling chart interaction.");
        cancelZoomAnimation();
        cleanupInteraction(true);
    }, [cancelZoomAnimation, cleanupInteraction]);

    const handleContainerMouseDown = React.useCallback((event: React.MouseEvent<HTMLElement>) => {
        if (event.button === 2)
            cancelCurrentOperation(event);
    }, [cancelCurrentOperation]);

    const handleContainerContextMenu = React.useCallback((event: React.MouseEvent<HTMLElement>) => {
        event.preventDefault();
        cancelCurrentOperation(event);
    }, [cancelCurrentOperation]);

    const handleChartMouseDown = React.useCallback((state: any) => {
        if (!hasBaseDomains)
            return;

        const nativeEvent = getNativeMouseEvent(state);
        const mouseButton = Number(nativeEvent?.button ?? 0);

        if (mouseButton === 2) {
            cancelCurrentOperation(nativeEvent ?? undefined);
            return;
        }

        if (mouseButton !== 0)
            return;

        nativeEvent?.preventDefault?.();

        if (typeof nativeEvent?.clientX === "number" && typeof nativeEvent?.clientY === "number") {
            pointerDownInfoRef.current = {
                clientX: nativeEvent.clientX,
                clientY: nativeEvent.clientY,
            };
            dragExceededThresholdRef.current = false;
        }

        const context = buildPointerContext(state);
        const preventedByAction = (callbackRef.current.pointActions ?? []).some((action) => (
            action.preventDefaultInteraction === true && action.shouldHandle(context)
        ));

        if (preventedByAction)
            return;

        if (typeof state?.chartX !== "number" || typeof state?.chartY !== "number")
            return;

        const scales = getChartScales();
        if (!scales)
            return;

        const xValue = normalizeScaleValueToNumber(scales.xScale.invert(state.chartX));
        const yValue = normalizeScaleValueToNumber(scales.yScale.invert(state.chartY));

        if (!Number.isFinite(xValue) || !Number.isFinite(yValue)) {
            log.warn("Invalid chart interaction scale inversion.", { xValue, yValue });
            return;
        }

        const chartOffset = getChartOffset();
        const chartRegion = getChartContainerRect();

        if (!chartOffset || chartOffset.width <= 0 || chartOffset.height <= 0 || !chartRegion) {
            log.warn("Invalid chart offset or container rect.", { chartOffset, chartRegion });
            return;
        }

        const startClientX = typeof nativeEvent?.clientX === "number"
            ? nativeEvent.clientX
            : chartRegion.left + state.chartX;
        const startClientY = typeof nativeEvent?.clientY === "number"
            ? nativeEvent.clientY
            : chartRegion.top + state.chartY;
        const startContainerX = Math.max(
            chartOffset.left,
            Math.min(chartOffset.left + chartOffset.width, startClientX - chartRegion.left),
        );
        const startContainerY = Math.max(
            chartOffset.top,
            Math.min(chartOffset.top + chartOffset.height, startClientY - chartRegion.top),
        );
        const currentViewXDomain: [number, number] | null = xDomainOverride ?? baseXDomain;
        const currentViewYDomain: [number, number] | null = yDomainOverride ?? baseYDomain;
        const wantsPan = context.modifiers.shiftHeld;

        cancelZoomAnimation();

        if (wantsPan) {
            if (!panEnabled || !currentViewXDomain || !currentViewYDomain)
                return;

            isDraggingRef.current = true;
            setInteracting(true);
            panStateRef.current = {
                baseXDomain: currentViewXDomain,
                baseYDomain: currentViewYDomain,
                startClientX,
                startClientY,
                chartWidth: chartOffset.width,
                chartHeight: chartOffset.height,
            };
            selectionMetaRef.current = null;
            updateSelection(null);
            pendingPanDomainRef.current = null;
            const deferredPanActive = beginPanPreview();
            deferredPanActiveRef.current = deferredPanActive;
            setIsDeferredPanning(deferredPanActive);
            return;
        }

        if (!selectionZoomEnabled)
            return;

        isDraggingRef.current = true;
        setInteracting(true);
        panStateRef.current = null;
        selectionMetaRef.current = {
            startClientX,
            startClientY,
            startChartX: state.chartX,
            startChartY: state.chartY,
            startContainerX,
            startContainerY,
            plotLeft: chartOffset.left,
            plotTop: chartOffset.top,
            chartWidth: chartOffset.width,
            chartHeight: chartOffset.height,
        };

        updateSelection({
            x1: xValue,
            y1: yValue,
            x2: xValue,
            y2: yValue,
        });
        updateSelectionPreview(selectionMetaRef.current, startClientX, startClientY);
    }, [
        hasBaseDomains,
        buildPointerContext,
        getChartScales,
        getChartOffset,
        getChartContainerRect,
        xDomainOverride,
        yDomainOverride,
        baseXDomain,
        baseYDomain,
        cancelZoomAnimation,
        panEnabled,
        selectionZoomEnabled,
        setInteracting,
        cancelCurrentOperation,
        beginPanPreview,
        updateSelection,
        updateSelectionPreview,
    ]);

    const handleChartClick = React.useCallback((state: any) => {
        if (ignoreNextClickRef.current) {
            ignoreNextClickRef.current = false;
            return;
        }

        const context = buildPointerContext(state);

        for (const action of callbackRef.current.pointActions ?? []) {
            if (action.shouldHandle(context)) {
                action.onPoint(context);
                return;
            }
        }

        callbackRef.current.onPointIntent?.(context);
    }, [buildPointerContext]);

    const handleWheel = React.useCallback((event: React.WheelEvent<HTMLElement>) => {
        if (!hasBaseDomains || !wheelZoomEnabled || isDraggingRef.current)
            return;

        const chartOffset = getChartOffset();
        const chartRegion = getChartContainerRect();

        if (!chartOffset || !chartRegion)
            return;

        const clientX = event.clientX;
        const clientY = event.clientY;
        const containerX = clientX - chartRegion.left;
        const containerY = clientY - chartRegion.top;
        const chartX = containerX - chartOffset.left;
        const chartY = containerY - chartOffset.top;

        const outOfRange = (
            chartX < 0
            || chartX > chartOffset.width
            || chartY < 0
            || chartY > chartOffset.height
        );

        if (outOfRange)
            return;

        const deltaY = event.deltaY;
        if (!Number.isFinite(deltaY) || deltaY === 0)
            return;

        const scales = getChartScales();
        if (!scales)
            return;

        let applyX = false;
        let applyY = false;

        if (event.ctrlKey || event.metaKey) {
            applyY = allowedY;
        } else if (event.shiftKey) {
            applyX = allowedX;
        } else {
            applyX = allowedX;
            applyY = allowedY;
        }

        if (!applyX && !applyY)
            return;

        let zoomFactor = Math.exp(deltaY * zoomSpeed);
        zoomFactor = Math.max(0.2, Math.min(zoomFactor, 5));

        const [baseXMin, baseXMax] = baseXDomain!;
        const [baseYMin, baseYMax] = baseYDomain!;
        const baseXSpan = baseXMax - baseXMin || 1;
        const baseYSpan = baseYMax - baseYMin || 1;
        const minXSpan = baseXSpan * minSpanFactor;
        const minYSpan = baseYSpan * minSpanFactor;
        const currentXDomain: [number, number] | null = xDomainOverride ?? baseXDomain;
        const currentYDomain: [number, number] | null = yDomainOverride ?? baseYDomain;
        let nextXDomain: [number, number] | null = null;
        let nextYDomain: [number, number] | null = null;

        if (applyX && currentXDomain) {
            const xCenter = normalizeScaleValueToNumber(scales.xScale.invert(chartX));
            if (Number.isFinite(xCenter)) {
                const [currXMin, currXMax] = currentXDomain;
                const currXSpan = currXMax - currXMin;
                if (Number.isFinite(currXSpan) && currXSpan > 0) {
                    const newXSpan = Math.max(minXSpan, currXSpan * zoomFactor);
                    const relX = (xCenter - currXMin) / currXSpan;
                    const xMin = xCenter - (relX * newXSpan);
                    const xMax = xMin + newXSpan;
                    if (Number.isFinite(xMin) && Number.isFinite(xMax))
                        nextXDomain = constrainXDomain
                            ? clampDomainToBase([xMin, xMax], baseXDomain)
                            : [xMin, xMax];
                }
            }
        }

        if (applyY && currentYDomain) {
            const yCenter = normalizeScaleValueToNumber(scales.yScale.invert(chartY));
            if (Number.isFinite(yCenter)) {
                const [currYMin, currYMax] = currentYDomain;
                const currYSpan = currYMax - currYMin;
                if (Number.isFinite(currYSpan) && currYSpan > 0) {
                    const newYSpan = Math.max(minYSpan, currYSpan * zoomFactor);
                    const relY = (yCenter - currYMin) / currYSpan;
                    const yMin = yCenter - (relY * newYSpan);
                    const yMax = yMin + newYSpan;
                    if (Number.isFinite(yMin) && Number.isFinite(yMax))
                        nextYDomain = constrainYDomain
                            ? clampDomainToBase([yMin, yMax], baseYDomain)
                            : [yMin, yMax];
                }
            }
        }

        if (!nextXDomain && !nextYDomain)
            return;

        setInteracting(true);

        if (wheelInteractingTimeoutRef.current !== null)
            window.clearTimeout(wheelInteractingTimeoutRef.current);

        wheelInteractingTimeoutRef.current = window.setTimeout(() => {
            setInteracting(false);
            wheelInteractingTimeoutRef.current = null;
        }, 150);

        event.preventDefault();
        startZoomAnimation(nextXDomain, nextYDomain, currentXDomain, currentYDomain);
    }, [
        hasBaseDomains,
        wheelZoomEnabled,
        getChartOffset,
        getChartContainerRect,
        getChartScales,
        allowedX,
        allowedY,
        zoomSpeed,
        baseXDomain,
        baseYDomain,
        minSpanFactor,
        xDomainOverride,
        yDomainOverride,
        constrainXDomain,
        constrainYDomain,
        setInteracting,
        startZoomAnimation,
    ]);

    const processPointerMove = React.useCallback((clientX: number, clientY: number) => {
        if (!isDraggingRef.current)
            return;

        const panState = panStateRef.current;
        const selectionMeta = selectionMetaRef.current;
        const activeSelection = selectionRef.current;

        if (activeSelection && selectionMeta && !panState) {
            const scales = getChartScales();
            if (!scales)
                return;

            const {
                startClientX,
                startClientY,
                startChartX,
                startChartY,
                chartWidth,
                chartHeight,
            } = selectionMeta;

            if (chartWidth <= 0 || chartHeight <= 0)
                return;

            let chartX = startChartX + (clientX - startClientX);
            let chartY = startChartY + (clientY - startClientY);
            chartX = Math.max(0, Math.min(chartWidth, chartX));
            chartY = Math.max(0, Math.min(chartHeight, chartY));
            updateSelectionPreview(selectionMeta, clientX, clientY);

            const xValue = normalizeScaleValueToNumber(scales.xScale.invert(chartX));
            const yValue = normalizeScaleValueToNumber(scales.yScale.invert(chartY));

            if (!Number.isFinite(xValue) || !Number.isFinite(yValue))
                return;

            updateSelectionRef((prev) => prev
                ? { ...prev, x2: xValue, y2: yValue }
                : prev,
            );
            return;
        }

        if (!panState)
            return;

        const {
            startClientX,
            startClientY,
            baseXDomain: panBaseXDomain,
            baseYDomain: panBaseYDomain,
            chartWidth,
            chartHeight,
        } = panState;

        if (chartWidth <= 0 || chartHeight <= 0)
            return;

        const dxFraction = (clientX - startClientX) / chartWidth;
        const dyFraction = (clientY - startClientY) / chartHeight;
        const xSpan = panBaseXDomain[1] - panBaseXDomain[0];
        const ySpan = panBaseYDomain[1] - panBaseYDomain[0];

        if (!Number.isFinite(xSpan) || xSpan === 0 || !Number.isFinite(ySpan) || ySpan === 0)
            return;

        cancelZoomAnimation();

        const nextXDomain = allowedX
            ? [
                panBaseXDomain[0] - (dxFraction * xSpan),
                panBaseXDomain[1] - (dxFraction * xSpan),
            ] as [number, number]
            : null;

        const nextYDomain = allowedY
            ? [
                panBaseYDomain[0] + (dyFraction * ySpan),
                panBaseYDomain[1] + (dyFraction * ySpan),
            ] as [number, number]
            : null;

        const constrainedXDomain = nextXDomain
            ? (constrainXDomain ? clampDomainToBase(nextXDomain, baseXDomain) : nextXDomain)
            : null;
        const constrainedYDomain = nextYDomain
            ? (constrainYDomain ? clampDomainToBase(nextYDomain, baseYDomain) : nextYDomain)
            : null;

        if (panStrategy === "deferred" && deferredPanActiveRef.current) {
            pendingPanDomainRef.current = {
                xDomain: constrainedXDomain,
                yDomain: constrainedYDomain,
            };
            updatePanPreview(
                allowedX ? clientX - startClientX : 0,
                allowedY ? clientY - startClientY : 0,
                constrainedXDomain,
                constrainedYDomain,
            );
            return;
        }

        React.startTransition(() => {
            if (constrainedXDomain)
                setXDomainOverride(constrainedXDomain);

            if (constrainedYDomain)
                setYDomainOverride(constrainedYDomain);
        });
    }, [
        getChartScales,
        cancelZoomAnimation,
        allowedX,
        allowedY,
        constrainXDomain,
        constrainYDomain,
        baseXDomain,
        baseYDomain,
        panStrategy,
        updatePanPreview,
        updateSelectionPreview,
        updateSelectionRef,
    ]);

    React.useEffect(() => {
        if (!hasBaseDomains)
            return;

        const handlePointerMove = (event: PointerEvent) => {
            const pointerDownInfo = pointerDownInfoRef.current;
            if (pointerDownInfo) {
                const dx = event.clientX - pointerDownInfo.clientX;
                const dy = event.clientY - pointerDownInfo.clientY;
                if (Math.hypot(dx, dy) > clickThresholdPx)
                    dragExceededThresholdRef.current = true;
            }

            if (!isDraggingRef.current)
                return;

            event.preventDefault();
            pendingPointerMoveRef.current = {
                clientX: event.clientX,
                clientY: event.clientY,
            };

            if (pointerMoveRafRef.current !== null)
                return;

            pointerMoveRafRef.current = window.requestAnimationFrame(() => {
                pointerMoveRafRef.current = null;
                const pending = pendingPointerMoveRef.current;
                pendingPointerMoveRef.current = null;

                if (!pending)
                    return;

                processPointerMove(pending.clientX, pending.clientY);
            });
        };

        window.addEventListener("pointermove", handlePointerMove);
        return () => {
            window.removeEventListener("pointermove", handlePointerMove);

            if (pointerMoveRafRef.current !== null) {
                window.cancelAnimationFrame(pointerMoveRafRef.current);
                pointerMoveRafRef.current = null;
            }

            pendingPointerMoveRef.current = null;
        };
    }, [
        hasBaseDomains,
        clickThresholdPx,
        processPointerMove,
    ]);

    React.useEffect(() => {
        if (!hasBaseDomains)
            return;

        const handlePointerUp = () => {
            if (pointerMoveRafRef.current !== null) {
                window.cancelAnimationFrame(pointerMoveRafRef.current);
                pointerMoveRafRef.current = null;
            }

            const pendingMove = pendingPointerMoveRef.current;
            pendingPointerMoveRef.current = null;

            if (pendingMove)
                processPointerMove(pendingMove.clientX, pendingMove.clientY);

            if (!isDraggingRef.current) {
                pointerDownInfoRef.current = null;
                dragExceededThresholdRef.current = false;
                return;
            }

            isDraggingRef.current = false;
            setInteracting(false);

            let appliedRegion = false;
            const activeSelection = selectionRef.current;
            const pendingPanDomain = pendingPanDomainRef.current;

            if (
                activeSelection
                && activeSelection.x1 !== null
                && activeSelection.x2 !== null
                && activeSelection.y1 !== null
                && activeSelection.y2 !== null
                && baseXDomain
                && baseYDomain
            ) {
                const xStart = Math.min(activeSelection.x1, activeSelection.x2);
                const xEnd = Math.max(activeSelection.x1, activeSelection.x2);
                const yStart = Math.min(activeSelection.y1, activeSelection.y2);
                const yEnd = Math.max(activeSelection.y1, activeSelection.y2);
                const nextXDomain = allowedX && xStart !== xEnd
                    ? (constrainXDomain ? clampDomainToBase([xStart, xEnd], baseXDomain) : [xStart, xEnd]) as [number, number]
                    : null;
                const nextYDomain = allowedY && yStart !== yEnd
                    ? (constrainYDomain ? clampDomainToBase([yStart, yEnd], baseYDomain) : [yStart, yEnd]) as [number, number]
                    : null;
                const validRegion = (
                    (zoomMode === "x" && nextXDomain !== null)
                    || (zoomMode === "y" && nextYDomain !== null)
                    || (zoomMode === "xy" && nextXDomain !== null && nextYDomain !== null)
                );

                if (validRegion) {
                    cancelZoomAnimation();

                    if (nextXDomain)
                        setXDomainOverride(nextXDomain);

                    if (nextYDomain)
                        setYDomainOverride(nextYDomain);

                    callbackRef.current.onRegionSelect?.({
                        x1: nextXDomain?.[0] ?? activeXDomain?.[0] ?? baseXDomain[0],
                        x2: nextXDomain?.[1] ?? activeXDomain?.[1] ?? baseXDomain[1],
                        y1: nextYDomain?.[0] ?? activeYDomain?.[0] ?? baseYDomain[0],
                        y2: nextYDomain?.[1] ?? activeYDomain?.[1] ?? baseYDomain[1],
                        mode: zoomMode,
                    });

                    appliedRegion = true;
                }
            }

            if (pendingPanDomain) {
                cancelZoomAnimation();

                if (pendingPanDomain.xDomain)
                    setXDomainOverride(pendingPanDomain.xDomain);

                if (pendingPanDomain.yDomain)
                    setYDomainOverride(pendingPanDomain.yDomain);

                schedulePanPreviewClear(false);
            } else {
                clearPanPreview();
            }

            ignoreNextClickRef.current = dragExceededThresholdRef.current || appliedRegion;
            panStateRef.current = null;
            deferredPanActiveRef.current = false;
            setIsDeferredPanning(false);
            pendingPanDomainRef.current = null;
            selectionMetaRef.current = null;
            pointerDownInfoRef.current = null;
            dragExceededThresholdRef.current = false;
            hideSelectionPreview();
            updateSelection(null);
        };

        window.addEventListener("pointerup", handlePointerUp);
        window.addEventListener("pointercancel", handlePointerUp);

        return () => {
            window.removeEventListener("pointerup", handlePointerUp);
            window.removeEventListener("pointercancel", handlePointerUp);
        };
    }, [
        hasBaseDomains,
        baseXDomain,
        baseYDomain,
        activeXDomain,
        activeYDomain,
        allowedX,
        allowedY,
        constrainXDomain,
        constrainYDomain,
        zoomMode,
        cancelZoomAnimation,
        processPointerMove,
        setInteracting,
        clearPanPreview,
        hideSelectionPreview,
        schedulePanPreviewClear,
        updateSelection,
    ]);

    const cursorClassName = React.useMemo(() => {
        if (panEnabled && modifiers.shiftHeld)
            return isInteracting ? "cursor-grabbing!" : "cursor-grab!";

        return "cursor-default!";
    }, [isInteracting, modifiers.shiftHeld, panEnabled]);

    return {
        chartRef,
        xDomainOverride,
        yDomainOverride,
        activeXDomain,
        activeYDomain,
        selection,
        selectionMode: zoomMode,
        selectionOverlayRef,
        isInteracting,
        isDeferredPanning,
        modifiers,
        cursorClassName: cn(cursorClassName),
        handleChartMouseDown,
        handleChartClick,
        handleContainerMouseDown,
        handleContainerContextMenu,
        handleWheel,
        resetView,
    };
}
