// ngafid-frontend/src/app/pages/protected/flights/_filters/types.ts
export type FilterGroupOperators = "AND" | "OR";

export type FilterGroup = {
    id: string;
    rules?: FilterRule[];
    groups?: FilterGroup[];
    operator: FilterGroupOperators;
};

export type FilterConditionType = 'select' | 'input' | 'time' | 'date' | 'datetime-local' | 'number';
export type FilterCondition = {
    type: FilterConditionType;
    name: string;
    options?: any[];
}

export type FilterRule = {
    id: string;
    name: string;
    conditions: FilterCondition[];
};

export type Filter = FilterGroup;