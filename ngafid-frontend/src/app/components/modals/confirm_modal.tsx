// ngafid-frontend/src/app/components/modals/confirm_modal.tsx
import React, { use } from "react";
import { Card, CardContent, CardHeader, CardDescription, CardFooter, CardTitle, CardAction } from "@/components/ui/card"
import { Button, ButtonVariant } from '@/components/ui/button';
import { motion } from "motion/react";

import '@/index.css';
import { X } from 'lucide-react';
import type { ModalData, ModalProps } from "./types";
import { useModal } from './modal_provider';

export type ModalDataError = ModalData & {
    title: string;
    message: string;
    onConfirm?: () => void;
    buttonVariant?: ButtonVariant;
};

export default function ConfirmModal({ data }: ModalProps) {

    const { close } = useModal();
    const { title, message, onConfirm, buttonVariant } = (data as ModalDataError);

    const CONFIRM_BUTTON_VARIANT_DEFAULT: ButtonVariant = "destructive";

    console.log("Rendering ConfirmModal with title:", title, "and message:", message);

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
                        <CardTitle>Confirm</CardTitle>
                        <CardDescription>
                            <div>
                                {title as string}
                            </div>
                        </CardDescription>
                    </div>

                    <CardAction>
                        <Button variant="link" onClick={close}>
                            <X />
                        </Button>
                    </CardAction>

                </CardHeader>
                <CardContent>
                    <div>
                        {
                            (message && message.length > 0)
                                ?
                                <p>{message as string}</p>
                                :
                                <p>An unknown operation is in progress, and cannot be confirmed. Please try again later.</p>
                        }
                    </div>
                </CardContent>

                <CardFooter className="flex justify-end space-x-2">
                    <Button variant="outline" onClick={close}>Cancel</Button>
                    <Button
                        variant={buttonVariant ?? CONFIRM_BUTTON_VARIANT_DEFAULT}
                        onClick={() => {
                            onConfirm?.();
                            close();
                        }}
                    >
                        Confirm
                    </Button>
                </CardFooter>
            </Card>
        </motion.div>
    );
}