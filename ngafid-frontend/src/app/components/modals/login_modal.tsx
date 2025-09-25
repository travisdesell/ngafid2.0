// ngafid-frontend/src/app/components/modals/login_modal.tsx
import React from "react";
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


export default function LoginModal({ setModal }: ModalProps) {

    const close = () => setModal(undefined);

    const [email, setEmail] = React.useState("");
    const [password, setPassword] = React.useState("");
    const [errorMessage, setErrorMessage] = React.useState("");
    const [isLoading, setIsLoading] = React.useState(false);


    const validateEmail = () => {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line no-useless-escape

        const isValidEmail = re.test(String(email).toLowerCase());  
    };

    const validatePassword = () => {
        const PASSWORD_LENGTH_MIN = 8;
        const isValidPassword = (password.length >= PASSWORD_LENGTH_MIN);


    };

    const submitLogin = () => {

        console.log("Attempting to submit login....");

        //Flag as loading
        setIsLoading(true);

        //Email is empty, exit
        const emailEmpty = (email.trim().length === 0);
        if (emailEmpty) {
            console.error("Preventing login submission - Email is required.");
            setIsLoading(false);
            setErrorMessage("Preventing login submission - Email is required.");
            return false;
        }

        //Password is empty, exit
        const passwordEmpty = (password.trim().length === 0);
        if (passwordEmpty) {
            console.error("Preventing login submission - Email is required.");
            setIsLoading(false);
            setErrorMessage("Preventing login submission - Password is required.");
            return false;
        }

        const form = new URLSearchParams({
            'email': email,
            'password': password
        });


        fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: form,
        })
        .then(response => response.json())
        .then(data => {

            //Clear password field in UI state
            setPassword("");

            //Logged out, show error message
            if (data.loggedOut) {
                setErrorMessage(data.message || "You have been logged out.");
                return false;
            }

            //Got an error, show error modal
            if (data.errorTitle) {
                setModal(ErrorModal, {title : data.errorTitle, message:data.errorMessage});
                return false;
            }

            //Successful login
            console.log("Login successful!");

            //Waiting or denied, go to Waiting page
            if (data.waiting || data.denied)
                openRoute("waiting", true);
            
            //Otherwise, go to Summary page
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
    

    const submitDisabled = (email.trim().length === 0 || password.trim().length === 0);

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
                            Enter your email and password below to login to your account
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
                                errorMessage
                                &&
                                // <div className="p-4 bg-red-100 text-red-700 border border-red-300 rounded">
                                //     {errorMessage}
                                // </div>
                                <Alert variant="destructive">
                                    <AlertCircleIcon size={16} />
                                    <AlertTitle>Error logging in.</AlertTitle>
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
                            <div className="grid gap-2">
                                <div className="flex items-center">
                                    <Label htmlFor="password">Password</Label>
                                    <a
                                        href="#"
                                        className="ml-auto inline-block text-sm underline-offset-4 hover:underline"
                                    >
                                        Forgot your password?
                                    </a>
                                </div>
                                <Input
                                    id="password"
                                    type="password"
                                    required
                                    onChange={(e) => setPassword(e.target.value)}
                                    value={password}
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
                        <Button type="submit" className="w-full" disabled={submitDisabled} onClick={submitLogin}>
                            Login
                        </Button>
                    }
                </CardFooter>
            </Card>
        </motion.div>
    );
}