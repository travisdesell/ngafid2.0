// ngafid-frontend/src/app/components/modals/login_modal.tsx
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { motion } from "motion/react";
import React, { useCallback } from "react";

import ForgotPasswordModal from "@/components/modals/forgot_password_modal";
import { getLogger } from "@/components/providers/logger";
import { openRoute } from '@/lib/route_utils';
import { AlertCircleIcon, Loader2Icon, X } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '../ui/alert';
import ErrorModal, { ModalDataError } from './error_modal';
import type { ModalProps } from "./types";
import { Separator } from '@/components/ui/separator';

const log = getLogger("LoginModal", "black", "Modal");

interface LoginResponse {
    loggedOut?: boolean;
    waiting?: boolean;
    denied?: boolean;
    loggedIn?: boolean;
    message?: string;
    errorTitle?: string;
    errorMessage?: string;
}

type TwoFactorMode = "authenticator" | "backup";

export default function LoginModal({ setModal }: ModalProps) {

    const close = () => setModal(undefined);

    const [email, setEmail] = React.useState("");
    const [password, setPassword] = React.useState("");
    const [requires2FA, setRequires2FA] = React.useState(false);
    const [twoFactorCode, setTwoFactorCode] = React.useState("");
    const [twoFactorMode, setTwoFactorMode] = React.useState<TwoFactorMode>("authenticator");
    const [errorMessage, setErrorMessage] = React.useState("");
    const [isLoading, setIsLoading] = React.useState(false);
    const twoFactorInputRef = React.useRef<HTMLInputElement>(null);


    const emailIsValid = useCallback(() => {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line no-useless-escape

        const isValidEmail = re.test(String(email).toLowerCase()); 
        return isValidEmail;
    }, [email]);

    React.useEffect(() => {
        if (requires2FA)
            twoFactorInputRef.current?.focus();
    }, [requires2FA]);
    


    const submitLogin = (event?: React.FormEvent) => {

        event?.preventDefault();

        log("Attempting to submit login....");

        //Flag as loading
        setIsLoading(true);
        setErrorMessage("");

        //Email is empty, exit
        const emailEmpty = (email.trim().length === 0);
        if (emailEmpty) {
            log.error("Preventing login submission - Email is required.");
            setIsLoading(false);
            setErrorMessage("Preventing login submission - Email is required.");
            return false;
        }

        //Password is empty, exit
        const passwordEmpty = (password.trim().length === 0);
        if (passwordEmpty) {
            log.error("Preventing login submission - Password is required.");
            setIsLoading(false);
            setErrorMessage("Preventing login submission - Password is required.");
            return false;
        }

        if (requires2FA) {
            const expectedCodeLength = (twoFactorMode === "backup") ? 8 : 6;
            if (twoFactorCode.trim().length !== expectedCodeLength) {
                setIsLoading(false);
                setErrorMessage(`Please enter a valid ${expectedCodeLength}-digit verification code.`);
                return false;
            }
        }

        const form = new URLSearchParams({
            'email': email,
            'password': password
        });

        if (requires2FA) {
            if (twoFactorMode === "backup")
                form.set("backupCode", twoFactorCode);
            else
                form.set("totpCode", twoFactorCode);
        }


        fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: form,
        })
        .then(response => response.json())
        .then((data: LoginResponse) => {

            //Logged out, show error message
            if (data.loggedOut) {
                setErrorMessage(data.message || "You have been logged out.");
                setTwoFactorCode("");
                return false;
            }

            if (data.message === "2FA_CODE_REQUIRED") {
                setRequires2FA(true);
                setTwoFactorCode("");
                return false;
            }

            if (data.message === "2FA_SETUP_REQUIRED") {
                setErrorMessage("Two-factor authentication setup is incomplete. Please finish setup from Account Security.");
                return false;
            }

            //Got an error, show error modal
            if (data.errorTitle) {

                setModal(ErrorModal,
                    {
                        title : data.errorTitle,
                        message:data.errorMessage ?? "An unknown error occurred during login."
                    } as ModalDataError
                );

                return false;

            }

            //Successful login
            log("Login successful!");

            // Clear password field in UI state
            setPassword("");
            setTwoFactorCode("");

            // Waiting or denied, go to Waiting page
            if (data.waiting || data.denied)
                openRoute("waiting", true);
            
            // Otherwise, go to Summary page
            else
                openRoute("summary", true);

        })
        .catch((error) => {
            setModal(ErrorModal, {title : "Error during login submission", message: error.toString()} as ModalDataError);
        })
        .finally(() => {

            //Reset loading state
            setIsLoading(false);

        });
    };

    const expectedTwoFactorCodeLength = (twoFactorMode === "backup") ? 8 : 6;
    const submitDisabled = (
        email.trim().length === 0
        || password.trim().length === 0
        || !emailIsValid()
        || isLoading
        || (requires2FA && twoFactorCode.trim().length !== expectedTwoFactorCodeLength)
    );

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
                        <CardTitle>Login to your account</CardTitle>
                        <CardDescription>
                            {
                                requires2FA
                                ? "Enter your verification code to finish logging in."
                                : "Enter your email and password below to login to your account"
                            }
                        </CardDescription>
                    </div>

                    <CardAction>
                        <Button variant="link" onClick={close} disabled={isLoading}>
                            <X/>
                        </Button>
                    </CardAction>

                </CardHeader>
                <CardContent>
                    <form id="login-form" onSubmit={submitLogin}>
                        <div className="flex flex-col gap-6">

                            {/* Error Message */}
                            {
                                (errorMessage)
                                &&
                                <Alert variant="destructive">
                                    <AlertCircleIcon size={16} />
                                    <AlertTitle>Error logging in.</AlertTitle>
                                    <AlertDescription>
                                        {errorMessage}
                                    </AlertDescription>
                                </Alert>
                            }

                            {/* Email & PW Fields */}
                            <div className="grid gap-2">
                                <Label htmlFor="email">Email</Label>
                                <Input
                                    id="email"
                                    type="email"
                                    placeholder="name@example.com"
                                    required
                                    onChange={(e) => setEmail(e.target.value)}
                                    value={email}
                                    disabled={requires2FA || isLoading}
                                />
                            </div>
                            <div className="grid gap-2">
                                <div className="flex items-center">
                                    <Label htmlFor="password">Password</Label>
                                    <Button
                                        variant="link"
                                        type="button"
                                        onClick={() => setModal(ForgotPasswordModal)}
                                        className="ml-auto inline-block text-sm underline-offset-4 hover:underline"
                                        disabled={requires2FA || isLoading}
                                    >
                                        Forgot your password?
                                    </Button>
                                </div>
                                <Input
                                    id="password"
                                    type="password"
                                    required
                                    onChange={(e) => setPassword(e.target.value)}
                                    value={password}
                                    disabled={requires2FA || isLoading}
                                />
                            </div>

                            {/* 2FA Fields */}
                            {
                                (requires2FA)
                                &&
                                <div className="grid gap-2">
                                    <Separator />
                                    <div className="flex items-center">
                                        <Label htmlFor="two-factor-code">
                                            {twoFactorMode === "backup" ? "Backup Code" : "Authenticator Code"}
                                        </Label>
                                        <Button
                                            variant="link"
                                            type="button"
                                            onClick={() => {
                                                setTwoFactorMode(twoFactorMode === "backup" ? "authenticator" : "backup");
                                                setTwoFactorCode("");
                                                setErrorMessage("");
                                            }}
                                            className="ml-auto inline-block text-sm underline-offset-4 hover:underline"
                                            disabled={isLoading}
                                        >
                                            {twoFactorMode === "backup" ? "Use authenticator code" : "Use backup code"}
                                        </Button>
                                    </div>
                                    <Input
                                        ref={twoFactorInputRef}
                                        id="two-factor-code"
                                        type="text"
                                        inputMode="numeric"
                                        autoComplete="one-time-code"
                                        placeholder={twoFactorMode === "backup" ? "8-digit backup code (one-time use!)" : "6-digit code"}
                                        value={twoFactorCode}
                                        maxLength={expectedTwoFactorCodeLength}
                                        onChange={(e) => setTwoFactorCode(e.target.value.replace(/\D/g, ""))}
                                        disabled={isLoading}
                                        data-modal-initial-focus
                                    />
                                </div>
                            }
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
                        <Button type="submit" form="login-form" className="w-full" disabled={submitDisabled}>
                            {requires2FA ? "Login With Code" : "Login"}
                        </Button>
                    }
                </CardFooter>
            </Card>
        </motion.div>
    );
}
