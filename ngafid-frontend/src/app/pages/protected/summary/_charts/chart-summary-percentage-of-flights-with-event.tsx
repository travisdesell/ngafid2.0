// ngafid-frontend/src/app/pages/summary/charts/chart-summary-event-counts.tsx
"use client"

import { Bar, BarChart, CartesianGrid, Cell, XAxis, YAxis } from "recharts"
import {
    BAR_RADIUS_HORIZONTAL_FIRST,
    BAR_RADIUS_HORIZONTAL_LAST,
    BAR_RADIUS_HORIZONTAL_MIDDLE,
    BAR_RADIUS_HORIZONTAL_SOLO,
    ChartConfig,
    ChartContainer,
    ChartLegend,
    ChartLegendContent,
    ChartTooltip,
    ChartTooltipContent,
} from "@/components/ui/chart"
import { useTimeHeader } from "@/components/time_header/time_header_provider"
import { AirframeEventCounts } from "src/types"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { JSX } from "react"
import { useTheme } from "@/components/theme-provider"

type ChartSummaryEventCountsProps = {
    data: AirframeEventCounts[] | Record<string, AirframeEventCounts>;
    renderNoDataAvailableMessage: () => JSX.Element;
};

export function ChartSummaryPercentageOfFlightsWithEvent({ data, renderNoDataAvailableMessage }: ChartSummaryEventCountsProps) {

    const { useHighContrastCharts } = useTheme();

    //Normalize to array
    const rows: AirframeEventCounts[] = Array.isArray(data)
        ? data
        : data && (typeof data === "object")
            ? Object.values(data as Record<string, AirframeEventCounts>)
            : [];


    const { renderDateRangeMonthly } = useTimeHeader();
    console.log("Chart Summary Percentage of Flights With Event - rows:", rows);


    //Union of event names (preserve first row's order, then append any extras)
    const primaryOrder = rows[0]?.names ?? [];
    const extra = rows.flatMap(r => r.names.filter(n => !primaryOrder.includes(n)));
    const eventNames = [...primaryOrder, ...extra];


    //One series per airframe (safe keys for CSS vars)
    const afKeys = rows.map((_, i) => `af${i}`);


    //Chart config (labels, colors)
    const chartConfig = rows.reduce((acc, r, i) => {
        const key = afKeys[i];
        const color = (
            useHighContrastCharts
            ? `var(--chart-hc-${(i % 12) + 1})`  // High-contrast palette
            : `var(--chart-${(i % 12) + 1})`     // Standard palette
        )
        acc[key] = {
            label: r.airframeName,  //<-- Legend label
            color,
        };
        return acc;
    }, {} as ChartConfig);


    //Pivot (event row -> counts per airframe)
    const chartData = eventNames.map((eventName) => {
        const point: Record<string, string | number> = { eventName };
        rows.forEach((r, i) => {
            const j = r.names.indexOf(eventName);
            point[afKeys[i]] = j >= 0 ? (r.totalEventsCounts[j] ?? 0) : 0;
        });
        return point;
    });


    //Sort events by total descending
    chartData.sort((a, b) => {
        const sum = (o: any) => afKeys.reduce((s, k) => s + Number(o[k] || 0), 0);
        return sum(b) - sum(a);
    });

    const chartHasData = (chartData.length > 0 && rows.length > 0);


    const firstIdxForRow = chartData.map((point) =>

        afKeys.findIndex((key) => Number(point[key] || 0) > 0)

    );

    const lastIdxForRow = chartData.map((point) => {

        for (let idx = afKeys.length - 1; idx >= 0; idx--) {
            if (Number(point[afKeys[idx]] || 0) > 0)
                return idx;
        }

        return -1;

    });


    return (
        <Card className="card-glossy">
            <CardHeader>
                <CardTitle className="flex justify-between">
                    Percentage of Flights With Event (WIP)
                    {renderDateRangeMonthly()}
                </CardTitle>
                <CardDescription>
                    Percentage of flights in this fleet with at least one event, by airframe and event type.
                </CardDescription>
            </CardHeader>

            <CardContent>

                <ChartContainer config={chartConfig} className="min-h-0 w-full">
                    {
                        (!chartHasData)
                        ?
                        renderNoDataAvailableMessage()
                        :
                        <BarChart
                            data={chartData}
                            layout="vertical"
                            accessibilityLayer
                            margin={{ left: 4, bottom: 8 }}
                            barCategoryGap={8}
                        >
                            {/* Horizontal numeric axis (Event Counts) */}
                            <XAxis type="number" tickLine={false} axisLine={false} tickMargin={8} />

                            {/* Vertical category axis (Event Names) */}
                            <YAxis
                                type="category"
                                dataKey="eventName"
                                tickLine={false}
                                axisLine={false}
                                tickMargin={8}
                                width={150}
                            />

                            <CartesianGrid vertical={false} />
                            <ChartTooltip content={<ChartTooltipContent hideLabel />} />
                            <ChartLegend content={<ChartLegendContent />} />

                            {/* One stacked bar per Airframe */}
                            {afKeys.map((k, iAf) => (
                                <Bar
                                    key={k}
                                    dataKey={k}
                                    stackId="total"
                                    fill={`var(--color-${k})`}
                                    maxBarSize={48}
                                >
                                    {chartData.map((_, jRow) => {
                                        const first = firstIdxForRow[jRow];
                                        const last = lastIdxForRow[jRow];

                                        // Whole row is zero, 'middle'
                                        const isSolo = (first === last && first === iAf && first !== -1);
                                        const isFirst = (iAf === first && first !== -1);
                                        const isLast = (iAf === last && last !== -1);

                                        const radius =
                                            isSolo
                                                ? BAR_RADIUS_HORIZONTAL_SOLO
                                                : isFirst
                                                    ? BAR_RADIUS_HORIZONTAL_FIRST
                                                    : isLast
                                                        ? BAR_RADIUS_HORIZONTAL_LAST
                                                        : BAR_RADIUS_HORIZONTAL_MIDDLE;

                                        // @ts-expect-error recharts Cell typing lacks 'radius'
                                        return <Cell key={`${k}-${jRow}`} radius={radius} />;
                                    })}
                                </Bar>
                            ))}
                        </BarChart>
                    }
                </ChartContainer>
                
            </CardContent>
        </Card>
    );
}
