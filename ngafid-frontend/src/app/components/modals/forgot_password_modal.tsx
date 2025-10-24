// ngafid-frontend/src/app/components/modals/forgot_password_modal.tsx
import React, { useCallback } from "react";
import { Card, CardContent, CardHeader, CardDescription, CardFooter, CardTitle, CardAction } from "@/components/ui/card"
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { motion } from "motion/react";

import '@/index.css';
import { AlertCircleIcon, Loader2Icon, X } from 'lucide-react';
import type { ModalProps } from "./types";
import ErrorModal, { ModalDataError } from './error_modal';
import RegisterModal from './register_modal';
import { Alert, AlertDescription, AlertTitle } from '../ui/alert';
import { openRoute } from '@/main';
import { fetchJson } from "@/fetchJson";
import SuccessModal from "@/components/modals/success_modal";


export default function ForgotPasswordModal({ setModal }: ModalProps) {

    const close = () => setModal(undefined);

    const [email, setEmail] = React.useState("");
    const [errorMessage, setErrorMessage] = React.useState("");
    const [isLoading, setIsLoading] = React.useState(false);


    const emailIsValid = useCallback(() => {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line no-useless-escape

        const isValidEmail = re.test(String(email).toLowerCase()); 
        return isValidEmail;
    }, [email]);
    

    const submitPasswordReset = () => {

        console.log("Forgot Password Modal - Attempting to submit password reset...");

        const submissionData = {
            email: email,
        };

        fetchJson.post('/api/auth/forgot-password', submissionData)
            .then((response) => {
                setModal(SuccessModal, { title: "Password Reset Email Sent", message: "If an account with that email exists, a password reset email has been sent." });
            })
            .catch((error) => {
                setModal(ErrorModal, { title: "Error sending password reset email", message: `Error sending password reset email: ${error.message}` });
            });
            
    }

    const submitDisabled = (email.trim().length === 0 || !emailIsValid());

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
                        <CardTitle>Reset your password</CardTitle>
                        <CardDescription>
                            Enter your email below to receive a password reset link
                        </CardDescription>
                    </div>

                    <CardAction>
                        <Button variant="link" onClick={close} disabled={isLoading}>
                            <X/>
                        </Button>
                    </CardAction>

                </CardHeader>
                <CardContent>
                    <form>
                        <div className="flex flex-col gap-6">
                            {
                                (errorMessage)
                                &&
                                <Alert variant="destructive">
                                    <AlertCircleIcon size={16} />
                                    <AlertTitle>Error sending password reset email.</AlertTitle>
                                    <AlertDescription>
                                        {errorMessage}
                                    </AlertDescription>
                                </Alert>
                            }
                            <div className="grid gap-2">
                                <Label htmlFor="email">Email</Label>
                                <Input
                                    id="email"
                                    type="email"
                                    placeholder="name@example.com"
                                    required
                                    onChange={(e) => setEmail(e.target.value)}
                                    value={email}
                                />
                            </div>
                        </div>
                    </form>
                </CardContent>
                <CardFooter className="flex-col gap-2">
                    {
                        isLoading
                        ?
                        <Button className="w-full" disabled>
                            <Loader2Icon className="animate-spin" />
                            Please wait...
                        </Button>
                        :
                        <Button type="submit" className="w-full" disabled={submitDisabled} onClick={submitPasswordReset}>
                            Reset Password
                        </Button>
                    }
                </CardFooter>
            </Card>
        </motion.div>
    );
}