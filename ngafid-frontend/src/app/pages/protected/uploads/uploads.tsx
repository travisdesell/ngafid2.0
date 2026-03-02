// ngafid-frontend/src/app/pages/protected/uploads/uploads.tsx
"use client";

import ConfirmModal from "@/components/modals/confirm_modal";
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { Badge, BadgeVariant } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { fetchJson } from "@/fetchJson";
import { CHUNK_SIZE } from "@/workers/md5.shared";
import Md5Worker from "@/workers/md5.worker.ts?worker";
import {
    AlertTriangle,
    Check,
    CircleAlert,
    CloudDownload,
    Download,
    List,
    Loader,
    RotateCcw,
    Trash
} from "lucide-react";
import { motion } from "motion/react";
import React, { useCallback, useEffect, useRef, useState } from "react";
import SparkMD5 from "spark-md5";

import { openModal } from "@/components/modals/modal_store";
import SuccessModal from "@/components/modals/success_modal";
import UploadDetailsModal from "@/components/modals/upload_details_modal/upload_details_modal";
import PanelAlert from "@/components/panel_alert";
import { getLogger } from "@/components/providers/logger";
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination";
import { Separator } from "@/components/ui/separator";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import UploadsDropzone from "@/pages/protected/uploads/_uploads_dropzone";
import type {
    APIError,
    ImportsListResponse,
    ImportsPageItem,
    UploadErrorsPayload,
    UploadImportItem,
    UploadInfo,
    UploadListResponse,
    UploadStatus
} from "./_types/types";


const log = getLogger("Uploads", "black", "Page");


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

async function pickRetryFile(): Promise<File | null> {

    return await new Promise((resolve) => {

        const input = document.createElement("input");
        input.type = "file";

        input.onchange = () => {
            const file = input.files?.item(0) ?? null;
            resolve(file);
        };

        input.click();

    });

}




const isObjectRecord = (v: unknown): v is Record<string, unknown> =>
    (typeof v === "object" && v !== null);

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

