// ngafid-frontend/src/app/pages/summary/summary.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import ProtectedNavbar from "@/components/navbars/protected_navbar";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useEffect, useState, useRef } from "react";
import { Bell, Calendar, Check, CircleAlert, CloudDownload, Hourglass, Loader2Icon, RefreshCw, TriangleAlert, Upload } from "lucide-react";
import TimeHeader from "@/components/time_header/time_header";
import { Select, SelectItem, SelectTrigger, SelectValue, SelectContent } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { fetchJson } from "@/fetchJson";
import { ALL_AIRFRAMES_ID, ALL_AIRFRAMES_NAME, useAirframes } from "@/airframes_provider";
import { useTimeHeader } from "@/components/time_header/time_header_provider";
import { ChartSummaryEventCounts } from "./charts/chart-summary-event-counts";
import { AirframeEventCounts } from "src/types";
import { AIRFRAME_NAMES_IGNORED } from "@/lib/airframe_names_ignored";
import { ChartSummaryPercentageOfFlightsWithEvent } from "./charts/chart-summary-percentage-of-flights-with-event";
import { ChartSummaryEventTotals } from "./charts/chart-summary-event-totals";
import { ChartConfig, ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { Legend, Pie, PieChart } from "recharts";
import { ChartSummaryEventTotalsCopy } from "./charts/chart-summary-event-totals-copy";


type FlightHoursByAirframe = {
    airframe: string;
    airframe_id: number;
    num_flights: number;
    total_flight_hours: number;
};

type NotificationsData = {
    notifications: Array<{
        count: number;
        message: string;
        badgeType: string;
        name: string | undefined;
    }>;
}

type UploadStatistics = {
    total: number;
    ok: number;
    pending: number;
    errors: number;
}

type FlightImportStatistics = {
    total: number;
    valid: number;
    warnings: number;
    errors: number;
}

const UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED = -1;

export default function SummaryPage() {

    const { setModal } = useModal();
    const { airframes, airframeIDSelected, setAirframeIDSelected, airframeNameSelected, setAirframeNameSelected} = useAirframes();
    const { endpointStartDate, endpointEndDate, reapplyTrigger, renderDateRangeMonthly } = useTimeHeader();

    const [flightHoursByAirframe, setFlightHoursByAirframe] = useState<FlightHoursByAirframe[]>([]);
    const [notifications, setNotifications] = useState<NotificationsData | null>(null);
    const [uploadStatistics, setUploadStatistics] = useState<UploadStatistics | null>(null);
    const [flightImportStatistics, setFlightImportStatistics] = useState<FlightImportStatistics | null>(null);
    const [eventCountsByAirframe, setEventCountsByAirframe] = useState<AirframeEventCounts[]>([]);

    const fetchAllSummaryData = () => {

        //Fetch airframe flight hours / flight count
        fetchFlightHoursByAirframe();

        //Fetch notification statistics
        // fetchNotificationStatistics();

        //Fetch upload statistics
        fetchUploadStatistics();

        //Fetch flight import statistics
        fetchFlightImportStatistics();

        //Fetch event counts by airframe
        fetchEventCountsByAirframe();

    }

    //Fetch all summary data on page load
    useEffect(() => {
        fetchAllSummaryData();
    }, []);


    //...
    useEffect(() => {
        console.log("[EX] Summary - Time range or airframe changed, re-fetching all summary data...");
        fetchAllSummaryData();
    }, [reapplyTrigger]);


    const fetchFlightHoursByAirframe = async () => {

        console.log("Summary - Fetching fleet flight hours by airframe...");

        const params = new URLSearchParams({
            startDate: endpointStartDate,
            endDate: endpointEndDate,
            airframeID: airframeIDSelected.toString()
        });

        let data = await fetchJson.get<FlightHoursByAirframe[]>(
            "/api/flight/flight_hours_by_airframe",
            {params}
        ).catch((error) => {
            setModal(ErrorModal, { title: "Error fetching flight hours by airframe", message: error.toString() });
            return [];
        });

        //Filter out ignored airframe names
        data = (data ?? []).filter(d => !AIRFRAME_NAMES_IGNORED.includes(d.airframe));

        console.log("Summary - Fetched flight hours by airframe:", data);
        setFlightHoursByAirframe(data);

    };

    const fetchNotificationStatistics = () => {

        console.log("Summary - Fetching notification statistics...");

        const NOTIFICATION_TYPES = [
            'waitingUserCount',
            'unconfirmedTailsCount'
        ];

        for (const type of NOTIFICATION_TYPES) {

            fetch(
                `/api/notifications/statistics?type=${type}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                }
            )
                .then(response => response.json())
                .then(data => {

                    console.log(`Fetched notification statistics for type ${type}:`, data);

                    const newNotification = {
                        count: data.count,
                        message: data.message,
                        badgeType: data.badgeType,
                        name: data.name
                    };

                    console.log("New notification data:", newNotification);

                    setNotifications(prev => {
                        if (prev) {
                            return {
                                notifications: [...prev.notifications, newNotification]
                            };
                        } else {
                            return {
                                notifications: [newNotification]
                            };
                        }
                    });

                })
                .catch(error => {
                    setModal(ErrorModal, { title: "Error fetching notifications", message: error.toString() });
                });

        }

    };

    const fetchUploadStatistics = async () => {

        console.log("Summary - Fetching upload statistics...");

        const base = '/api/upload/count';
        const endpoints = {
            total: `${base}`,
            ok: `${base}/success`,
            pending: `${base}/pending`,
            errors: `${base}/error`,
        } as const;

        try {

            const handleUploadCountFetchError = (error: any, endpoint: string) => {

                setModal(ErrorModal, { title: "Error fetching upload statistics", message: `Error fetching from ${endpoint}: ${String(error)}` });
                return 0;

            }

            const [total, ok, pending, errors] = await Promise.all([
                fetchJson.get(endpoints.total).catch((e) => handleUploadCountFetchError(e, endpoints.total)),
                fetchJson.get(endpoints.ok).catch((e) => handleUploadCountFetchError(e, endpoints.ok)),
                fetchJson.get(endpoints.pending).catch((e) => handleUploadCountFetchError(e, endpoints.pending)),
                fetchJson.get(endpoints.errors).catch((e) => handleUploadCountFetchError(e, endpoints.errors)),
            ]);

            const next: UploadStatistics = {
                total: total ?? 0,
                ok: ok ?? 0,
                pending: pending ?? 0,
                errors: errors ?? 0,
            };

            console.log("Summary - Fetched upload statistics:", next);
            setUploadStatistics(next);

        } catch (error) {
            setModal(ErrorModal, { title: "Error fetching upload statistics", message: String(error) });
        }
        
    };

    const fetchFlightImportStatistics = async () => {

        console.log("Summary - Fetching flight import statistics...");

        const base = "/api/flight/count";
        const endpoints = {
            total: `${base}`,
            //  valid: `${base}/...`, //<-- Calculated implicitly as total - (warnings + errors)
            warnings: `${base}/with-warning`,
            errors: `${base}/with-error`,
        } as const;


        try {

            const handleFlightImportCountFetchError = (error: any, endpoint: string) => {

                setModal(ErrorModal, { title: "Error fetching flight import statistics", message: `Error fetching from ${endpoint}: ${String(error)}` });
                return 0;

            }

            const [total, warnings, errors] = await Promise.all([
                fetchJson.get(endpoints.total).catch((e) => handleFlightImportCountFetchError(e, endpoints.total)),
                fetchJson.get(endpoints.warnings).catch((e) => handleFlightImportCountFetchError(e, endpoints.warnings)),
                fetchJson.get(endpoints.errors).catch((e) => handleFlightImportCountFetchError(e, endpoints.errors)),
            ]);

            const valid = (total ?? 0) - ((warnings ?? 0) + (errors ?? 0));

            const next: FlightImportStatistics = {
                total: total ?? 0,
                valid: valid >= 0 ? valid : 0,
                warnings: warnings ?? 0,
                errors: errors ?? 0,
            };

            console.log("Summary - Fetched flight import statistics:", next);
            setFlightImportStatistics(next);

        } catch (error) {
            setModal(ErrorModal, { title: "Error fetching flight import statistics", message: String(error) });
        }

    };

    const fetchEventCountsByAirframe = async () => {

        console.log("Summary - Fetching event counts by airframe...");

        const fetchingAllAirframesData = (airframeIDSelected === ALL_AIRFRAMES_ID);

        const params = new URLSearchParams({
            startDate: endpointStartDate,
            endDate: endpointEndDate,
            airframeID: airframeIDSelected.toString()
        });

        const baseURL = "/api/event/count/by-airframe";
        let data = await fetchJson.get<AirframeEventCounts[]>(
            baseURL,
            {params}
        ).catch((error) => {
            setModal(ErrorModal, { title: "Error fetching event counts by airframe", message: error.toString() });
            return [];
        });

        let eventCountsArray = Object.values(data ?? {}) as AirframeEventCounts[];

        //Filter out ignored airframe names
        eventCountsArray = eventCountsArray.filter(d => !AIRFRAME_NAMES_IGNORED.includes(d.airframeName));

        //Not fetching all airframes, filter out non-selected airframes
        if (!fetchingAllAirframesData)
            eventCountsArray = eventCountsArray.filter(d => d.airframeName === airframeNameSelected);

        console.log("Summary - Fetched event counts by airframe:", eventCountsArray);
        setEventCountsByAirframe(eventCountsArray);

    }


    const renderSummaryBadge = (icon: React.ReactNode, label: string, value: string | number, color?: string) => (

        <Badge className={`flex flex-row items-center space-x-2 px-3 py-2 ${color} pointer-events-none dark:text-shadow-md`} variant={"outline"}>
            {icon}
            <span>{label}:</span>
            {
                (value === UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED)
                ?
                <Loader2Icon className="animate-spin ml-auto" size={16}/>
                :
                <span className="font-bold ml-auto">{value.toLocaleString('en', { useGrouping: true })}</span>
            }
        </Badge>

    );


    //Destructure upload statistics (with defaults)
    const {
        uploadsTotal,
        uploadsOk,
        uploadsPending,
        uploadsErrors
    } = {
        uploadsTotal: uploadStatistics?.total ?? UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED,
        uploadsOk: uploadStatistics?.ok ?? UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED,
        uploadsPending: uploadStatistics?.pending ?? UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED,
        uploadsErrors: uploadStatistics?.errors ?? UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED
    };

    //Destructure flight import statistics (with defaults)
    const {
        flightsTotal,
        flightsValid,
        flightsWarnings,
        flightsErrors
    } = {
        flightsTotal: flightImportStatistics?.total ?? UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED,
        flightsValid: flightImportStatistics?.valid ?? UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED,
        flightsWarnings: flightImportStatistics?.warnings ?? UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED,
        flightsErrors: flightImportStatistics?.errors ?? UPLOAD_FLIGHT_STAT_VALUE_NOT_LOADED
    };

    const renderNoDataAvailableMessage = () => (
        <span className="text-(--muted-foreground) text-base">No data available for the selected time range and airframe type(s).</span>
    )

    const render = () => (
        <div className="overflow-x-hidden flex flex-col h-[100vh]">

            {/* Navbar */}
            <ProtectedNavbar />

            {/* Page Content */}
            <div className="flex flex-col p-4 flex-1 min-h-0 overflow-y-auto gap-4">

                {/* Time Header */}
                <TimeHeader
                    onApply={() => { console.log("Summary - Applying time range...") }}
                    dependencies={[airframeIDSelected]}
                >

                    {/* Airframe Selection */}
                    <div className="flex flex-col gap-2 justify-start items-start">
                        <Label className="px-1">
                            Airframe Type
                        </Label>
                        <Select
                            value={airframeIDSelected.toString()}
                            onValueChange={(value) => {
                                const id = parseInt(value);
                                console.log("Summary - Selected airframe ID:", id);
                                setAirframeIDSelected(id);
                                setAirframeNameSelected(airframes.find(pair => pair.id === id)?.name || ALL_AIRFRAMES_NAME);
                            }}
                        >
                            <Button asChild variant="outline">
                                <SelectTrigger className="w-[200px]">
                                    <SelectValue placeholder="Airframe" />
                                </SelectTrigger>
                            </Button>
                            <SelectContent>
                                {
                                    airframes.map((pair) => (
                                        <SelectItem key={pair.id} value={pair.id.toString()}>{pair.name}</SelectItem>
                                    ))
                                }
                            </SelectContent>
                        </Select>
                    </div>

                </TimeHeader>

                {/* Summary Content */}
                <div className="grid gap-2 grid-cols-2">

                    {/* Flight Hours Table */}
                    <Card className="card-glossy">
                        <CardHeader>
                            <CardTitle className="flex justify-between">
                                Flight Hours
                                {renderDateRangeMonthly()}
                            </CardTitle>
                            <CardDescription>
                                Total number of flights and flight hours by airframe for this fleet.
                                {/* <div className="flex flex-row items-center gap-1 mt-1 text-sm text-[var(--c_text_subtle)] opacity-50">
                                    <Calendar size={16} className="mb-0.5"/> {endpointStartDate} — {endpointEndDate}
                                </div> */}
                            </CardDescription>
                        </CardHeader>
                        <CardContent>

                            {/* Flight Hours by Airframe Table */}
                            <table className="table-hover table-fixed rounded-lg w-full">

                                <colgroup>
                                    <col style={{ width: "40%" }} />
                                    <col style={{ width: "30%" }} />
                                    <col style={{ width: "30%" }} />
                                </colgroup>

                                <thead className="leading-16 border-b-1">
                                    <tr>
                                        <th>Airframe</th>
                                        <th className="text-right">Flights</th>
                                        <th className="text-right">Hours</th>
                                    </tr>
                                </thead>

                                <tbody className="leading-8 before:content-['\A']">

                                    {/* Empty spacer row */}
                                    <tr className="pointer-none bg-transparent">
                                        <td colSpan={3} className="h-6" />
                                    </tr>

                                    {
                                        flightHoursByAirframe.map((data, index) => (
                                            <tr key={index}>
                                                <td className="truncate whitespace-nowrap overflow-hidden">
                                                    {data.airframe}
                                                </td>
                                                <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                    {data.num_flights}
                                                </td>
                                                <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                    {data.total_flight_hours.toFixed(2)}
                                                </td>
                                            </tr>
                                        ))
                                    }

                                </tbody>

                            </table>

                            {
                                (flightHoursByAirframe.length === 0)
                                &&
                                renderNoDataAvailableMessage()
                            }

                        </CardContent>
                    </Card>

                    {/* Right Rows */}
                    <div className="grid gap-2 grid-rows-2 grid-cols-1">

                        {/* Event Count Pie & Notifications */}
                        <div className="grid gap-2 grid-rows-1 grid-cols-[minmax(0,_2fr)_minmax(0,_1fr)]">

                            {/* Event Count Pie */}
                            <Card className="card-glossy">

                                <CardHeader>
                                    <CardTitle className="flex justify-between">
                                        Event Totals (WIP)
                                        {renderDateRangeMonthly()}
                                    </CardTitle>
                                    <CardDescription>
                                        Total event count for this fleet.
                                    </CardDescription>
                        
                                </CardHeader>

                                <CardContent className="flex">

                                    {/* Event Totals Chart */}
                                    <ChartSummaryEventTotals />

                                    {/* Event Totals Chart (ALT) */}
                                    <ChartSummaryEventTotalsCopy />

                                </CardContent>

                            </Card>

                            {/* Notifications */}
                            <Card className="card-glossy">
                                <CardHeader>
                                    <CardTitle>Notifications (WIP)</CardTitle>
                                    <CardDescription>
                                        View notifications and alerts.
                                    </CardDescription>
                                </CardHeader>
                                <CardContent>
                                    {/* <p>
                                        Here you can add various summary statistics, charts, and other relevant information about your fleet.
                                    </p> */}
                                    {/* <i>No notifications at this time...</i> */}

                                    {/* [EX] Unconfirmed Tail Numbers */}
                                    <Badge className={`flex flex-row items-center space-x-2 px-3 py-2 bg-(--notification) pointer-events-none w-full justify-between`} variant={"outline"}>
                                        <div className="flex flex-row items-center space-x-2">
                                            <Bell />
                                            <span>Unconfirmed Tail Numbers:</span>
                                        </div>
                                        <span className="font-bold">{(99999).toLocaleString('en', { useGrouping: true })}</span>
                                    </Badge>

                                </CardContent>
                            </Card>
                        </div>

                        {/* Uploads & Imports */}
                        <Card className="card-glossy">
                            <CardHeader>
                                <CardTitle>Uploads</CardTitle>
                                <CardDescription>
                                    Total number of file uploads and imported flights.
                                </CardDescription>
                            </CardHeader>
                            <CardContent className="grid gap-2">

                                {/* Uploads Row */}
                                <div className="grid gap-2 grid-cols-4 grid-rows-1">
                                    {renderSummaryBadge(<Upload size={16} />, "Uploads", uploadsTotal, 'bg-(--muted)')}
                                    {renderSummaryBadge(<Check size={16} />, "Uploads OK", uploadsOk, 'bg-(--normal)')}
                                    {renderSummaryBadge(<Hourglass size={16} />, "Uploads Pending", uploadsPending, 'bg-(--warning)')}
                                    {renderSummaryBadge(<CircleAlert size={16} />, "Uploads with Errors", uploadsErrors, 'bg-(--error)')}
                                </div>

                                {/* Uploads Row */}
                                <div className="grid gap-2 grid-cols-4 grid-rows-1">
                                    {renderSummaryBadge(<CloudDownload size={16} />, "Flights Imported", flightsTotal, 'bg-(--muted)')}
                                    {renderSummaryBadge(<Check size={16} />, "Flights Valid", flightsValid, 'bg-(--normal)')}
                                    {renderSummaryBadge(<TriangleAlert size={16} />, "Flights with Warnings", flightsWarnings, 'bg-(--warning)')}
                                    {renderSummaryBadge(<CircleAlert size={16} />, "Flights with Errors", flightsErrors, 'bg-(--error)')}
                                </div>

                            </CardContent>
                        </Card>

                    </div>
                </div>

                {/* Plots */}
                <div className="grid gap-2 grid-cols-2 grid-rows-1">
                    <ChartSummaryEventCounts data={eventCountsByAirframe} renderNoDataAvailableMessage={renderNoDataAvailableMessage} />
                    <ChartSummaryPercentageOfFlightsWithEvent data={eventCountsByAirframe} renderNoDataAvailableMessage={renderNoDataAvailableMessage} />
                    {/* <ChartSummaryPercentageOfFlightsWithEvent data={eventCountsByAirframe} /> */}
                    {/* <ChartSummaryEventCounts data={eventCountsByAirframe} /> */}
                    {/* <ChartBarStacked /> */}
                </div>
                {/* <Card> <CardHeader> <CardTitle>Plots</CardTitle> <CardDescription> Placeholder for future plots and visualizations. </CardDescription> </CardHeader> <CardContent className="min-h-0 h-[400px] max-h-[400px]"> <p> Future implementation will include various plots and visualizations to represent fleet data. </p> </CardContent> </Card> */}

            </div>

        </div>

    );

    return render();

}