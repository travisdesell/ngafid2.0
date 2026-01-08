// ngafid-frontend/src/app/workers/md5.shared.ts
export const CHUNK_SIZE = (2 * 1024 * 1024);

export type Md5WorkerMessage =
    | { type: "progress"; done: number; total: number }
    | { type: "done"; hash: string }
    | { type: "error"; message: string };