// ngafid-frontend/src/app/pages/protected/uploads/upload_item.tsx

import { Badge, BadgeVariant } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { UploadImportItem, UploadInfo, UploadStatus } from "@/pages/protected/uploads/types";
import { AlertCircle, AlertTriangle, Check, CloudDownload, Download, Info, Loader, RotateCcw, Trash } from "lucide-react";


function bytesToKB(num: number) {
    return (num / 1000).toFixed(2);
}

function percent(progress: number, total: number) {

    // Total is unset or negative, assume 0%
    if (!total || total <= 0)
        return 0;

    // Clamp to [0.00, 100.00]
    return Math.min(
        100,
        Math.max(
            0,
            Number(((progress / total) * 100).toFixed(2))
        )
    );

}

function statusBadgeVariant(status: UploadStatus): { label: string; variant: BadgeVariant } {
    switch (status) {
        case "HASHING":
        case "ENQUEUED":
        case "UPLOADED":
            return { label: status.replace("_", " "), variant: "secondary" };
        case "UPLOADING":
        case "PROCESSING":
            return { label: status, variant: "outline" };
        case "PROCESSED_OK":
            return { label: "PROCESSED OK", variant: "outline" };
        case "PROCESSED_WARNING":
            return { label: "PROCESSED WARNING", variant: "outline" };
        case "UPLOADING_FAILED":
        case "FAILED_INTERRUPTED":
        case "FAILED_FILE_TYPE":
        case "FAILED_AIRCRAFT_TYPE":
        case "FAILED_ARCHIVE_TYPE":
        case "FAILED_UNKNOWN":
            return { label: status.replace("_", " "), variant: "destructive" };
        case "DERIVED":
            return { label: "DERIVED", variant: "outline" };
        default:
            return { label: status, variant: "secondary" };
    }
}

interface UploadItemProps {
    u: UploadInfo | UploadImportItem;
    isPending: boolean;
    busy: boolean;
    pending: Array<UploadInfo>;
    deletingUploadIds: Set<number>;
    retryUpload: (u: UploadInfo | UploadImportItem) => Promise<void>;
    deleteUpload: (u: UploadInfo | UploadImportItem) => Promise<void>;
    downloadUpload: (u: UploadInfo | UploadImportItem) => void;
    openUploadDetailsModal: (u: UploadImportItem) => void;
}

function UploadValidityProportion(u: UploadImportItem) {

    const totalFlights = (u.validFlights + u.warningFlights + u.errorFlights);
    
    // No flights, can't render proportion
    if (totalFlights === 0)
        return null;

    const validProportion = u.validFlights / totalFlights;
    const warningProportion = u.warningFlights / totalFlights;
    const errorProportion = u.errorFlights / totalFlights;

    return (
        <Tooltip disableHoverableContent>
            <TooltipTrigger className="flex w-full border border-border bg-muted rounded overflow-hidden h-3">
                <div className="h-full bg-(--normal)" style={{ width: `${validProportion * 100}%` }} />
                <div className="h-full bg-(--warning)" style={{ width: `${warningProportion * 100}%` }} />
                <div className="h-full bg-(--error)" style={{ width: `${errorProportion * 100}%` }} />
            </TooltipTrigger>
            <TooltipContent>

                <div className="grid grid-cols-3 grid-rows-3 gap-y-2">

                    {/* Valid */}
                    <div className="flex items-center gap-1">
                        <Check className="w-4 h-4 inline text-(--normal)" />
                        <span>Valid:</span>
                    </div>
                    <span className="text-right">{`${u.validFlights} flights`}</span>
                    <span className="text-right">{`${((100 * validProportion).toFixed(1))}%`}</span>
                    
                    {/* Warning */}
                    <div className="flex items-center gap-1">
                        <AlertTriangle className="w-4 h-4 inline text-(--warning)" />
                        <span>Warnings:</span>
                    </div>
                    <span className="text-right">{`${u.warningFlights} flights`}</span>
                    <span className="text-right">{`${((100 * warningProportion).toFixed(1))}%`}</span>
                    
                    {/* Error */}
                    <div className="flex items-center gap-1">
                        <AlertCircle className="w-4 h-4 inline text-(--error)" />
                        <span>Errors:</span>
                    </div>
                    <span className="text-right">{`${u.errorFlights} flights`}</span>
                    <span className="text-right">{`${((100 * errorProportion).toFixed(1))}%`}</span>

                </div>

            </TooltipContent>
        </Tooltip>
    );

}

