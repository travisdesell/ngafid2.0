// ngafid-frontend/src/app/pages/status/status.tsx
import React from "react";
import ProtectedNavbar from "@/components/navbars/protected_navbar";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { CircleQuestionMark, CircleCheck, TriangleAlert, CircleAlert, ArrowBigRightDash, LucideProps } from "lucide-react";
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import Ping, { PingColor } from "@/components/pings/ping";


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
};

type StatusResponse = {
    status?: StatusName;
    message?: string;
};

const STATUS_DEFAULT = StatusName.UNKNOWN;
const STATUS_DEFAULT_MESSAGE = "No message available...";

const STATUS_NAMES_LIST_KAFKA = ["flight-processing", "event-processing", "kafka", "chart-service", "database"] as const;
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
});


export default function Status() {

    const { setModal } = useModal();

    
    const [kafkaEntries, setKafkaEntries] = React.useState<StatusEntry[]>(
        () => STATUS_NAMES_LIST_KAFKA.map((name) => makeEntry(name))
    );
    const [dockerEntries, setDockerEntries] = React.useState<StatusEntry[]>(
        () => STATUS_NAMES_LIST_DOCKER.map((name) => makeEntry(name, (displayName) => displayName.replace(/^Ngafid/i, "")))
    );

    const kafkaEntriesOKCount = kafkaEntries.filter((entry) => entry.status.name === StatusName.OK).length;
    const allKafkaEntriesOK = (kafkaEntriesOKCount === kafkaEntries.length);
    const allKafkaEntriesUnchecked = (kafkaEntries.every((entry) => entry.status.name === StatusName.UNCHECKED));
    const kafkaEntriesStatusColor = allKafkaEntriesOK
        ? PingColor.GREEN
        : (allKafkaEntriesUnchecked ? PingColor.NEUTRAL : (kafkaEntriesOKCount > 0 ? PingColor.AMBER : PingColor.RED));

    const dockerEntriesOKCount = dockerEntries.filter((entry) => entry.status.name === StatusName.OK).length;
    const allDockerEntriesOK = (dockerEntriesOKCount === dockerEntries.length);
    const dockerEntriesStatusColor = allDockerEntriesOK
        ? PingColor.GREEN
        : (dockerEntriesOKCount > 0 ? PingColor.AMBER : PingColor.RED);


    React.useEffect(() => {
        
        const abort = new AbortController();

        const fetchStatuses = async (names: readonly string[]) => {

            // Fetch all statuses in parallel from the incoming names list
            const results = await Promise.allSettled(
                names.map(async (name) => {

                    const BASE_URL = `/api/status/`;
                    const targetURL = `${BASE_URL}${encodeURIComponent(name)}`;

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
                    return { name, statusName, message };

                })
            );

            // Map results to StatusEntry objects
            const updated: StatusEntry[] = results.map((r, i) => {

                const name = names[i];
                if (r.status === "fulfilled") {

                    const Icon = iconFor(r.value.statusName);
                    const display =
                        name.startsWith("ngafid-")
                            ? formatName(name).replace(/^Ngafid/i, "")
                            : formatName(name);
                    return {
                        name,
                        nameDisplay: display,
                        status: { name: r.value.statusName, icon: Icon },
                        message: r.value.message,
                    };

                } else {

                    const display =
                        name.startsWith("ngafid-")
                            ? formatName(name).replace(/^Ngafid/i, "")
                            : formatName(name);
                    return {
                        name,
                        nameDisplay: display,
                        status: { name: StatusName.ERROR, icon: CircleAlert },
                        message: "Request failed",
                    };

                }

            });

            return updated;

        };

        // Fetch both Kafka and Docker statuses in parallel
        (async () => {
            try {
                const [kafka, docker] = await Promise.all([
                    fetchStatuses(STATUS_NAMES_LIST_KAFKA),
                    fetchStatuses(STATUS_NAMES_LIST_DOCKER),
                ]);
                if (!abort.signal.aborted) {
                    setKafkaEntries(kafka);
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
            <ProtectedNavbar />

            <div className="page-content grid! grid-rows-2 gap-2 mx-auto w-full max-w-[1280px]">

                {/* Kafka Services */}
                <Card className="relative card-glossy my-auto h-[512px] w-full">

                    <Ping color={kafkaEntriesStatusColor} />

                    <CardHeader>
                        <CardTitle>Kafka Services</CardTitle>
                        <CardDescription>Displays the status of all back-end Kafka services.</CardDescription>
                    </CardHeader>

                    <CardContent>
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

                {/* Docker Services */}
                <Card className="relative card-glossy mx-auto my-auto h-[512px] w-full">

                    <Ping color={dockerEntriesStatusColor} />

                    <CardHeader>
                        <CardTitle>Docker Services</CardTitle>
                        <CardDescription>Displays the status of all back-end Docker services.</CardDescription>
                    </CardHeader>

                    <CardContent>
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

            </div>
        </div>
    );
}
