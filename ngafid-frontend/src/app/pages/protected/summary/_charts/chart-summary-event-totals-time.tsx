// ngafid-frontend/src/app/pages/summary/charts/chart-summary-event-totals-time.tsx
"use client"

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

type DataItem = {
    label: string;
    count: number;
    fill: string;
}

type ChartSummaryEventTotalsTimeProps = {
    selectedPercentage: number;
    data: [DataItem, DataItem];
}


export function ChartSummaryEventTotalsTime(props : ChartSummaryEventTotalsTimeProps) {

    const { selectedPercentage } = props;
    const chartData = props.data.map(item => ({
        fleet: item.label,
        events: item.count,
        fill: item.fill,
    }));

    const { useHighContrastCharts } = useTheme();

    const allTimeColor = useHighContrastCharts ? "var(--chart-hc-1)" : "var(--chart-1)";
    const selectedRangeColor = useHighContrastCharts ? "var(--chart-hc-2)" : "var(--chart-2)";
    const chartConfig = {
        'all_time': {
            label: "All Time (Unselected)",
            color: allTimeColor,
        },
        selected_range: {
            label: `Selected Range (${selectedPercentage.toFixed(1)}%)`,
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
