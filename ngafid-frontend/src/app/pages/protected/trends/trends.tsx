// ngafid-frontend/src/app/pages/protected/trends/trends.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import PanelAlert from "@/components/panel_alert";
import { ALL_AIRFRAMES_ID, ALL_AIRFRAMES_NAME, useAirframes } from "@/components/providers/airframes_provider";
import { getLogger } from "@/components/providers/logger";
import TimeHeader from "@/components/providers/time_header/time_header";
import { useTimeHeader } from "@/components/providers/time_header/time_header_provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ChartConfig, ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { fetchJson } from "@/fetchJson";
import { AIRFRAME_NAMES_IGNORED } from "@/lib/airframe_names_ignored";
import { CircleQuestionMark, Download } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import { CartesianGrid, Line, LineChart, XAxis, YAxis } from "recharts";

const log = getLogger("Trends", "black", "Page");

const EVENT_ANY = "ANY Event";

type TrendsDataRaw = {
	airframeName?: unknown;
	eventName?: unknown;
	dates?: unknown;
	aggregateFlightsWithEventCounts?: unknown;
	aggregateTotalEventsCounts?: unknown;
	aggregateTotalFlightsCounts?: unknown;
	flightsWithEventCounts?: unknown;
	totalEventsCounts?: unknown;
	totalFlightsCounts?: unknown;
};

type TrendsData = {
	airframeName: string;
	eventName: string;
	dates: string[];
	aggregateFlightsWithEventCounts: number[];
	aggregateTotalEventsCounts: number[];
	aggregateTotalFlightsCounts: number[];
	flightsWithEventCounts: number[];
	totalEventsCounts: number[];
	totalFlightsCounts: number[];
};

type EventCountsByEvent = Record<string, Record<string, TrendsData>>;
type EventDescriptionResponse = Record<string, Record<string, string>>;

type CsvValue = {
	eventCount: number;
	flightsWithEventCount: number;
	totalFlights: number;
};

const toNumberArray = (value: unknown, expectedLength: number): number[] => {
	if (!Array.isArray(value))
		return new Array(expectedLength).fill(0);

	const out = value.map((v) => {
		const n = Number(v);
		return Number.isFinite(n) ? n : 0;
	});

	if (out.length < expectedLength)
		return [...out, ...new Array(expectedLength - out.length).fill(0)];

	if (out.length > expectedLength)
		return out.slice(0, expectedLength);

	return out;
};

const normalizeTrendsData = (value: unknown): TrendsData | null => {
	if (!value || typeof value !== "object")
		return null;

	const raw = value as TrendsDataRaw;
	const airframeName = String(raw.airframeName ?? "").trim();
	const eventName = String(raw.eventName ?? "").trim();
	const dates = Array.isArray(raw.dates)
		? raw.dates.map((d) => String(d ?? "")).filter((d) => d.length > 0)
		: [];

	if (!airframeName || !eventName || dates.length === 0)
		return null;

	return {
		airframeName,
		eventName,
		dates,
		aggregateFlightsWithEventCounts: toNumberArray(raw.aggregateFlightsWithEventCounts, dates.length),
		aggregateTotalEventsCounts: toNumberArray(raw.aggregateTotalEventsCounts, dates.length),
		aggregateTotalFlightsCounts: toNumberArray(raw.aggregateTotalFlightsCounts, dates.length),
		flightsWithEventCounts: toNumberArray(raw.flightsWithEventCounts, dates.length),
		totalEventsCounts: toNumberArray(raw.totalEventsCounts, dates.length),
		totalFlightsCounts: toNumberArray(raw.totalFlightsCounts, dates.length),
	};
};

const formatMonthTick = (dateText: string) => {
	const parsed = new Date(dateText);
	if (Number.isNaN(parsed.getTime()))
		return dateText;
	return parsed.toLocaleDateString(undefined, { month: "short", year: "2-digit" });
};

const percent = (num: number, den: number) => {
	if (!Number.isFinite(num) || !Number.isFinite(den) || den <= 0)
		return 0;
	return (100 * num) / den;
};

