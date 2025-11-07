// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search_group.tsx

import DatePicker from "@/components/date-picker";
import { NumberInput } from "@/components/number_input";
import { getLogger } from "@/components/providers/logger";
import TimeInput from "@/components/time_input";
import { Button } from "@/components/ui/button";
import { ButtonGroup } from "@/components/ui/button-group";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectGroup, SelectItem, SelectLabel, SelectTrigger, SelectValue } from "@/components/ui/select";
import { RULES } from "@/pages/protected/flights/_filters/flights_filter_rules";
import { FilterCondition, FilterRule } from "@/pages/protected/flights/_filters/types";
import { FILTER_RULE_NAME_NEW, useFlights } from "@/pages/protected/flights/flights";
import { Trash } from "lucide-react";

const log = getLogger("FlightsPanelSearchRule", "blue", "Component");

type Props = {
    rule: FilterRule;
    indexPath: number[];
}
export default function FlightsPanelSearchRule({ rule, indexPath }: Props) {

    const { filter, setFilter } = useFlights();

    const updateCurrentRule = (updater: (prev: FilterRule) => FilterRule) => {

        /*
            Update the name of the current rule
            and the conditions associated with it.
        */

        log("Flights Panel Search Rule - Updating current rule:", rule);

        setFilter((prev) => {

            // Deep clone (for immutability)
            const next = structuredClone(prev);

            // Find and update the rule in the filter
            const updateRuleInGroup = (group: any) => {

                if (group.rules) {

                    for (let i = 0; i < group.rules.length; i++) {
                        
                        // Found rule, update it
                        if (group.rules[i].id === rule.id) {
                            group.rules[i] = updater(group.rules[i]);
                            return true;
                        }

                    }

                }
                
                if (group.groups) {

                    for (const subGroup of group.groups) {

                        // Found rule in a subgroup
                        if (updateRuleInGroup(subGroup))
                            return true;
                        
                    }

                }

                // Rule not found in this group
                return false;

            };

            updateRuleInGroup(next);

            return next;

        });

        log("Flights Panel Search Rule - Current rule updated. Updated filter (will reflect after render):", filter);

    }

    const renderDeleteRuleButton = (indexPath: number[]) => {

        /*
            Unlike the Group delete button,
            this should always just remove
            the rule immediately.
        */

        const handleDeleteRule = () => {

            log("Flights Panel Search Rule - Deleting rule at index path:", indexPath);

            setFilter((prev) => {

                // Deep clone (for immutability)
                const next = structuredClone(prev);

                // Navigate to the parent group of the rule to delete
                let currentGroup: any = next;
                for (let i = 0; i < indexPath.length - 1; i++) {
                    const groupIndex = indexPath[i];
                    currentGroup = currentGroup.groups ? currentGroup.groups[groupIndex] : currentGroup;
                }

                // Remove the rule from the parent group
                const ruleIndexToDelete = indexPath[indexPath.length - 1];
                if (currentGroup.rules && ruleIndexToDelete >= 0 && ruleIndexToDelete < currentGroup.rules.length)
                    currentGroup.rules.splice(ruleIndexToDelete, 1);

                return next;

            });

            log("Flights Panel Search Rule - Rule deleted. Updated filter (will reflect after render):", filter);

        };

        return <Button onClick={handleDeleteRule} variant="outline" className="hover:bg-red-200 hover:dark:bg-red-900 hover:text-red-500 focus:ring-red-500">
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

        const valueCurrentOrDefault = condition.value ?? (condition.options?.length ? condition.options[0] : undefined);
        const hasOptions = !!condition.options?.length;
        const updateConditionValue = (value: string | number | undefined) => {

            updateCurrentRule((prev) => {
                const next = structuredClone(prev);
                next.conditions[idx].value = (value as any);
                return next;
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
                    className="min-w-[128px]"
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

                const setDateValue = (date: Date) => {
                    updateCurrentRule((prev) => {
                        const next = structuredClone(prev);
                        next.conditions[idx].value = date.toISOString();
                        return next;
                    });
                }

                let dateValue = valueCurrentOrDefault;
                if (!dateValue) {
                    dateValue = new Date().toISOString();
                    setDateValue(new Date(dateValue));
                } else {
                    dateValue = new Date(dateValue);
                }

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

                return <NumberInput
                    className="min-w-[128px] rounded-none!"
                    key={key}
                    value={valueNumeric}
                    onValueChange={(n) => updateConditionValue(n)}
                    placeholder={condition.name}
                />;

            }

            /* Unknown Condition Type */
            default:
                return <Input
                    key={key}
                    placeholder={condition.name}
                    className="min-w-[128px] bg-fuchsia-500 after:content-['⚠']! pointer-events-none"
                />;

        };

    }

    const render = () => {

        function updateRuleName(value: string): void {

            // No change, exit
            if (value === rule.name)
                return;

            const matching = RULES.find(r => r.name === value);

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
            <Select onValueChange={updateRuleName} defaultValue={rule.name} value={rule.name}>

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
                            RULES.map((ruleFromList) => (
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