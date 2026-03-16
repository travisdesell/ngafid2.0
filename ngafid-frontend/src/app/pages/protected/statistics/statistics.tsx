// ngafid-frontend/src/app/pages/protected/statistics/statistics.tsx

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { ALL_AIRFRAMES_ID, useAirframes } from "@/components/providers/airframes_provider";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { fetchJson } from "@/fetchJson";
import { AIRFRAME_NAMES_IGNORED } from "@/lib/airframe_names_ignored";
import { ChevronDown, ChevronUp, Loader2 } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";


const log = getLogger("Statistics", "black", "Page");

type MonthStatsRowRaw = {
	rowName?: unknown;
	flightsWithoutError?: unknown;
	flightsWithEvent?: unknown;
	totalEvents?: unknown;
	minSeverity?: unknown;
	avgSeverity?: unknown;
	maxSeverity?: unknown;
	minDuration?: unknown;
	avgDuration?: unknown;
	maxDuration?: unknown;
	aggFlightsWithoutError?: unknown;
	aggFlightsWithEvent?: unknown;
	aggTotalEvents?: unknown;
	aggMinSeverity?: unknown;
	aggAvgSeverity?: unknown;
	aggMaxSeverity?: unknown;
	aggMinDuration?: unknown;
	aggAvgDuration?: unknown;
	aggMaxDuration?: unknown;
};

type EventStatsRaw = {
	eventName?: unknown;
	totalFlights?: unknown;
	processedFlights?: unknown;
	humanReadable?: unknown;
	eventId?: unknown;
	monthStats?: unknown;
};

type AirframeStatsRaw = {
	airframeNameId?: unknown;
	airframeName?: unknown;
	events?: unknown;
};

type MonthStatsRow = {
	rowName: string;
	flightsWithoutError: number;
	flightsWithEvent: number;
	totalEvents: number;
	minSeverity: number;
	avgSeverity: number;
	maxSeverity: number;
	minDuration: number;
	avgDuration: number;
	maxDuration: number;
	aggFlightsWithoutError: number;
	aggFlightsWithEvent: number;
	aggTotalEvents: number;
	aggMinSeverity: number;
	aggAvgSeverity: number;
	aggMaxSeverity: number;
	aggMinDuration: number;
	aggAvgDuration: number;
	aggMaxDuration: number;
};

type EventStats = {
	eventName: string;
	totalFlights: number;
	processedFlights: number;
	humanReadable: string;
	eventId: number | null;
	monthStats: MonthStatsRow[];
};

type AirframeStats = {
	airframeNameId: number;
	airframeName: string;
	events: EventStats[];
};

type AirframeCardData = {
	id: number;
	name: string;
};

type AirframeStatisticsCardProps = {
	airframeId: number;
	airframeName: string;
};

const toNumber = (value: unknown, fallback = 0): number => {
	const numberParsed = Number(value);
	return Number.isFinite(numberParsed) ? numberParsed : fallback;
};

const toCount = (value: unknown): number => Math.max(0, Math.round(toNumber(value, 0)));

const toText = (value: unknown, fallback: string): string => {
	if (typeof value === "string") {
		const trimmed = value.trim();
		return (trimmed.length > 0) ? trimmed : fallback;
	}

	if (typeof value === "number" && Number.isFinite(value))
		return String(value);

	return fallback;
};

const formatPercent = (numerator: number, denominator: number): string => {
	const value = (denominator > 0) ? ((100 * numerator) / denominator) : 0;
	return value.toFixed(2);
};

const formatRatio = (numerator: number, denominator: number): string =>
	`${numerator.toLocaleString()} / ${denominator.toLocaleString()} (${formatPercent(numerator, denominator)}%)`;

const formatTriple = (min: number, avg: number, max: number): string =>
	`${min.toFixed(2)} / ${avg.toFixed(2)} / ${max.toFixed(2)}`;

const isZeroish = (value: number): boolean => Math.abs(value) < 1e-9;

const dimIfEmpty = (baseClassName: string, isEmpty: boolean): string =>
	isEmpty
		? `${baseClassName} text-foreground/55`
		: baseClassName;

