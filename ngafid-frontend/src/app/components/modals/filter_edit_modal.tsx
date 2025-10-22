// ngafid-frontend/src/app/components/modals/filter_edit_modal.tsx
import React, { useState } from "react";
import { Card, CardContent, CardHeader, CardDescription, CardFooter, CardTitle, CardAction } from "@/components/ui/card"
import { Button } from '@/components/ui/button';
import { motion } from "motion/react";

import '@/index.css';
import { AlertCircleIcon, X } from 'lucide-react';
import type { ModalData, ModalProps } from "./types";
import { useModal } from './modal_provider';
import { ColorPicker } from "@/components/color_picker";
import { Input } from "@/components/ui/input";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export type ModalDataFilterEdit = ModalData & {
    colorIn: string;
    nameIn: string;
};

export default function FilterEditModal({ data }: ModalProps) {

    const { close } = useModal();
    const { colorIn, nameIn } = (data as ModalDataFilterEdit);
    console.log("Rendering FilterEditModal with incoming color:", colorIn, "and name:", nameIn);


    const randomHexColor = () => {
        const letters = '0123456789ABCDEF';
        let color = '#';
        for (let i = 0; i < 6; i++) {
            color += letters[Math.floor(Math.random() * 16)];
        }
        return color;
    }

    const [colorPickerValue, setColorPickerValue] = useState<string>(colorIn || randomHexColor());
    const [nameInputValue, setNameInputValue] = useState<string>(nameIn || "");

    const allowFilterSave = () => {
        return nameInputValue.trim().length > 0;
    }

    const renderFilterEditRow = () => {

        return <div className="flex flex-row w-full">

            {/* Filter Color Input */}
            <ColorPicker value={colorPickerValue} onChange={setColorPickerValue} />

            {/* Filter Name Input */}
            <Input className="ml-4" placeholder="Filter Name" value={nameInputValue} onChange={(e) => setNameInputValue(e.target.value)} />

        </div>

    }

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
                        <CardTitle>Editing Filter</CardTitle>
                        <CardDescription>
                            Save a new filter or edit an existing saved filter.
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
                            onClick={() => {
                                // onConfirm?.();
                                close();
                            }}
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