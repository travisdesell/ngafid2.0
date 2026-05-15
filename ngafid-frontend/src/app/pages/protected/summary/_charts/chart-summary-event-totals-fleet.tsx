// ngafid-frontend/src/app/pages/summary/charts/chart-summary-event-totals-fleet.tsx
"use client";

import { Pie, PieChart } from "recharts";

import { useTheme } from "@/components/providers/theme-provider";
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

interface ChartSummaryEventTotalsFleetProps {
    selectedFleetCount: number;
    aggregateFleetCount: number;
}





export function ChartSummaryEventTotalsFleet({ selectedFleetCount, aggregateFleetCount }: ChartSummaryEventTotalsFleetProps) {

    const { useHighContrastCharts } = useTheme();

    const thisFleetEvents = Math.max(selectedFleetCount, 0);
    const allOtherFleetsEvents = Math.max(aggregateFleetCount - thisFleetEvents, 0);
    const totalEvents = thisFleetEvents + allOtherFleetsEvents;
    const thisFleetPercentage = totalEvents > 0 ? (thisFleetEvents / totalEvents) * 100 : 0;

    const chartData = [
        { fleet: "this_fleet", events: thisFleetEvents, fill: "var(--color-this_fleet)" },
        { fleet: "all_other_fleets", events: allOtherFleetsEvents, fill: "var(--color-all_other_fleets)" },
    ];

    const allFleetsColor = useHighContrastCharts ? "var(--chart-hc-1)" : "var(--chart-1)";
    const thisFleetColor = useHighContrastCharts ? "var(--chart-hc-2)" : "var(--chart-2)";
    const chartConfig = {
        all_other_fleets: {
            label: "All Other Fleets",
            color: allFleetsColor,
        },
        this_fleet: {
            label: `This Fleet (${thisFleetPercentage.toFixed(1)}%)`,
            color: thisFleetColor,
        },
    } satisfies ChartConfig;

    return (
        <CardContent className="flex-1 p-0 w-full">
            <ChartContainer
                config={chartConfig}
                className="max-h-[250px] mx-auto"
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
    );
}