const renderDescriptionWithCode = (description: string): ReactNode[] => {
	const nodes: ReactNode[] = [];
	let keyIndex = 0;

	let plainTextBuffer = "";
	let codeBuffer = "";
	let depth = 0;

	const flushPlainText = () => {
		if (plainTextBuffer.length === 0)
			return;

		nodes.push(plainTextBuffer);
		plainTextBuffer = "";
	};

	const flushCode = () => {
		if (codeBuffer.length === 0)
			return;

		nodes.push(
			<code
				key={`event-description-code-${keyIndex++}`}
				className="rounded bg-background px-1 py-0.5 font-mono"
			>
				{codeBuffer}
			</code>
		);

		codeBuffer = "";
	};

	for (const char of description) {
		if (char === "(") {
			if (depth === 0)
				flushPlainText();

			depth += 1;
			codeBuffer += char;
			continue;
		}

		if (char === ")" && depth > 0) {
			codeBuffer += char;
			depth -= 1;

			if (depth === 0)
				flushCode();

			continue;
		}

		if (depth > 0)
			codeBuffer += char;
		else
			plainTextBuffer += char;
	}

	// Unbalanced opening parenthesis: fall back to plain text rendering for remainder.
	if (depth > 0)
		plainTextBuffer += codeBuffer;

	flushPlainText();

	return nodes;
};

const normalizeMonthStatsRow = (value: unknown, rowIndex: number): MonthStatsRow | null => {
	if (!value || typeof value !== "object")
		return null;

	const raw = value as MonthStatsRowRaw;

	return {
		rowName: toText(raw.rowName, `Row ${rowIndex + 1}`),
		flightsWithoutError: toCount(raw.flightsWithoutError),
		flightsWithEvent: toCount(raw.flightsWithEvent),
		totalEvents: toCount(raw.totalEvents),
		minSeverity: toNumber(raw.minSeverity),
		avgSeverity: toNumber(raw.avgSeverity),
		maxSeverity: toNumber(raw.maxSeverity),
		minDuration: toNumber(raw.minDuration),
		avgDuration: toNumber(raw.avgDuration),
		maxDuration: toNumber(raw.maxDuration),
		aggFlightsWithoutError: toCount(raw.aggFlightsWithoutError),
		aggFlightsWithEvent: toCount(raw.aggFlightsWithEvent),
		aggTotalEvents: toCount(raw.aggTotalEvents),
		aggMinSeverity: toNumber(raw.aggMinSeverity),
		aggAvgSeverity: toNumber(raw.aggAvgSeverity),
		aggMaxSeverity: toNumber(raw.aggMaxSeverity),
		aggMinDuration: toNumber(raw.aggMinDuration),
		aggAvgDuration: toNumber(raw.aggAvgDuration),
		aggMaxDuration: toNumber(raw.aggMaxDuration),
	};
};

const normalizeEventStats = (value: unknown, eventIndex: number): EventStats | null => {
	if (!value || typeof value !== "object")
		return null;

	const raw = value as EventStatsRaw;
	const eventName = toText(raw.eventName, `Event ${eventIndex + 1}`);
	const eventIdRaw = Number(raw.eventId);
	const eventId = Number.isFinite(eventIdRaw) ? Math.round(eventIdRaw) : null;

	const monthStats = Array.isArray(raw.monthStats)
		? raw.monthStats
			.map((row, index) => normalizeMonthStatsRow(row, index))
			.filter((row): row is MonthStatsRow => row !== null)
		: [];

	return {
		eventName,
		totalFlights: toCount(raw.totalFlights),
		processedFlights: toCount(raw.processedFlights),
		humanReadable: toText(raw.humanReadable, "No description available."),
		eventId,
		monthStats,
	};
};

const normalizeAirframeStats = (value: unknown, fallbackAirframeId: number, fallbackAirframeName: string): AirframeStats | null => {
	if (!value || typeof value !== "object")
		return null;

	const raw = value as AirframeStatsRaw;

	const events = Array.isArray(raw.events)
		? raw.events
			.map((event, index) => normalizeEventStats(event, index))
			.filter((event): event is EventStats => event !== null)
		: [];

	return {
		airframeNameId: toCount(raw.airframeNameId ?? fallbackAirframeId),
		airframeName: toText(raw.airframeName, fallbackAirframeName),
		events,
	};
};

