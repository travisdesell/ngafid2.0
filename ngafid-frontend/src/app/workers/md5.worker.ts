// src/workers/md5.worker.ts
import { CHUNK_SIZE, type Md5WorkerMessage } from "@/workers/md5.shared";
import SparkMD5 from "spark-md5";

const context = (self as unknown) as DedicatedWorkerGlobalScope;

function isFileLike(x: unknown): x is File {

    return (typeof x === "object")
        && (x !== null)
        && (typeof (x as File).size === "number")
        && (typeof (x as File).slice === "function")
    ;

}

context.onmessage = async (e: MessageEvent) => {

    const file: File = e.data;

    // Validate input
    if (!isFileLike(file)) {
        context.postMessage({ type: "error", message: "Input is not a valid File object." } satisfies Md5WorkerMessage);
        return;
    }

    const spark = new SparkMD5.ArrayBuffer();
    const chunks = Math.ceil(file.size / CHUNK_SIZE);

    for (let i = 0; i < chunks; i++) {

        const start = (i * CHUNK_SIZE);
        const end = Math.min(start + CHUNK_SIZE, file.size);

        const buf = await file.slice(start, end).arrayBuffer();
        spark.append(buf);
        
        context.postMessage({ type: "progress", done: end, total: file.size } satisfies Md5WorkerMessage);

    }

    context.postMessage({ type: "done", hash: spark.end() } satisfies Md5WorkerMessage);

};