// ngafid-frontend/src/app/components/providers/useCartesianZoomPan.ts
import { getLogger } from "@/components/providers/logger";
import {
    useCallback,
    useEffect,
    useRef,
    useState,
    type DependencyList,
} from "react";

const log = getLogger("useCartesianZoomPan", "blue", "Provider");

/**
 * Normalize whatever the scale's invert returns into a number.
 *      - Time scale => Date => MS since epoch
 *      - Linear => number
 *      - Anything else => Number(...)
 */
const normalizeScaleValueToNumber = (value: any): number => {

    // Null value -> NaN
    if (value == null)
        return NaN;

    // Date -> MS since epoch
    if (value instanceof Date)
        return value.getTime();

    // Number -> Output directly
    if (typeof value === "number")
        return value;

    // Fallback: attempt to convert to number
    const numeric = Number(value);
    return Number.isNaN(numeric)
        ? NaN
        : numeric;

};

type SelectionState = {
    x1: number | null;
    x2: number | null;
    y1: number | null;
    y2: number | null;
};

type PanState = {
    baseXDomain: [number, number];
    baseYDomain: [number, number];
    startClientX: number;
    startClientY: number;
    chartWidth: number;
    chartHeight: number;
};

type SelectionMeta = {
    startClientX: number;
    startClientY: number;
    startChartX: number;
    startChartY: number;
    chartWidth: number;
    chartHeight: number;
};

export type CartesianZoomPanOptions = {

    // Whether the chart currently has data (disables interactions otherwise)
    hasData: boolean;

    // Underlying X domain for the data (for clamp + reset range)
    baseXDomain: [number, number] | null;

    // Underlying Y domain for the data (for clamp + reset range)
    baseYDomain: [number, number] | null;

    // Extra deps that should reset zoom/pan when changed (alignment mode, scale mode, etc.).
    resetDeps?: DependencyList;

    // Wheel zoom speed (higher -> more aggressive zoom per wheel tick)
    zoomSpeed?: number;

    // Smallest fraction of the base span allowed when zooming in
    minSpanFactor?: number;

    // Duration of the zoom animation in MS
    animationDurationMs?: number;

};

export type CartesianZoomPanResult = {

    // Ref that must be attached to the target Recharts chart (LineChart / AreaChart / etc.)
    chartRef: React.RefObject<any>;

    /*
        Non-null, overrides the X axis domain.
        Otherwise, use the full baseXDomain.
    */
    xDomainOverride: [number, number] | null;

    /*
        Non-null, overrides the Y axis domain.
        Otherwise, use the full baseYDomain.
    */
    yDomainOverride: [number, number] | null;

    // Current drag-selection rectangle for the zoom-box
    selection: SelectionState | null;

    // True while dragging or wheel-zooming
    isInteracting: boolean;

    // Attach to global mouse events to cancel current interaction on right-click
    handleChartMouseOperationCancel: (e: MouseEvent) => void;

    // Attach to the chart's onMouseDown
    handleChartMouseDown: (e: any) => void;

    // Attach to the chart container's onWheel (e.g., ChartContainer)
    handleWheel: (e: any) => void;

    // Reset view to base domains
    resetView: () => void;

};

