// ngafid-frontend/src/app/pages/reset_password/reset_password.tsx
import ErrorModal, { ModalDataError } from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { useEffect } from "react";
import { setPageTitle } from "@/components/page_title";
import ResetPasswordModal, { ModalDataResetPassword } from "@/components/modals/reset_password_modal";

export default function ResetPasswordPage() {

    setPageTitle("Reset Password");

    const { setModal } = useModal();

    const urlParams = new URLSearchParams(window.location.search);
    const resetPhrase = urlParams.get("resetPhrase")?.trim() ?? "";

    useEffect(() => {

        if (!resetPhrase) {
            setModal(
                ErrorModal,
                { allowReport: false, title: "Invalid Password Reset Link", message: "No reset token was found. Please use the link from your email to reset your password." } as ModalDataError,
                () => { window.location.href = "/welcome"; }
            );

            return;
        }

        setModal(
            ResetPasswordModal,
            { resetPhrase } as ModalDataResetPassword,
            () => { window.location.href = "/welcome"; }
        );

    }, [resetPhrase, setModal]);
    
    return null;

}
