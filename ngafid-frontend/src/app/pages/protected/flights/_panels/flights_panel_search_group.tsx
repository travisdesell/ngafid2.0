// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search_group.tsx
import ConfirmModal from "@/components/modals/confirm_modal";
import { useModal } from "@/components/modals/modal_provider";
import Ping from "@/components/pings/ping";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { FilterGroup } from "@/pages/protected/flights/_filters/types";
import FlightsPanelSearchRule from "@/pages/protected/flights/_panels/flights_panel_search_rule";
import { Bolt, Folder, Trash } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { FILTER_RULE_NAME_NEW, useFlights } from "../flights";


const log = getLogger("FlightsPanelSearchGroup", "green", "Component");


type Props = {
    depth: number;
    group: FilterGroup;
    indexPath: number[];
}
export function FlightsPanelSearchGroup({ depth, group, indexPath }: Props) {

    const { filter, setFilter, newID, filterIsEmpty } = useFlights();
    const { setModal } = useModal();



    /* -- Logic -- */
    const createNewRule = () => {

        log("Attempting to create new rule at depth", depth, group);

        setFilter((prev) => {

            // Deep clone for immutability
            const next = structuredClone(prev) as FilterGroup;

            // Walk down to the current group using the path
            let cursor: FilterGroup = next;
            for (const idx of indexPath) {
                cursor.groups = cursor.groups ?? [];
                cursor = cursor.groups[idx];
            }

            const newEmptyRule = {
                id: newID(),
                name: FILTER_RULE_NAME_NEW,
                conditions: []
            };

            cursor.rules = cursor.rules ?? [];
            cursor.rules.push(newEmptyRule);

            return next;

        });

        log("New rule created. Updated filter (will reflect after render):", filter);

    }

    const createNewGroup = () => {

        log("Attempting to create new group at depth", depth, group);

        setFilter((prev) => {

            // Deep clone (for immutability)
            const next = structuredClone(prev) as FilterGroup;

            // Walk down to the current group using the given path
            let cursor: FilterGroup = next;
            for (const idx of indexPath) {
                cursor.groups = cursor.groups ?? [];
                cursor = cursor.groups[idx];
            }

            const newEmptyGroup: FilterGroup = {
                id: newID(),
                operator: "AND",
                rules: [],
                groups: []
            };

            cursor.groups = cursor.groups ?? [];
            cursor.groups.push(newEmptyGroup);

            return next;

        });

        log("Filter Panel Search Group - New group created. Updated filter (will reflect after render):", filter);

    };


    /* -- Rendering -- */
    const renderRuleType = () => {

        const toggleOperator = () => {

            log("Toggling operator for group at depth", depth, group);

            setFilter((prev) => {

                // Deep clone (for immutability)
                const next = structuredClone(prev) as FilterGroup;

                // Walk down to the current group using the given path
                let cursor: FilterGroup = next;
                for (const idx of indexPath) {
                    cursor.groups = cursor.groups ?? [];
                    cursor = cursor.groups[idx];
                }

                // Toggle operator
                cursor.operator = (cursor.operator === "AND") ? "OR" : "AND";

                return next;

            });

            log("Operator toggled. Updated filter (will reflect after render):", filter);

        }

        return <Button
            variant={"outline"}
            onClick={toggleOperator}
            className="min-w-16"
        >
            {group.operator}
        </Button>

        // return <ButtonGroup>
        //     <Button variant={"outline"}>AND</Button>
        //     <Button variant={"outline"}>OR</Button>
        // </ButtonGroup>

    }

    const renderNewRuleButton = (isRoot: boolean) => {

        const displayPing = (isRoot && filterIsEmpty(filter));

        return <Button
            onClick={createNewRule}
            className="relative"
        >
            {(displayPing) && <Ping />}
            <Bolt />{isRoot && <span>New Rule</span>}
        </Button>

    }

    const renderNewGroupButton = (isRoot: boolean) => {

        return <Button onClick={createNewGroup}>
            <Folder />{isRoot && <span> New Group</span>}
        </Button>

    }

    const renderDeleteGroupButton = (isRoot: boolean, indexPath: number[]) => {

        const handleDeleteGroup = () => {

            log("Attempting to delete group at depth", depth, group);

            setFilter((prev) => {

                // At the root, clear the entire filter
                if (isRoot) {

                    return {
                        ...prev,
                        rules: [],
                        groups: []
                    }

                }

                // Deep clone (for immutability)
                const next = structuredClone(prev) as FilterGroup;

                // Walk down to the parent of the current group using the given path
                let cursor: FilterGroup = next;
                for (let i = 0; i < indexPath.length - 1; i++) {
                    const idx = indexPath[i];
                    cursor.groups = cursor.groups ?? [];
                    cursor = cursor.groups[idx];
                }

                // Remove the group from its parent's groups array
                const groupIdxToDelete = indexPath[indexPath.length - 1];
                if (cursor.groups)
                    cursor.groups.splice(groupIdxToDelete, 1);

                return next;

            });

            log("Group deleted. Updated filter (will reflect after render):", filter);

        }

        const handleDeleteGroupButtonClicked = () => {

            // Group is empty, delete immediately
            if (
                (group.rules === undefined || group.rules.length === 0)
                &&
                (group.groups === undefined || group.groups.length === 0)
            ) {
                handleDeleteGroup();
                return;
            }

            const confirmModalDeleteMessage = (isRoot)
                ? "Are you sure you want to clear the entire filter?"
                : "Are you sure you want to delete this group and all its rules?";

            // Group is not empty, show confirmation modal
            setModal(
                ConfirmModal,
                {
                    title: "Delete Group",
                    message: confirmModalDeleteMessage,
                    buttonVariant: "destructive",
                    onConfirm: handleDeleteGroup
                }
            );

        }

        // At root, disable if there are no rules/groups
        let disableDelete = false;
        if (isRoot) {
            const hasAnyRules = (group.rules && group.rules.length > 0);
            const hasAnySubGroups = (group.groups && group.groups.length > 0);
            disableDelete = !(hasAnyRules || hasAnySubGroups);
        }

        return <Button
            variant="destructive"
            onClick={handleDeleteGroupButtonClicked}
            disabled={disableDelete}
        >
            <Trash />{isRoot && <span>Delete Group</span>}
        </Button>

    }

    const render = () => {

        log("Rendering at depth", depth, group);

        const subGroups = group.groups ?? [];

        const isRoot = (depth === 0);

        const hasAnyRules = (group.rules && group.rules.length > 0);
        const hasAnySubGroups = (subGroups.length > 0);

        const groupClasses = `
            w-full overflow-clip ${
                isRoot
                ? ""
                : "border-1 border-gray-400 rounded-lg bg-neutral-500/10"
            }`;

        const subGroupContainerClasses = `
            gap-2 flex flex-col
            ${hasAnyRules || hasAnySubGroups ? "p-2 " : ""}
        `

        return (
            <motion.div
                layout
                layoutRoot
                className={groupClasses}
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.2 }}
                exit={{ opacity: 0, scale: 0.95 }}
            >

                {/* Group Header */}
                <div className="flex flex-row justify-between items-center w-full p-2">

                    {/* Rule Type */}
                    {renderRuleType()}

                    {/* Group Button Row */}
                    <div className="flex flex-row gap-2">
                        {renderNewRuleButton(isRoot)}
                        {renderNewGroupButton(isRoot)}
                        {renderDeleteGroupButton(isRoot, indexPath)}
                    </div>

                </div>

                {/* Group Rules & Subgroups */}
                <motion.div
                    layout
                    className={subGroupContainerClasses}
                >
                    {/* Rules */}
                    <AnimatePresence mode="popLayout">
                        {group.rules?.map((rule, index) => (
                            <motion.div
                                key={rule.id}
                                layout
                                initial={{ opacity: 0, scale: 0.95 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0, scale: 0.95 }}
                                transition={{ duration: 0.2 }}
                            >
                                <FlightsPanelSearchRule
                                    rule={rule}
                                    indexPath={[...indexPath, index]}
                                />
                            </motion.div>
                        ))}
                    </AnimatePresence>

                    {/* Sub-Groups */}
                    <AnimatePresence mode="popLayout">
                        {(group.groups ?? []).map((sg, index) => (
                            <motion.div
                                key={sg.id}
                                layout
                                initial={{ opacity: 0, scale: 0.95 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0, scale: 0.95 }}
                                transition={{ duration: 0.2 }}
                            >
                                <FlightsPanelSearchGroup
                                    depth={depth + 1}
                                    group={sg}
                                    indexPath={[...indexPath, index]}
                                />
                            </motion.div>
                        ))}
                    </AnimatePresence>
                </motion.div>


            </motion.div>

        );

    }

    return render();

}