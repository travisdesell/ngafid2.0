// ngafid-frontend/src/app/fetchJson.ts
type Primitive = string | number | boolean | Date;
type Params =
    | Record<string, Primitive | Primitive[] | null | undefined>
    | URLSearchParams;


//Convert Params to URLSearchParams
function toSearchParams(input?: Params): URLSearchParams | undefined {

    //No input, no params
    if (!input)
        return undefined;

    //Already URLSearchParams, return as-is
    if (input instanceof URLSearchParams)
        return input;

    const searchParams = new URLSearchParams();
    const push = (k: string, v: Primitive) => {

        //Coerce dates to strings
        if (v instanceof Date) {
            const iso = v.toISOString();
            searchParams.append(k, iso.slice(0, 10));
        } else {
            searchParams.append(k, String(v));
        }

    };

    for (const [k, v] of Object.entries(input)) {

        //Skip null/undefined values
        if (v === null || v === undefined)
            continue;

        //Got an array, push each value
        if (Array.isArray(v)) {
            for (const each of v) push(k, each);

        //Otherwise, just push the one value
        } else {
            push(k, v);
        }

    }

    return searchParams;

}


//Append params to URL
function withParams(url: string, params?: Params): string {

    const searchParams = toSearchParams(params);

    //No params, return as-is
    if (!searchParams)
        return url;
    
    const hasQ = url.includes("?");
    return `${url}${hasQ ? "&" : "?"}${searchParams.toString()}`;

}


async function coreFetchJson<T = any>(url: string, init: RequestInit = {}): Promise<T> {

    const method = (init.method || "GET").toUpperCase();

    //Strip body for GET/HEAD requests
    if ((method === "GET" || method === "HEAD") && "body" in init) {
        const { body, ...rest } = init;
        init = rest;
    }

    //Default Accept: JSON
    init.headers = {
        Accept: "application/json",
        ...(init.headers || {}),
    };

    const res = await fetch(url, init);

    //Response not ok, throw error
    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`${method} ${url} -> ${res.status} ${res.statusText}${text ? ` - ${text.slice(0, 180)}` : ""}`);
    }

    const contentType = (res.headers.get("content-type") || "").toLowerCase();

    //Content-Type not JSON, throw error
    if (!contentType.includes("application/json")) {
        const text = await res.text().catch(() => "");
        throw new Error(`Expected JSON from ${url}, got '${contentType || "unknown"}'${text ? ` - ${text.slice(0, 180)}` : ""}`);
    }

    return res.json();
}


type GetInit = Omit<RequestInit, "method" | "body"> & { params?: Params };


export const fetchJson = {

    get<T = any>(url: string, init?: GetInit) {
        const fullUrl = withParams(url, init?.params);
        const { params: _ignored, ...rest } = init || {};
        return coreFetchJson<T>(fullUrl, { ...rest, method: "GET" });
    },

    post<T = any>(url: string, body?: unknown, init?: RequestInit) {
        return coreFetchJson<T>(url, {
            ...init,
            method: "POST",
            headers: { "Content-Type": "application/json", Accept: "application/json", ...(init?.headers || {}) },
            body: body == null ? undefined : JSON.stringify(body),
        });
    },

    put<T = any>(url: string, body?: unknown, init?: RequestInit) {
        return coreFetchJson<T>(url, {
            ...init,
            method: "PUT",
            headers: { "Content-Type": "application/json", Accept: "application/json", ...(init?.headers || {}) },
            body: body == null ? undefined : JSON.stringify(body),
        });
    },
    
    delete<T = any>(url: string, init?: RequestInit) {
        return coreFetchJson<T>(url, { ...init, method: "DELETE" });
    },

};
