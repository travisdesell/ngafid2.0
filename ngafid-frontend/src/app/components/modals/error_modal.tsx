// ngafid-frontend/src/app/components/modals/error_modal.tsx
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { motion } from "motion/react";

import BugReportModal from '@/components/modals/bug_report_modal';
import { useModal } from '@/components/modals/modal_context';
import { useAuth } from '@/components/providers/auth_provider';
import { getLogger } from "@/components/providers/logger";
import { X } from 'lucide-react';
import type { ModalData, ModalProps } from "./types";


const log = getLogger("ErrorModal", "black", "Modal");


export type ModalDataError = ModalData & {
    title: string;
    message: string;
    code?: string|object;
    allowReport?: boolean;
};

export default function ErrorModal({ data }: ModalProps<ModalDataError>) {

    const { close, setModal } = useModal();
    const { title, message, code, allowReport=true } = (data as ModalDataError) ?? {};
    const { user } = useAuth();

    let codeString: string|undefined;
    
    // 'Code' is an object, convert to string
    if (code && typeof code !== "string") {

        try {
            codeString = JSON.stringify(code, null, 2);
        } catch (e) {
            codeString = String(code);
        }

    }

    // No 'code' provided, log without it
    if (!code)
        log.error(`Rendering with title: '%c${title}%c' and message: '%c${message}%c'`, "color: aqua;", "", "color: aqua;", "");

    // 'code' provided, include it in log
    else 
        log.error(`Rendering with title: '%c${title}%c', message: '%c${message}%c' and code: '%c${codeString}%c'`, "color: aqua;", "", "color: aqua;", "", "color: aquamarine;", "");


    const openBugReportFromError = () => {

        let reportDescription = message;
        if (codeString)
            reportDescription += `\n\n${codeString}`;

        log("Opening Bug Report Modal...");

        setModal(BugReportModal, { user: user ?? undefined, titleIn: title, descriptionIn: reportDescription });
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
                        (codeString && codeString.length > 0)
                        &&
                        <pre className="bg-background border p-4 rounded-md overflow-x-auto select-all w-full">
                            <code>{codeString}</code>
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