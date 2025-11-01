// ngafid-frontend/src/app/components/modals/filter_edit_modal.tsx
import React, { useState } from "react";
import { Card, CardContent, CardHeader, CardDescription, CardFooter, CardTitle, CardAction } from "@/components/ui/card"
import { Button } from '@/components/ui/button';
import { motion } from "motion/react";

import '@/index.css';
import { AlertCircleIcon, Pencil, X, Check, Trash } from 'lucide-react';
import type { ModalData, ModalProps } from "./types";
import { useModal } from './modal_provider';
import { ColorPicker } from "@/components/color_picker";
import { Input } from "@/components/ui/input";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import type { FlightFilter } from "@/components/providers/flight_filters_provider";
import FilterEditModal from "@/components/modals/filter_edit_modal";
import ConfirmModal from "@/components/modals/confirm_modal";
import { getLogger } from "@/components/providers/logger";


const log = getLogger("FilterListModal", "black", "Modal");


export type ModalDataFilterList = ModalData & {
    filters: FlightFilter[];
    saveFilter: (filter: FlightFilter) => Promise<void>;
    deleteFilterByName(filterName: string): Promise<void>;
}

export default function FilterListModal({ data }: ModalProps<ModalDataFilterList>) {

    const { close, setModal } = useModal();
    const { filters, saveFilter, deleteFilterByName } = (data as ModalDataFilterList);

    const renderFilterViewRow = (filter: FlightFilter, index: number) => {

        return <div key={index} className="flex flex-row items-center p-2 border-b last:border-b-0 gap-4 hover:bg-background">

            {/* Filter Color Box */}
            <div className="w-6 h-6 rounded" style={{ backgroundColor: filter.color }}></div>

            {/* Filter Name */}
            <div className="font-medium">{filter.name}</div>

            {/* Filter Delete Button */}
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

            {/* Filter Edit Button */}
            <Button variant="ghost" className="aspect-square" onClick={() => setModal(FilterEditModal, {saveFilter, colorIn: filter.color, nameIn: filter.name})}>
                <Pencil size={16} />
            </Button>

            {/* Filter Select Button */}
            <Button variant="ghost" className="aspect-square">
                <Check size={16} />
            </Button>


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

                    {/* Filters List Empty */}
                    {filters.length === 0 && <div className="text-center text-muted-foreground">
                        No saved filters found.
                    </div>}

                    {/* Filter List */}
                    <div className="max-h-128 overflow-y-auto border rounded">
                        {filters.map((filter, index) => renderFilterViewRow(filter, index))}
                    </div>

                </CardContent>
            </Card>
        </motion.div>
    );
}