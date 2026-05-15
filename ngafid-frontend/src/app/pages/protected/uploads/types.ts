// ngafid-frontend/src/app/pages/protected/uploads/_types/types.ts
export type UploadStatus =
    | "HASHING"
    | "UPLOADING"
    | "UPLOADING_FAILED"
    | "FAILED_INTERRUPTED"
    | "UPLOADED"
    | "ENQUEUED"
    | "PROCESSING"
    | "PROCESSED_OK"
    | "PROCESSED_WARNING"
    | "FAILED_FILE_TYPE"
    | "FAILED_AIRCRAFT_TYPE"
    | "FAILED_ARCHIVE_TYPE"
    | "FAILED_UNKNOWN"
    | "DERIVED";


export const UPLOADS_PER_PAGE_OPTIONS = [
    10,
    25,
    50,
    100,
];

export interface UploadInfo {
    id: number;
    identifier: string;
    filename: string;
    md5Hash: string;
    startTime: string;
    endTime: string;
    status: UploadStatus;
    sizeBytes: number;
    bytesUploaded: number;
    progressSize: number;
    totalSize: number;
    numberChunks: number;
    chunkStatus: string;   //<-- String of '0'/'1'
    position: number;
    file: File;            //<-- Client-side only
    uploaderId?: number;
    fleetId: number;
    kind: string;
}

export interface ImportsPageItem {
    id: number;
    identifier: string;
    filename: string;
    endTime?: string;
    status: UploadStatus;
    validFlights: number;
    warningFlights: number;
    errorFlights: number;
    sizeBytes: number;
    bytesUploaded: number;
    position?: number;
}

export type UploadImportItem = UploadInfo & ImportsPageItem;

export interface UploadErrorsPayload {
    uploadErrors: Array<{ id: number; message: string }>;
    flightWarnings: Array<{ id: number; filename: string; message: string; sameFilename?: boolean }>;
    flightErrors: Array<{ id: number; filename: string; message: string; sameFilename?: boolean }>;
}

export interface UploadListResponse {
    uploads: Array<UploadInfo>;
    numberPages: number;
    currentPage: number;
}

export interface ImportsListResponse {
    imports: Array<ImportsPageItem>;
    numberPages: number;
    currentPage: number;
}

export interface APIError { errorTitle?: string; errorMessage?: string }