async function md5BestEffort(file: File, onProgress: (done: number, total: number) => void): Promise<string> {

    // Attempt to use the worker path first...
    try {

        const worker = createMd5Worker();
        return await new Promise((resolve, reject) => {

            let settled = false;
            const cleanup = () => {

                // Attempt to clean up the worker
                try {
                    worker.terminate();
                } catch {
                    /* ... */
                }

            };

            const finish = (hash: string) => {

                // Already settled, exit
                if (settled)
                    return;

                // Flag as settled
                settled = true;

                // Clean up resources
                cleanup();

                // Resolve the hash
                resolve(hash);

            };


            const fallback = () => {

                // Already settled, exit
                if (settled)
                    return;

                void md5OnMainThread(file, onProgress)
                    .then(finish)
                    .catch((err) => {

                        // Already settled, exit
                        if (settled)
                            return;

                        // Flag as settled
                        settled = true;

                        // Clean up resources
                        cleanup();

                        // Reject the main-thread error
                        reject(err);
                        
                    });
            };

            worker.onmessage = (ev: MessageEvent) => {

                const msg = ev.data;

                if (msg?.type === "progress")
                    onProgress(msg.done, msg.total);

                else if (msg?.type === "done")
                    finish(msg.hash);

                else if (msg?.type === "error")
                    fallback();

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
        log.warn("Failed to create MD5 worker, falling back to main thread hashing.");

        return await md5OnMainThread(file, onProgress);

    }

}


export default function UploadsPage() {

    useEffect(() => {
        document.title = `NGAFID — Uploads`;
    });

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
    const [deletingUploadIds, setDeletingUploadIds] = useState<Set<number>>(new Set());
    const activeUploadIdsRef = useRef<Set<number>>(new Set());
    const pendingIdentifiersRef = useRef<Set<string>>(new Set());

    // UI handling
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [expandedErrors, setExpandedErrors] = useState<Record<number, UploadErrorsPayload | "loading" | "error" | undefined>>({});

    useEffect(() => {
        pendingIdentifiersRef.current = new Set(pending.map((p) => p.identifier));
    }, [pending]);


    const loadUploads = useCallback(async (page = uploadsPage, pageSize = 10, silent = false) => {

        const params = new URLSearchParams({
            currentPage: String(page),
            pageSize: String(pageSize)
        });

        const uploadsListResponse = await fetchJson.get<UploadListResponse | APIError>("/api/upload", { params });

        // Error loading uploads, display it
        if (isAPIError(uploadsListResponse)) {
            if (!silent)
                setError(`${uploadsListResponse.errorTitle}: ${uploadsListResponse.errorMessage}`);
            return false;
        }

        const filtered = (uploadsListResponse.uploads ?? [])
            .filter((u: UploadInfo) => u.status !== "DERIVED")
            .map((u: UploadInfo) => {

                const uploadIsLocalActive = (
                    activeUploadIdsRef.current.has(u.id)
                    || pendingIdentifiersRef.current.has(u.identifier)
                );

                if (u.status === "UPLOADING" && !uploadIsLocalActive)
                    return { ...u, status: "FAILED_INTERRUPTED" as UploadStatus };

                return u;

            });

        setUploads(filtered);
        setUploadsPages(uploadsListResponse.numberPages ?? 0);
        return true;
        
    }, [uploadsPage]);


    const loadImports = useCallback(async (page = importsPage, pageSize = 10, silent = false) => {

        const params = new URLSearchParams({
            currentPage: String(page),
            pageSize: String(pageSize)
        });

        const importsListResponse = await fetchJson.get<ImportsListResponse | APIError>("/api/upload/imported", { params });

        // Error loading imports, display it
        if (isAPIError(importsListResponse)) {
            if (!silent)
                setError(`${importsListResponse.errorTitle}: ${importsListResponse.errorMessage}`);
            return false;
        }

        setImports(importsListResponse.imports ?? []);
        setImportsPages(importsListResponse.numberPages ?? 0);
        return true;

    }, [importsPage]);


    useEffect(() => {

        /*
            Automatically open the ErrorModal
            when the 'error' state changes.
        */

        const clearError = () => setError(null);

        if (error)
            log.error("Error changed:", error);
        else
            log.log("Error cleared");

        if (error)
            setModal(
                ErrorModal,
                { title: "Error", message: error },
                clearError
            );

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
        setPending((prev) => prev.filter((p) => {

            // Keep active local work visible even if a matching server row exists.
            if (p.status === "HASHING" || p.status === "UPLOADING")
                return true;

            // Non-active pending entries can be removed once server has same identifier.
            return !serverIDs.has(p.identifier);

        }));

    }, [uploads, pending.length]);

    const isPollingRef = useRef(false);
    const pollingFailuresRef = useRef(0);

    useEffect(() => {

        const hasLocalActiveUpload = (busy || pending.some((p) => p.status === "HASHING" || p.status === "UPLOADING"));
        const hasServerActiveUpload = uploads.some((u) =>
            u.status === "UPLOADED"
            || u.status === "ENQUEUED"
            || u.status === "PROCESSING"
            || u.status === "UPLOADING"
        );

        const BASE_DELAY_MS_DEFAULT = 15_000;
        const BASE_DELAY_MS_INACTIVE = 30_000;
        const BASE_DELAY_MS_ACTIVE_LOCAL = 3_000;
        const BASE_DELAY_MS_ACTIVE_SERVER = 5_000;

        // Use increased polling frequency when there are active uploads
        const baseDelayMS = (() => {

            if (document.hidden)
                return BASE_DELAY_MS_INACTIVE;

            if (hasLocalActiveUpload)
                return BASE_DELAY_MS_ACTIVE_LOCAL;

            if (hasServerActiveUpload)
                return BASE_DELAY_MS_ACTIVE_SERVER;

            return BASE_DELAY_MS_DEFAULT;

        })();

        const failureBackoffMS = Math.min(BASE_DELAY_MS_DEFAULT, pollingFailuresRef.current * 2500);
        const nextDelayMS = Math.min(BASE_DELAY_MS_INACTIVE, baseDelayMS + failureBackoffMS);

        let isCancelled = false;
        let timeoutId: number | undefined;

        const poll = async () => {

            // Polling cancelled, exit
            if (isCancelled)
                return;

            // Skip polling while local uploads are actively hashing/uploading to avoid request contention.
            if (hasLocalActiveUpload) {
                timeoutId = window.setTimeout(() => { void poll(); }, nextDelayMS);
                return;
            }

            // Page is not visible or we're already polling, delay the next poll
            if (isPollingRef.current || document.hidden) {
                timeoutId = window.setTimeout(() => { void poll(); }, nextDelayMS);
                return;
            }

            log(`Polling uploads/imports (local active: ${hasLocalActiveUpload}, server active: ${hasServerActiveUpload}, base delay: ${baseDelayMS}ms, backoff: ${failureBackoffMS}ms, next delay: ${nextDelayMS}ms)`);

            isPollingRef.current = true;

            const UPLOAD_IMPORT_LOAD_COUNT = 10;

            try {
                const [uploadsOK, importsOK] = await Promise.all([
                    loadUploads(uploadsPage, UPLOAD_IMPORT_LOAD_COUNT, true),
                    loadImports(importsPage, UPLOAD_IMPORT_LOAD_COUNT, true)
                ]);

                // Both uploads and imports loaded successfully, reset failure count
                if (uploadsOK && importsOK)
                    pollingFailuresRef.current = 0;

                // Otherwise, increment failure count (which will increase backoff delay)
                else
                    pollingFailuresRef.current = Math.min(6, pollingFailuresRef.current + 1);

            } catch {
                pollingFailuresRef.current = Math.min(6, pollingFailuresRef.current + 1);
            } finally {

                isPollingRef.current = false;

                // Not canceled, schedule the next poll
                if (!isCancelled)
                    timeoutId = window.setTimeout(() => { void poll(); }, nextDelayMS);
            }

        };

        timeoutId = window.setTimeout(() => { void poll(); }, nextDelayMS);

        return () => {
            isCancelled = true;
            if (timeoutId)
                window.clearTimeout(timeoutId);
        };

    }, [loadUploads, loadImports, uploadsPage, importsPage, busy, pending, uploads]);



    
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

        const pendingIdentifier = `${file.size}-${safeFilename(file.name)}`;
        let createdUploadId: number | null = null;

        try {

            // Flag as busy
            setBusy(true);

            // Clear previous errors
            setError(null);

            // Seed pending entry
            const pendingEntry = {
                id: -1,
                identifier: pendingIdentifier,
                filename: file.name,
                status: "HASHING",
                sizeBytes: file.size,
                bytesUploaded: 0,
                progressSize: 0,
                totalSize: file.size,
                numberChunks: Math.ceil(file.size / CHUNK_SIZE),
                position: pending.length,
                file,
                md5Hash: "",
                uploaderId: undefined,
                fleetId: -1,
            } as UploadInfo;

            const existingPending = pending.find((u) => u.identifier === pendingIdentifier);
            const hasActiveDuplicate = (
                !!existingPending
                && (existingPending.status === "HASHING" || existingPending.status === "UPLOADING")
            );

            if (hasActiveDuplicate) {
                setModal(ErrorModal, {
                    title: "Duplicate File",
                    message: `The file "${file.name}" is already being uploaded. Please wait for the current upload to finish before uploading it again.`
                });
                return;
            }

            setPending((p) => {

                // Replace any non-active duplicate so retried uploads visibly restart from HASHING.
                const next = p.filter((u) => u.identifier !== pendingEntry.identifier);
                return [...next, pendingEntry];
                
            });

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
            activeUploadIdsRef.current.add(created.id);
            createdUploadId = created.id;

            // Chunk upload loop (supports resuming interrupted uploads)
            const nChunks = Math.ceil(file.size / CHUNK_SIZE);
            const normalizedChunkStatus = (created.chunkStatus ?? "").slice(0, nChunks);

            let uploadedBytes = 0;
            for (let n = 0; n < nChunks; n++) {
                if (normalizedChunkStatus[n] === "1") {
                    const start = n * CHUNK_SIZE;
                    const end = Math.min(start + CHUNK_SIZE, file.size);
                    uploadedBytes += (end - start);
                }
            }

            setPending((p) =>
                p.map((u) =>
                    u.identifier === pendingEntry.identifier
                        ? { ...u, status: "UPLOADING", progressSize: uploadedBytes, bytesUploaded: uploadedBytes, totalSize: file.size }
                        : u
                )
            );

            for (let n = 0; n < nChunks; n++) {

                // This chunk is already present on the server from a previous interrupted upload.
                if (normalizedChunkStatus[n] === "1")
                    continue;

                const start = n * CHUNK_SIZE;
                const end = Math.min(start + CHUNK_SIZE, file.size);
                const blob = file.slice(start, end);
                const form = new FormData();
                form.append("chunk", blob, file.name);

                let putRes: Response | null = null;
                let lastChunkError: unknown = null;
                const MAX_CHUNK_ATTEMPTS = 3;

                for (let attempt = 1; attempt <= MAX_CHUNK_ATTEMPTS; attempt++) {
                    try {
                        putRes = await fetch(
                            `/api/upload/${created.id}/chunk/${n}`,
                            { method: "PUT", body: form }
                        );

                        if (!putRes.ok) {
                            const text = await putRes.text();
                            throw new Error(text || `Chunk ${n} failed (${putRes.status})`);
                        }

                        lastChunkError = null;
                        break;
                    } catch (err) {
                        lastChunkError = err;

                        if (attempt < MAX_CHUNK_ATTEMPTS) {
                            const retryDelayMS = 500 * attempt;
                            await sleep(retryDelayMS);
                            continue;
                        }

                        throw err;
                    }
                }

                // Chunk upload failed, throw error
                if (!putRes)
                    throw new Error((lastChunkError as Error)?.message || `Chunk ${n} failed.`);

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

            // Refresh the uploads list from server (best effort; upload already completed locally)
            try {
                await loadUploads();
            } catch (refreshError) {
                log.warn("Upload completed, but failed to refresh uploads list:", refreshError);
            }

        } catch (e: any) {

            const errorMessage = e?.message || String(e);
            setError(errorMessage);

            const interruptedError = /Failed to fetch|timed out|NetworkError|ERR_CONNECTION_TIMED_OUT/i.test(errorMessage);

            // Mark only this pending upload as failed
            setPending((p) =>
                p.map((u) =>
                    (u.identifier === pendingIdentifier && (u.status === "UPLOADING" || u.status === "HASHING"))
                        ? { ...u, status: (interruptedError ? "FAILED_INTERRUPTED" : "UPLOADING_FAILED") }
                        : u
                )
            );

        } finally {

            if (createdUploadId !== null)
                activeUploadIdsRef.current.delete(createdUploadId);

            // No longer busy
            setBusy(false);

        }

    };

    const deleteUpload = async (u: UploadInfo) => {

        setModal(ConfirmModal, {
            title: "Delete Upload",
            message: `Are you sure you want to delete the upload '${u.filename}'? This will remove all associated flights and data.`,
            onConfirm: async ()=>{

                log("Confirmed deletion of upload:", u);

                const hasAnyLocalActiveUpload = pending.some((p) => p.status === "HASHING" || p.status === "UPLOADING") || busy;
                if (hasAnyLocalActiveUpload) {
                    setModal(ErrorModal, {
                        title: "Delete Disabled During Upload",
                        message: "Please wait until active uploads finish before deleting an upload."
                    });
                    return;
                }

                setDeletingUploadIds((prev) => {
                    const next = new Set(prev);
                    next.add(u.id);
                    return next;
                });

                try {

                    const deleteURL = `/api/upload/${u.id}`;
                    const deleteResponse = await fetchJson.delete<{} | APIError>(deleteURL);

                    // Client returned a number/boolean/undefined for 204 (No Content), treat as success
                    if (!isObjectRecord(deleteResponse)) {

                        log("Uploads - Delete response is not an object, assuming success:", deleteResponse);

                        setModal(SuccessModal, {
                            title: "Upload Deleted",
                            message: `The upload '${u.filename}' has been successfully deleted.`
                        });

                        await loadUploads();
                        await loadImports();
                        return;
                    }

                    // Got an object, check for APIError shape
                    if ("errorTitle" in deleteResponse) {
                        throw new Error(
                            `${(deleteResponse as APIError).errorTitle}: ${(deleteResponse as APIError).errorMessage}`
                        );
                    }

                    // Re-sync both lists
                    await loadUploads();
                    await loadImports();

                    setModal(SuccessModal, {
                        title: "Upload Deleted",
                        message: `The upload '${u.filename}' has been successfully deleted.`
                    });

                } catch (e: any) {

                    // Rollback by reloading both lists
                    await loadUploads();
                    await loadImports();

                    setError(e?.message || String(e));

                } finally {

                    setDeletingUploadIds((prev) => {
                        const next = new Set(prev);
                        next.delete(u.id);
                        return next;
                    });

                }

            }

        });

    };


    const retryUpload = async (u: UploadInfo | UploadImportItem) => {

        setError(null);

        if (u.status === "FAILED_INTERRUPTED") {

            const selectedFile = await pickRetryFile();

            if (!selectedFile)
                return;

            if (selectedFile.name !== u.filename) {
                setModal(ErrorModal, {
                    title: "Retry Upload",
                    message: `Please select the original file named '${u.filename}'.`
                });
                return;
            }

            void startUploadFlow(selectedFile);
            return;
        }

        if (u.status === "UPLOADING_FAILED") {

            const fileToRetry = ("file" in u && u.file instanceof File) ? u.file : null;

            if (!fileToRetry) {
                setModal(ErrorModal, {
                    title: "Retry Upload",
                    message: "This upload can only be retried by re-selecting the original file from your device."
                });
                return;
            }

            setPending((list) => list.filter((x) => x.identifier !== u.identifier));
            void startUploadFlow(fileToRetry);
            return;
        }

        if (u.status === "FAILED_UNKNOWN") {

            try {

                const retryResponse = await fetchJson.post<UploadInfo | APIError>(`/api/upload/${u.id}/retry`, new URLSearchParams());

                if (isAPIError(retryResponse))
                    throw new Error(`${retryResponse.errorTitle}: ${retryResponse.errorMessage}`);

                setModal(SuccessModal, {
                    title: "Upload Requeued",
                    message: `The upload '${u.filename}' has been requeued for processing.`
                });

                await loadUploads();
                await loadImports();

            } catch (e: any) {

                setError(e?.message || String(e));

            }

        }

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

    const openUploadDetailsModal = (uploadImportData: UploadImportItem) => {

        openModal(UploadDetailsModal, { uploadImportData });

    }

    const UploadCard = (u:UploadInfo|UploadImportItem, isPending?:boolean) => {

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
        const isImported = (u.status === "PROCESSED_OK");
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

        return (
            <Card className={`card-glossy w-full bg-background! h-48 transition-opacity ${isDeleting ? "opacity-50 pointer-events-none" : ""}`}>
                <CardContent className="px-6 space-y-4 mt-3">

                    {/* Top Row */}
                    <div className="flex items-center justify-between gap-3">

                        {/* File Info */}
                        <div className="min-w-0">
                            <div className="font-medium truncate">{u.filename}</div>
                            <div className="text-xs text-muted-foreground">Uploaded at: {u.startTime ?? ""}</div>
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
                            <Progress value={uploadProgress} className="h-2" indicatorClassName={progressBarColor} />
                            <div className="text-xs text-muted-foreground whitespace-nowrap">
                                {bytesToKB((u.progressSize ?? u.bytesUploaded ?? 0))}/{bytesToKB(u.totalSize ?? u.sizeBytes ?? 0)} kB ({uploadProgress.toFixed(2)}%)
                            </div>
                        </div>

                        {/* Delete & Download Buttons */}
                        <div className="flex justify-end gap-2">
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
                                <Badge className="inline-flex justify-between grow items-center gap-1 rounded-md px-2 py-1 bg-muted dark:text-shadow-md" variant={"outline"}>
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
                                <Badge className="inline-flex justify-between grow items-center gap-1 rounded-md px-2 py-1 bg-(--error) dark:text-shadow-md" variant={"outline"}>
                                    <span className="flex items-center gap-1">
                                        <CircleAlert className="h-3 w-3" />
                                        Errors:
                                    </span>
                                    <span>
                                        {u.errorFlights}
                                    </span>
                                </Badge>
                            </div>

                            <Button
                                variant={"ghost"} 
                                onClick={() => openUploadDetailsModal(u)}
                            >
                                <List />
                                Details
                            </Button>

                        </div>
                        :
                        <div className="flex items-center gap-2 text-xs text-muted-foreground py-2">
                            <Loader size={20} className="animate-spin duration-2000" />
                            <span>{importPendingMessage}</span>
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

        <Pagination className="mx-0 w-full justify-start p-4">
            <PaginationContent>

                {/* Previous */}
                <PaginationItem>
                    <PaginationPrevious href="#" />
                </PaginationItem>

                {/* Current */}
                {
                    Array.from({ length: pages+1 }, (_, i) => i).map((p) => (
                        <PaginationItem key={`page-${p}`}>
                            <PaginationLink
                                href="#"
                                onClick={(e) => { e.preventDefault(); onPage(p); }}
                                aria-current={p === page ? "page" : undefined}
                                isActive={p === page}
                            >
                                {p + 1}
                            </PaginationLink>
                        </PaginationItem>
                    ))
                }

                {/* Ellipsis */}
                <PaginationItem>
                    <PaginationEllipsis />
                </PaginationItem>

                {/* Next */}
                <PaginationItem>
                    <PaginationNext href="#" />
                </PaginationItem>

            </PaginationContent>
        </Pagination>
    );

    /*
        Combine the Uploads and Imports so they
        can be shown together on one card.

        NOTE: This is getting retriggered every
        frame when a new upload is added; might
        look into a more efficient way later.
    */
    const mergedUploadsImports = React.useMemo(() => {

        const byIdentifier = new Map(uploads.map((u) => [u.identifier, u]));
        const visiblePending = pending.filter((p) => {

            // No server upload with this identifier, always show pending.
            if (!byIdentifier.has(p.identifier))
                return true;

            // If local upload is actively hashing/uploading, prefer local pending card.
            return (p.status === "HASHING" || p.status === "UPLOADING");

        });

        const activePendingIdentifiers = new Set(
            visiblePending
                .filter((p) => p.status === "HASHING" || p.status === "UPLOADING")
                .map((p) => p.identifier)
        );

        const combined: (UploadInfo | UploadImportItem)[] = [];

        // Add pending uploads first
        for (const p of visiblePending) {
            combined.push(p);
        }

        // Add uploads, merging in import data (if it exists)
        for (const u of uploads) {

            // Local active pending card takes precedence over same server identifier.
            if (activePendingIdentifiers.has(u.identifier))
                continue;

            const importData = imports.find((i) => i.id === u.id);

            // Got corresponding import data, merge it in
            if (importData)
                combined.push({ ...u, ...importData } as UploadImportItem);

            // Otherwise, just add the upload
            else
                combined.push(u as UploadInfo);

        }


        /*
            log("Combined uploads & imports:", combined);
        */

        return combined;

    }, [uploads, imports, pending]);


    const hasAnyUploadsOrImports = (mergedUploadsImports.length > 0);

    const render = () => (
        <div className="page-container">

            <div className="page-content space-y-4 w-full max-w-7xl h-full mx-auto">

                <UploadsDropzone onPickFiles={onPickFiles} />

                {/* <Card className="card-glossy grow"> */}
                <Card className="w-full h-full min-h-0 card-glossy flex flex-col justify-between overflow-clip relative">
                    <CardHeader className="flex flex-row items-center justify-between">

                        <div className="flex flex-col space-y-1.5">
                            <CardTitle>Uploads</CardTitle>
                            <CardDescription>Manage file uploads and review imported results.</CardDescription>
                        </div>

                    </CardHeader>
                    <CardContent className="w-full h-full  space-y-4 pt-6 grid grid-cols-2 gap-2 mb-auto overflow-y-auto">

                        {
                            (hasAnyUploadsOrImports)
                            ?
                            mergedUploadsImports.map((u: UploadInfo | UploadImportItem, i) =>
                                <motion.div
                                    key={`upload-${u.identifier}`}
                                    initial={{ opacity: 0.00 }}
                                    animate={{ opacity: 1.00 }}
                                    transition={{ duration: 0.50, delay: 0.03 * i }}
                                >
                                    {UploadCard(u, u.id === -1)}
                                </motion.div>
                            )
                            :
                            <PanelAlert
                                title="No Uploads"
                                description={["Use the 'Upload Files' button above to add files for upload.", "Active and finalized uploads will appear here."]}
                            />
                        }

                    </CardContent>

                    
                    <CardFooter className="flex flex-col w-full p-0 bg-muted">
                        <Separator />
                        {Pager(uploadsPage, uploadsPages, setUploadsPage)}
                    </CardFooter>

                </Card>
            </div>
        </div>
    );

    return render();
}
