// ngafid-frontend/src/app/pages/protected/flights/_filters/types.ts
export type FilterGroupOperators = "AND" | "OR";

export const SPECIAL_FILTER_GROUP_ID = "special-flight-id-group";
export type FilterID = string | typeof SPECIAL_FILTER_GROUP_ID;
export type FilterGroup = {
    id: FilterID;
    rules?: FilterRule[];
    groups?: FilterGroup[];
    operator: FilterGroupOperators;
};

export type FilterConditionType = 'select' | 'input' | 'time' | 'date' | 'datetime-local' | 'number';
export type FilterCondition = {
    type: FilterConditionType;
    name: string;
    options?: any[];
    value?: string;
    min?: number;
    max?: number;
}

export type FilterRule = {
    id: string;
    name: string;
    conditions: FilterCondition[];
};

export type FilterRuleDefinition = Omit<FilterRule, 'id'>;

export interface Filter extends FilterGroup {};