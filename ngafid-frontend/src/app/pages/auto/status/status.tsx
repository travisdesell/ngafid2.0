// ngafid-frontend/src/app/pages/status/status.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import Ping, { PingColor } from "@/components/pings/ping";
import { getLogger } from "@/components/providers/logger";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ArrowBigRightDash, CircleAlert, CircleCheck, CircleQuestionMark, Loader2, LucideProps, TriangleAlert } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import React, { useEffect } from "react";


const log = getLogger("Status", "black", "Page");


enum StatusName {
    UNKNOWN = "UNKNOWN",
    OK = "OK",
    WARNING = "WARNING",
    ERROR = "ERROR",
    UNCHECKED = "UNCHECKED",
}

const STATUS_TUPLE_INDEX_NAME = 0;
const STATUS_TUPLE_INDEX_ICON = 1;
const STATUS_TUPLES = [
    [StatusName.UNKNOWN, CircleQuestionMark],
    [StatusName.OK, CircleCheck],
    [StatusName.WARNING, TriangleAlert],
    [StatusName.ERROR, CircleAlert],
    [StatusName.UNCHECKED, ArrowBigRightDash],
] as const;

type Status = {
    name: StatusName;
    icon: React.ForwardRefExoticComponent<
        Omit<LucideProps, "ref"> & React.RefAttributes<SVGSVGElement>
    >;
};

type StatusEntry = {
    name: string;
    nameDisplay: string;
    status: Status;
    message: string;
    messageDisplay: string;
};

type StatusResponse = {
    status?: StatusName;
    message?: string;
};

const STATUS_DEFAULT = StatusName.UNKNOWN;
const STATUS_DEFAULT_MESSAGE = "No message available...";

const STATUS_NAMES_LIST_KAFKA = ["flight-processing", "event-processing", "kafka", "chart-service"] as const;
const STATUS_NAMES_LIST_DATABASE = ["database"] as const;
const STATUS_NAMES_LIST_DOCKER = ["ngafid-email-consumer", "ngafid-event-consumer", "ngafid-event-observer", "ngafid-upload-consumer"] as const;



const formatName = (name: string) =>
    name
        .split("-")
        .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
        .join(" ");


const iconFor = (name: StatusName) =>
    (STATUS_TUPLES.find((t) => t[STATUS_TUPLE_INDEX_NAME] === name)
        ?.[STATUS_TUPLE_INDEX_ICON])
        ??
        CircleQuestionMark;


const makeEntry = (rawName: string, displayNameTransform?: (displayName: string) => string): StatusEntry => ({
    name: rawName,
    nameDisplay: displayNameTransform ? displayNameTransform(formatName(rawName)) : formatName(rawName),
    status: { name: STATUS_DEFAULT, icon: CircleQuestionMark },
    message: STATUS_DEFAULT_MESSAGE,
    messageDisplay: "",
});