function AirframeStatisticsCard({ airframeId, airframeName }: AirframeStatisticsCardProps) {

	const { setModal } = useModal();

	const [expanded, setExpanded] = useState(false);
	const [isLoading, setIsLoading] = useState(false);
	const [isLoaded, setIsLoaded] = useState(false);
	const [stats, setStats] = useState<AirframeStats | null>(null);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);

	const fetchAirframeStats = useCallback(async () => {
		setIsLoading(true);
		setErrorMessage(null);

		const response = await fetchJson.get<unknown>(`/api/event/count/by-airframe/${airframeId}`).catch((error) => {
			const message = String(error);

			setModal(ErrorModal, {
				title: "Error Getting Event Statistics",
				message,
			});

			setErrorMessage(message);
			return null;
		});

		if (!response) {
			setIsLoading(false);
			return;
		}

		const normalized = normalizeAirframeStats(response, airframeId, airframeName);
		if (!normalized) {
			const message = "Received malformed event statistics payload.";

			setModal(ErrorModal, {
				title: "Error Parsing Event Statistics",
				message,
				code: response,
			});

			setErrorMessage(message);
			setIsLoading(false);
			return;
		}

		setStats(normalized);
		setIsLoaded(true);
		setIsLoading(false);
	}, [airframeId, airframeName, setModal]);

	useEffect(() => {
		if (expanded && !isLoaded && !isLoading)
			void fetchAirframeStats();
	}, [expanded, isLoaded, isLoading, fetchAirframeStats]);

	const events = stats?.events ?? [];

	return (
		<Card className="card-glossy">
			<CardHeader>
				<div className="flex items-center justify-between gap-4">
					<div className="flex flex-col gap-1">
						<CardTitle className="text-xl">{airframeName} Event Statistics</CardTitle>
						<CardDescription>
							Expand to view per-event monthly statistics for your fleet versus other fleets.
						</CardDescription>
					</div>
					<Button
						variant="outline"
						onClick={() => setExpanded((previous) => !previous)}
						aria-expanded={expanded}
						aria-controls={`airframe-stats-${airframeId}`}
					>
						{expanded ? <ChevronUp /> : <ChevronDown />}
					</Button>
				</div>
			</CardHeader>

			{
				expanded
				&&
				<CardContent id={`airframe-stats-${airframeId}`} className="pt-0 space-y-3">
					{
						(isLoading)
						&&
						<div className="flex items-center gap-2">
							<Loader2 size={16} className="animate-spin" />
							<span>Loading event statistics...</span>
						</div>
					}

					{
						(!isLoading && errorMessage)
						&&
						<div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-destructive text-sm">
							{errorMessage}
						</div>
					}

					{
						(!isLoading && !errorMessage && events.length === 0)
						&&
						<div>
							No event statistics available for this airframe.
						</div>
					}

					{
						(!isLoading && !errorMessage)
						&&
						events.map((eventInfo, eventIndex) => {
							const eventKey = (eventInfo.eventId != null)
								? String(eventInfo.eventId)
								: `${eventInfo.eventName}_${eventIndex}`;

							const processedPercent = Math.max(
								0,
								Math.min(
									100,
									toNumber(formatPercent(eventInfo.processedFlights, eventInfo.totalFlights))
								)
							);

							return (
								<Card key={eventKey}>
									<CardHeader className="pb-3">
										<div className="flex flex-wrap items-center gap-3">
											<CardTitle className="text-base w-full md:w-auto md:min-w-56">
												{eventInfo.eventName}
											</CardTitle>

											<div className="min-w-72 flex-1">
												<Progress value={processedPercent} className="h-4" />
												<div className="text-xs text-muted-foreground mt-1">
													{`${eventInfo.processedFlights.toLocaleString()} / ${eventInfo.totalFlights.toLocaleString()} (${processedPercent.toFixed(2)}%) flights processed`}
												</div>
											</div>
										</div>
									</CardHeader>

									<CardContent className="pt-0">
												<p className="text-sm text-muted-foreground mb-3 whitespace-pre-wrap">
													{renderDescriptionWithCode(eventInfo.humanReadable)}
												</p>

										<Table>
											<TableHeader>
												<TableRow>
													<TableHead rowSpan={2} className="whitespace-nowrap">Period</TableHead>
													<TableHead colSpan={4} className="text-center border-r border-border pr-6">
														Your Fleet
													</TableHead>
													<TableHead colSpan={4} className="text-center">
														Other Fleets
													</TableHead>
												</TableRow>

												<TableRow>
													<TableHead className="text-right whitespace-nowrap">Flights With Event</TableHead>
													<TableHead className="text-right whitespace-nowrap">Total Events</TableHead>
													<TableHead className="text-right whitespace-nowrap">Severity (Min/Avg/Max)</TableHead>
													<TableHead className="text-right border-r border-border pr-6 whitespace-nowrap">Duration (s) (Min/Avg/Max)</TableHead>
													<TableHead className="text-right whitespace-nowrap">Flights With Event</TableHead>
													<TableHead className="text-right whitespace-nowrap">Total Events</TableHead>
													<TableHead className="text-right whitespace-nowrap">Severity (Min/Avg/Max)</TableHead>
													<TableHead className="text-right whitespace-nowrap">Duration (s) (Min/Avg/Max)</TableHead>
												</TableRow>
											</TableHeader>

											<TableBody>
												{
													eventInfo.monthStats.map((monthStats, monthIndex) => {
														const fleetFlightsEmpty = (monthStats.flightsWithEvent === 0);
														const fleetTotalEventsEmpty = (monthStats.totalEvents === 0);
														const fleetSeverityEmpty =
															isZeroish(monthStats.minSeverity)
															&& isZeroish(monthStats.avgSeverity)
															&& isZeroish(monthStats.maxSeverity);
														const fleetDurationEmpty =
															isZeroish(monthStats.minDuration)
															&& isZeroish(monthStats.avgDuration)
															&& isZeroish(monthStats.maxDuration);

														const aggregateFlightsEmpty = (monthStats.aggFlightsWithEvent === 0);
														const aggregateTotalEventsEmpty = (monthStats.aggTotalEvents === 0);
														const aggregateSeverityEmpty =
															isZeroish(monthStats.aggMinSeverity)
															&& isZeroish(monthStats.aggAvgSeverity)
															&& isZeroish(monthStats.aggMaxSeverity);
														const aggregateDurationEmpty =
															isZeroish(monthStats.aggMinDuration)
															&& isZeroish(monthStats.aggAvgDuration)
															&& isZeroish(monthStats.aggMaxDuration);

														return (
															<TableRow key={`${eventKey}_${monthIndex}`}>
																<TableCell>{monthStats.rowName}</TableCell>
																<TableCell className={dimIfEmpty("text-right", fleetFlightsEmpty)}>{formatRatio(monthStats.flightsWithEvent, monthStats.flightsWithoutError)}</TableCell>
																<TableCell className={dimIfEmpty("text-right", fleetTotalEventsEmpty)}>{monthStats.totalEvents.toLocaleString()}</TableCell>
																<TableCell className={dimIfEmpty("text-right", fleetSeverityEmpty)}>{formatTriple(monthStats.minSeverity, monthStats.avgSeverity, monthStats.maxSeverity)}</TableCell>
																<TableCell className={dimIfEmpty("text-right border-r border-border pr-6", fleetDurationEmpty)}>{formatTriple(monthStats.minDuration, monthStats.avgDuration, monthStats.maxDuration)}</TableCell>
																<TableCell className={dimIfEmpty("text-right", aggregateFlightsEmpty)}>{formatRatio(monthStats.aggFlightsWithEvent, monthStats.aggFlightsWithoutError)}</TableCell>
																<TableCell className={dimIfEmpty("text-right", aggregateTotalEventsEmpty)}>{monthStats.aggTotalEvents.toLocaleString()}</TableCell>
																<TableCell className={dimIfEmpty("text-right", aggregateSeverityEmpty)}>{formatTriple(monthStats.aggMinSeverity, monthStats.aggAvgSeverity, monthStats.aggMaxSeverity)}</TableCell>
																<TableCell className={dimIfEmpty("text-right", aggregateDurationEmpty)}>{formatTriple(monthStats.aggMinDuration, monthStats.aggAvgDuration, monthStats.aggMaxDuration)}</TableCell>
															</TableRow>
														);
													})
												}
											</TableBody>
										</Table>
									</CardContent>
								</Card>
							);
						})
					}
				</CardContent>
			}
		</Card>
	);

}

