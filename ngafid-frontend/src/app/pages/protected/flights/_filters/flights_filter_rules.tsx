// ngafid-frontend/src/app/pages/protected/flights/_filters/flights_filter_rules.tsx
import { getLogger } from "@/components/providers/logger";
import { FilterRuleDefinition } from "@/pages/protected/flights/_filters/types";


const log = getLogger("FlightsFilterRules", "black", "Component");


export type RuleOptions = {
    airframes: string[];
    systemIds: string[];
    tailNumbers: string[];
    timeZones?: string[];
    doubleTimeSeriesNames: string[];
    visitedAirports: string[];
    visitedRunways: string[];
    eventNames: string[];
    tagNames: string[];
};

const defaultTimeZones = ["UTC", "Local"];

export const SORTABLE_COLUMNS = {
    "Flight ID" : "id",
    "Flight Length" : "number_rows",
    "Start Date and Time" : "start_time",
    "End Date and Time" : "end_time",
    "System ID" : "system_id",
    "Tail Number" : "tail_number",
    "Airframe" : "airframe_id",
    "№ Airports Visited" : "airports_visited",
    "№ Tags Associated" : "flight_tags",
    "№ Events" : "events",
    "№ Takeoffs/Landings" : "itinerary",
} as Record<string, string>;

export const SORTABLE_COLUMN_NAMES = Object.keys(SORTABLE_COLUMNS);
export const SORTABLE_COLUMN_VALUES = Object.values(SORTABLE_COLUMNS);


export const createRules = (ruleOptions: RuleOptions): FilterRuleDefinition[] => {

    const {
        airframes,
        systemIds,
        tailNumbers,
        timeZones = defaultTimeZones,
        doubleTimeSeriesNames,
        visitedAirports,
        visitedRunways,
        eventNames,
        tagNames,
    } = ruleOptions;

    const rulesOut: FilterRuleDefinition[] = [

        {
            name: "Airframe",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["is", "is not"],
                },
                {
                    type: "select",
                    name: "airframes",
                    options: airframes,
                },
            ],
        },

        {
            name: "Tail Number",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["is", "is not"],
                },
                {
                    type: "select",
                    name: "tail numbers",
                    options: tailNumbers,
                },
            ],
        },

        {
            name: "System ID",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["is", "is not"],
                },
                {
                    type: "select",
                    name: "system id",
                    options: systemIds,
                },
            ],
        },

        {
            name: "Duration",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "number",
                    name: "hours",
                    min: 0,
                },
                {
                    type: "number",
                    name: "minutes",
                    min: 0,
                    max: 59
                },
                {
                    type: "number",
                    name: "seconds",
                    min: 0,
                    max: 59
                },
            ],
        },

        {
            name: "Start Date and Time",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "datetime-local",
                    name: "date and time",
                },
                {
                    type: "select",
                    name: "timezone",
                    options: timeZones,
                },
            ],
        },

        {
            name: "End Date and Time",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "datetime-local",
                    name: "date and time",
                },
                {
                    type: "select",
                    name: "timezone",
                    options: timeZones,
                },
            ],
        },

        {
            name: "Flight ID",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "number",
                    name: "number",
                },
            ],
        },

        {
            name: "Start Date",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "date",
                    name: "date",
                },
            ],
        },

        {
            name: "End Date",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "date",
                    name: "date",
                },
            ],
        },

        {
            name: "Start Time",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "time",
                    name: "time",
                },
                {
                    type: "select",
                    name: "timezone",
                    options: timeZones,
                },
            ],
        },

        {
            name: "End Time",
            conditions: [
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "time",
                    name: "time",
                },
                {
                    type: "select",
                    name: "timezone",
                    options: timeZones,
                },
            ],
        },

        {
            name: "Parameter",
            conditions: [
                {
                    type: "select",
                    name: "statistic",
                    options: ["min", "avg", "max"],
                },
                {
                    type: "select",
                    name: "doubleSeries",
                    options: doubleTimeSeriesNames,
                },
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "number",
                    name: "number",
                },
            ],
        },

        {
            name: "Airport",
            conditions: [
                {
                    type: "select",
                    name: "airports",
                    options: visitedAirports,
                },
                {
                    type: "select",
                    name: "condition",
                    options: ["visited", "not visited"],
                },
            ],
        },

        {
            name: "Runway",
            conditions: [
                {
                    type: "select",
                    name: "runways",
                    options: visitedRunways,
                },
                {
                    type: "select",
                    name: "condition",
                    options: ["visited", "not visited"],
                },
            ],
        },

        {
            name: "Event Count",
            conditions: [
                {
                    type: "select",
                    name: "eventNames",
                    options: eventNames,
                },
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "number",
                    name: "number",
                },
            ],
        },

        {
            name: "Event Severity",
            conditions: [
                {
                    type: "select",
                    name: "eventNames",
                    options: eventNames,
                },
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "number",
                    name: "number",
                },
            ],
        },

        {
            name: "Event Duration",
            conditions: [
                {
                    type: "select",
                    name: "eventNames",
                    options: eventNames,
                },
                {
                    type: "select",
                    name: "condition",
                    options: ["<=", "<", "=", ">", ">="],
                },
                {
                    type: "number",
                    name: "number",
                },
            ],
        },

        {
            name: "Tag",
            conditions: [
                {
                    type: "select",
                    name: "flight_tags",
                    options: tagNames,
                },
                {
                    type: "select",
                    name: "condition",
                    options: ["Is Associated", "Is Not Associated"],
                },
            ],
        }

    ];

    log.table("Generated Flights Filter Rules: ", rulesOut);

    return rulesOut;

};

export const BASE_RULE_DEFINITIONS: FilterRuleDefinition[] = createRules({
    airframes: [],
    systemIds: [],
    tailNumbers: [],
    timeZones: defaultTimeZones,
    doubleTimeSeriesNames: [],
    visitedAirports: [],
    visitedRunways: [],
    eventNames: [],
    tagNames: [],
});