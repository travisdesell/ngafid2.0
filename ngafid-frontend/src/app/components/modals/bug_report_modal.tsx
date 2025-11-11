// ngafid-frontend/src/app/components/modals/bug_report_modal.tsx
import { AlertCircleIcon, Mail, X } from "lucide-react";
import { motion } from "motion/react";
import { useState } from "react";

import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { NGAFIDUser } from "src/types";
import { Alert, AlertDescription, AlertTitle } from "../ui/alert";
import { Field, FieldLabel } from "../ui/field";
import { Input } from "../ui/input";
import { Textarea } from "../ui/textarea";
import ErrorModal from "./error_modal";
import { useModal } from "./modal_provider";
import SuccessModal from "./success_modal";
import type { ModalData, ModalProps } from "./types";

const log = getLogger("BugReportModal", "black", "Modal");

export type BugReportModalData = ModalData & {
    user: NGAFIDUser;
    titleIn?: string;
    descriptionIn?: string;
};

type Props = ModalProps<BugReportModalData>;

const BUG_REPORT_ENDPOINT = "/api/bug";
const BUG_REPORT_EMAIL_UNKNOWN = "(Unknown Email!)";

export default function BugReportModal({ data }: Props) {

    /*
        NOTE:

        Need to pass the 'user' data via props
        since the Modal Provider lives above
        the Auth Provider.
    */

    const { close, setModal } = useModal();

    const { titleIn, descriptionIn } = data ?? {};

    const userEmail = (data?.user?.email ?? undefined);
    const [title, setTitle] = useState(titleIn ?? "");
    const [description, setDescription] = useState(descriptionIn ?? "");
    const [includeEmail, setIncludeEmail] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);


    log(`Rendering with title: '%c${title}%c' and description: '%c${description}%c'`, "color: aqua;", "", "color: aqua;", "");


    async function handleSubmit() {

        log("Bug Report Modal - Submitting bug report...");

        setErrorMessage(null);

        const trimmedTitle = title.trim();
        const trimmedDescription = description.trim();

        // Missing title or description, show error
        if (!trimmedTitle || !trimmedDescription) {
            setErrorMessage("Please ensure the title and description fields are filled out.");
            return;
        }

        // Flag as submitting
        setSubmitting(true);

        try {

            const payload = {
                title: trimmedTitle,
                body: trimmedDescription,
                senderEmail: userEmail,
                includeEmail,
            };

            const response = await fetch(BUG_REPORT_ENDPOINT, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload),
            });

            // Response not OK, display error modal
            if (!response.ok) {

                const responseErrorText = await response.text().catch(() => "");
                setModal(
                    ErrorModal, {
                        title: "Error Submitting Bug Report",
                        message: responseErrorText || `Server responded with ${response.status}`,
                    }
                );

            }

            // Log success
            const sendEnd = new Date();
            log(`Bug report submitted successfully! (${sendEnd.toLocaleString()})`);

            // Display success modal
            setModal(SuccessModal, {
                title: "Bug Report Submitted",
                message: "Your bug report has been submitted successfully. Thank you for your feedback!",
            });

        } catch (error: unknown) {

            // Got unknown error submitting, update error message
            const sendEnd = new Date();
            log.warn(`Error submitting bug report. (${sendEnd.toLocaleString()})`);
            setErrorMessage(error instanceof Error ? error.message : "Unknown error submitting bug report.");

        } finally {

            // Clear submitting flag
            setSubmitting(false);

        }

    }

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-2xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                <CardHeader className="grid gap-2">
                    <div className="grid gap-2">
                        <CardTitle>Submit a Bug Report</CardTitle>
                        <CardDescription>Provide a brief title and a detailed description.</CardDescription>
                    </div>

                    <CardAction>
                        <Button variant="link" onClick={close} aria-label="Close bug report modal">
                            <X />
                        </Button>
                    </CardAction>
                </CardHeader>

                <CardContent className="space-y-4">

                    {/* Title Field */}
                    <Field  className="grid gap-2">
                        <FieldLabel htmlFor="bug-title" className="text-sm font-medium">
                            Bug Title
                        </FieldLabel>
                        <Input
                            id="bug-title"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="Short summary of the issue (required)"
                            className="w-full rounded-md border border-border bg-background p-2 outline-none focus:ring-1 focus:ring-ring"
                            disabled={submitting}
                        />
                    </Field>

                    {/* Description Field */}
                    <Field className="grid gap-2">
                        <FieldLabel htmlFor="bug-description" className="text-sm font-medium">
                            Bug Description
                        </FieldLabel>
                        <Textarea
                            id="bug-description"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="Steps to reproduce, expected vs actual behavior, links to screenshots, etc. (required)"
                            rows={6}
                            className="w-full resize-y rounded-md border border-border bg-background p-2 outline-none focus:ring-1 focus:ring-ring"
                            disabled={submitting}
                        />
                    </Field>

                    {/* Include Email Checkbox */}
                    <Field className="flex! flex-row!  w-full! items-center justify-between gap-4">
                        <FieldLabel htmlFor="bug-include-email" className="flex items-center gap-3 select-none cursor-pointer">
                            <span className="inline-flex h-8 w-8 items-center justify-center rounded-full border">
                                <Mail className="h-4 w-4" />
                            </span>
                            <div className="leading-tight">
                                <div className="font-semibold">Send BCC With Report?</div>
                                <div className="text-sm opacity-80">
                                    {
                                        (includeEmail)
                                        ? (userEmail ?? BUG_REPORT_EMAIL_UNKNOWN)
                                        : "Email will not be included"
                                    }
                                </div>
                            </div>
                        </FieldLabel>

                        <Input
                            id="bug-include-email"
                            type="checkbox"
                            className="h-6 w-fit! aspect-square"
                            checked={includeEmail}
                            onChange={(e) => setIncludeEmail(e.target.checked)}
                            disabled={submitting}
                        />
                    </Field>

                    {/* Tips Section */}
                    <div className="text-xs italic opacity-80">
                        Tips: Provide a concise title and detailed steps. You can include logs or links. Submission should complete quickly; if servers are busy, it may take a moment.
                    </div>

                    {
                        (errorMessage)
                        &&
                        <Alert variant="destructive">
                            <AlertCircleIcon size={16} />
                            <AlertTitle>Error submitting report.</AlertTitle>
                            <AlertDescription>
                                {errorMessage}
                            </AlertDescription>
                        </Alert>
                    }
                </CardContent>


                <CardFooter className="flex justify-end gap-2">

                    {/* Cancel Button */}
                    <Button variant="outline" onClick={close} disabled={submitting}>
                        Cancel
                    </Button>

                    {/* Submit Button */}
                    <Button onClick={handleSubmit} disabled={submitting}>
                        {
                            (submitting)
                            ?
                            <span className="inline-flex items-center gap-2">
                                <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-b-transparent" />
                                Submitting…
                            </span>
                            :
                            "Submit Bug Report"
                        }
                    </Button>
                </CardFooter>

                
            </Card>
        </motion.div>
    );
}
