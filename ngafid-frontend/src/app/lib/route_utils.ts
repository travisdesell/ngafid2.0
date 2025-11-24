// ngafid-frontend/src/app/lib/route_utils.ts

import { getLogger } from "@/components/providers/logger";

const log = getLogger("RouteUtils", "purple", "Utility");

export const ROUTE_BASE = "";
export const ROUTE_DEFAULT_LOGGED_OUT = `${ROUTE_BASE}/`;
export const ROUTE_DEFAULT_LOGGED_IN = `${ROUTE_BASE}/protected/summary`;

export function openRoute(url: string, isProtected: boolean = false) {

    let targetURLFull: string;

    if (isProtected)
        targetURLFull = `${ROUTE_BASE}/protected/${url}`;
    else
        targetURLFull = `${ROUTE_BASE}/${url}`;

    log(`Navigating to ${targetURLFull} (Protected: ${isProtected})`);

    window.location.replace(targetURLFull);

}