export default function StatisticsPage() {

	const { airframes } = useAirframes();

	useEffect(() => {
		document.title = "NGAFID — Event Statistics";
	}, []);

	const allAirframeCards = useMemo<AirframeCardData[]>(() => {
		const output: AirframeCardData[] = [{ id: 0, name: "Generic" }];

		for (const airframe of airframes) {
			if (airframe.id === ALL_AIRFRAMES_ID)
				continue;

			if (AIRFRAME_NAMES_IGNORED.includes(airframe.name))
				continue;

			output.push({ id: airframe.id, name: airframe.name });
		}

		const seenIds = new Set<number>();
		return output.filter((airframe) => {
			if (seenIds.has(airframe.id))
				return false;

			seenIds.add(airframe.id);
			return true;
		});
	}, [airframes]);

	log("Rendering Statistics page with airframes:", allAirframeCards);

	return (
		<div className="page-container">
			<div className="page-content gap-4 overflow-hidden">

				<Card className="card-glossy">
					<CardHeader>
						<CardTitle>Event Statistics</CardTitle>
						<CardDescription>
							Review per-airframe event performance, including monthly fleet statistics and comparisons against other fleets.
						</CardDescription>
					</CardHeader>
				</Card>

				<div className="flex flex-col flex-1 min-h-0 gap-3 overflow-y-auto">
					{
						allAirframeCards.map((airframe) => (
							<AirframeStatisticsCard
								key={airframe.id}
								airframeId={airframe.id}
								airframeName={airframe.name}
							/>
						))
					}
				</div>

			</div>
		</div>
	);

}