/*

    Generic 2D pan/zoom interaction layer
    for Recharts Cartesian charts.

    - Drag for box-zoom
    - Shift + Drag to pan
    - Wheel: 2D zoom
        - Shift + Wheel => X-only zoom
        - Ctrl + Wheel => Y-only zoom
    
*/
export function useCartesianZoomPan({
    hasData,
    baseXDomain,
    baseYDomain,
    resetDeps,
    zoomSpeed = 0.005,
    minSpanFactor = 0.001,
    animationDurationMs = 140,
}: CartesianZoomPanOptions): CartesianZoomPanResult {
    const chartRef = useRef<any>(null);

    // Current overrides for active view window (if null => base domain)
    const [xDomainOverride, setXDomainOverride] = useState<[number, number] | null>(null);
    const [yDomainOverride, setYDomainOverride] = useState< [number, number] | null >(null);

    // Active zoom selection rectangle
    const [selection, setSelection] = useState<SelectionState | null>(null);

    // Used only to fade tooltip out while interacting
    const [isInteracting, setIsInteracting] = useState(false);

    // Refs for fast/imperative state
    const isDraggingRef = useRef(false);
    const panStateRef = useRef<PanState | null>(null);
    const selectionMetaRef = useRef<SelectionMeta | null>(null);
    const isShiftPressedRef = useRef(false);
    const wheelInteractingTimeoutRef = useRef<number | null>(null);

    // Small animation for wheel-zooming
    const zoomAnimRafRef = useRef<number | null>(null);
    const zoomAnimStateRef = useRef<{
        fromX: [number, number] | null;
        toX: [number, number] | null;
        fromY: [number, number] | null;
        toY: [number, number] | null;
        startTime: number;
        duration: number;
    } | null>(null);

    const hasBaseDomains =
        hasData
        && baseXDomain != null
        && baseYDomain != null
        && Number.isFinite(baseXDomain[0])
        && Number.isFinite(baseXDomain[1])
        && Number.isFinite(baseYDomain[0])
        && Number.isFinite(baseYDomain[1]);

    const cancelZoomAnimation = () => {

        // Zoom animation in progress, cancel it
        if (zoomAnimRafRef.current !== null) {

            window.cancelAnimationFrame(zoomAnimRafRef.current);
            zoomAnimRafRef.current = null;

        }

        zoomAnimStateRef.current = null;

    };

    const startZoomAnimation = (
        nextXDomain: [number, number] | null,
        nextYDomain: [number, number] | null,
        currentXDomain: [number, number] | null,
        currentYDomain: [number, number] | null,
    ) => {

        const hasX = (nextXDomain !== null && currentXDomain !== null);
        const hasY = (nextYDomain !== null && currentYDomain !== null);

        // No valid axis to animate, exit
        if (!hasX && !hasY)
            return;

        // Cancel any ongoing animation
        cancelZoomAnimation();

        const state = {
            fromX: hasX ? (currentXDomain as [number, number]) : null,
            toX: hasX ? (nextXDomain as [number, number]) : null,
            fromY: hasY ? (currentYDomain as [number, number]) : null,
            toY: hasY ? (nextYDomain as [number, number]) : null,
            startTime: performance.now(),
            duration: animationDurationMs,
        };

        // Assign current zoom animation state
        zoomAnimStateRef.current = state;

        const step = () => {

            const zoomAnimStateCurrent = zoomAnimStateRef.current;

            // No current animation, exit
            if (!zoomAnimStateCurrent)
                return;

            const now = performance.now();
            const tRaw = (now - zoomAnimStateCurrent.startTime) / zoomAnimStateCurrent.duration;
            const t = Math.max(0.0, Math.min(1.0, tRaw));
            const eased = 1.0 - Math.pow(1.0 - t, 3.0);     //<-- easeOutCubic

            if (zoomAnimStateCurrent.fromX && zoomAnimStateCurrent.toX) {

                const [fx0, fx1] = zoomAnimStateCurrent.fromX;
                const [tx0, tx1] = zoomAnimStateCurrent.toX;
                const xDom: [number, number] = [
                    fx0 + (tx0 - fx0) * eased,
                    fx1 + (tx1 - fx1) * eased,
                ];

                setXDomainOverride(xDom);

            }

            if (zoomAnimStateCurrent.fromY && zoomAnimStateCurrent.toY) {

                const [fy0, fy1] = zoomAnimStateCurrent.fromY;
                const [ty0, ty1] = zoomAnimStateCurrent.toY;
                const yDom: [number, number] = [
                    fy0 + (ty0 - fy0) * eased,
                    fy1 + (ty1 - fy1) * eased,
                ];

                setYDomainOverride(yDom);

            }

            // Not at the end yet, continue
            if (t < 1.00) {
                zoomAnimRafRef.current = window.requestAnimationFrame(step);

            // Otherwise, finalize
            } else {

                if (zoomAnimStateCurrent.toX)
                    setXDomainOverride(zoomAnimStateCurrent.toX);

                if (zoomAnimStateCurrent.toY)
                    setYDomainOverride(zoomAnimStateCurrent.toY);

                zoomAnimStateRef.current = null;
                zoomAnimRafRef.current = null;

            }

        };

        zoomAnimRafRef.current = window.requestAnimationFrame(step);

    };

    // Track 'Shift' key globally
    useEffect(() => {

        const handleKeyDown = (ev: KeyboardEvent) => {
            if (ev.key === "Shift")
                isShiftPressedRef.current = true;
        };

        const handleKeyUp = (ev: KeyboardEvent) => {
            if (ev.key === "Shift")
                isShiftPressedRef.current = false;
        };

        window.addEventListener("keydown", handleKeyDown);
        window.addEventListener("keyup", handleKeyUp);

        return () => {
            window.removeEventListener("keydown", handleKeyDown);
            window.removeEventListener("keyup", handleKeyUp);
        };

    }, []);

    useEffect(() => {

        return () => {
            if (wheelInteractingTimeoutRef.current !== null)
                window.clearTimeout(wheelInteractingTimeoutRef.current);

            cancelZoomAnimation();
        };

    }, []);

    const resetView = () => {

        // No data, just clear everything
        if (!hasBaseDomains) {
            setXDomainOverride(null);
            setYDomainOverride(null);
            setSelection(null);
            panStateRef.current = null;
            selectionMetaRef.current = null;
            isDraggingRef.current = false;
            setIsInteracting(false);

            return;
        }

        cancelZoomAnimation();
        setXDomainOverride(null);
        setYDomainOverride(null);
        setSelection(null);
        panStateRef.current = null;
        selectionMetaRef.current = null;
        isDraggingRef.current = false;
        setIsInteracting(false);
    };

    // Automatically reset zoom and pan whenever base domains / supplied resetDeps change
    useEffect(
        resetView,
        [hasBaseDomains, baseXDomain?.[0], baseXDomain?.[1], baseYDomain?.[0], baseYDomain?.[1], ...(resetDeps ?? []),]
    );

    const getChartScales = useCallback(() => {

        const chartAny = chartRef.current as any;

        // Chart is missing or doesn't have the scale getters, exit
        if (
            !chartAny 
            || typeof chartAny.getXScales !== "function"
            || typeof chartAny.getYScales !== "function"
        )
            return null;

        const xScalesObj = chartAny.getXScales();
        const yScalesObj = chartAny.getYScales();

        const firstScale = (obj: any) => {

            if (!obj)
                return null;

            const keys = Object.keys(obj);

            // Didn't find any scales -> null
            if (keys.length === 0)
                return null;

            return obj[keys[0]!];

        };

        const xScale = firstScale(xScalesObj);
        const yScale = firstScale(yScalesObj);

        // Invalid scales -> null
        if (
            !xScale
            || !yScale
            || typeof xScale.invert !== "function"
            || typeof yScale.invert !== "function"
        )
            return null;

        return { xScale, yScale };

    }, []);

    const getChartOffset = () => {

        const chartAny = chartRef.current as any;
        const offset = chartAny?.state?.offset;

        // Invalid offset -> null
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

    };

    const getChartContainerRect = (): DOMRect | null => {

        const chartAny = chartRef.current as any;
        const container: HTMLElement | undefined = chartAny?.container;

        // Container element not found -> null
        if (!container)
            return null;

        return container.getBoundingClientRect();

    };

    const handleChartMouseOperationCancel = (e: any) => {

        log("handleChartMouseOperationCancel: ", e);

        const MOUSE_BUTTON_RIGHT = 2;

        // Not right clicking, ignore
        if (e.button != MOUSE_BUTTON_RIGHT)
            return;

        e.preventDefault();

        log("Right Clicked, cancelling any current interaction...");

        // Clean up any current interaction state
        isDraggingRef.current = false;
        setIsInteracting(false);
        panStateRef.current = null;
        selectionMetaRef.current = null;
        setSelection(null);

    };

    /* MouseDown inside chart, start zoom/pan */
    const handleChartMouseDown = (e: any) => {

        log("handleChartMouseDown: ", e);

        // No data, exit
        if (!hasBaseDomains)
            return;

        // Invalid event, exit
        if (!e || typeof e.chartX !== "number" || typeof e.chartY !== "number")
            return;

        const { chartX, chartY } = e;

        const scales = getChartScales();
        if (!scales) return;

        const { xScale, yScale } = scales;
        const rawX = xScale.invert(chartX);
        const rawY = yScale.invert(chartY);
        const xValue = normalizeScaleValueToNumber(rawX);
        const yValue = normalizeScaleValueToNumber(rawY);

        // Invalid scale inversion, exit
        if (!Number.isFinite(xValue) || !Number.isFinite(yValue)) {

            log.warn("Invalid scale inversion results:", { rawX, rawY, xValue, yValue, });
            return;

        }

        const chartOffset = getChartOffset();
        const chartRegion = getChartContainerRect();

        // Invalid chart offset/container, exit
        if (
            !chartOffset
            || chartOffset.width <= 0
            || chartOffset.height <= 0
            || !chartRegion
        ) {

            log.warn("Invalid chart offset or container rect:", { chartOffset, chartRegion, });
            return;

        }

        const chartWidth = chartOffset.width;
        const chartHeight = chartOffset.height;

        const startClientX = chartRegion.left + chartX;
        const startClientY = chartRegion.top + chartY;

        // Current view domains at the moment pan starts
        const currentViewXDomain: [number, number] | null = (xDomainOverride ?? baseXDomain);
        const currentViewYDomain: [number, number] | null = (yDomainOverride ?? baseYDomain);

        cancelZoomAnimation();
        isDraggingRef.current = true;
        setIsInteracting(true);

        // Holding shift, start panning
        if (isShiftPressedRef.current) {

            if (!currentViewXDomain || !currentViewYDomain)
                return;

            // Start pan from the current view window (zoomed or not)
            panStateRef.current = {
                baseXDomain: currentViewXDomain,
                baseYDomain: currentViewYDomain,
                startClientX,
                startClientY,
                chartWidth,
                chartHeight,
            };

            selectionMetaRef.current = null;
            setSelection(null);

        // Otherwise, start zoom selection
        } else {
            
            panStateRef.current = null;
            selectionMetaRef.current = {
                startClientX,
                startClientY,
                startChartX: chartX,
                startChartY: chartY,
                chartWidth,
                chartHeight,
            };

            setSelection({
                x1: xValue,
                y1: yValue,
                x2: xValue,
                y2: yValue,
            });

        }

    };

    // Wheel zoom (2D / axis-constrained with modifiers)
    const handleWheel = (e: any) => {

        // No data, exit
        if (!hasBaseDomains)
            return;

        if (isDraggingRef.current)
            return;

        const chartOffset = getChartOffset();
        const chartRegion = getChartContainerRect();
        
        // Invalid chart offset/container, exit
        if (!chartOffset || !chartRegion)
            return;

        const clientX = e.clientX as number | undefined;
        const clientY = e.clientY as number | undefined;

        // Got non-numeric client coords, exit
        if (typeof clientX !== "number" || typeof clientY !== "number")
            return;

        const containerX = clientX - chartRegion.left;
        const containerY = clientY - chartRegion.top;

        const chartX = containerX - chartOffset.left;
        const chartY = containerY - chartOffset.top;

        const outOfRange = (
            chartX < 0
            || chartX > chartOffset.width
            || chartY < 0
            || chartY > chartOffset.height);

        // Cursor outside plot area, ignore
        if (outOfRange)
            return;

        const deltaY = e.deltaY;
        if (!Number.isFinite(deltaY) || deltaY === 0)
            return;

        const scales = getChartScales();
        if (!scales)
            return;

        const { xScale, yScale } = scales;

        const isShift = !!e.shiftKey;
        const isCtrl = !!e.ctrlKey;

        /*
            Ctrl => Y-axis only
            Shift => X-axis only

            Neither => 2D
        */
        let applyX = false;
        let applyY = false;

        // Holding Ctrl, y-only zoom
        if (isCtrl) {

            applyY = true;

        // Otherwise, holding Shift, x-only zoom
        } else if (isShift) {
        
            applyX = true;

        // Otherwise (no modifier), 2D zoom
        } else {

            applyX = true;
            applyY = true;

        }

        let zoomFactor = Math.exp(deltaY * zoomSpeed);
        zoomFactor = Math.max(0.2, Math.min(zoomFactor, 5));

        const [baseXMin, baseXMax] = baseXDomain!;
        const [baseYMin, baseYMax] = baseYDomain!;

        const baseXSpan = baseXMax - baseXMin || 1;
        const baseYSpan = baseYMax - baseYMin || 1;

        const MIN_X_SPAN = baseXSpan * minSpanFactor;
        const MIN_Y_SPAN = baseYSpan * minSpanFactor;

        // Current active domains (override or base)
        const currentXDomain: [number, number] | null = (xDomainOverride ?? baseXDomain);
        const currentYDomain: [number, number] | null = (yDomainOverride ?? baseYDomain);

        let nextXDomain: [number, number] | null = null;
        let nextYDomain: [number, number] | null = null;

        // Compute next X domain
        if (applyX && currentXDomain) {

            const rawX = xScale.invert(chartX);
            const xCenter = normalizeScaleValueToNumber(rawX);

            // Got valid center point, proceed
            if (Number.isFinite(xCenter)) {

                const [currXMin, currXMax] = currentXDomain;
                const currXSpan = currXMax - currXMin;

                // Valid current span, proceed
                if (Number.isFinite(currXSpan) && currXSpan > 0) {

                    const newSpanRaw = currXSpan * zoomFactor;
                    const newXSpan = Math.max(MIN_X_SPAN, newSpanRaw);

                    const relX = (xCenter - currXMin) / currXSpan;
                    let xMin = xCenter - relX * newXSpan;
                    let xMax = xMin + newXSpan;

                    // Valid domain, set it
                    if (Number.isFinite(xMin) && Number.isFinite(xMax))
                        nextXDomain = [xMin, xMax];
                }

            }

        }

        // Compute next Y domain
        if (applyY && currentYDomain) {

            const rawY = yScale.invert(chartY);
            const yCenter = normalizeScaleValueToNumber(rawY);

            // Got valid center point, proceed
            if (Number.isFinite(yCenter)) {

                const [currYMin, currYMax] = currentYDomain;
                const currYSpan = currYMax - currYMin;

                // Valid current span, proceed
                if (Number.isFinite(currYSpan) && currYSpan > 0) {

                    const newSpanRaw = currYSpan * zoomFactor;
                    const newYSpan = Math.max(MIN_Y_SPAN, newSpanRaw);

                    const relY = (yCenter - currYMin) / currYSpan;
                    let yMin = yCenter - relY * newYSpan;
                    let yMax = yMin + newYSpan;

                    // Valid domain, set it
                    if (Number.isFinite(yMin) && Number.isFinite(yMax))
                        nextYDomain = [yMin, yMax];
                }

            }

        }

        // No valid domain changes, exit
        if (!nextXDomain && !nextYDomain)
            return;

        // Hide tooltip briefly while zooming
        setIsInteracting(true);
        if (wheelInteractingTimeoutRef.current !== null)
            window.clearTimeout(wheelInteractingTimeoutRef.current);

        const WHEEL_TIMEOUT_MS = 150;
        wheelInteractingTimeoutRef.current = window.setTimeout(() => {
            setIsInteracting(false);
            wheelInteractingTimeoutRef.current = null;
        }, WHEEL_TIMEOUT_MS);

        // Prevent page scroll only when we actually zoom
        e.preventDefault?.();

        startZoomAnimation(
            nextXDomain,
            nextYDomain,
            currentXDomain,
            currentYDomain,
        );

    };

    // Globally track the pointermove for drag-zoom / pan (even when the cursor leaves the chart)
    useEffect(() => {

        if (!hasBaseDomains)
            return;

        const handlePointerMove = (ev: PointerEvent) => {

            if (!isDraggingRef.current)
                return;

            const panState = panStateRef.current;
            const selectionMeta = selectionMetaRef.current;

            const scales = getChartScales();
            if (!scales)
                return;

            const { xScale, yScale } = scales;

            // Zoom selection: Use starting chart coords + client delta, clamp into chart area
            if (selection && selectionMeta && !panState) {

                const {
                    startClientX,
                    startClientY,
                    startChartX,
                    startChartY,
                    chartWidth,
                    chartHeight,
                } = selectionMeta;

                // Invalid chart size, exit
                if (chartWidth <= 0 || chartHeight <= 0)
                    return;

                const dxClient = ev.clientX - startClientX;
                const dyClient = ev.clientY - startClientY;

                // Got non-finite delta, exit
                if (!Number.isFinite(dxClient) || !Number.isFinite(dyClient))
                    return;

                let chartX = startChartX + dxClient;
                let chartY = startChartY + dyClient;

                chartX = Math.max(0, Math.min(chartWidth, chartX));
                chartY = Math.max(0, Math.min(chartHeight, chartY));

                const rawX = xScale.invert(chartX);
                const rawY = yScale.invert(chartY);
                const xValue = normalizeScaleValueToNumber(rawX);
                const yValue = normalizeScaleValueToNumber(rawY);

                // Got invalid scale inversion, exit
                if (!Number.isFinite(xValue) || !Number.isFinite(yValue))
                    return;

                setSelection((prev) => prev
                    ? { ...prev, x2: xValue, y2: yValue, }
                    : prev,
                );

            } else if (panState) {

                const {
                    startClientX,
                    startClientY,
                    baseXDomain,
                    baseYDomain,
                    chartWidth,
                    chartHeight,
                } = panState;

                // Invalid chart size, exit
                if (chartWidth <= 0 || chartHeight <= 0)
                    return;

                const dxPx = ev.clientX - startClientX;
                const dyPx = ev.clientY - startClientY;

                // Got non-finite delta, exit
                if (!Number.isFinite(dxPx) || !Number.isFinite(dyPx))
                    return;

                const dxFraction = dxPx / chartWidth;
                const dyFraction = dyPx / chartHeight;

                const xSpan = baseXDomain[1] - baseXDomain[0];
                const ySpan = baseYDomain[1] - baseYDomain[0];

                // Invalid spans, exit
                if ( !Number.isFinite(xSpan) || xSpan === 0 || !Number.isFinite(ySpan) || ySpan === 0 )
                    return;

                const xMin = baseXDomain[0] - dxFraction * xSpan;
                const xMax = baseXDomain[1] - dxFraction * xSpan;

                const yMin = baseYDomain[0] + dyFraction * ySpan;
                const yMax = baseYDomain[1] + dyFraction * ySpan;

                // Invalid computed domains, exit
                if (
                    !Number.isFinite(xMin)
                    || !Number.isFinite(xMax)
                    || !Number.isFinite(yMin)
                    || !Number.isFinite(yMax)
                )
                    return;

                cancelZoomAnimation();
                setXDomainOverride([xMin, xMax]);
                setYDomainOverride([yMin, yMax]);
            }
        };

        window.addEventListener("pointermove", handlePointerMove);
        return () => window.removeEventListener("pointermove", handlePointerMove);
    }, [hasBaseDomains, selection, getChartScales]);

    // Globally track pointerup to end drag-zoom / pan (even when the cursor leaves the chart)
    useEffect(() => {

        if (!hasBaseDomains)
            return;

        const handlePointerUp = () => {

            if (!isDraggingRef.current)
                return;

            isDraggingRef.current = false;
            setIsInteracting(false);

            if (
                selection
                && selection.x1 !== null
                && selection.x2 !== null
                && selection.y1 !== null
                && selection.y2 !== null
                && baseXDomain
                && baseYDomain
            ) {

                const [baseXMin, baseXMax] = baseXDomain;
                const [baseYMin, baseYMax] = baseYDomain;

                const xStart = Math.min(selection.x1, selection.x2);
                const xEnd = Math.max(selection.x1, selection.x2);
                const yStart = Math.min(selection.y1, selection.y2);
                const yEnd = Math.max(selection.y1, selection.y2);

                // Valid zoom box, apply it
                if (xStart !== xEnd && yStart !== yEnd) {

                    cancelZoomAnimation();
                    setXDomainOverride([xStart, xEnd]);
                    setYDomainOverride([yStart, yEnd]);

                }

            }

            panStateRef.current = null;
            selectionMetaRef.current = null;
            setSelection(null);
        };

        window.addEventListener("pointerup", handlePointerUp);
        window.addEventListener("pointercancel", handlePointerUp);

        return () => {
            window.removeEventListener("pointerup", handlePointerUp);
            window.removeEventListener("pointercancel", handlePointerUp);
        };

    }, [hasBaseDomains, selection, baseXDomain, baseYDomain]);

    return {
        chartRef,
        xDomainOverride,
        yDomainOverride,
        selection,
        isInteracting,
        handleChartMouseOperationCancel,
        handleChartMouseDown,
        handleWheel,
        resetView,
    };
    
}
