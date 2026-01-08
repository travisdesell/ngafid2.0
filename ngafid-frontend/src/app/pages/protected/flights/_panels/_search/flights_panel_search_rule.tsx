// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search_group.tsx

import DatePicker from "@/components/date-picker";
import { NumberInput } from "@/components/number_input";
import { getLogger } from "@/components/providers/logger";
import TimeInput from "@/components/time_input";
import { Button } from "@/components/ui/button";
import { ButtonGroup } from "@/components/ui/button-group";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectGroup, SelectItem, SelectLabel, SelectTrigger, SelectValue } from "@/components/ui/select";
import { FilterCondition, FilterGroup, FilterRule, FilterRuleDefinition } from "@/pages/protected/flights/_filters/types";
import { useFlightsFilter } from "@/pages/protected/flights/_flights_context_search_filter";
import { FILTER_RULE_NAME_NEW } from "@/pages/protected/flights/types";
import { Trash } from "lucide-react";
import { memo } from "react";

const log = getLogger("FlightsPanelSearchRule", "blue", "Component");

type Props = {
    rule: FilterRule;
    indexPath: number[];
    ruleDefinitions: FilterRuleDefinition[];
}
function FlightsPanelSearchRuleInner({ rule, indexPath, ruleDefinitions }: Props) {

    const { filter, setFilter } = useFlightsFilter();

    const updateCurrentRule = (updater: (prev: FilterRule) => FilterRule) => {

        log("Flights Panel Search Rule - Updating current rule:", rule);

        setFilter((prevFilter) => {
            
            // indexPath is: [g0, g1, ..., ruleIndex]
            if (indexPath.length === 0)
                return prevFilter;

            // Clone root filter group
            const nextFilter: FilterGroup = { ...prevFilter };

            // Walk groups down to the parent group of this rule
            let cursor: FilterGroup = nextFilter;
            for (let i = 0; i < indexPath.length - 1; i++) {
                const groupIndex = indexPath[i];

                const groups = cursor.groups ?? [];
                if (groupIndex < 0 || groupIndex >= groups.length)
                    return prevFilter; // Invalid path, bail

                const child = groups[groupIndex];
                const childClone: FilterGroup = { ...child };

                const newGroups = [...groups];
                newGroups[groupIndex] = childClone;
                cursor.groups = newGroups;

                cursor = childClone;
            }

            const ruleIndex = indexPath[indexPath.length - 1];
            const rules = cursor.rules ?? [];
            if (ruleIndex < 0 || ruleIndex >= rules.length)
                return prevFilter;

            const oldRule = rules[ruleIndex];
            const updatedRule = updater(oldRule);

            // No change -> bail to avoid useless re-renders
            if (updatedRule === oldRule)
                return prevFilter;

            const newRules = [...rules];
            newRules[ruleIndex] = updatedRule;
            cursor.rules = newRules;

            return nextFilter as any;
        });

        log("Flights Panel Search Rule - Current rule updated. Updated filter (will reflect after render):", filter);
    };

    const renderDeleteRuleButton = (indexPath: number[]) => {

        /*
            Unlike the Group delete button,
            this should always just remove
            the rule immediately.
        */

        const handleDeleteRule = () => {
            log("Flights Panel Search Rule - Deleting rule at index path:", indexPath);

            setFilter((prev) => {
                const next: FilterGroup = { ...prev };

                // Navigate to parent group
                let currentGroup: FilterGroup = next;
                for (let i = 0; i < indexPath.length - 1; i++) {
                    const groupIndex = indexPath[i];
                    const groups = currentGroup.groups ?? [];
                    if (groupIndex < 0 || groupIndex >= groups.length)
                        return prev; // invalid path

                    const child = groups[groupIndex];
                    const childClone: FilterGroup = { ...child };
                    const newGroups = [...groups];
                    newGroups[groupIndex] = childClone;
                    currentGroup.groups = newGroups;
                    currentGroup = childClone;
                }

                const ruleIndexToDelete = indexPath[indexPath.length - 1];
                const rules = currentGroup.rules ?? [];
                if (ruleIndexToDelete < 0 || ruleIndexToDelete >= rules.length)
                    return prev;

                const newRules = [...rules];
                newRules.splice(ruleIndexToDelete, 1);
                currentGroup.rules = newRules;

                return next as any;
            });

            log("Flights Panel Search Rule - Rule deleted. Updated filter (will reflect after render):", filter);
        };

        return <Button
            onClick={handleDeleteRule}
            variant="outline"
            className="hover:bg-red-200 hover:dark:bg-red-900 hover:text-red-500 focus:ring-red-500"
        >
            <Trash />
        </Button>;

    }

    const renderConditionInput = (condition: FilterCondition, idx: number) => {

        /*
            Use a different kind of input
            component based on the condition type.

            (e.g. 'select' => Select dropdown component)
        */

        const key = `${rule.id}-condition-${idx}`;

        const valueCurrentOrDefault = (condition.value ?? "");

        const hasOptions = !!condition.options?.length;
        const updateConditionValue = (value: string | number | undefined) => {

            updateCurrentRule((prevRule) => {
                const nextConditions = [...prevRule.conditions];
                nextConditions[idx] = {
                    ...nextConditions[idx],
                    value: value as any,
                };
                return {
                    ...prevRule,
                    conditions: nextConditions,
                };
            });
        };

        switch (condition.type) {

            /* Dropdown Select */
            case 'select':
                return <Select
                    key={key}
                    value={valueCurrentOrDefault}
                    onValueChange={updateConditionValue}
                    disabled={!hasOptions}
                >

                    <Button asChild variant="outline">
                        <SelectTrigger className="w-full">
                            <SelectValue placeholder={condition.name} />
                        </SelectTrigger>
                    </Button>

                    <SelectContent>
                        <SelectGroup>
                            {
                                condition.options?.map((option) => (
                                    <SelectItem key={option} value={option}>
                                        {option}
                                    </SelectItem>
                                ))
                            }
                        </SelectGroup>
                    </SelectContent>
                </Select>;

            /* Generic Text Input */
            case 'input':
                return <Input
                    key={key}
                    value={(valueCurrentOrDefault as string) ?? ''}
                    onChange={(e) => updateConditionValue(e.target.value)}
                    placeholder={condition.name}
                    className="min-w-32"
                />;

            /* Time Input */
            case 'time':
                return <TimeInput
                    key={key}
                    value={(valueCurrentOrDefault as string) ?? ''}
                    onChange={(e) => updateConditionValue(e.target.value)}
                />;

            /* Date Input */
            case 'date':
            case 'datetime-local': {

                const setDateValue = (date?: Date) => {
                    updateCurrentRule((prevRule) => {
                        const nextConditions = [...prevRule.conditions];
                        nextConditions[idx] = {
                            ...nextConditions[idx],
                            value: date ? date.toISOString() : undefined,
                        };
                        return {
                            ...prevRule,
                            conditions: nextConditions,
                        };
                    });
                };

                // No date value set, use today as default
                if (!valueCurrentOrDefault) {

                    const dateToday = new Date();

                    return <Button key={key} asChild variant="outline" className="rounded-none! bg-fuchsia-500">
                        <DatePicker date={dateToday} setDate={setDateValue} />
                    </Button>;

                }

                const dateValue = new Date(valueCurrentOrDefault as string);
                return <Button key={key} asChild variant="outline" className="rounded-none! bg-fuchsia-500">
                        <DatePicker date={dateValue} setDate={setDateValue} />
                    </Button>;

            }

            /* Number Input */
            case 'number': {

                const valueNumeric = (typeof valueCurrentOrDefault === 'number')
                    ? valueCurrentOrDefault
                    : (valueCurrentOrDefault != null && valueCurrentOrDefault !== '')
                        ? Number(valueCurrentOrDefault)
                        : undefined;

                const min = (typeof condition.min === 'number') ? condition.min : undefined;
                const max = (typeof condition.max === 'number') ? condition.max : undefined;

                return <NumberInput
                    className="min-w-32 rounded-none!"
                    key={key}
                    value={valueNumeric}
                    onValueChange={(n) => updateConditionValue(n)}
                    placeholder={condition.name}
                    min={min}
                    max={max}
                />;

            }

            /* Unknown Condition Type */
            default:
                return <Input
                    key={key}
                    placeholder={condition.name}
                    className="min-w-32 bg-fuchsia-500 after:content-['⚠']! pointer-events-none"
                />;

        };

    }

    const render = () => {

        function updateRuleName(value: string): void {

            // No change, exit
            if (value === rule.name)
                return;

            const matching = ruleDefinitions .find(r => r.name === value);

            updateCurrentRule((prev) => {

                // Start from the existing rule object
                const next: FilterRule = { ...prev, name: value };

                // Found a rule definition, replace conditions with a deep-cloned copy
                if (matching && matching.conditions) {
                    
                    const cloned = structuredClone(matching.conditions);
                    for (const c of cloned) {
                        if (c.type === 'select')
                            c.value = c.value ?? (c.options && c.options.length ? c.options[0] : undefined);
                    }

                    next.conditions = cloned;

                // Otherwise, keep existing conditions
                } else {
                    next.conditions = prev.conditions ?? [];
                }

                return next;

            });

        }

        const isNewRule = (rule.name === FILTER_RULE_NAME_NEW);

        return <ButtonGroup>

            {/* Delete Button */}
            {renderDeleteRuleButton(indexPath)}

            {/* Rule Name */}
            <Select onValueChange={updateRuleName} value={rule.name}>

                <Button asChild variant="outline">
                    <SelectTrigger className="min-w-[256px]">
                        <SelectValue placeholder="Select Rule" />
                        {isNewRule ? 'Select Rule' : ''}
                    </SelectTrigger>
                </Button>

                <SelectContent>
                    <SelectGroup>
                        <SelectLabel>Rules</SelectLabel>
                        {
                            ruleDefinitions.map((ruleFromList) => (
                                <SelectItem key={ruleFromList.name} value={ruleFromList.name}>{ruleFromList.name}</SelectItem>
                            ))
                        }
                    </SelectGroup>
                </SelectContent>
            </Select>

            {/* Rule Conditions */}
            {
                rule.conditions.map((condition, idx) => (
                    renderConditionInput(condition, idx)
                ))
            }

        </ButtonGroup>

    };

    return render();

    
}   

export default memo(FlightsPanelSearchRuleInner);