// ngafid-frontend/src/app/components/modals/reset_password_modal.tsx

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { motion } from "motion/react";
import React, { useCallback } from "react";

import { getLogger } from "@/components/providers/logger";
import { openRoute } from '@/lib/route_utils';
import { AlertCircleIcon, Loader2Icon } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '../ui/alert';
import type { ModalData, ModalProps } from "./types";
import { useModal } from '@/components/modals/modal_context';
import { Separator } from '@/components/ui/separator';
import { validateConfirmedPassword } from '@/lib/password_validation';

const log = getLogger("ResetPasswordModal", "black", "Modal");


export type ModalDataResetPassword = ModalData & {
    resetPhrase: string;
};

type ResetPasswordResponse = {
    loggedOut?: boolean;
    waiting?: boolean;
    denied?: boolean;
    loggedIn?: boolean;
    message?: string;
    errorTitle?: string;
    errorMessage?: string;
};

export default function ResetPasswordModal({ data }: ModalProps<ModalDataResetPassword>) {

    const { renderModalHeader } = useModal();
    const { resetPhrase } = (data as ModalDataResetPassword) ?? {};

    const [email, setEmail] = React.useState("");
    const [errorMessage, setErrorMessage] = React.useState("");
    const [isLoading, setIsLoading] = React.useState(false);
    const [newPassword, setNewPassword] = React.useState("");
    const [confirmPassword, setConfirmPassword] = React.useState("");

    const emailIsValid = useCallback(() => {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line no-useless-escape

        const isValidEmail = re.test(String(email).toLowerCase()); 
        return isValidEmail;
    }, [email]);

    const submitResetPassword = (event?: React.FormEvent) => {

        event?.preventDefault();
        
        log("Attempting to submit reset password request....");

        setIsLoading(true);
        setErrorMessage("");

        try {
            if (!resetPhrase)
                throw new Error("This password reset link is missing its reset token.");

            if (email.trim().length === 0)
                throw new Error("Email is required.");

            if (!emailIsValid())
                throw new Error("Please enter a valid email address.");

            const passwordValidation = validateConfirmedPassword(newPassword, confirmPassword, "New password", "Confirmation password");
            if (!passwordValidation.valid)
                throw new Error(passwordValidation.message);

            const form = new URLSearchParams({
                emailAddress: email,
                passphrase: resetPhrase,
                newPassword,
                confirmPassword,
            });

            fetch("/api/auth/reset-password", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                },
                body: form,
            })
            .then(response => response.json())
            .then((data: ResetPasswordResponse) => {

                setNewPassword("");
                setConfirmPassword("");

                if (data.errorTitle) {
                    setErrorMessage(data.errorMessage || data.errorTitle);
                    return;
                }

                if (data.loggedOut) {
                    setErrorMessage(data.message || "Could not reset password.");
                    return;
                }

                if (!data.loggedIn) {
                    setErrorMessage(data.message || "Could not reset password.");
                    return;
                }

                log("Password reset successfully.");

                if (data.waiting || data.denied)
                    openRoute("waiting", true);
                else
                    openRoute("summary", true);

            })
            .catch((error) => {
                log.error("An error occurred while submitting reset password request:", error);
                setErrorMessage("An error occurred while resetting your password. Please try again later.");
            })
            .finally(() => {
                setIsLoading(false);
            });

        } catch (error) {
            if (error instanceof Error)
                setErrorMessage(error.message);
            else
                setErrorMessage("An unknown error occurred.");

            setIsLoading(false);
        }
    };

    const submitDisabled = (
        isLoading
        || email.trim().length === 0
        || !emailIsValid()
        || newPassword.trim().length === 0
        || confirmPassword.trim().length === 0
    );

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">

                {renderModalHeader(`Reset Password`, 'Enter your email and new password below.')}

                {/* Email & New Password Inputs */}
                <CardContent>

                    <form id="reset-password-form" onSubmit={submitResetPassword}>
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">
                            <Label className="md:w-40" htmlFor="reset-email">Email Address</Label>
                            <Input
                                id="reset-email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="flex-1"
                                autoComplete="email"
                                data-modal-initial-focus
                                required
                            />
                        </div>
                        <Separator className="mt-4" />

                        <div className="flex flex-col gap-2 md:flex-row md:items-center mt-4">
                            <Label className="md:w-40" htmlFor="reset-password">New Password</Label>
                            <Input
                                id="reset-password"
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                className="flex-1"
                                autoComplete="new-password"
                                required
                            />
                        </div>
                        <div className="flex flex-col gap-2 md:flex-row md:items-center mt-4">
                            <Label className="md:w-40" htmlFor="reset-confirm-password">Confirm New Password</Label>
                            <Input
                                id="reset-confirm-password"
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                className="flex-1"
                                autoComplete="new-password"
                                required
                            />
                        </div>
                        
                        {
                            errorMessage && (
                                <Alert variant="destructive" className="mt-4">
                                    <AlertCircleIcon size={16} />
                                    <AlertTitle>Password reset failed.</AlertTitle>
                                    <AlertDescription>{errorMessage}</AlertDescription>
                                </Alert>
                            )
                        }
                    </form>
                </CardContent>

                {/* Submit Button */}
                <CardFooter>
                    {
                        isLoading
                        ?
                        <Button className="ml-auto" disabled>
                            <Loader2Icon className="animate-spin" />
                            Please wait...
                        </Button>
                        :
                        <Button className="ml-auto" type="submit" form="reset-password-form" disabled={submitDisabled}>
                            Reset Password
                        </Button>
                    }
                </CardFooter>

            </Card>
        </motion.div>
    );

}
