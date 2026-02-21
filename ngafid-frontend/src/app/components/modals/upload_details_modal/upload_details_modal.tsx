// ngafid-frontend/src/app/components/modals/upload_details_modal.tsx
import { Card, CardContent } from "@/components/ui/card";
import { motion } from "motion/react";

import { useModal } from '@/components/modals/modal_context';
import type { ModalData, ModalProps } from "@/components/modals/types";
import { getLogger } from "@/components/providers/logger";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { Badge } from "@/components/ui/badge";
import { fetchJson } from "@/fetchJson";
import { APIError, UploadErrorsPayload, UploadImportItem } from "@/pages/protected/uploads/_types/types";
import { AlertTriangle, Check, CircleAlert, CloudDownload } from "lucide-react";
import { useEffect, useState } from "react";

const log = getLogger("UploadDetailsModal", "black", "Modal");


export type ModalDataUploadDetails = ModalData & {
    uploadImportData: UploadImportItem;
};

export default function UploadDetailsModal({ data }: ModalProps) {

    const { renderModalHeader } = useModal();
    const { uploadImportData } = (data as ModalDataUploadDetails) ?? {};

    const [errorDetails, setErrorDetails] = useState<UploadErrorsPayload>({
        uploadErrors: [],
        flightWarnings: [],
        flightErrors: []
    });
    const [loadingDetails, setLoadingDetails] = useState(false);
    const [loadingError, setLoadingError] = useState<string | null>(null);

    log(`Rendering with uploadImportData: ${JSON.stringify(uploadImportData)}`);

    useEffect(() => {

        let cancelled = false;

        const flagRepeatedFilenames = <T extends { filename: string; sameFilename?: boolean }>(entries: T[]) =>
            entries.map((entry, index) => ({
                ...entry,
                sameFilename: (index > 0 && entries[index - 1].filename === entry.filename)
            }));

        const loadErrorDetails = async () => {

            if (!uploadImportData?.id || uploadImportData.id <= 0)
                return;

            setLoadingDetails(true);
            setLoadingError(null);

            try {

                const response = await fetchJson.get<UploadErrorsPayload | APIError>(`/api/upload/${uploadImportData.id}/errors`);

                if (cancelled)
                    return;

                if (response && typeof response === "object" && "errorTitle" in response) {
                    const apiError = response as APIError;
                    setLoadingError(`${apiError.errorTitle ?? "Error Loading Details"}: ${apiError.errorMessage ?? "Unknown error"}`);
                    return;
                }

                const payload = (response as UploadErrorsPayload) ?? {
                    uploadErrors: [],
                    flightWarnings: [],
                    flightErrors: []
                };

                setErrorDetails({
                    uploadErrors: payload.uploadErrors ?? [],
                    flightWarnings: flagRepeatedFilenames(payload.flightWarnings ?? []),
                    flightErrors: flagRepeatedFilenames(payload.flightErrors ?? [])
                });

            } catch (err: any) {

                if (!cancelled)
                    setLoadingError(err?.message || "Unknown error while loading upload details.");

            } finally {

                if (!cancelled)
                    setLoadingDetails(false);

            }

        };

        void loadErrorDetails();

        return () => {
            cancelled = true;
        };

    }, [uploadImportData?.id]);

    function UploadMetaRow({ label, value }: { label: string; value: string | number | boolean | (() => string | number | boolean) }) {

        return (
            <div className="flex items-center justify-between">
                <span className="font-medium">{label}</span>
                <hr className="border-gray-500/25 mx-2 w-full" />
                <span>{typeof value === "function" ? value() : String(value)}</span>
            </div>
        );
        
    }

    function UploadMetaGroup({ groupName, children }: { groupName: string; children: React.ReactNode }) {

        return (
            <div className="rounded-lg p-4 border-muted-foreground border relative overflow-visible! my-2 flex flex-col gap-2">
                <h3 className="font-bold absolute bg-muted px-2 left-1/2 top-0 -translate-x-1/2 -translate-y-1/2">{groupName}</h3>
                <ol className="flex flex-col gap-2" type="1">
                    {children}
                </ol>
            </div>
        );

    }

    function UploadMeta() {

        return <Accordion type="single" collapsible className="w-full">
            <AccordionItem value="optionalInfo" className="border-transparent">
                <AccordionTrigger className="mb-6 mt-4">
                    <hr className='border-b border-gray-500/25 flex flex-1 mx-4'/>
                    Upload Metadata
                    <hr className='border-b border-gray-500/25 flex flex-1 mx-4'/>
                </AccordionTrigger>

                <AccordionContent className="flex flex-col gap-1 px-4 *:text-nowrap">

                        {/* Identifiers */}
                        <UploadMetaGroup groupName="Identifiers">
                            {UploadMetaRow({ label: "ID", value: uploadImportData.id })}
                            {UploadMetaRow({ label: "Identifier", value: uploadImportData.identifier })}
                            {UploadMetaRow({ label: "MD5 Hash", value: uploadImportData.md5Hash })}
                        </UploadMetaGroup>

                        {/* File Info */}
                        <UploadMetaGroup groupName="File Info">
                            {UploadMetaRow({ label: "Filename", value: uploadImportData.filename })}
                            {UploadMetaRow({ label: "Size (MB)", value: ()=>(uploadImportData.sizeBytes / (1024 * 1024)).toFixed(2) })}
                            {UploadMetaRow({ label: "Kind", value: uploadImportData.kind })}
                        </UploadMetaGroup>

                        {/* Uploader Info */}
                        <UploadMetaGroup groupName="Uploader Info">
                            {UploadMetaRow({ label: "Uploader", value: () => uploadImportData.uploaderId ? `User No. ${uploadImportData.uploaderId}` : "Unknown" })}
                            {UploadMetaRow({ label: "Fleet", value: () => uploadImportData.fleetId ? `Fleet No. ${uploadImportData.fleetId}` : "Unknown" })}
                        </UploadMetaGroup>

                        {/* Details */}
                        <UploadMetaGroup groupName="Details">
                            {UploadMetaRow({ label: "Start Time", value: uploadImportData.startTime })}
                            {UploadMetaRow({ label: "End Time", value: uploadImportData.endTime })}
                            {UploadMetaRow({ label: "Number of Chunks", value: uploadImportData.numberChunks })}
                        </UploadMetaGroup>

                </AccordionContent>

            </AccordionItem>
        </Accordion>

    }

    function FlightIssueRow({ filename, message, level, sameFilename }: { filename: string; message: string; level: "warning" | "error"; sameFilename?: boolean }) {

        const rowClass = (level === "error")
            ? "border-(--error) text-(--error)"
            : "border-(--warning) text-(--warning)";

        return (
            <li className={`${rowClass}`}>
                <span className={`font-semibold ${rowClass}`}>
                    {sameFilename ? "" : filename} 
                </span> — {message}
            </li>
        );

    }

    function UploadIssueRow({ message }: { message: string }) {
        return <div className="rounded-md border border-(--error) text-(--error) px-2 py-1">{message}</div>;
    }

    function ErrorDetailItems({ groups }: { groups: { label: string; filenames: Set<string>; count: number }[] }) {

        return <div className="flex flex-col gap-2">
            {groups.map((group, index) => (
                <li key={`error-group-${index}`} className="border-(--error) rounded-md p-2 text-wrap wrap-break-word">
                    <div className="flex underline underline-offset-4 mb-2">
                        <span className="font-semibold">{group.label}</span>
                        &nbsp;<span>({group.count} flights):</span>
                    </div>
                    <div className="text-xs text-muted-foreground">
                        {Array.from(group.filenames).join(", ")}
                    </div>
                </li>
            ))}
        </div>

    }

    function ErrorDetails() {

        const allowErrorDetailDisplay = (uploadImportData.errorFlights > 0 || uploadImportData.warningFlights > 0);
        const hasDetails = (
            (errorDetails.uploadErrors?.length ?? 0) > 0
            || (errorDetails.flightWarnings?.length ?? 0) > 0
            || (errorDetails.flightErrors?.length ?? 0) > 0
        );

        // Group flight errors by the error name, and collect the unique filenames associated with each error
        const errorGroups = errorDetails.flightErrors.reduce((acc, entry) => {
            const key = entry.message;
            if (!acc[key]) {
                acc[key] = {
                    label: key,
                    filenames: new Set<string>(),
                    count: 0
                };
            }
            acc[key].filenames.add(entry.filename);
            acc[key].count += 1;
            return acc;
        }, {} as Record<string, { label: string; filenames: Set<string>; count: number }>);

        // Convert the error groups into an array and sort by count descending
        const sortedErrorGroups = Object.values(errorGroups).sort((a, b) => b.count - a.count);
        log("Sorted Error Groups:", sortedErrorGroups);


        const warningGroups = errorDetails.flightWarnings.reduce((acc, entry) => {
            const key = entry.message;
            if (!acc[key]) {
                acc[key] = {
                    label: key,
                    filenames: new Set<string>(),
                    count: 0
                };
            }
            acc[key].filenames.add(entry.filename);
            acc[key].count += 1;
            return acc;
        }, {} as Record<string, { label: string; filenames: Set<string>; count: number }>);

        const sortedWarningGroups = Object.values(warningGroups).sort((a, b) => b.count - a.count);
        log("Sorted Warning Groups:", sortedWarningGroups);

        return <Accordion
            type="single"
            collapsible
            className="w-full **:disabled:opacity-50 **:disabled:pointer-events-none **:disabled:select-none"
            disabled={!allowErrorDetailDisplay}
        >
            <AccordionItem value="optionalInfo" className="border-transparent">
                <AccordionTrigger className="mb-6 mt-4">
                    <hr className='border-b border-gray-500/25 flex flex-1 mx-4'/>
                    Error Details
                    <hr className='border-b border-gray-500/25 flex flex-1 mx-4'/>
                </AccordionTrigger>

                <AccordionContent className="flex flex-col gap-1 px-4 *:text-wrap max-h-128 overflow-y-auto">

                    {
                        loadingDetails
                        ? <div className="text-xs text-muted-foreground py-2">Loading warnings and errors...</div>
                        : null
                    }

                    {
                        loadingError
                        ? <div className="text-xs text-(--error) py-2">{loadingError}</div>
                        : null
                    }

                    {
                        (!loadingDetails && !loadingError && !hasDetails)
                        ? <div className="text-xs text-muted-foreground py-2">No error or warning details were returned for this upload.</div>
                        : null
                    }

                    {
                        (errorDetails.uploadErrors?.length ?? 0) > 0
                        ? (
                            <UploadMetaGroup groupName="Upload Errors">
                                {errorDetails.uploadErrors.map((entry) => (
                                    <UploadIssueRow key={`upload-error-${entry.id}-${entry.message}`} message={entry.message} />
                                ))}
                            </UploadMetaGroup>
                        )
                        : null
                    }

                    {
                        (errorDetails.flightWarnings?.length ?? 0) > 0
                        ? (
                            <UploadMetaGroup groupName="Flight Warnings">
                                <ErrorDetailItems groups={sortedWarningGroups} />
                            </UploadMetaGroup>
                        )
                        : null
                    }

                    {
                        (errorDetails.flightErrors?.length ?? 0) > 0
                        ? (
                            <UploadMetaGroup groupName="Flight Errors">
                                <ErrorDetailItems groups={sortedErrorGroups} />
                            </UploadMetaGroup>
                        )
                        : null
                    }
                </AccordionContent>
            </AccordionItem>
        </Accordion>

    }

    function UploadCountBadge({ label, count, icon: Icon, colorClass }: { label: string; count: number; icon: React.ComponentType<{ className?: string }>; colorClass: string }) {

        return (
            <Badge className={`inline-flex justify-between grow items-center gap-1 rounded-md px-2 py-1 ${colorClass} dark:text-shadow-md`} variant={"outline"}>
                <span className="flex items-center gap-1">
                    <Icon className="h-3 w-3" />
                    {label}:
                </span>
                <span>
                    {count}
                </span>
            </Badge>
        );

    }

    const totalFlights = uploadImportData.validFlights + uploadImportData.errorFlights + uploadImportData.warningFlights;

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                {renderModalHeader("Upload Details", uploadImportData.filename, true)}
                <CardContent>

                    {/* Flight Counts */}
                    <div className="flex gap-2 text-xs w-full">
                        <UploadCountBadge label="Total" count={totalFlights} icon={CloudDownload} colorClass="bg-(--muted) dark:text-shadow-md" />
                        <UploadCountBadge label="Valid" count={uploadImportData.validFlights} icon={Check} colorClass="bg-(--info) dark:text-shadow-md" />
                        <UploadCountBadge label="Warnings" count={uploadImportData.warningFlights} icon={AlertTriangle} colorClass="bg-(--warning) dark:text-shadow-md" />
                        <UploadCountBadge label="Errors" count={uploadImportData.errorFlights} icon={CircleAlert} colorClass="bg-(--error) dark:text-shadow-md" />
                    </div>

                    {/* Error Details */}
                    {ErrorDetails()}

                    {/* Upload Metadata */}
                    {UploadMeta()}

                </CardContent>
            </Card>
        </motion.div>
    );
}