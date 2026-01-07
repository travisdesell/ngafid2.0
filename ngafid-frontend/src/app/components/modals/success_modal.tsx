// ngafid-frontend/src/app/components/modals/success_modal.tsx
import { Card, CardContent } from "@/components/ui/card";
import { motion } from "motion/react";

import { useModal } from '@/components/modals/modal_context';
import { getLogger } from "@/components/providers/logger";
import type { ModalData, ModalProps } from "./types";


const log = getLogger("SuccessModal", "black", "Modal");


export type ModalDataSuccess = ModalData & {
    title: string;
    message: string;
};

export default function SuccessModal({ data }: ModalProps) {

    const { renderModalHeader } = useModal();
    const { title, message } = (data as ModalDataSuccess) ?? {};
    const isUnknownOperation = (!message || message.length === 0);

    log(`Rendering with title: '%c${title}%c' and message: '%c${message}%c'`, "color: aqua;", "", "color: aqua;", "");

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                {renderModalHeader("Success", title as string, true)}
                <CardContent>
                    <div>
                        {
                            (isUnknownOperation)
                            ?
                            <p>An unknown operation was processed successfully.</p>
                            :
                            <p>{message as string}</p>                            
                        }
                    </div>
                </CardContent>
            </Card>
        </motion.div>
    );
}