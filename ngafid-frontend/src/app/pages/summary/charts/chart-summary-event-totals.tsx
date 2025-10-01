// ngafid-frontend/src/app/pages/summary/charts/chart-summary-event-totals.tsx
"use client"

import { Legend, Pie, PieChart } from "recharts";

import {
    CardContent,
} from "@/components/ui/card";
import {
    ChartConfig,
    ChartContainer,
    ChartLegend,
    ChartLegendContent,
    ChartTooltip,
    ChartTooltipContent,
} from "@/components/ui/chart";
import { useTheme } from "@/components/theme-provider";

const chartData = [
    { fleet: "selected_range", events: 42902, fill: "var(--color-selected_range)" },
    { fleet: "all_time", events: 2148524, fill: "var(--color-all_time)" },
];


export function ChartSummaryEventTotals() {

    const { useHighContrastCharts } = useTheme();

    const allTimeColor = useHighContrastCharts ? "var(--chart-hc-1)" : "var(--chart-1)";
    const selectedRangeColor = useHighContrastCharts ? "var(--chart-hc-2)" : "var(--chart-2)";
    const chartConfig = {
        all_time: {
            label: "All Time",
            color: allTimeColor,
        },
        selected_range: {
            label: "Selected Range",
            color: selectedRangeColor,
        },
    } satisfies ChartConfig;

    return (
        <CardContent className="flex-1 p-0 w-full">
            <ChartContainer
                config={chartConfig}
                className="max-h-[250px]"
            >
                <PieChart>
                    <ChartTooltip
                        cursor={false}
                        content={<ChartTooltipContent hideLabel className="gap-8! flex! flex-row!"/>}
                    />
                    <Pie
                        data={chartData}
                        dataKey="events"
                        nameKey="fleet"
                        innerRadius={40}
                    />

                    <ChartTooltip content={<ChartTooltipContent hideLabel />} />
                    <ChartLegend content={<ChartLegendContent />} />

                </PieChart>
            </ChartContainer>
        </CardContent>
    )
}
