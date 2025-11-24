// ngafid-frontend/src/app/pages/event_definitions/event_definitions.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { getLogger } from "@/components/providers/logger";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { fetchJson } from "@/fetchJson";
import { useEffect, useMemo, useState } from "react";
import { EventDefinitions } from "src/types";


const log = getLogger("EventDefinitions", "black", "Page");


type RowGeneric = {
    eventName: string;
    description: string;
};

type RowSpecific = {
    eventName: string;
    airframe: string;
    description: string;
};

const ANY_KEYS = new Set(["Any", "Any Airframe"]);
const PADDING_TOP_GROUP = "64px";
const PADDING_TOP_NORMAL = "6px";

const isUnknownAirframe = (airframeName: string) => /^Unknown Airframe \((\d+)\)$/.test(airframeName);
const unknownIndex = (airframeName: string) => {
    const airframeIsUnknown = airframeName.match(/^Unknown Airframe \((\d+)\)$/);
    return airframeIsUnknown ? parseInt(airframeIsUnknown[1], 10) : Number.POSITIVE_INFINITY;
};


export default function EventDefinitionsPage() {

    useEffect(() => {
        document.title = `NGAFID — Definitions`;
    });

    const { setModal } = useModal();

    const [descriptions, setDescriptions] = useState<EventDefinitions>({});
    const [loading, setLoading] = useState(true);
    const [filterText, setFilterText] = useState("");
    const descriptionsFiltered = useMemo(() => {

        // No filter, return all descriptions
        if (filterText.trim() === "")
            return descriptions;

        const filterTextLower = filterText.toLowerCase();
        const out: EventDefinitions = {};

        for (const eventName of Object.keys(descriptions)) {

            const perAirframe = descriptions[eventName];
            for (const airframe of Object.keys(perAirframe)) {

                const desc = perAirframe[airframe];

                // Event name, airframe, or description matches filter...
                if (
                    eventName.toLowerCase().includes(filterTextLower) ||
                    airframe.toLowerCase().includes(filterTextLower) ||
                    desc.toLowerCase().includes(filterTextLower)
                ) {

                    // No entry yet for this event, create it
                    if (!out[eventName])
                        out[eventName] = {};

                    // Add the matching description
                    out[eventName][airframe] = desc;
                }

            }

        }

        return out;

    }, [descriptions, filterText]);

    useEffect(() => {

        log("Fetching event definition descriptions...");

        fetchJson
            .get("/api/event/definition/description")
            .then((data: any) => {

                const errorTitle = data?.err_title ?? data?.errorTitle;
                const errorMessage = data?.err_msg ?? data?.errorMessage;

                // Data contains an error message, display error modal
                if (errorMessage) {
                    setModal(ErrorModal, { title: errorTitle || "Error", message: errorMessage });
                    return;
                }

                setDescriptions(data as EventDefinitions);

                log.table("Fetched event definitions:", data);

            })
            .catch((error: any) => {

                setModal(ErrorModal, {
                    title: "Error Getting Event Description",
                    message: String(error),
                });

            })
            .finally(() => {
                setLoading(false);
            });

        log("Finished fetching event definition descriptions.");

    }, [setModal]);


    // Construct rows for the Generic and Specific tables
    const { rowsGeneric, rowsSpecific } = useMemo(() => {

        const rowsGenericOut: RowGeneric[] = [];
        const rowsSpecificOut: RowSpecific[] = [];

        // Deterministic event order
        const eventNames = Object.keys(descriptionsFiltered).sort((a, b) => a.localeCompare(b));

        for (const eventName of eventNames) {

            const perAirframe = descriptionsFiltered[eventName] || {};

            // Collect keys deterministically
            const airframes = Object.keys(perAirframe).sort((a, b) => a.localeCompare(b));

            for (const airframe of airframes) {
                const desc = perAirframe[airframe];

                if (ANY_KEYS.has(airframe))
                    rowsGenericOut.push({ eventName, description: desc });
                else
                    rowsSpecificOut.push({ eventName, airframe, description: desc });

            }

        }

        /*
            Sort specific (per-airframe) rows:

            1. Alphabetically by event name
            2. Within event, known airframes alphabetically first
            3. 'Unknown Airframe (N)' after known airframes (sorted by N)
        */
        rowsSpecificOut.sort((r1, r2) => {

            const byEvent = r1.eventName.localeCompare(r2.eventName);

            // Different events, sort by event name
            if (byEvent !== 0)
                return byEvent;

            const u1 = isUnknownAirframe(r1.airframe);
            const u2 = isUnknownAirframe(r2.airframe);

            if (u1 && !u2)
                return 1;
            if (!u1 && u2)
                return -1;
            if (u1 && u2)
                return unknownIndex(r1.airframe) - unknownIndex(r2.airframe);
            
            // Both known airframes, sort alphabetically
            return r1.airframe.localeCompare(r2.airframe);

        });

        /*
            Sort generic rows:

            1. Alphabetically by event name
            2. Within event, alphabetically by description
        */
        rowsGenericOut.sort((a, b) => {

            const byEvent = a.eventName.localeCompare(b.eventName);

            // Different events, sort by event name
            if (byEvent !== 0)
                return byEvent;

            // Same event, sort by description
            return a.description.localeCompare(b.description);

        });

        return { rowsGeneric: rowsGenericOut, rowsSpecific: rowsSpecificOut };

    }, [descriptionsFiltered]);


    const topPaddingForSpecificRow = (index: number): string => {

        // At first row, apply 0 padding
        if (index === 0)
            return PADDING_TOP_NORMAL;

        const rowSpecificPrev = rowsSpecific[index - 1];
        const rowSpecificCur = rowsSpecific[index];
        return (rowSpecificPrev.eventName === rowSpecificCur.eventName) ? PADDING_TOP_NORMAL : PADDING_TOP_GROUP;

    };


    const render = () => {

        return (
            <div className="flex flex-col flex-1 min-h-0 overflow-hidden">
                <div className="max-w-[1280px] w-full mx-auto flex flex-col p-4 flex-1 min-h-0 gap-2">
                    <Card className="card-glossy flex-1 min-h-0 overflow-hidden flex flex-col">

                        <CardHeader className="shrink-0">
                            <CardTitle>Event Definitions</CardTitle>
                            <CardDescription>View the event definitions for each aircraft type.</CardDescription>
                        </CardHeader>

                        <CardContent className="flex-1 min-h-0 overflow-y-auto">
                            {
                                (loading)
                                ?
                                <p>Loading event definitions...</p>
                                :
                                <>
                                    {/* Generic Entries */}
                                    <h1 className="ml-1 mb-6 mt-8 text-2xl">
                                        Generic Event Definitions
                                    </h1>
                                    <Table className="w-full">
                                        <TableHeader>
                                            <TableRow>
                                                <TableHead>Event Name</TableHead>
                                                <TableHead>Event Definition</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {
                                                rowsGeneric.map((row, idx) => (
                                                    <TableRow key={`g-${row.eventName}-${idx}`}>

                                                        {/* Event Name */}
                                                        <TableCell className="font-medium">
                                                            {row.eventName}
                                                        </TableCell>

                                                        {/* Event Description */}
                                                        <TableCell className="font-normal">
                                                            {row.description}
                                                        </TableCell>

                                                    </TableRow>
                                                ))
                                            }
                                        </TableBody>
                                    </Table>


                                    {/* Specific Entries */}
                                    <h1 className="ml-1 mb-6 mt-12 text-2xl">
                                        Per-Airframe Event Definitions
                                    </h1>
                                    <Table className="w-full">
                                        <TableHeader>
                                            <TableRow>
                                                <TableHead>Event Name</TableHead>
                                                <TableHead>Aircraft Type</TableHead>
                                                <TableHead>Event Definition</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {
                                                rowsSpecific.map((row, idx) => {

                                                    const paddingTop = topPaddingForSpecificRow(idx);
                                                    const currentAirframeUnknown = isUnknownAirframe(row.airframe);

                                                    return (
                                                        <TableRow key={`s-${row.eventName}-${row.airframe}-${idx}`}>

                                                            {/* Event Name */}
                                                            <TableCell style={{ paddingTop }} className="font-bold">
                                                                {row.eventName}
                                                            </TableCell>

                                                            {/* Airframe Name */}
                                                            <TableCell style={{ paddingTop }}>
                                                                {
                                                                    (currentAirframeUnknown)
                                                                    ?
                                                                    <i className="opacity-50">{row.airframe}</i>
                                                                    :
                                                                    <span className="font-bold">{row.airframe}</span>
                                                                }
                                                            </TableCell>

                                                            {/* Event Description */}
                                                            <TableCell style={{ paddingTop }} className="font-normal">
                                                                {row.description}
                                                            </TableCell>

                                                        </TableRow>
                                                    );

                                                })
                                            }
                                        </TableBody>
                                    </Table>
                                </>
                            }
                        </CardContent>
                    </Card>

                    <Input
                        className="bg-background w-full shrink-0"
                        placeholder="Search event definitions..."
                        value={filterText}
                        onChange={(e) => setFilterText(e.target.value)}
                    />

                </div>
            </div>
        );

    };

    return render();

}