export default function Status() {

    useEffect(() => {
        document.title = `NGAFID — Status`;
    });

    const { setModal } = useModal();

    
    const [kafkaEntries, setKafkaEntries] = React.useState<StatusEntry[]>(
        () => STATUS_NAMES_LIST_KAFKA.map((name) => makeEntry(name))
    );
    const [databaseEntries, setDatabaseEntries] = React.useState<StatusEntry[]>(
        () => STATUS_NAMES_LIST_DATABASE.map((name) => makeEntry(name))
    );
    const [dockerEntries, setDockerEntries] = React.useState<StatusEntry[]>(
        () => STATUS_NAMES_LIST_DOCKER.map((name) => makeEntry(name, (displayName) => displayName.replace(/^Ngafid/i, "")))
    );

    const kafkaEntriesOKCount = kafkaEntries.filter((entry) => entry.status.name === StatusName.OK).length;
    const allKafkaEntriesOK = (kafkaEntriesOKCount === kafkaEntries.length);
    const allKafkaEntriesUnchecked = (kafkaEntries.every((entry) => entry.status.name === StatusName.UNCHECKED));
    const kafkaEntriesStatusColor = (allKafkaEntriesOK)
        ? PingColor.GREEN
        : (allKafkaEntriesUnchecked ? PingColor.NEUTRAL : (kafkaEntriesOKCount > 0 ? PingColor.AMBER : PingColor.RED));

    const databaseEntriesOKCount = databaseEntries.filter((entry) => entry.status.name === StatusName.OK).length;
    const allDatabaseEntriesOK = (databaseEntriesOKCount === databaseEntries.length);
    const allDatabaseEntriesUnchecked = (databaseEntries.every((entry) => entry.status.name === StatusName.UNCHECKED));
    const databaseEntriesStatusColor = (allDatabaseEntriesOK)
        ? PingColor.GREEN
        : (allDatabaseEntriesUnchecked ? PingColor.NEUTRAL : (databaseEntriesOKCount > 0 ? PingColor.AMBER : PingColor.RED));

    const dockerEntriesOKCount = dockerEntries.filter((entry) => entry.status.name === StatusName.OK).length;
    const allDockerEntriesOK = (dockerEntriesOKCount === dockerEntries.length);
    const dockerEntriesStatusColor = (allDockerEntriesOK)
        ? PingColor.GREEN
        : (dockerEntriesOKCount > 0 ? PingColor.AMBER : PingColor.RED);


    React.useEffect(() => {
        
        const abort = new AbortController();

        const fetchStatuses = async (names: readonly string[]) => {

            log(`Fetching statuses for: `, names);

            // Fetch all statuses in parallel from the incoming names list
            const results = await Promise.allSettled(
                names.map(async (name) => {

                    const BASE_URL = `/api/status/`;
                    const targetURL = `${BASE_URL}${encodeURIComponent(name)}`;

                    log("Fetching status for ", name, "from", targetURL);

                    const res = await fetch(targetURL, {
                        method: "GET",
                        headers: { Accept: "application/json" },
                        signal: abort.signal,
                    });

                    // Status not OK, show error modal
                    if (!res.ok)
                        setModal(ErrorModal, {
                            title: `Error fetching status for ${name}`,
                            message: `${res.status} ${res.statusText}`,
                        });
                    
                    const data = (await res.json()) as StatusResponse;
                    const statusName = data.status ?? STATUS_DEFAULT;
                    const message = data.message ?? STATUS_DEFAULT_MESSAGE;

                    log(`Fetched status for ${name}:`, { statusName, message });

                    return { name, statusName, message };

                })
            );

            // Map results to StatusEntry objects
            const updated: StatusEntry[] = results.map((r, i) => {

                const name = names[i];
                let entryOut: StatusEntry;
                if (r.status === "fulfilled") {

                    const Icon = iconFor(r.value.statusName);
                    const display =
                        name.startsWith("ngafid-")
                            ? formatName(name).replace(/^Ngafid/i, "")
                            : formatName(name);

                    entryOut = {
                        name,
                        nameDisplay: display,
                        status: { name: r.value.statusName, icon: Icon },
                        message: r.value.message,
                        messageDisplay: "",
                    };

                } else {

                    const display =
                        name.startsWith("ngafid-")
                            ? formatName(name).replace(/^Ngafid/i, "")
                            : formatName(name);
                    
                    entryOut = {
                        name,
                        nameDisplay: display,
                        status: { name: StatusName.ERROR, icon: CircleAlert },
                        message: "Request failed",
                        messageDisplay: "",
                    };

                }

                const ANIMATION_DELAY_BASE = 400;
                const animationDelay = (ANIMATION_DELAY_BASE + i * 100); //<-- Stagger animations
                const animateMessage = (entry: StatusEntry, delay: number=0) => {

                    const MESSAGE_ANIMATION_RATE_MS = 10;

                    if (entry.messageDisplay.length === entry.message.length)
                        return;

                    setTimeout(animateMessage, MESSAGE_ANIMATION_RATE_MS+delay, entry);

                    entry.messageDisplay = entry.message.slice(0, entry.messageDisplay.length + 1);
                    setDockerEntries((prev) =>
                        prev.map((e) => (e.name === entry.name ? entry : e))
                    );
                    setKafkaEntries((prev) =>
                        prev.map((e) => (e.name === entry.name ? entry : e))
                    );

                }
                // animateMessage(entryOut, animationDelay);
                setTimeout(animateMessage, animationDelay, entryOut);

                return entryOut;

            });

            return updated;

        };

        // Fetch both Kafka and Docker statuses in parallel
        (async () => {
            try {
                const [kafka, database, docker] = await Promise.all([
                    fetchStatuses(STATUS_NAMES_LIST_KAFKA),
                    fetchStatuses(STATUS_NAMES_LIST_DATABASE),
                    fetchStatuses(STATUS_NAMES_LIST_DOCKER),
                ]);
                if (!abort.signal.aborted) {
                    setKafkaEntries(kafka);
                    setDatabaseEntries(database);
                    setDockerEntries(docker);
                }
            } catch (error) {
                setModal(ErrorModal, { title: "Error fetching statuses", message: (error instanceof Error) ? error.message : "Unknown error" });
            }
        })();

        return () => abort.abort();

    }, []);

    return (
        <div className="page-container">

            <div className="page-content grid grid-rows-2 gap-8 mx-auto w-full max-w-7xl min-h-screen py-8">

                {/* Kafka Services */}
                <Card className="relative card-glossy h-80 w-full self-center my-auto">

                    <Ping color={kafkaEntriesStatusColor} />

                    <CardHeader>
                        <CardTitle>Kafka Services</CardTitle>
                        <CardDescription>Displays the status of all back-end Kafka services.</CardDescription>
                    </CardHeader>

                    <CardContent className="overflow-y-auto">
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead className="w-[256px]">Service</TableHead>
                                    <TableHead className="w-[256px]">Status</TableHead>
                                    <TableHead>Message</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {
                                    kafkaEntries.map((entry) => (
                                        <TableRow key={entry.name}>
                                            <TableCell className="font-medium">{entry.nameDisplay}</TableCell>
                                            <TableCell className="flex items-center">
                                                <entry.status.icon className="inline mr-2" size={16} />
                                                {entry.status.name}
                                            </TableCell>
                                            <TableCell>{entry.message}</TableCell>
                                        </TableRow>
                                    ))
                                }
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>

                {/* Database Services */}
                <Card className="relative card-glossy h-64 w-full self-center my-auto">

                    <Ping color={databaseEntriesStatusColor} />

                    <CardHeader>
                        <CardTitle>Database Services</CardTitle>
                        <CardDescription>Displays the status of all back-end database services.</CardDescription>
                    </CardHeader>

                    <CardContent className="overflow-y-auto">
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead className="w-[256px]">Service</TableHead>
                                    <TableHead className="w-[256px]">Status</TableHead>
                                    <TableHead>Message</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {
                                    databaseEntries.map((entry) => (
                                        <TableRow key={entry.name}>
                                            <TableCell className="font-medium">{entry.nameDisplay}</TableCell>
                                            <TableCell className="flex items-center">
                                                <entry.status.icon className="inline mr-2" size={16} />
                                                {entry.status.name}
                                            </TableCell>
                                            <TableCell>{entry.message}</TableCell>
                                        </TableRow>
                                    ))
                                }
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>

                {/* Docker Services */}
                <Card className="relative card-glossy h-80 w-full self-center my-auto">

                    <Ping color={dockerEntriesStatusColor} />

                    <CardHeader>
                        <CardTitle>Docker Services</CardTitle>
                        <CardDescription>Displays the status of all back-end Docker services.</CardDescription>
                    </CardHeader>

                    <CardContent className="overflow-y-auto">
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead className="w-[256px]">Service</TableHead>
                                    <TableHead className="w-[256px]">Status</TableHead>
                                    <TableHead>Message</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {
                                    dockerEntries.map((entry) => (
                                        <TableRow key={entry.name}>
                                            <TableCell className="font-medium">{entry.nameDisplay}</TableCell>
                                            <TableCell className="flex items-center">
                                                <AnimatePresence>
                                                    <motion.div
                                                        className="absolute translate-y-1/2"
                                                        key={entry.status.name}
                                                        initial={{ opacity: 0 }}
                                                        animate={{ opacity: 1 }}
                                                        exit={{ opacity: 0 }}
                                                        transition={{ duration: 0.3 }}
                                                    >
                                                        <entry.status.icon className="inline mr-2" size={16} />
                                                        {entry.status.name}
                                                    </motion.div>
                                                </AnimatePresence>
                                            </TableCell>
                                            <TableCell className="relative">
                                                {entry.messageDisplay}
                                                <Loader2
                                                    className="animate-spin duration-200 transition-opacity absolute left-0 top-0 translate-x-1/2 translate-y-1/2"
                                                    size={16}
                                                    style={{
                                                        opacity: (entry.messageDisplay.length) ? 0.00 : 1.00
                                                    }}
                                                />
                                            </TableCell>
                                        </TableRow>
                                    ))
                                }
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>

            </div>
        </div>
    );
}
