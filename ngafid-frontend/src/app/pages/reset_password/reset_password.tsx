// ngafid-frontend/src/app/pages/reset_password/reset_password.tsx
import ErrorModal, { ModalDataError } from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { useEffect } from "react";
import { setPageTitle } from "@/components/page_title";

export default function PageMissing() {

    setPageTitle("Reset Password");

    const { setModal } = useModal();


    // Pull 'resetPhrase' token from URL query parameters
    const urlParams = new URLSearchParams(window.location.search);
    const resetPhrase = urlParams.get("resetPhrase");

    // No reset phrase found, show an error modal
    if (!resetPhrase) {
        useEffect(() => {

            setModal(
                ErrorModal,
                {allowReport:false, title : "Invalid Password Reset Link", message: "No reset token was found. Please use the link from your email to reset your password."} as ModalDataError,
                () => { window.location.href = "/welcome"; }
            );

        }, []);
        
        return null;
    }


    useEffect(() => {

        setModal(
            ErrorModal,
            {allowReport:false, title : "Reset Password", message: "This is a placeholder for the reset password page."} as ModalDataError,
            () => { window.location.href = "/welcome"; }
        );

    }, []);
    
    return null;

}