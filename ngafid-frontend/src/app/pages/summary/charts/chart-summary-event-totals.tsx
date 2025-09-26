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

const chartData = [
    { fleet: "selected_range", events: 42902, fill: "var(--color-selected_range)" },
    { fleet: "all_time", events: 2148524, fill: "var(--color-all_time)" },
];

const chartConfig = {
    selected_range: {
        label: "Selected Range",
        color: "var(--chart-1)",
    },
    all_time: {
        label: "All Time",
        color: "var(--chart-3)",
    },
} satisfies ChartConfig;


export function ChartSummaryEventTotals() {
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
