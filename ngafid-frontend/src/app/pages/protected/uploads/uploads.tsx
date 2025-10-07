// ngafid-frontend/src/app/pages/protected/uploads/uploads.tsx
"use client";

import React, { useCallback, useEffect, useState } from "react";
import SparkMD5 from "spark-md5";
import ProtectedNavbar from "@/components/navbars/protected_navbar";
import { useModal } from "@/components/modals/modal_provider";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge, BadgeVariant } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Input } from "@/components/ui/input";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import {
    Download,
    ChevronLeft,
    ChevronRight,
    AlertTriangle,
    CheckCircle2,
    Check,
    CloudDownload,
    Upload as UploadIcon,
    XCircle,
    Info,
    List,
    Trash,
    Loader,
    CircleAlert,
} from "lucide-react";
import ErrorModal from "@/components/modals/error_modal";
import { motion } from "motion/react";
import ConfirmModal from "@/components/modals/confirm_modal";
import { fetchJson } from "@/fetchJson";
import Md5Worker from "@/workers/md5.worker.ts?worker";
import { CHUNK_SIZE } from "@/workers/md5.worker";

import type {
    UploadStatus, UploadInfo,
    ImportsPageItem,
    UploadImportItem,
    UploadErrorsPayload,
    UploadListResponse, ImportsListResponse,
    APIError
} from "./_types/types";



// UI Helpers
function bytesToKB(num: number) {
    return (num / 1000).toFixed(2);
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

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms));
const raf = () => new Promise<void>((r) => requestAnimationFrame(() => r()));

function safeFilename(name: string) {

    const UPLOAD_FILE_NAME_DEFAULT = "UnknownFlightData.zip";

    return name?.replace(/[^0-9a-zA-Z._-]/g, "") || UPLOAD_FILE_NAME_DEFAULT;

}



function isAPIError(x: unknown): x is APIError {
    return !!x && typeof x === "object" && "errorTitle" in (x as Record<string, unknown>);
}



// MD5 Helpers
const createMd5Worker = () => new Md5Worker();

async function md5OnMainThread(
    file: File,
    onProgress: (done: number, total: number) => void
): Promise<string> {

    const spark = new SparkMD5.ArrayBuffer();
    const total = file.size;

    const CHUNK_STEP = 4;

    /*
        Prefer streaming to keep memory low
        (might not work on some browsers)
    */

    // Streaming is supported...
    if (file.stream) {

        const reader = file.stream().getReader();
        let processed = 0;
        let sinceYield = 0;

        const YIELD_EVERY = (CHUNK_SIZE * CHUNK_STEP); //<-- ~8MB by default

        while (true) {

            const { value, done } = await reader.read();

            // Done reading, end loop
            if (done)
                break;

            processed += value.byteLength;
            sinceYield += value.byteLength;

            // Value is a Uint8Array, SparkMD5 uses an ArrayBuffer
            spark.append(value.buffer);
            onProgress(processed, total);

            // Yield so the UI can update
            if (sinceYield >= YIELD_EVERY) {
                sinceYield = 0;
                await new Promise(r => setTimeout(r, 0));
            }

        }

    // Otherwise, fall back to manual chunking
    } else {

        const chunks = Math.ceil(total / CHUNK_SIZE);
        for (let i = 0; i < chunks; i++) {

            const start = i * CHUNK_SIZE;
            const end = Math.min(start + CHUNK_SIZE, total);
            const buf = await file.slice(start, end).arrayBuffer();
            spark.append(buf);
            onProgress(end, total);

            if (i % CHUNK_STEP === 0)
                await new Promise(r => setTimeout(r, 0));

        }

    }

    return spark.end();

}