const buildMergedAnyEvent = (source: EventCountsByEvent): Record<string, TrendsData> => {
	const merged: Record<string, TrendsData> = {};

	for (const byAirframe of Object.values(source)) {
		for (const series of Object.values(byAirframe)) {
			if (AIRFRAME_NAMES_IGNORED.includes(series.airframeName))
				continue;

			const prev = merged[series.airframeName];
			if (!prev) {
				merged[series.airframeName] = {
					...series,
					eventName: EVENT_ANY,
					dates: [...series.dates],
					aggregateFlightsWithEventCounts: [...series.aggregateFlightsWithEventCounts],
					aggregateTotalEventsCounts: [...series.aggregateTotalEventsCounts],
					aggregateTotalFlightsCounts: [...series.aggregateTotalFlightsCounts],
					flightsWithEventCounts: [...series.flightsWithEventCounts],
					totalEventsCounts: [...series.totalEventsCounts],
					totalFlightsCounts: [...series.totalFlightsCounts],
				};
				continue;
			}

			for (let i = 0; i < prev.dates.length; i++) {
				prev.aggregateFlightsWithEventCounts[i] += series.aggregateFlightsWithEventCounts[i] ?? 0;
				prev.aggregateTotalEventsCounts[i] += series.aggregateTotalEventsCounts[i] ?? 0;
				prev.aggregateTotalFlightsCounts[i] += series.aggregateTotalFlightsCounts[i] ?? 0;
				prev.flightsWithEventCounts[i] += series.flightsWithEventCounts[i] ?? 0;
				prev.totalEventsCounts[i] += series.totalEventsCounts[i] ?? 0;
				prev.totalFlightsCounts[i] += series.totalFlightsCounts[i] ?? 0;
			}
		}
	}

	return merged;
};