export default function UploadItem(props: UploadItemProps) {

    const { u, isPending, busy, pending, deletingUploadIds, retryUpload, deleteUpload, downloadUpload, openUploadDetailsModal } = props;

    const uploadProgress = percent(u.progressSize ?? u.bytesUploaded ?? 0, u.totalSize ?? u.sizeBytes ?? 0);
    const progressBarColor = (u.status === "HASHING" ? "bg-secondary-foreground" : "");
    const { label, variant } = statusBadgeVariant(u.status);
    const isDeleting = (u.id !== -1 && deletingUploadIds.has(u.id));

    const hasAnyLocalActiveUpload = (busy || pending.some((p) => p.status === "HASHING" || p.status === "UPLOADING"));
    const disableAll = (u.status === "HASHING" || u.status === "UPLOADING" || isDeleting);
    const canRetry = (u.status === "UPLOADING_FAILED" || u.status === "FAILED_INTERRUPTED" || u.status === "FAILED_UNKNOWN");
    const retryDisabled = (disableAll || !canRetry || (u.status === "FAILED_UNKNOWN" && u.id === -1));
    const deleteDisabled = (disableAll || isPending || u.id === -1 || hasAnyLocalActiveUpload);
    const retryTooltipMessage = (() => {

        if (u.status === "FAILED_INTERRUPTED")
            return `Select the original file '${u.filename}' to resume this interrupted upload.`;

        if (u.status === "UPLOADING_FAILED")
            return "Retries the upload using the original local file if it is still available.";

        if (u.status === "FAILED_UNKNOWN")
            return "Requeues this upload on the server for processing again.";

        return "Retry is only available for interrupted or failed uploads.";

    })();
    const isImported = (u.status.includes("PROCESSED") || u.status === "DERIVED");
    const hasImportData = ("validFlights" in u);
    const totalFlights = hasImportData ? (u.validFlights + u.warningFlights + u.errorFlights) : 0;

    const importPendingMessage = (() => {

        switch (u.status) {

            case "HASHING":
                return "Hashing, please do not refresh or navigate away...";

            case "UPLOADING":
                return "Uploading, please do not refresh or navigate away...";

            case "UPLOADING_FAILED":
            case "FAILED_INTERRUPTED":
                return "Awaiting file reupload...";

            default:
                return "Awaiting import processing...";

        }

    })();

    const downloadIncomplete = (u.bytesUploaded < u.sizeBytes);
    const downloadDisabled = (
        disableAll
        || isPending
        || u.id === -1
        || downloadIncomplete
    );

    console.log("Progress Size: ", u.progressSize);

    return (
        <Card className={`w-full bg-background! transition-all ${isDeleting ? "opacity-50 pointer-events-none" : ""} `}>
            <CardContent className="p-0 px-4 py-2 flex items-center gap-4 h-16 *:h-full">

                {/* Left Items */}
                <div className="flex flex-row gap-4 w-[75%]">

                    {/* File Info */}
                    <div className="w-40 min-w-40 max-w-40 flex flex-col justify-around">
                        <Tooltip>
                            <TooltipContent>
                                <p>{u.filename}</p>
                            </TooltipContent>
                            <TooltipTrigger className="font-medium truncate underline decoration-dotted w-full text-left">
                                {u.filename}
                            </TooltipTrigger>
                        </Tooltip>
                        <div className="text-xs text-muted-foreground text-nowrap">{u.startTime ?? ""}</div>
                    </div>

                    {/* Upload Progress Bar */}
                    <div className="w-full flex flex-col justify-around gap-1 h-full ">
                        <Progress value={uploadProgress} className="h-3 my-1" indicatorClassName={progressBarColor} />
                        <div className="text-xs text-muted-foreground whitespace-nowrap">
                            {
                                (downloadIncomplete)
                                ?
                                `${bytesToKB(u.progressSize ?? u.bytesUploaded ?? 0)} / ${bytesToKB(u.totalSize ?? u.sizeBytes ?? 0)} kB (${uploadProgress.toFixed(2)}%)`
                                :
                                `${bytesToKB(u.totalSize ?? u.sizeBytes ?? 0)} kB`
                            }
                        </div>
                    </div>
                </div>

                {/* Status Badge */}
                <Badge variant={variant} className="w-36 h-fit!">
                    {label}
                </Badge>

                {/* Import Info / Loader */}
                <div className="flex items-center justify-between w-[25%]">
                {
                    (isImported && hasImportData)
                    ?
                    
                        <div className="flex flex-col gap-2 text-xs w-full">

                            {/* Validity */}
                            <UploadValidityProportion {...u} />

                            {/* Total Flights */}
                            <Badge className=" inline-flex justify-between items-center gap-1 bg-muted" variant={"outline"}>
                                <span className="flex items-center gap-1">
                                    <CloudDownload className="h-3 w-3" />
                                    Total Flights:
                                </span>
                                <span>
                                    {totalFlights}
                                </span>
                            </Badge>

                        </div>
                    :
                    <div className="mx-auto flex gap-2 text-xs text-muted-foreground py-2  items-center">
                        <Loader size={20} className="animate-spin duration-2000" />
                        <span>{importPendingMessage}</span>
                    </div>
                }
                </div>

                {/* Buttons */}
                <div className="flex justify-end gap-2 items-center">

                    {/* Delete / Retry Button */}
                    {
                        (canRetry)
                        ?
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <span>
                                    <Button
                                        size="icon"
                                        variant="ghost"
                                        disabled={retryDisabled}
                                        onClick={() => { void retryUpload(u); }}
                                        title="Retry upload"
                                    >
                                        <RotateCcw className="h-4 w-4" />
                                    </Button>
                                </span>
                            </TooltipTrigger>
                            <TooltipContent>
                                {retryTooltipMessage}
                            </TooltipContent>
                        </Tooltip>
                        :
                        <Button
                            size="icon"
                            variant="ghost"
                            disabled={deleteDisabled}
                            onClick={() => (u.id !== -1 ? deleteUpload(u) : null)}
                            title="Delete upload"
                            className="hover:bg-red-500/25 hover:text-red-500 focus:ring-red-500"
                        >
                            <Trash className="h-4 w-4" />
                        </Button>
                    }

                    {/* Download Button */}
                    <Button
                        size="icon"
                        variant="ghost"
                        disabled={downloadDisabled}
                        onClick={() => (u.id !== -1 ? downloadUpload(u) : null)}
                        title="Download uploaded file"
                    >
                        <Download className="h-4 w-4" />
                    </Button>


                    {/* Details Button */}
                    <Button
                        size="icon"
                        variant={"ghost"}
                        onClick={() => openUploadDetailsModal(u as UploadImportItem)}
                    >
                        <Info />
                        {/* Details */}
                    </Button>

                </div>

            </CardContent>
        </Card>
    );
};