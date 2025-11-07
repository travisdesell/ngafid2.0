// ngafid-frontend/src/app/components/modals/error_modal.tsx
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { motion } from "motion/react";

import BugReportModal from '@/components/modals/bug_report_modal';
import { useAuth } from '@/components/providers/auth_provider';
import { getLogger } from "@/components/providers/logger";
import '@/index.css';
import { X } from 'lucide-react';
import { useModal } from './modal_provider';
import type { ModalData, ModalProps } from "./types";


const log = getLogger("ErrorModal", "black", "Modal");


export type ModalDataError = ModalData & {
    title: string;
    message: string;
    code?: string;
    allowReport?: boolean;
};

export default function ErrorModal({ data }: ModalProps) {

    const { close, setModal } = useModal();
    const { title, message, code, allowReport=true } = (data as ModalDataError) ?? {};
    const { user } = useAuth();

    // No 'code' provided, log without it
    if (!code)
        log.error(`Rendering with title: '%c${title}%c' and message: '%c${message}%c'`, "color: aqua;", "", "color: aqua;", "");

    // 'code' provided, include it in log
    else 
        log.error(`Rendering with title: '%c${title}%c', message: '%c${message}%c' and code: '%c${code}%c'`, "color: aqua;", "", "color: aqua;", "", "color: aquamarine;", "");


    const openBugReportFromError = () => {

        let reportDescription = message;
        if (code)
            reportDescription += `\n\n${code}`;

        log("Opening Bug Report Modal...");

        setModal(BugReportModal, { user: user!, titleIn: title, descriptionIn: reportDescription });
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
                        <CardTitle>Error</CardTitle>
                        <CardDescription>
                            <div>
                                {title as string}
                            </div>
                        </CardDescription>
                    </div>

                    <CardAction>
                        <Button variant="link" onClick={close}>
                            <X/>
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
                            <p>An unknown error occurred. Please try again later.</p>
                        }
                    </div>
                </CardContent>

                <CardFooter className="flex flex-col gap-4">

                    {/* Code Section */}
                    {
                        (code && code.length > 0)
                        &&
                        <pre className="bg-background border-1 p-4 rounded-md overflow-x-auto select-all w-full">
                            <code>{code as string}</code>
                        </pre>
                    }

                    {/* Open Bug Report Modal */}
                    {
                        (allowReport)
                        &&
                        <Button
                            variant="link"
                            onClick={openBugReportFromError}
                            className="ml-auto inline-block text-sm underline-offset-4 hover:underline"
                        >
                            Report this issue
                        </Button>
                    }

                </CardFooter>    

            </Card>
        </motion.div>
    );
}