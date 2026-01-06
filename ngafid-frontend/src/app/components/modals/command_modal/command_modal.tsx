// ngafid-frontend/src/app/components/modals/command_modal/command_modal.tsx
import { Card, CardContent } from "@/components/ui/card";
import { motion } from "motion/react";

import CommandModalContent from "@/components/modals/command_modal/command_modal_content";
import { useModal } from '@/components/modals/modal_context';
import { ModalData, ModalProps } from '@/components/modals/types';
import { getLogger } from "@/components/providers/logger";


const log = getLogger("CommandModal", "black", "Modal");


export type ModalDataCommand = ModalData & {
    inputRef: React.RefObject<HTMLInputElement>;
};

export default function CommandModal({ data }: ModalProps) {

    const { close, renderModalHeader } = useModal();

    log("Rendering...");

    const { inputRef } = (data as ModalDataCommand) ?? {};

    const submitCommand = (command: any) => {

        // Submit the command
        command();

        // Close the modal
        close();
    
    }

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
  
                {renderModalHeader("Action Menu", "Perform various actions quickly.")}
                <CardContent>
                    <CommandModalContent submitCommand={submitCommand} inputRef={inputRef} />
                </CardContent>

            </Card>
        </motion.div>

        
    );
}