import type { AirframeNameID } from "./types";

export type AircraftCategory = "all" | "fixed-wing" | "rotorcraft" | "uas";

export const AIRCRAFT_CATEGORIES: ReadonlyArray<{ value: AircraftCategory; label: string }> = [
    { value: "all", label: "All" },
    { value: "fixed-wing", label: "Fixed Wing" },
    { value: "rotorcraft", label: "Rotorcraft" },
    { value: "uas", label: "UAS" },
];

export function aircraftMatchesCategory(airframe: AirframeNameID, category: AircraftCategory): boolean {
    if (category === "all" || airframe.id < 0)
        return true;
    return aircraftTypeMatchesCategory(airframe.type, category);
}

export function aircraftTypeMatchesCategory(
    type: string | undefined,
    category: AircraftCategory,
): boolean {
    if (category === "all")
        return true;
    if (category === "fixed-wing")
        return type === "Fixed Wing";
    if (category === "rotorcraft")
        return type === "Rotorcraft";
    return type?.startsWith("UAS ") === true;
}

export function airframesForCategory(
    source: AirframeNameID[],
    category: AircraftCategory,
): AirframeNameID[] {
    return source.filter(airframe => aircraftMatchesCategory(airframe, category));
}
