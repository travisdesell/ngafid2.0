// ngafid-frontend/src/app/pages/protected/flights/_filters/types.ts
export type FilterGroupOperators = "AND" | "OR";

export const SPECIAL_FILTER_GROUP_ID = "special-flight-id-group";
export type FilterID = string | typeof SPECIAL_FILTER_GROUP_ID;
export interface FilterGroup {
    id: FilterID;
    rules?: Array<FilterRule>;
    groups?: Array<FilterGroup>;
    operator: FilterGroupOperators;
}

export type FilterConditionType = 'select' | 'input' | 'time' | 'date' | 'datetime-local' | 'number';
export interface FilterCondition {
    type: FilterConditionType;
    name: string;
    options?: Array<any>;
    value?: string;
    min?: number;
    max?: number;
}

export interface FilterRule {
    id: string;
    name: string;
    conditions: Array<FilterCondition>;
}

export type FilterRuleDefinition = Omit<FilterRule, 'id'>;

export interface Filter extends FilterGroup {};