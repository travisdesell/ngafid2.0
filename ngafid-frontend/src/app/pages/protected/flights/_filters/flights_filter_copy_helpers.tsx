// ngafid-frontend/src/app/pages/protected/flights/_filters/flights_filter_copy_helpers.tsx


/*
    Helper functions to convert
    flight filters to/from compact
    URL-encodable representations.

    Does not contain the actual
    function for copying to the
    clipboard; it instead lives
    next to the "Copy Filter URL"
    button in the FlightsPanelSearch
    component.
*/


import { RULES } from "@/pages/protected/flights/_filters/flights_filter_rules";
import { FilterGroup, SPECIAL_FILTER_GROUP_ID } from "@/pages/protected/flights/_filters/types";
import { FILTER_RULE_NAME_NEW } from "@/pages/protected/flights/flights";

// Build rule-name <-> tiny-id maps once
const RULE_NAME_TO_ID = Object.fromEntries(RULES.map((r, i) => [r.name, i.toString(36)]));
const RULE_ID_TO_DEF = Object.fromEntries(RULES.map((r, i) => [i.toString(36), r]));

// Base64url helpers for Uint8Array
export function u8ToBase64url(u8: Uint8Array): string {

    let s = "";
    for (let i = 0; i < u8.length; i++)
        s += String.fromCharCode(u8[i]);

    const b64 = btoa(s);
    return b64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");

}

export function base64urlToU8(b64url: string): Uint8Array {

    const b64 = b64url.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((b64url.length + 3) % 4);
    const bin = atob(b64);
    const out = new Uint8Array(bin.length);

    for (let i = 0; i < bin.length; i++)
        out[i] = bin.charCodeAt(i);

    return out;

}

// Convert filter to compact wire format
type OperatorShort = "A" | "O";
type WireRule = { n: string; c: any[] };
type WireGroup = { op: OperatorShort; r?: WireRule[]; g?: WireGroup[]; s?: 1};

export function toWire(group: FilterGroup): WireGroup | null {

    const operator: OperatorShort = (group.operator === "AND")
        ? "A"
        : "O";

    const rules: WireRule[] = (group.rules ?? [])
        .filter((ru: any) => ru?.name && ru.name !== FILTER_RULE_NAME_NEW)
        .map((ru: any) => {

            const n = RULE_NAME_TO_ID[ru.name] ?? ru.name; // <-- Fall back to raw name if unknown
            const c = (ru.conditions ?? []).map((cond: any) => cond?.value);

            // Drop rules with missing values
            if (c.some((v: string | null | undefined) => v === undefined || v === null || v === ""))
                return null;

            return { n, c } as WireRule;

        })
        .filter(Boolean) as WireRule[];

    const groups: WireGroup[] = (group.groups ?? [])
        .map((sg: any) => toWire(sg))
        .filter((x: any) => x !== null) as WireGroup[];

    // No rules or groups -> null
    if ((rules.length === 0) && (groups.length === 0))
        return null;

    const out: WireGroup = {
        op: operator,
        r: (rules.length) ? rules : undefined,
        g: (groups.length) ? groups : undefined
    };

    // Flag special Flight ID group
    if (group.id === SPECIAL_FILTER_GROUP_ID)
        out.s = 1;

    return out;

}

// Convert compact wire format back to filter
export function fromWire(w: WireGroup, newID: () => string): any {

    const operator = (w.op === "A")
        ? "AND"
        : "OR";

    const id = (w.s)
        ? SPECIAL_FILTER_GROUP_ID
        : newID();

    const rules = (w.r ?? []).map((wr) => {
        const def = RULE_ID_TO_DEF[wr.n];
        const name = def?.name ?? wr.n; //<-- Fall back to raw

        // Rebuild conditions from RULES shape
        const conditions = (def?.conditions ?? []).map((c, i) => ({ ...c, value: wr.c?.[i] }));
        return { id: newID(), name, conditions };
    });

    const groups = (w.g ?? []).map((wg) => fromWire(wg, newID));
    return { id, operator, rules, groups };

}