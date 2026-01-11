// ngafid-frontend/src/app/components/modals/filter_edit_modal.tsx
import { Button } from '@/components/ui/button';
import { Card, CardContent } from "@/components/ui/card";
import { motion } from "motion/react";
import { useState } from "react";

import { ColorPicker, randomHexColor } from "@/components/color_picker";
import ErrorModal from '@/components/modals/error_modal';
import { useModal } from "@/components/modals/modal_context";
import type { FlightFilter } from "@/components/providers/flight_filters_provider";
import { getLogger } from "@/components/providers/logger";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Input } from "@/components/ui/input";
import { FilterGroup } from '@/pages/protected/flights/_filters/types';
import { AlertCircleIcon } from 'lucide-react';
import type { ModalData, ModalProps } from "./types";


const log = getLogger("FilterEditModal", "black", "Modal");


export type ModalDataFilterEdit = ModalData & {
    colorIn: string;
    nameIn: string;
    filter: FilterGroup;
    saveFilter: (filter: FlightFilter) => Promise<void>;
};

export default function FilterEditModal({ data }: ModalProps) {

    const { setModal, close, renderModalHeader } = useModal();
    const { colorIn, nameIn, saveFilter, filter } = (data as ModalDataFilterEdit);


    const [colorPickerValue, setColorPickerValue] = useState<string>(colorIn || randomHexColor());
    const [nameInputValue, setNameInputValue] = useState<string>(nameIn || "");

    const allowFilterSave = () => {
        return (nameInputValue.trim().length > 0);
    }

    const renderFilterEditRow = () => {

        return <div className="flex flex-row w-full">

            {/* Filter Color Input */}
            <ColorPicker value={colorPickerValue} onChange={setColorPickerValue} />

            {/* Filter Name Input */}
            <Input
                data-modal-initial-focus
                id="filter-name-input"
                className="ml-4"
                placeholder="Filter Name"
                value={nameInputValue}
                onChange={(e) => setNameInputValue(e.target.value)}
            />

        </div>

    }

    const attemptSaveFilter = () => {

        if (!allowFilterSave()) {
            log.warn("Filter name is empty. Cannot save filter.");
            return;
        }

        log.table("Attempting to save filter:", {
            name: nameInputValue,
            color: colorPickerValue,
            filter: filter,
        });

        saveFilter({
            name: nameInputValue.trim(),
            color: colorPickerValue,
            filter: JSON.stringify(filter),
        }).then(() => {

            log("Filter saved successfully.");
            close();

        }).catch((error) => {

            setModal(ErrorModal, { title: "Error Saving Filter", message: "An error occurred while saving the filter.", code: (error as Error).message });
            
        });

    }

    log("Rendering with incoming color:", colorIn, "and name:", nameIn);
    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                {renderModalHeader("Editing Filter", "Save a new filter or edit an existing saved filter.")}    
                <CardContent className="space-y-8">

                    {/* Overwrite Warning */}
                    <Alert variant="destructive">
                        <AlertCircleIcon size={16} />
                        <AlertDescription className="mt-1">
                            Using a filter name that already exists will overwrite the existing filter.
                        </AlertDescription>
                    </Alert>

                    {/* Filter Edit Row */}
                    {renderFilterEditRow()}

                    {/* Action Buttons */}
                    <div className="flex justify-end space-x-2">

                        <Button variant="outline" onClick={close}>Cancel</Button>
                        <Button
                            variant={"default"}
                            onClick={attemptSaveFilter}
                            disabled={!allowFilterSave()}
                        >
                            Save
                        </Button>
                    </div>

                </CardContent>
            </Card>
        </motion.div>
    );
}