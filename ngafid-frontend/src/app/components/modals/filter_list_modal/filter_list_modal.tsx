// ngafid-frontend/src/app/components/modals/filter_list_modal/filter_list_modal.tsx

"use client";

import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { motion } from "motion/react";

import ConfirmModal from "@/components/modals/confirm_modal";
import ErrorModal from '@/components/modals/error_modal';
import FilterEditModal from "@/components/modals/filter_edit_modal";
import { useModal } from "@/components/modals/modal_context";
import type { FlightFilter } from "@/components/providers/flight_filters_provider";
import { getLogger } from "@/components/providers/logger";
import { usePlatform } from '@/components/providers/platform_provider';
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { Filter } from '@/pages/protected/flights/_filters/types';
import { AlertCircleIcon, Check, ClipboardCopy, Pencil, Trash, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { ModalData, ModalProps } from "../types";


const log = getLogger("FilterListModal", "black", "Modal");


export type ModalDataFilterList = ModalData & {
    filters: FlightFilter[];
    saveFilter(filter: FlightFilter): Promise<void>;
    deleteFilterByName(filterName: string): Promise<void>;
    setFilterFromJSON: (json: string) => void;
    copyFilterURL: (filterTarget: Filter) => void;
}

export default function FilterListModal({ data }: ModalProps<ModalDataFilterList>) {

    const { close, setModal } = useModal();
    const { filters, saveFilter, deleteFilterByName, setFilterFromJSON, copyFilterURL } = (data as ModalDataFilterList);
    const { commandKeyStr, testCtrlCmd } = usePlatform();

    const [hasCtrlHeld, setHasCtrlHeld] = useState(false);

    const KEYBOARD_SHORTCUT_INDEX_MAX = 9;

    // Keyboard shortcuts for opening filters 1-9 with Ctrl+1 ... Ctrl+9
    useEffect(() => {

        const handleKeyDown = (event: KeyboardEvent) => {

            // Ctrl key not held / key is being repeated, exit
            if (!testCtrlCmd(event) || event.repeat)
                return;

            // Ctrl key held, set flag
            setHasCtrlHeld(true);

            const key = event.key;
            const index = parseInt(key) - 1;

            if (!(index >= 0 && index < filters.length)) 
                return;

            log("Applying filter via keyboard shortcut: ", filters[index]);

            try {
                setFilterFromJSON(filters[index].filter);
                close();
            } catch (error) {
                const errorCode = `${error}\n\n${JSON.stringify(filters[index])}`
                setModal(ErrorModal, { title: "Invalid Filter", message: `The selected filter is invalid and cannot be applied.`, code: errorCode });
            }

        };

        const handleKeyUp = (event: KeyboardEvent) => {

            // Ctrl key released, clear flag
            if (!event.ctrlKey)
                setHasCtrlHeld(false);
            
        }

        window.addEventListener('keydown', handleKeyDown);
        window.addEventListener('keyup', handleKeyUp);
        return () => {
            window.removeEventListener('keydown', handleKeyDown);
            window.removeEventListener('keyup', handleKeyUp);
        };

    }, [filters, setFilterFromJSON, close, setModal]);


    const renderFilterViewRow = (filter: FlightFilter, index: number) => {

        const applyFilter = () => {

            log("Applying filter: ", filter);

            try {

                setFilterFromJSON(filter.filter);
                close();

            } catch (error) {

                const errorCode = `${error}\n\n${JSON.stringify(filter)}`
                setModal(ErrorModal, { title: "Invalid Filter", message: `The selected filter is invalid and cannot be applied.`, code: errorCode });

            }

        }

        const tooltipKeyboardAction = (index <= (KEYBOARD_SHORTCUT_INDEX_MAX-1))
            ? `${commandKeyStr}+${index + 1}`
            : undefined;

        return <div key={index} className="flex flex-row items-center p-2 border-b last:border-b-0 gap-1 hover:bg-background">

            {/* Filter Color Box */}
            <div className="w-6 h-6 aspect-square rounded mr-3" style={{ backgroundColor: filter.color }}></div>

            {/* Filter Name */}
            <div className="font-medium truncate select-all">{filter.name}</div>

            {/* Filter Delete Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button
                        variant="ghostDestructive"
                        className="aspect-square ml-auto"
                        onClick={() => setModal(ConfirmModal, {
                            title: "Delete Filter",
                            message: `Are you sure you want to delete the filter "${filter.name}"? This action cannot be undone.`,
                            onConfirm: async () => {
                                await deleteFilterByName(filter.name);
                            }
                        })}
                    >
                        <Trash size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Delete Filter
                </TooltipContent>
            </Tooltip>

            {/* Copy Filter URL Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button
                        variant="ghost"
                        className="aspect-square"
                        onClick={() => copyFilterURL(JSON.parse(filter.filter))}
                    >
                        <ClipboardCopy size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Copy Filter URL
                </TooltipContent>
            </Tooltip>

            {/* Filter Edit Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button variant="ghost" className="aspect-square" onClick={() => setModal(FilterEditModal, {saveFilter, colorIn: filter.color, nameIn: filter.name})}>
                        <Pencil size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Edit Filter
                </TooltipContent>
            </Tooltip>

            {/* Filter Apply Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button variant="ghost" className="aspect-square" onClick={applyFilter}>
                        {
                            (hasCtrlHeld && index <= (KEYBOARD_SHORTCUT_INDEX_MAX-1))
                            ?
                            <kbd className="font-sans text-base">{index + 1}</kbd>
                            :
                            <Check size={16} />
                        }
                    </Button>
                </TooltipTrigger>
                <TooltipContent keyboardAction={tooltipKeyboardAction}>
                    Apply Filter
                </TooltipContent>
            </Tooltip>

        </div>

    }


    log.table("Rendering filters list: ", filters);
    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                <CardHeader className="grid gap-2">

                    <div className="grid gap-2">
                        <CardTitle>Loading Filter</CardTitle>
                        <CardDescription>
                            Select a saved filter to overwrite the current filter rules.
                        </CardDescription>
                    </div>

                    <CardAction>
                        <Button variant="link" onClick={close}>
                            <X/>
                        </Button>
                    </CardAction>

                </CardHeader>
                <CardContent className="space-y-8">

                    {/* Overwrite Warning */}
                    <Alert variant="destructive">
                        <AlertCircleIcon size={16} />
                        <AlertDescription className="mt-1">
                            Any current filter rules will be lost when a saved filter is loaded.
                        </AlertDescription>
                    </Alert>

                    {/* Filters List */}
                    {
                        (filters.length === 0)
                        ?
                        <div className="text-center text-muted-foreground">
                            No saved filters found.
                        </div>
                        :
                        <div className="max-h-128 overflow-y-auto border rounded">
                            {filters.map((filter, index) => renderFilterViewRow(filter, index))}
                        </div>
                    }

                </CardContent>
            </Card>
        </motion.div>
    );
}