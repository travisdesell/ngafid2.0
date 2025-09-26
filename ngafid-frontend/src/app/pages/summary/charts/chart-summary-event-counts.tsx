// ngafid-frontend/src/app/pages/summary/charts/chart-summary-event-counts.tsx
"use client"

import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts"
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

type ChartSummaryEventCountsProps = {
    data: AirframeEventCounts[] | Record<string, AirframeEventCounts>;
    renderNoDataAvailableMessage: () => JSX.Element;
};

export function ChartSummaryEventCounts({ data, renderNoDataAvailableMessage }: ChartSummaryEventCountsProps) {

    //Normalize to array
    const rows: AirframeEventCounts[] = Array.isArray(data)
        ? data
        : data && (typeof data === "object")
            ? Object.values(data as Record<string, AirframeEventCounts>)
            : [];


    const { renderDateRangeMonthly } = useTimeHeader();
    console.log("Chart Summary Events Counts - rows:", rows);


    //Union of event names (preserve first row's order, then append any extras)
    const primaryOrder = rows[0]?.names ?? [];
    const extra = rows.flatMap(r => r.names.filter(n => !primaryOrder.includes(n)));
    const eventNames = [...primaryOrder, ...extra];


    //One series per airframe (safe keys for CSS vars)
    const afKeys = rows.map((_, i) => `af${i}`);


    //Chart config (labels, colors)
    const chartConfig = rows.reduce((acc, r, i) => {
        const key = afKeys[i];
        acc[key] = {
            label: r.airframeName,  //<-- Legend label
            color: `var(--chart-${(i % 12) + 1})`, //<-- Color palette
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

    return (
        <Card className="card-glossy">
            <CardHeader>
                <CardTitle className="flex justify-between">
                    Event Counts
                    {renderDateRangeMonthly()}
                </CardTitle>
                <CardDescription>
                    Total number of events by airframe and event type.
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
                                {/* horizontal numeric axis */}
                                <XAxis type="number" tickLine={false} axisLine={false} tickMargin={8} />

                                {/* vertical category axis: event names */}
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

                                {/* one stacked bar per AIRFRAME */}
                                {afKeys.map((k, i) =>{

                                    let isFirst = i === 0;
                                    let isLast = i === afKeys.length - 1;

                                    //Not first, but all preceding airframe event counts are zero, so this is effectively first
                                    const isFirstForRow =
                                        afKeys.slice(0, i-1).every(prevKey => Number(chartData[i][prevKey] || 0) === 0);
                                    if (isFirstForRow)
                                        isFirst = true;

                                    //Not last, but all subsequent airframe event counts are zero, so this is effectively last
                                    const isLastForRow =
                                        afKeys.slice(i + 1).every(nextKey => Number(chartData[i][nextKey] || 0) === 0);
                                    if (isLastForRow)
                                        isLast = true;


                                    const isSolo = (isFirst && isLast);

                                    let radius;
                                    if (isSolo)
                                        radius = BAR_RADIUS_HORIZONTAL_SOLO;
                                    else if (isFirst)
                                        radius = BAR_RADIUS_HORIZONTAL_FIRST;
                                    else if (isLast)
                                        radius = BAR_RADIUS_HORIZONTAL_LAST;
                                    else
                                        radius = BAR_RADIUS_HORIZONTAL_MIDDLE;

                                    return (
                                        <Bar
                                            key={k}
                                            dataKey={k}
                                            stackId="total"
                                            fill={`var(--color-${k})`}
                                            radius={radius}
                                            maxBarSize={48}
                                        />
                                    );
                                })}
                            </BarChart>
                        }
                    </ChartContainer>
                
            </CardContent>
        </Card>
    );
}
