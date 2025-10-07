// ngafid-frontend/src/app/pages/protected/uploads/_types/types.ts
export type UploadStatus =
    | "HASHING"
    | "UPLOADING"
    | "UPLOADING_FAILED"
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

export type UploadInfo = {
    id: number;
    identifier: string;
    filename: string;
    md5Hash: string;
    startTime?: string;
    endTime?: string;
    status: UploadStatus;
    sizeBytes: number;
    bytesUploaded: number;
    progressSize?: number;
    totalSize?: number;
    numberChunks?: number;
    chunkStatus?: string;   //<-- String of '0'/'1'
    position?: number;
    file?: File;            //<-- Client-side only
};

export type ImportsPageItem = {
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
};

export type UploadImportItem = UploadInfo & ImportsPageItem;

export type UploadErrorsPayload = {
    uploadErrors: { id: number; message: string }[];
    flightWarnings: { id: number; filename: string; message: string; sameFilename?: boolean }[];
    flightErrors: { id: number; filename: string; message: string; sameFilename?: boolean }[];
};

export type UploadListResponse = {
    uploads: UploadInfo[];
    numberPages: number;
};

export type ImportsListResponse = {
    imports: ImportsPageItem[];
    numberPages: number;
};

export type APIError = { errorTitle?: string; errorMessage?: string };