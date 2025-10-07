// src/workers/md5.worker.ts
import SparkMD5 from "spark-md5";

export const CHUNK_SIZE = 2 * 1024 * 1024;

self.onmessage = async (e: MessageEvent) => {

    const file: File = e.data;
    const spark = new SparkMD5.ArrayBuffer();
    const chunks = Math.ceil(file.size / CHUNK_SIZE);

    for (let i = 0; i < chunks; i++) {

        const start = i * CHUNK_SIZE;
        const end = Math.min(start + CHUNK_SIZE, file.size);
        const buf = await file.slice(start, end).arrayBuffer();
        spark.append(buf);
        (self as any).postMessage({ type: "progress", done: end, total: file.size });

    }

    (self as any).postMessage({ type: "done", hash: spark.end() });
    (self as any).close();

};