async function md5BestEffort(
    file: File,
    onProgress: (done: number, total: number) => void
): Promise<string> {

    // Attempt to use the worker path first...
    try {

        const worker = createMd5Worker();
        return await new Promise((resolve) => {

            const finish = (hash: string) => {

                // Attempt to clean up the worker
                try {
                    worker.terminate();
                } catch {
                    /* ... */
                }

                // Resolve the hash
                resolve(hash);

            };

            const fallback = async () => {

                // Worker blocked or errored, compute on main thread instead
                console.warn("MD5 worker failed, falling back to main thread hashing.");

                const hash = await md5OnMainThread(file, onProgress);
                finish(hash);

            };

            worker.onmessage = (ev: MessageEvent) => {

                const msg = ev.data;

                if (msg?.type === "progress")
                    onProgress(msg.done, msg.total);

                if (msg?.type === "done")
                    finish(msg.hash);

            };

            // Fallback on any worker errors
            worker.onerror = fallback;
            worker.onmessageerror = fallback;

            // Post the file to the worker
            try {
                worker.postMessage(file);
            } catch {
                // Posting failed (probably from cross-origin restrictions)
                void fallback();
            }

        });

    } catch {

        // Failed to construct the worker, compute on main thread instead
        console.warn("Failed to create MD5 worker, falling back to main thread hashing.");

        return await md5OnMainThread(file, onProgress);

    }

}


