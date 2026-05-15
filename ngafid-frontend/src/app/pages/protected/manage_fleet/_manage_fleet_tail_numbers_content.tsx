// ngafid-frontend/src/app/pages/protected/manage_fleet/_manage_fleet_tail_numbers_content.tsx

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { fetchJson } from "@/fetchJson";
import { Check, Loader2, Save } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

interface TailRecordResponse {
    systemId: string | number;
    fleetId: number;
    tail?: string | null;
    confirmed?: boolean;
}

interface TailRecord {
    systemId: string;
    fleetId: number;
    tail: string;
    originalTail: string;
    draftTail: string;
    confirmed: boolean;
}

interface UpdateTailResponse {
    fleetId?: number;
    systemId?: string | number;
    tail?: string | null;
    errorTitle?: string;
    errorMessage?: string;
}

function normalizeTailRecord(record: TailRecordResponse): TailRecord {
    const tail = record.tail ?? "";

    return {
        systemId: String(record.systemId),
        fleetId: record.fleetId,
        tail,
        originalTail: tail,
        draftTail: tail,
        confirmed: Boolean(record.confirmed),
    };
}

function sortTailRecords(records: Array<TailRecord>) {
    return [...records].sort((a, b) => {
        const aNumeric = Number(a.systemId);
        const bNumeric = Number(b.systemId);

        if (Number.isFinite(aNumeric) && Number.isFinite(bNumeric))
            return aNumeric - bNumeric;

        return a.systemId.localeCompare(b.systemId);
    });
}

function TailNumbersTable({
    title,
    description,
    records,
    savingSystemId,
    onDraftChange,
    onSave,
}: {
    title: string;
    description: string;
    records: Array<TailRecord>;
    savingSystemId: string;
    onDraftChange: (systemId: string, value: string) => void;
    onSave: (record: TailRecord) => void;
}) {
    return (
        <Card className="card-glossy">

            {/* Header */}
            <CardHeader className="gap-4 sm:flex-row sm:items-start sm:justify-between sm:space-y-0">
                <div className="flex flex-col space-y-1.5">
                    <CardTitle>{title}</CardTitle>
                    <CardDescription>{description}</CardDescription>
                </div>

                {/* Unconfirmed Tail Number Count */}
                <span className="rounded-md border bg-background px-2 py-1 text-sm text-foreground">
                    Count: {records.length}
                </span>

            </CardHeader>

            <CardContent>
                <div className="overflow-x-auto rounded-md border bg-background">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="min-w-32">System ID</TableHead>
                                <TableHead className="min-w-44">Tail Number</TableHead>
                                <TableHead className="w-32" />
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {records.map((record) => {
                                const draftTail = record.draftTail.trim();
                                const isModified = draftTail.length > 0 && draftTail !== record.originalTail;

                                return (
                                    <TableRow key={record.systemId}>
                                        <TableCell className="font-medium">{record.systemId}</TableCell>
                                        <TableCell>
                                            <Input
                                                value={record.draftTail}
                                                onChange={(event) => onDraftChange(record.systemId, event.target.value)}
                                                placeholder={record.originalTail || "Enter tail number"}
                                                disabled={savingSystemId.length > 0}
                                            />
                                        </TableCell>
                                        <TableCell>
                                            <Button
                                                type="button"
                                                size="sm"
                                                disabled={!isModified || savingSystemId.length > 0}
                                                onClick={() => onSave(record)}
                                            >
                                                {savingSystemId === record.systemId ? <Loader2 className="animate-spin" /> : <Save />}
                                                Save
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}

                            {records.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={3} className="h-24 text-center text-muted-foreground">
                                        No tail numbers to show.
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </div>
            </CardContent>
        </Card>
    );
}

export default function ManageFleetTailNumbersContent() {

    const { setModal } = useModal();

    const [records, setRecords] = useState<Array<TailRecord>>([]);
    const [loading, setLoading] = useState(true);
    const [savingSystemId, setSavingSystemId] = useState("");

    const loadTailRecords = useCallback(async () => {
        setLoading(true);

        const response = await fetchJson.get<Array<TailRecordResponse>>("/api/aircraft/system-id").catch((error: Error) => {
            setModal(ErrorModal, { title: "Failed to fetch tail numbers", message: error.message });
            return null;
        });

        if (response)
            setRecords(sortTailRecords(response.map(normalizeTailRecord)));

        setLoading(false);
    }, [setModal]);

    useEffect(() => {
        loadTailRecords();
    }, [loadTailRecords]);

    const unconfirmedRecords = useMemo(
        () => records.filter((record) => !record.confirmed),
        [records],
    );

    const confirmedRecords = useMemo(
        () => records.filter((record) => record.confirmed),
        [records],
    );

    const updateDraftTail = (systemId: string, value: string) => {
        setRecords((current) => current.map((record) => (
            record.systemId === systemId
                ? { ...record, draftTail: value }
                : record
        )));
    };

    const saveTailNumber = async (record: TailRecord) => {
        const tail = record.draftTail.trim();

        if (tail.length === 0)
            return;

        setSavingSystemId(record.systemId);

        const payload = new URLSearchParams({ tail });
        const response = await fetchJson.patch<UpdateTailResponse>(`/api/aircraft/system-id/${encodeURIComponent(record.systemId)}`, payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Updating Tail Number", message: error.message });
            return null;
        });

        if (!response) {
            setSavingSystemId("");
            return;
        }

        if (response.errorTitle) {
            setModal(ErrorModal, { title: response.errorTitle, message: response.errorMessage ?? "" });
            setSavingSystemId("");
            return;
        }

        setRecords((current) => sortTailRecords(current.map((tailRecord) => (
            tailRecord.systemId === record.systemId
                ? {
                    ...tailRecord,
                    tail,
                    originalTail: tail,
                    draftTail: tail,
                    confirmed: true,
                }
                : tailRecord
        ))));
        setSavingSystemId("");
    };

    // Loading, show loader circle
    if (loading)
        return <Loader2 size={128} className="animate-spin mr-2 text-gray-500 absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2" />;

    return (
        <div className="flex flex-col gap-6 pb-6">

            <div className="grid gap-6 xl:grid-cols-2">
                <TailNumbersTable
                    title="Unconfirmed Tail Numbers"
                    description="Assign tail numbers to newly discovered system IDs."
                    records={unconfirmedRecords}
                    savingSystemId={savingSystemId}
                    onDraftChange={updateDraftTail}
                    onSave={saveTailNumber}
                />

                <TailNumbersTable
                    title="Confirmed Tail Numbers"
                    description="Update tail numbers that have already been confirmed."
                    records={confirmedRecords}
                    savingSystemId={savingSystemId}
                    onDraftChange={updateDraftTail}
                    onSave={saveTailNumber}
                />
            </div>

        </div>
    );
}
