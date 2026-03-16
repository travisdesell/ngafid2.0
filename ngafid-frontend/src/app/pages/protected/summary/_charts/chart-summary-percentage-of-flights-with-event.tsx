// ngafid-frontend/src/app/pages/summary/charts/chart-summary-percentage-of-flights-with-event.tsx
"use client"

import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts"
import {
    BAR_RADIUS_HORIZONTAL_SOLO,
    ChartConfig,
    ChartContainer,
    ChartLegend,
    ChartLegendContent,
    ChartTooltip,
    ChartTooltipContent,
} from "@/components/ui/chart"
import { useTimeHeader } from "@/components/providers/time_header/time_header_provider"
import { AirframeEventCounts } from "src/types"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { JSX } from "react"
import { useTheme } from "@/components/providers/theme-provider"
import { getLogger } from "@/components/providers/logger"


const log = getLogger("ChartSummaryPercentageOfFlightsWithEvent", "black", "Chart");


type ChartSummaryEventCountsProps = {
    data: AirframeEventCounts[] | Record<string, AirframeEventCounts>;
    renderNoDataAvailableMessage: () => JSX.Element;
};

type PercentageChartDatum = {
    eventName: string;
    fleetPercent: number;
    aggregatePercent: number;
    fleetFlightsWithEvent: number;
    fleetTotalFlights: number;
    aggregateFlightsWithEvent: number;
    aggregateTotalFlights: number;
};

const toPercent = (numerator: number, denominator: number): number => {
    if (denominator <= 0)
        return 0;

    return (100 * numerator) / denominator;
};

const formatPercent = (value: number): string => {
    if (!Number.isFinite(value) || value <= 0)
        return "0.00%";

    if (value < 1)
        return `${value.toFixed(-Math.ceil(Math.log10(value)) + 2)}%`;

    return `${value.toFixed(2)}%`;
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
    log.table("Rows:", rows);


    // Union of event names (preserve first row order, append extras)
    const primaryOrder = rows[0]?.names ?? [];
    const extra = rows.flatMap(r => r.names.filter(n => !primaryOrder.includes(n)));
    const eventNames = [...primaryOrder, ...extra];


    // Fleet vs all-fleets percentage values per event.
    const chartData: PercentageChartDatum[] = eventNames.map((eventName) => {
        let fleetFlightsWithEvent = 0;
        let fleetTotalFlights = 0;
        let aggregateFlightsWithEvent = 0;
        let aggregateTotalFlights = 0;

        rows.forEach((row) => {
            const index = row.names.indexOf(eventName);
            if (index < 0)
                return;

            fleetFlightsWithEvent += Number(row.flightsWithEventCounts[index] ?? 0);
            fleetTotalFlights += Number(row.totalFlightsCounts[index] ?? 0);

            aggregateFlightsWithEvent += Number(row.aggregateFlightsWithEventCounts[index] ?? 0);
            aggregateTotalFlights += Number(row.aggregateTotalFlightsCounts[index] ?? 0);
        });

        return {
            eventName,
            fleetPercent: toPercent(fleetFlightsWithEvent, fleetTotalFlights),
            aggregatePercent: toPercent(aggregateFlightsWithEvent, aggregateTotalFlights),
            fleetFlightsWithEvent,
            fleetTotalFlights,
            aggregateFlightsWithEvent,
            aggregateTotalFlights,
        };
    }).filter((point) => point.fleetTotalFlights > 0 || point.aggregateTotalFlights > 0);

    const chartConfig = {
        fleetPercent: {
            label: "Your Fleet",
            color: useHighContrastCharts ? "var(--chart-hc-1)" : "var(--chart-1)",
        },
        aggregatePercent: {
            label: "All Fleets",
            color: useHighContrastCharts ? "var(--chart-hc-2)" : "var(--chart-2)",
        },
    } satisfies ChartConfig;

    log.table("Chart Data:", chartData);

    // Sort events by combined percentage descending.
    chartData.sort((a, b) => {
        const sumA = a.fleetPercent + a.aggregatePercent;
        const sumB = b.fleetPercent + b.aggregatePercent;
        return sumB - sumA;
    });

    const chartHasData = (chartData.length > 0);

    const BASE_HEIGHT = 300;
    const HEIGHT_PER_EVENT = 25;
    const chartHeight = BASE_HEIGHT + (chartData.length * HEIGHT_PER_EVENT);


    log("Rendering...");
    return (
        <Card className="card-glossy">
            <CardHeader>
                <CardTitle className="flex justify-between">
                    Percentage of Flights With Event
                    {renderDateRangeMonthly()}
                </CardTitle>
                <CardDescription>
                    Percentage of flights with at least one event for your fleet versus all fleets, grouped by event type.
                </CardDescription>
            </CardHeader>

            <CardContent>

                <ChartContainer config={chartConfig} className="min-h-0 w-full" style={{ minHeight: chartHeight }}>
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
                            {/* Horizontal numeric axis (Percent) */}
                            <XAxis
                                type="number"
                                tickLine={false}
                                axisLine={false}
                                tickMargin={8}
                                tickFormatter={(value) => `${value}%`}
                            />

                            {/* Vertical category axis (Event Names) */}
                            <YAxis
                                type="category"
                                dataKey="eventName"
                                tickLine={false}
                                axisLine={false}
                                tickMargin={8}
                                width={200}
                            />

                            <CartesianGrid vertical={false} />
                            <ChartTooltip
                                content={
                                    <ChartTooltipContent
                                        hideLabel
                                        formatter={(value, name, item: any) => {
                                            const point = item?.payload as PercentageChartDatum;
                                            const isFleet = item?.dataKey === "fleetPercent";

                                            const flightsWithEvent = isFleet
                                                ? (point?.fleetFlightsWithEvent ?? 0)
                                                : (point?.aggregateFlightsWithEvent ?? 0);

                                            const totalFlights = isFleet
                                                ? (point?.fleetTotalFlights ?? 0)
                                                : (point?.aggregateTotalFlights ?? 0);

                                            const percentValue = Array.isArray(value)
                                                ? Number(value[0] ?? 0)
                                                : Number(value ?? 0);

                                            return (
                                                <div className="flex flex-1 justify-between leading-none gap-2 items-center">
                                                    <div className="grid gap-1">
                                                        <span className="text-muted-foreground">{name}</span>
                                                        <span className="text-muted-foreground/80">
                                                            {`${flightsWithEvent.toLocaleString()} / ${totalFlights.toLocaleString()} flights`}
                                                        </span>
                                                    </div>
                                                    <span className="text-foreground font-mono font-medium tabular-nums">
                                                        {formatPercent(percentValue)}
                                                    </span>
                                                </div>
                                            );
                                        }}
                                    />
                                }
                            />
                            <ChartLegend content={<ChartLegendContent />} />

                            <Bar
                                dataKey="fleetPercent"
                                fill="var(--color-fleetPercent)"
                                maxBarSize={32}
                                radius={BAR_RADIUS_HORIZONTAL_SOLO}
                            />

                            <Bar
                                dataKey="aggregatePercent"
                                fill="var(--color-aggregatePercent)"
                                maxBarSize={32}
                                radius={BAR_RADIUS_HORIZONTAL_SOLO}
                            />
                        </BarChart>
                    }
                </ChartContainer>
                
            </CardContent>
        </Card>
    );
}