export default function UploadsPage() {

    const { setModal } = useModal();

    // Active uploads
    const [uploads, setUploads] = useState<UploadInfo[]>([]);
    const [uploadsPage, setUploadsPage] = useState(0);
    const [uploadsPages, setUploadsPages] = useState(0);

    // Imported uploads
    const [imports, setImports] = useState<ImportsPageItem[]>([]);
    const [importsPage, setImportsPage] = useState(0);
    const [importsPages, setImportsPages] = useState(0);

    // Pending (client-side) uploads that are currently hashing/uploading
    const [pending, setPending] = useState<UploadInfo[]>([]);

    // UI handling
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [expandedErrors, setExpandedErrors] = useState<Record<number, UploadErrorsPayload | "loading" | "error" | undefined>>({});


    const loadUploads = useCallback(async (page = uploadsPage, pageSize = 10) => {

        const params = new URLSearchParams({
            currentPage: String(page),
            pageSize: String(pageSize)
        });

        const uploadsListResponse = await fetchJson.get<UploadListResponse | APIError>("/api/upload", { params });

        // Error loading uploads, display it
        if (isAPIError(uploadsListResponse)) {
            setError(`${uploadsListResponse.errorTitle}: ${uploadsListResponse.errorMessage}`);
            return;
        }

        const filtered = (uploadsListResponse.uploads ?? []).filter((u: UploadInfo) => u.status !== "DERIVED");
        setUploads(filtered);
        setUploadsPages(uploadsListResponse.numberPages ?? 0);
        
    }, [uploadsPage]);


    const loadImports = useCallback(async (page = importsPage, pageSize = 10) => {

        const params = new URLSearchParams({
            currentPage: String(page),
            pageSize: String(pageSize)
        });

        const importsListResponse = await fetchJson.get<ImportsListResponse | APIError>("/api/upload/imported", { params });

        // Error loading imports, display it
        if (isAPIError(importsListResponse)) {
            setError(`${importsListResponse.errorTitle}: ${importsListResponse.errorMessage}`);
            return;
        }

        setImports(importsListResponse.imports ?? []);
        setImportsPages(importsListResponse.numberPages ?? 0);

    }, [importsPage]);


    useEffect(() => {

        /*
            Automatically open the ErrorModal
            when the 'error' state changes.
        */

        console.log("Error changed:", error);

        if (error)
            setModal(ErrorModal, { title: "Error", message: error });

    }, [error]);
    
    useEffect(() => {

        /*
            Reload the uploads list
            when the page changes.
        */

        loadUploads();

    }, [uploadsPage, loadUploads]);

    useEffect(() => {

        /*
            Reload the imports list
            when the page changes.
        */

        loadImports();

    }, [importsPage, loadImports]);

    useEffect(() => {

        /*
            Remove any pending uploads that have
            been completed.
        */

        // No pending or uploads, exit
        if (pending.length === 0 || uploads.length === 0)
            return;

        const serverIDs = new Set(uploads.map((u) => u.identifier));
        setPending((prev) => prev.filter((p) => !serverIDs.has(p.identifier)));

    }, [uploads, pending.length]);



    

    const onPickFiles = async (files: FileList | null) => {

        // No files picked, exit
        if (!files || files.length === 0)
            return;

        // Start upload flow for each file
        for (const file of Array.from(files)) {
            void startUploadFlow(file);
        }

    };

    const startUploadFlow = async (file: File) => {

        try {

            // Flag as busy
            setBusy(true);

            // Clear previous errors
            setError(null);

            // Seed pending entry
            const pendingEntry: UploadInfo = {
                id: -1,
                identifier: `${file.size}-${safeFilename(file.name)}`,
                filename: file.name,
                status: "HASHING",
                sizeBytes: file.size,
                bytesUploaded: 0,
                progressSize: 0,
                totalSize: file.size,
                numberChunks: Math.ceil(file.size / CHUNK_SIZE),
                position: pending.length,
                file,
                md5Hash: ""
            };
            setPending((p) => [...p, pendingEntry]);

            await raf();
            await sleep(0); //<-- Wait for event loop to yield before hashing

            // Get the md5 hash (in worker)
            const md5 = await md5BestEffort(file, (done, total) => {
                setPending((p) => p.map((u) => u.identifier === pendingEntry.identifier
                    ? { ...u, progressSize: done, bytesUploaded: done }
                    : u
                ));
            });
            pendingEntry.md5Hash = md5;

            // Set status to UPLOADING
            setPending((p) =>
                p.map((u) =>
                    u.identifier === pendingEntry.identifier ? { ...u, status: "UPLOADING", progressSize: 0, bytesUploaded: 0 } : u
                )
            );

            // Create the upload
            const createForm = new FormData();
            createForm.set("request", "NEW_UPLOAD");
            createForm.set("filename", file.name);
            createForm.set("identifier", pendingEntry.identifier);
            createForm.set("numberChunks", String(pendingEntry.numberChunks));
            createForm.set("sizeBytes", String(file.size));
            createForm.set("md5Hash", md5);

            const createRes = await fetch("/api/upload", { method: "POST", body: createForm });

            // Upload failed, throw error
            if (!createRes.ok) {
                const responseText = await createRes.text();
                throw new Error(responseText || `Create upload failed (${createRes.status})`);
            }

            const created = (await createRes.json()) as UploadInfo & APIError;

            // Created upload has error, throw it
            if (created.errorTitle)
                throw new Error(`${created.errorTitle}: ${created.errorMessage}`);

            setPending((p) => p.map((x) =>
                (x.identifier === pendingEntry.identifier) ? { ...x, id: created.id } : x
            ));

            // Chunk upload loop
            const nChunks = Math.ceil(file.size / CHUNK_SIZE);
            let uploadedBytes = 0;

            for (let n = 0; n < nChunks; n++) {

                const start = n * CHUNK_SIZE;
                const end = Math.min(start + CHUNK_SIZE, file.size);
                const blob = file.slice(start, end);
                const form = new FormData();
                form.append("chunk", blob, file.name);

                const putRes = await fetch(
                    `/api/upload/${created.id}/chunk/${n}`,
                    { method: "PUT", body: form }
                );

                // Chunk upload failed, throw error
                if (!putRes.ok) {
                    const text = await putRes.text();
                    throw new Error(text || `Chunk ${n} failed (${putRes.status})`);
                }
                const updated = (await putRes.json()) as UploadInfo & APIError;

                // Server returned an error, throw it
                if (updated.errorTitle)
                    throw new Error(`${updated.errorTitle}: ${updated.errorMessage}`);

                uploadedBytes = end;
                setPending((p) =>
                    p.map((u) =>
                        u.identifier === pendingEntry.identifier
                            ? { ...u, status: "UPLOADING", progressSize: uploadedBytes, bytesUploaded: uploadedBytes, totalSize: file.size }
                            : u
                    )
                );

                await raf();

                // Throttle to allow UI updates every few chunks
                const THROTTLE_CHUNKS = 4;
                if (n % THROTTLE_CHUNKS === 0)
                    await sleep(0);

            }

            // Finished uploading, mark as UPLOADED
            setPending((p) =>
                p.map((u) =>
                    u.identifier === pendingEntry.identifier
                        ? { ...u, status: "UPLOADED", progressSize: file.size, bytesUploaded: file.size }
                        : u
                )
            );

            // Refresh the uploads list from server
            await loadUploads();

        } catch (e: any) {

            setError(e?.message || String(e));

            // Mark any pending upload that was HASHING or UPLOADING as failed
            setPending((p) =>
                p.map((u) => (u.status === "UPLOADING" || u.status === "HASHING" ? { ...u, status: "UPLOADING_FAILED" } : u))
            );

        } finally {

            // No longer busy
            setBusy(false);

        }

    };

    const deleteUpload = async (u: UploadInfo) => {

        setModal(ConfirmModal, {
            title: "Delete Upload",
            message: `Are you sure you want to delete the upload '${u.filename}'? This will remove all associated flights and data.`,
            onConfirm: async () => {

                console.log("Confirmed deletion of upload:", u);

                // Optimistic removal from UI (will be reloaded from server after)
                setUploads((list) => list.filter((x) => x.id !== u.id));
                setImports((list) => list.filter((x) => x.id !== u.id));
                setPending((list) => list.filter((x) => x.identifier !== u.identifier));

                try {

                    const deleteURL = `/api/upload/${u.id}`;
                    const deleteResponse = await fetchJson.delete<{} | APIError>(deleteURL);

                    // Failed to delete, throw error
                    if (typeof (deleteResponse === "object") && (deleteResponse !== null) && ("errorTitle" in deleteResponse))
                        throw new Error(`${(deleteResponse as APIError).errorTitle}: ${(deleteResponse as APIError).errorMessage}`);

                    // Re-sync both lists
                    await loadUploads();
                    await loadImports();

                } catch (e: any) {

                    // Rollback by reloading both lists
                    await loadUploads();
                    await loadImports();

                    setError(e?.message || String(e));

                }

            }

        });

    };


    const downloadUpload = async (u: UploadInfo) => {

        setBusy(true);
        setError(null);

        try {

            const params = new URLSearchParams({
                md5hash: u.md5Hash
            });
            const downloadResponse = await fetchJson.get(`/api/upload/${u.id}/file`, { params });

            // Download failed, throw error
            if (!downloadResponse.ok)
                throw new Error(`Download failed (${downloadResponse.status})`);

            const downloadContentType = downloadResponse.headers.get("content-type") || "";

            // Content type is JSON, assume it's an error message
            if (downloadContentType.includes("application/json")) {
                const responseJSON = (await downloadResponse.json()) as APIError;
                throw new Error(`${responseJSON.errorTitle ?? "Download Error"}: ${responseJSON.errorMessage ?? "Unknown error"}`);
            }

            // Generate a blob URL and click a temporary link to download it
            const blob = await downloadResponse.blob();
            const blobURL = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = blobURL;
            a.download = safeFilename(u.filename);
            document.body.appendChild(a);
            a.click();
            a.remove();

            URL.revokeObjectURL(blobURL);

        } catch (e: any) {

            setError(e?.message || String(e));

        } finally {

            setBusy(false);

        }

    };


    const UploadCard = (u:UploadInfo|UploadImportItem, isPending?:boolean) => {

        const uploadProgress = percent(u.progressSize ?? u.bytesUploaded ?? 0, u.totalSize ?? u.sizeBytes ?? 0);
        const { label, variant } = statusBadgeVariant(u.status);

        const disableAll = (u.status === "HASHING" || u.status === "UPLOADING");
        const isImported = (u.status === "PROCESSED_OK");
        const hasImportData = ("validFlights" in u);
        const totalFlights = hasImportData ? (u.validFlights + u.errorFlights) : 0;

        return (
            <Card className="card-glossy w-full h-full bg-(--background)!">
                <CardContent className="py-4 px-6 space-y-4">

                    {/* Top Row */}
                    <div className="flex items-center justify-between gap-3">

                        {/* File Info */}
                        <div className="min-w-0">
                            <div className="font-medium truncate">{u.filename}</div>
                            <div className="text-xs text-muted-foreground">{u.startTime ?? ""}</div>
                        </div>

                        {/* Status Badge */}
                        <Badge variant={variant} className="shrink-0">
                            {label}
                        </Badge>

                    </div>

                    {/* Bottom Row */}
                    <div className="flex gap-10 justify-between items-center">

                        {/* Upload Progress Bar */}
                        <div className="flex flex-col justify-around gap-1 w-full">
                            <Progress value={uploadProgress} className="h-2" />
                            <div className="text-xs text-muted-foreground whitespace-nowrap">
                                {bytesToKB((u.progressSize ?? u.bytesUploaded ?? 0))}/{bytesToKB(u.totalSize ?? u.sizeBytes ?? 0)} kB ({uploadProgress.toFixed(2)}%)
                            </div>
                        </div>

                        {/* Delete & Download Buttons */}
                        <div className="flex justify-end gap-2">
                            <Button
                                size="icon"
                                variant="ghost"
                                disabled={disableAll || isPending || u.id === -1}
                                onClick={() => (u.id !== -1 ? deleteUpload(u) : null)}
                                title="Delete upload"
                                className="hover:bg-red-500/25 hover:text-red-500 focus:ring-red-500"
                            >
                                <Trash className="h-4 w-4" />
                            </Button>
                            <Button
                                size="icon"
                                variant="ghost"
                                disabled={disableAll || isPending || u.id === -1}
                                onClick={() => (u.id !== -1 ? downloadUpload(u) : null)}
                                title="Download uploaded file"
                            >
                                <Download className="h-4 w-4" />
                            </Button>
                        </div>

                    </div>

                    {/* Import Info */}
                    {
                        (isImported && hasImportData)
                        ?
                        <div className="flex items-center justify-between">
                            <div className="flex gap-2 text-xs min-w-[75%] flex-wrap">
                                <Badge className="inline-flex justify-between grow items-center gap-1 rounded-md px-2 py-1 bg-(--muted) dark:text-shadow-md" variant={"outline"}>
                                    <span className="flex items-center gap-1">
                                        <CloudDownload className="h-3 w-3" />
                                        Total:
                                    </span>
                                    <span>
                                        {totalFlights}
                                    </span>
                                </Badge>
                                <Badge className="inline-flex justify-between grow items-center gap-1 rounded-md px-2 py-1 bg-(--info) dark:text-shadow-md" variant={"outline"}>
                                    <span className="flex items-center gap-1">
                                        <Check className="h-3 w-3" />
                                        Valid:
                                    </span>
                                    <span>
                                        {u.validFlights}
                                    </span>
                                </Badge>
                                <Badge className="inline-flex justify-between grow items-center gap-1 rounded-md px-2 py-1 bg-(--warning) dark:text-shadow-md" variant={"outline"}>
                                    <span className="flex items-center gap-1">
                                        <AlertTriangle className="h-3 w-3" />
                                        Warnings:
                                    </span>
                                    <span>
                                        {u.warningFlights}
                                    </span>
                                </Badge>
                                <Badge className="inline-flex grow items-center gap-1 rounded-md px-2 py-1 bg-(--error) dark:text-shadow-md" variant={"outline"}>
                                    <span className="flex items-center gap-1">
                                        <CircleAlert className="h-3 w-3" />
                                        Errors:
                                    </span>
                                    <span>
                                        {u.errorFlights}
                                    </span>
                                </Badge>
                            </div>

                            <Button variant={"ghost"} >
                                <List />
                                Details
                            </Button>

                        </div>
                        :
                        <div className="flex items-center gap-2 text-xs text-muted-foreground py-[8px]">
                            <Loader size={20} className="animate-spin duration-2000" />
                            <span>Awaiting import processing...</span>
                        </div>
                    }

                </CardContent>
            </Card>
        );
    };

    /*
        Placeholder Paginator Component
    */
    const Pager = (page: number, pages: number, onPage: (n: number) => void) => (
        <div className="flex items-center justify-end gap-2">
            <Button variant="outline" size="icon" disabled={page <= 0} onClick={() => onPage(page - 1)} title="Previous">
                <ChevronLeft className="h-4 w-4" />
            </Button>
            <div className="text-xs text-muted-foreground">Page {page + 1} / {Math.max(1, pages)}</div>
            <Button variant="outline" size="icon" disabled={pages === 0 || page >= pages - 1} onClick={() => onPage(page + 1)} title="Next">
                <ChevronRight className="h-4 w-4" />
            </Button>
        </div>
    );

    /*
        Combine the Uploads and Imports so they
        can be shown together on one card.
    */
    const mergedUploadsImports = React.useMemo(() => {

        const byIdentifier = new Map(uploads.map((u) => [u.identifier, u]));
        const visiblePending = pending.filter((p) => !byIdentifier.has(p.identifier));

        const combined: (UploadInfo | UploadImportItem)[] = [];

        // Add pending uploads first
        for (const p of visiblePending) {
            combined.push(p);
        }

        // Add uploads, merging in import data (if it exists)
        for (const u of uploads) {

            const importData = imports.find((i) => i.id === u.id);

            // Got corresponding import data, merge it in
            if (importData)
                combined.push({ ...u, ...importData } as UploadImportItem);

            // Otherwise, just add the upload
            else
                combined.push(u as UploadInfo);

        }


        console.log("Combined uploads & imports:", combined);

        return combined;

    }, [uploads, imports, pending]);



    const render = () => (
        <div className="page-container">
            <ProtectedNavbar />

            <div className="page-content space-y-4 w-full max-w-[1280px] h-full mx-auto">

                <Card className="card-glossy h-full">
                    <CardHeader className="flex flex-row items-center justify-between">

                        <div className="flex flex-col space-y-1.5">
                            <CardTitle>Uploads</CardTitle>
                            <CardDescription>Manage file uploads and review imported results.</CardDescription>
                        </div>

                        {/* File Picker */}
                        <div className="flex flex-wrap items-center w-fit">

                            <Input className="w-[200px] hidden!" type="file" multiple onChange={(e) => onPickFiles(e.target.files)} disabled={busy} />
                            <Button onClick={() => (document.querySelector<HTMLInputElement>('input[type="file"]')?.click())} disabled={busy}>
                                <UploadIcon className="h-4 w-4 mr-2" />
                                Choose Files
                            </Button>
                            {busy && (
                                <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                                    <Info className="h-3 w-3" /> Working…
                                </span>
                            )}

                        </div>

                    </CardHeader>
                    <CardContent className="space-y-4">

                        <div className="space-y-3 pt-3 grid grid-cols-2 grid-rows-1 gap-2">

                            {mergedUploadsImports.map((u: UploadInfo | UploadImportItem, i) =>

                                <motion.div
                                    key={`upload-${u.identifier}`}
                                    initial={{ opacity: 0.00 }}
                                    animate={{ opacity: 1.00 }}
                                    transition={{ duration: 0.50, delay: 0.03 * i }}
                                >
                                    {UploadCard(u, u.id === -1)}
                                </motion.div>
                            )}

                            {Pager(uploadsPage, uploadsPages, setUploadsPage)}
                        </div>

                    </CardContent>
                </Card>
            </div>
        </div>
    );

    return render();
}
