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
    { fleet: "this_fleet", events: 345345, fill: "var(--color-this_fleet)" },
    { fleet: "all_fleets", events: 2148524, fill: "var(--color-all_fleets)" },
];





export function ChartSummaryEventTotalsCopy() {

    const { useHighContrastCharts } = useTheme();

    const allFleetsColor = useHighContrastCharts ? "var(--chart-hc-1)" : "var(--chart-1)";
    const thisFleetColor = useHighContrastCharts ? "var(--chart-hc-2)" : "var(--chart-2)";
    const chartConfig = {
        all_fleets: {
            label: "All Fleets",
            color: allFleetsColor,
        },
        this_fleet: {
            label: "This Fleet",
            color: thisFleetColor,
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
