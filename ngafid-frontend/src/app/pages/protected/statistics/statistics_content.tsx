// ngafid-frontend/src/app/pages/protected/statistics/statistics_card.tsx

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { getLogger } from "@/components/providers/logger";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { fetchJson } from "@/fetchJson";
import { AirframeStats, AirframeStatsRaw, EVENT_STAT_MALFORMED, EventStats, EventStatsRaw, MonthStatsRow, MonthStatsRowRaw } from "@/pages/protected/statistics/types";
import { ReactNode, useCallback, useEffect, useState } from "react";


const log = getLogger("AirframeStatisticsContent", "black", "Page");


interface AirframeStatisticsCardProps {
    airframeId: number;
    airframeName: string;
    setIsLoading: (isLoading: boolean) => void;
    setShowNoDataAlert: (show: boolean) => void;
}

/*

    Formatting Methods

*/
const toNumber = (value: unknown, fallback = 0): number => {
    const numberParsed = Number(value);
    return Number.isFinite(numberParsed) ? numberParsed : fallback;
};

const toCount = (value: unknown): number => Math.max(0, Math.round(toNumber(value, 0)));

const toText = (value: unknown, fallback: string): string => {

    // ...
    if (typeof value === "string") {
        const trimmed = value.trim();
        return (trimmed.length > 0) ? trimmed : fallback;
    }

    // Finite numeric value -> Stringified value
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



/*

    Normalization Methods

*/
const normalizeEventStats = (value: unknown, eventIndex: number): EventStats | typeof EVENT_STAT_MALFORMED => {

    // Value is missing / not an object -> malformed
    if (!value || typeof value !== "object")
        return EVENT_STAT_MALFORMED;

    const raw = value as EventStatsRaw;
    const eventName = toText(raw.eventName, `Event ${eventIndex + 1}`);
    const eventIdRaw = Number(raw.eventId);
    const eventId = Number.isFinite(eventIdRaw) ? Math.round(eventIdRaw) : null;

    const monthStats = Array.isArray(raw.monthStats)
        ? raw.monthStats
            .map((row, index) => normalizeMonthStatsRow(row, index))
            .filter((row): row is MonthStatsRow => row !== EVENT_STAT_MALFORMED)
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

const normalizeMonthStatsRow = (value: unknown, rowIndex: number): MonthStatsRow | typeof EVENT_STAT_MALFORMED => {

    if (!value || typeof value !== "object")
        return EVENT_STAT_MALFORMED;

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


const normalizeAirframeStats = (value: unknown, fallbackAirframeId: number, fallbackAirframeName: string): AirframeStats | typeof EVENT_STAT_MALFORMED => {

    // Value is missing / not an object -> malformed
    if (!value || typeof value !== "object")
        return EVENT_STAT_MALFORMED;

    const raw = value as AirframeStatsRaw;

    const events = Array.isArray(raw.events)
        ? raw.events
            .map((event, index) => normalizeEventStats(event, index))
            .filter((event): event is EventStats => event !== EVENT_STAT_MALFORMED)
        : [];

    return {
        airframeNameId: toCount(raw.airframeNameId ?? fallbackAirframeId),
        airframeName: toText(raw.airframeName, fallbackAirframeName),
        events,
    };

};


const renderDescriptionWithCode = (description: string): Array<ReactNode> => {

    // Quick fix for singular "1 second" case (instead of "1 seconds")
    description = description.replaceAll("1 seconds", "1 second");

    const nodes: Array<ReactNode> = [];
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

        // Code buffer is empty, do nothing.
        if (codeBuffer.length === 0)
            return;

        nodes.push(
            <code
                key={`event-description-code-${keyIndex++}`}
                className="rounded bg-sidebar px-1 py-0.5 font-mono"
            >
                {codeBuffer}
            </code>
        );

        codeBuffer = "";
    };

    for (const char of description) {

        // Opening parenthesis, flush plain text buffer and start filling code buffer
        if (char === "(") {
            if (depth === 0)
                flushPlainText();

            depth += 1;
            codeBuffer += char;
            continue;
        }

        // Closing parenthesis, add to code buffer and flush if balanced
        if (char === ")" && depth > 0) {
            codeBuffer += char;
            depth -= 1;

            if (depth === 0)
                flushCode();

            continue;
        }

        // Inside parentheses, add to code buffer
        if (depth > 0)
            codeBuffer += char;

        // Otherwise, add to plain text buffer
        else
            plainTextBuffer += char;

    }

    // Unbalanced opening parenthesis: fall back to plain text rendering for remainder.
    if (depth > 0)
        plainTextBuffer += codeBuffer;

    flushPlainText();

    return nodes;
};

export default function AirframeStatisticsContent(props: AirframeStatisticsCardProps) {

    const { airframeId, airframeName, setIsLoading, setShowNoDataAlert } = props;

    const { setModal } = useModal();

    const [isLoaded, setIsLoaded] = useState(false);
    const [stats, setStats] = useState<AirframeStats | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const fetchAirframeStats = useCallback(async () => {

        log(`Fetching event statistics for airframe ID: ${airframeId}...`);

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

        // No response, exit
        if (!response) {
            setIsLoading(false);
            return;
        }

        log("Normalizing event statistics response for airframe ID", airframeId, "with response:", response);
        const normalized = normalizeAirframeStats(response, airframeId, airframeName);

        // Failed to normalize response, show error
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

        if (!isLoaded)
            void fetchAirframeStats();

    }, [isLoaded, fetchAirframeStats]);

    const events = stats?.events ?? [];
    const hasNoData = (isLoaded && !errorMessage && events.length === 0);

    useEffect(() => {
        setShowNoDataAlert(hasNoData);
    }, [hasNoData, setShowNoDataAlert]);

    return (
        <CardContent id={`airframe-stats-${airframeId}`} className="pt-0 space-y-2">

            {/* Error Message */}
            {
                (errorMessage)
                &&
                <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-destructive text-sm">
                    {errorMessage}
                </div>
            }

            {
                (!errorMessage && events.length > 0)
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
                        <Card key={eventKey} className="">
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

                                <Table className="w-full text-xs">
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead rowSpan={2} className="whitespace-nowrap">Period</TableHead>
                                            <TableHead colSpan={4} className="text-center border-r border-border pr-6">
                                                This Fleet
                                            </TableHead>
                                            <TableHead colSpan={4} className="text-center">
                                                Other Fleets
                                            </TableHead>
                                        </TableRow>

                                        <TableRow>

                                            {/* This Fleet */}
                                            <TableHead className="text-right whitespace-nowrap">Flights With Event</TableHead>
                                            <TableHead className="text-right whitespace-nowrap">Total Events</TableHead>
                                            <TableHead className="text-right ">Severity (Min/Avg/Max)</TableHead>
                                            <TableHead className="text-right border-r border-border pr-6">Duration (s) (Min/Avg/Max)</TableHead>

                                            {/* Other Fleets */}
                                            <TableHead className="text-right whitespace-nowrap">Flights With Event</TableHead>
                                            <TableHead className="text-right whitespace-nowrap">Total Events</TableHead>
                                            <TableHead className="text-right">Severity (Min/Avg/Max)</TableHead>
                                            <TableHead className="text-right">Duration (s) (Min/Avg/Max)</TableHead>

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

                                                        {/* Row Name */}
                                                        <TableCell>{monthStats.rowName}</TableCell>

                                                        {/* This Fleet */}
                                                        <TableCell className={dimIfEmpty("whitespace-nowrap text-right", fleetFlightsEmpty)}>{formatRatio(monthStats.flightsWithEvent, monthStats.flightsWithoutError)}</TableCell>
                                                        <TableCell className={dimIfEmpty("whitespace-nowrap text-right", fleetTotalEventsEmpty)}>{monthStats.totalEvents.toLocaleString()}</TableCell>
                                                        <TableCell className={dimIfEmpty("whitespace-nowrap text-right", fleetSeverityEmpty)}>{formatTriple(monthStats.minSeverity, monthStats.avgSeverity, monthStats.maxSeverity)}</TableCell>
                                                        <TableCell className={dimIfEmpty("whitespace-nowrap text-right border-r border-border pr-6", fleetDurationEmpty)}>{formatTriple(monthStats.minDuration, monthStats.avgDuration, monthStats.maxDuration)}</TableCell>

                                                        {/* Other Fleets */}
                                                        <TableCell className={dimIfEmpty("whitespace-nowrap text-right", aggregateFlightsEmpty)}>{formatRatio(monthStats.aggFlightsWithEvent, monthStats.aggFlightsWithoutError)}</TableCell>
                                                        <TableCell className={dimIfEmpty("whitespace-nowrap text-right", aggregateTotalEventsEmpty)}>{monthStats.aggTotalEvents.toLocaleString()}</TableCell>
                                                        <TableCell className={dimIfEmpty("whitespace-nowrap text-right", aggregateSeverityEmpty)}>{formatTriple(monthStats.aggMinSeverity, monthStats.aggAvgSeverity, monthStats.aggMaxSeverity)}</TableCell>
                                                        <TableCell className={dimIfEmpty("whitespace-nowrap text-right", aggregateDurationEmpty)}>{formatTriple(monthStats.aggMinDuration, monthStats.aggAvgDuration, monthStats.aggMaxDuration)}</TableCell>
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
    );

}