export default function TrendsPage() {
	const location = useLocation();
	const { setModal } = useModal();
	const { endpointStartDate, endpointEndDate, reapplyTrigger, renderDateRangeMonthly } = useTimeHeader();
	const { airframes, airframeIDSelected, setAirframeIDSelected, airframeNameSelected, setAirframeNameSelected } = useAirframes();

	const isAggregatePage = location.pathname.includes("aggregate_trends");

	const [loading, setLoading] = useState(false);
	const [eventCounts, setEventCounts] = useState<EventCountsByEvent>({});
	const [eventChecked, setEventChecked] = useState<Record<string, boolean>>({ [EVENT_ANY]: false });
	const [eventDescriptions, setEventDescriptions] = useState<Record<string, string>>({});

	useEffect(() => {
		document.title = `NGAFID — ${isAggregatePage ? "Aggregate Trends" : "Trends"}`;
	}, [isAggregatePage]);

	const fetchEventDescriptions = async () => {
		const descriptions = await fetchJson.get<EventDescriptionResponse>("/api/event/definition/description").catch((error) => {
			setModal(ErrorModal, {
				title: "Error Fetching Event Descriptions",
				message: String(error),
			});
			return {} as EventDescriptionResponse;
		});

		const flattened: Record<string, string> = {};
		for (const [eventName, byAirframe] of Object.entries(descriptions ?? {})) {
			const preferred = byAirframe?.Any || byAirframe?.["Any Airframe"];
			const fallback = Object.values(byAirframe ?? {}).find((d) => (d ?? "").trim().length > 0);
			flattened[eventName] = preferred ?? fallback ?? "No description available.";
		}

		setEventDescriptions(flattened);
	};

	const fetchMonthlyEventCounts = async () => {
		setLoading(true);
		const params = new URLSearchParams({
			startDate: endpointStartDate,
			endDate: endpointEndDate,
			aggregatePage: String(isAggregatePage),
		});

		const response = await fetchJson.get<unknown>("/api/event/count/monthly/by-name", { params }).catch((error) => {
			setModal(ErrorModal, {
				title: "Error Fetching Monthly Event Counts",
				message: String(error),
			});
			return {};
		});

		const normalized: EventCountsByEvent = {};
		if (response && typeof response === "object") {
			for (const [eventName, byAirframe] of Object.entries(response as Record<string, unknown>)) {
				if (!byAirframe || typeof byAirframe !== "object")
					continue;

				const seriesByAirframe: Record<string, TrendsData> = {};
				for (const [airframeName, rawSeries] of Object.entries(byAirframe as Record<string, unknown>)) {
					if (AIRFRAME_NAMES_IGNORED.includes(airframeName))
						continue;

					const normalizedSeries = normalizeTrendsData(rawSeries);
					if (!normalizedSeries)
						continue;

					seriesByAirframe[airframeName] = normalizedSeries;
				}

				if (Object.keys(seriesByAirframe).length > 0)
					normalized[eventName] = seriesByAirframe;
			}
		}

		normalized[EVENT_ANY] = buildMergedAnyEvent(normalized);

		setEventCounts(normalized);
		setEventChecked((prev) => {
			const next: Record<string, boolean> = {};
			for (const eventName of Object.keys(normalized).sort())
				next[eventName] = prev[eventName] ?? false;
			if (!(EVENT_ANY in next))
				next[EVENT_ANY] = false;
			return next;
		});

		setLoading(false);
	};

	useEffect(() => {
		fetchEventDescriptions();
	}, []);

	useEffect(() => {
		fetchMonthlyEventCounts();
	}, [reapplyTrigger, isAggregatePage]);

	const availableEventNames = useMemo(() => {
		const names = Object.keys(eventCounts).filter((name) => name !== EVENT_ANY).sort((a, b) => a.localeCompare(b));
		return [EVENT_ANY, ...names];
	}, [eventCounts]);

	const selectedAirframeNames = useMemo(() => {
		if (airframeIDSelected === ALL_AIRFRAMES_ID)
			return new Set(airframes.map((af) => af.name));
		return new Set([airframeNameSelected]);
	}, [airframeIDSelected, airframeNameSelected, airframes]);

	const eventsEmpty = useMemo(() => {
		const out: Record<string, boolean> = {};
		for (const eventName of availableEventNames) {
			const byAirframe = eventCounts[eventName] ?? {};
			let hasAnyData = false;

			for (const series of Object.values(byAirframe)) {
				if (!selectedAirframeNames.has(series.airframeName))
					continue;

				const values = isAggregatePage
					? series.aggregateTotalEventsCounts
					: series.totalEventsCounts;

				if (values.some((n) => n > 0)) {
					hasAnyData = true;
					break;
				}
			}

			out[eventName] = !hasAnyData;
		}

		return out;
	}, [availableEventNames, eventCounts, selectedAirframeNames, isAggregatePage]);

	const chartModels = useMemo(() => {
		const countRowsMap = new Map<string, Record<string, string | number>>();
		const percentRowsMap = new Map<string, Record<string, string | number>>();
		const countConfig: ChartConfig = {};
		const percentConfig: ChartConfig = {};
		let countSeriesIndex = 0;
		let percentSeriesIndex = 0;

		const getCountRow = (date: string) => {
			let row = countRowsMap.get(date);
			if (!row) {
				row = { date };
				countRowsMap.set(date, row);
			}
			return row;
		};

		const getPercentRow = (date: string) => {
			let row = percentRowsMap.get(date);
			if (!row) {
				row = { date };
				percentRowsMap.set(date, row);
			}
			return row;
		};

		for (const eventName of availableEventNames) {
			if (!eventChecked[eventName])
				continue;

			const byAirframe = eventCounts[eventName] ?? {};

			const fleetFlightsWithEvent: Record<string, number> = {};
			const fleetTotalFlights: Record<string, number> = {};
			const aggregateFlightsWithEvent: Record<string, number> = {};
			const aggregateTotalFlights: Record<string, number> = {};

			for (const series of Object.values(byAirframe)) {
				if (!selectedAirframeNames.has(series.airframeName))
					continue;

				const countKey = `count_${eventName}_${series.airframeName}`.replace(/[^a-zA-Z0-9_]+/g, "_");
				countConfig[countKey] = {
					label: `${eventName} - ${series.airframeName}`,
					color: `var(--chart-${(countSeriesIndex % 12) + 1})`,
				};
				countSeriesIndex += 1;

				const eventCountsSeries = isAggregatePage
					? series.aggregateTotalEventsCounts
					: series.totalEventsCounts;

				for (let i = 0; i < series.dates.length; i++) {
					const date = series.dates[i]!;
					getCountRow(date)[countKey] = eventCountsSeries[i] ?? 0;

					fleetFlightsWithEvent[date] = (fleetFlightsWithEvent[date] ?? 0) + (series.flightsWithEventCounts[i] ?? 0);
					fleetTotalFlights[date] = (fleetTotalFlights[date] ?? 0) + (series.totalFlightsCounts[i] ?? 0);
					aggregateFlightsWithEvent[date] =
						(aggregateFlightsWithEvent[date] ?? 0) + (series.aggregateFlightsWithEventCounts[i] ?? 0);
					aggregateTotalFlights[date] =
						(aggregateTotalFlights[date] ?? 0) + (series.aggregateTotalFlightsCounts[i] ?? 0);
				}
			}

			const sortedDates = Object.keys(aggregateTotalFlights).sort((a, b) => a.localeCompare(b));
			if (sortedDates.length === 0)
				continue;

			if (!isAggregatePage) {
				const fleetKey = `pct_fleet_${eventName}`.replace(/[^a-zA-Z0-9_]+/g, "_");
				percentConfig[fleetKey] = {
					label: `${eventName} - Your Fleet`,
					color: `var(--chart-${(percentSeriesIndex % 12) + 1})`,
				};
				percentSeriesIndex += 1;

				for (const date of sortedDates)
					getPercentRow(date)[fleetKey] = percent(fleetFlightsWithEvent[date] ?? 0, fleetTotalFlights[date] ?? 0);
			}

			const aggregateKey = `pct_aggregate_${eventName}`.replace(/[^a-zA-Z0-9_]+/g, "_");
			percentConfig[aggregateKey] = {
				label: `${eventName} - ${isAggregatePage ? "All Fleets" : "All Other Fleets"}`,
				color: `var(--chart-${(percentSeriesIndex % 12) + 1})`,
			};
			percentSeriesIndex += 1;

			for (const date of sortedDates)
				getPercentRow(date)[aggregateKey] = percent(aggregateFlightsWithEvent[date] ?? 0, aggregateTotalFlights[date] ?? 0);
		}

		const countRows = [...countRowsMap.values()].sort((a, b) => String(a.date).localeCompare(String(b.date)));
		const percentRows = [...percentRowsMap.values()].sort((a, b) => String(a.date).localeCompare(String(b.date)));

		return {
			countRows,
			percentRows,
			countConfig,
			percentConfig,
		};
	}, [availableEventNames, eventChecked, eventCounts, selectedAirframeNames, isAggregatePage]);

	const exportCSV = () => {
		const selectedEvents = availableEventNames.filter((eventName) => eventChecked[eventName]);
		if (selectedEvents.length === 0)
			return;

		const csvValues: Record<string, Record<string, Record<string, CsvValue>>> = {};
		const csvAirframeNames = new Set<string>();
		const csvDates = new Set<string>();

		for (const eventName of selectedEvents) {
			const byAirframe = eventCounts[eventName] ?? {};
			for (const series of Object.values(byAirframe)) {
				if (!selectedAirframeNames.has(series.airframeName))
					continue;

				csvAirframeNames.add(series.airframeName);
				if (!csvValues[eventName])
					csvValues[eventName] = {};
				if (!csvValues[eventName]![series.airframeName])
					csvValues[eventName]![series.airframeName] = {};

				for (let i = 0; i < series.dates.length; i++) {
					const date = series.dates[i]!;
					csvDates.add(date);

					const eventCount = isAggregatePage
						? series.aggregateTotalEventsCounts[i] ?? 0
						: series.totalEventsCounts[i] ?? 0;

					const flightsWithEventCount = isAggregatePage
						? series.aggregateFlightsWithEventCounts[i] ?? 0
						: series.flightsWithEventCounts[i] ?? 0;

					const totalFlights = isAggregatePage
						? series.aggregateTotalFlightsCounts[i] ?? 0
						: series.totalFlightsCounts[i] ?? 0;

					csvValues[eventName]![series.airframeName]![date] = {
						eventCount,
						flightsWithEventCount,
						totalFlights,
					};
				}
			}
		}

		const eventNamesSorted = [...selectedEvents].sort((a, b) => a.localeCompare(b));
		const airframeNamesSorted = [...csvAirframeNames].sort((a, b) => a.localeCompare(b));
		const datesSorted = [...csvDates].sort((a, b) => a.localeCompare(b));

		let filetext = "";

		const addGroupedHeaderRow = (valueFor: (eventName: string, airframeName: string, index: number) => string) => {
			const columns: string[] = [];
			for (const eventName of eventNamesSorted) {
				for (const airframeName of airframeNamesSorted) {
					if (!csvValues[eventName]?.[airframeName])
						continue;
					columns.push(valueFor(eventName, airframeName, 0));
					columns.push(valueFor(eventName, airframeName, 1));
					columns.push(valueFor(eventName, airframeName, 2));
				}
			}
			filetext += `${columns.join(",")}\n`;
		};

		addGroupedHeaderRow((eventName) => eventName);
		addGroupedHeaderRow((_, airframeName) => airframeName);
		addGroupedHeaderRow((_, __, index) => ["Events", "Flights With Event", "Total Flights"][index]!);

		for (const date of datesSorted) {
			const row: string[] = [];
			for (const eventName of eventNamesSorted) {
				for (const airframeName of airframeNamesSorted) {
					if (!csvValues[eventName]?.[airframeName])
						continue;

					const values = csvValues[eventName]![airframeName]![date];
					if (!values) {
						row.push("", "", "");
					} else {
						row.push(
							String(values.eventCount),
							String(values.flightsWithEventCount),
							String(values.totalFlights),
						);
					}
				}
			}

			filetext += `${row.join(",")}\n`;
		}

		const anchor = document.createElement("a");
		anchor.setAttribute("href", `data:text/plain;charset=utf-8,${encodeURIComponent(filetext)}`);
		anchor.setAttribute("download", "trends.csv");
		anchor.style.display = "none";
		document.body.appendChild(anchor);
		anchor.click();
		document.body.removeChild(anchor);
	};

	const selectedHasData = (chartModels.countRows.length > 0 || chartModels.percentRows.length > 0);

	return (
		<div className="page-container">
			<div className="page-content gap-4">

				<TimeHeader onApply={() => log("Applying Trends filters...")} dependencies={[airframeIDSelected, isAggregatePage]}>
					<div className="flex items-end gap-3">
						<div className="flex flex-col gap-2">
							<Label className="px-1">Airframe Type</Label>
							<Select
								value={airframeIDSelected.toString()}
								onValueChange={(value) => {
									const id = parseInt(value, 10);
									setAirframeIDSelected(id);

									const selected = airframes.find((af) => af.id === id);
									setAirframeNameSelected(selected?.name ?? ALL_AIRFRAMES_NAME);
								}}
							>
								<SelectTrigger className="w-[220px]">
									<SelectValue placeholder="Select Airframe" />
								</SelectTrigger>
								<SelectContent>
									{airframes.map((af) => (
										<SelectItem key={af.id} value={af.id.toString()}>
											{af.name}
										</SelectItem>
									))}
								</SelectContent>
							</Select>
						</div>

						<Button variant="outline" onClick={exportCSV}>
							<Download />
							Export CSV
						</Button>
					</div>
				</TimeHeader>

				<div className="grid grid-cols-1 lg:grid-cols-12 gap-4 min-h-0 flex-1">

					<Card className="lg:col-span-3 card-glossy min-h-95 lg:min-h-0">
						<CardHeader>
							<CardTitle className="flex items-center gap-2">
								Event Selection
								{renderDateRangeMonthly()}
							</CardTitle>
							<CardDescription>Choose events to display in both charts.</CardDescription>
						</CardHeader>
						<CardContent className="space-y-2 max-h-[80dvh] flex justify-between">

                            {/* Event Checklist */}
                            <div className="h-full w-full flex flex-col overflow-y-auto gap-2">
							{availableEventNames.map((eventName) => {
								const disabled = eventsEmpty[eventName] ?? true;
								const checked = eventChecked[eventName] ?? false;
								const description = eventDescriptions[eventName] ?? "No description available.";

								return (
									<div key={eventName} className={`flex items-center gap-2 ${disabled ? "opacity-50" : ""}`}>
										<Checkbox
											checked={checked}
											disabled={disabled}
											onCheckedChange={() => {
												setEventChecked((prev) => ({
													...prev,
													[eventName]: !prev[eventName],
												}));
											}}
										/>
										{
											eventName === EVENT_ANY
												? <Label className="font-normal">{eventName}</Label>
												: (
													<Tooltip>
														<TooltipTrigger asChild>
															<Label className="font-normal cursor-help inline-flex items-center gap-1">
																{eventName}
																<CircleQuestionMark className="size-3.5" />
															</Label>
														</TooltipTrigger>
														<TooltipContent className="max-w-sm">{description}</TooltipContent>
													</Tooltip>
												)
										}
									</div>
								);
							})}
                            </div>
                            
                            {/* Chart 1 - Event Counts Over Time */}
                            <div className="w-full h-full bg-red-500/20">
								{
									chartModels.countRows.length === 0
										? <PanelAlert title="No Data Available!" description={["Select one or more events to render count trends."]} />
										: (
											<ChartContainer config={chartModels.countConfig} className="h-full w-full">
												<LineChart data={chartModels.countRows} margin={{ left: 8, right: 8, top: 8, bottom: 8 }} accessibilityLayer>
													<CartesianGrid vertical={false} />
													<XAxis
														dataKey="date"
														tickLine={false}
														axisLine={false}
														tickMargin={8}
														tickFormatter={formatMonthTick}
													/>
													<YAxis tickLine={false} axisLine={false} tickMargin={8} />
													<ChartTooltip
														content={<ChartTooltipContent labelFormatter={(value) => `Date: ${formatMonthTick(String(value))}`} />}
													/>
													<ChartLegend content={<ChartLegendContent />} />
													{
														Object.keys(chartModels.countConfig).map((seriesKey) => (
															<Line
																key={seriesKey}
																dataKey={seriesKey}
																type="monotone"
																stroke={`var(--color-${seriesKey})`}
																strokeWidth={2}
																dot={false}
																connectNulls
															/>
														))
													}
												</LineChart>
											</ChartContainer>
										)
								}
                            </div>

                            {/* Chart 2 - Percentage of Flights With Event Over Time */}
                            <div className="w-full h-full">
                            </div>

						</CardContent>
					</Card>

					<div className="lg:col-span-9 flex flex-col gap-4 min-h-0">

						<Card className="card-glossy min-h-80">
							<CardHeader>
								<CardTitle>Percentage of Flights With Event Over Time</CardTitle>
								<CardDescription>
									{
										isAggregatePage
											? "Monthly percentage of flights with selected events across all fleets."
											: "Monthly percentage of flights with selected events for your fleet vs all other fleets."
									}
								</CardDescription>
							</CardHeader>
							<CardContent className="h-90">
								{
									chartModels.percentRows.length === 0
										? <PanelAlert title="No Data Available!" description={["Select one or more events to render percentage trends."]} />
										: (
											<ChartContainer config={chartModels.percentConfig} className="h-full w-full">
												<LineChart data={chartModels.percentRows} margin={{ left: 8, right: 8, top: 8, bottom: 8 }} accessibilityLayer>
													<CartesianGrid vertical={false} />
													<XAxis
														dataKey="date"
														tickLine={false}
														axisLine={false}
														tickMargin={8}
														tickFormatter={formatMonthTick}
													/>
													<YAxis
														tickLine={false}
														axisLine={false}
														tickMargin={8}
														domain={[0, 100]}
														tickFormatter={(value) => `${value}%`}
													/>
													<ChartTooltip
														content={<ChartTooltipContent formatter={(value) => `${Number(value).toFixed(2)}%`} />}
													/>
													<ChartLegend content={<ChartLegendContent />} />
													{
														Object.keys(chartModels.percentConfig).map((seriesKey) => (
															<Line
																key={seriesKey}
																dataKey={seriesKey}
																type="monotone"
																stroke={`var(--color-${seriesKey})`}
																strokeWidth={2}
																dot={false}
																connectNulls
															/>
														))
													}
												</LineChart>
											</ChartContainer>
										)
								}
							</CardContent>
						</Card>

					</div>
				</div>

				{
					loading && !selectedHasData
						? <PanelAlert title="Loading Trends..." description="Fetching monthly event data." />
						: null
				}

			</div>
		</div>
	);
}