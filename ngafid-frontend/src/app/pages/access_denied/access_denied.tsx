// ngafid-frontend/src/app/pages/access_denied/access_denied.tsx
import { useAuth } from "@/auth";
import ErrorModal, { ModalDataError } from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import { useEffect } from "react";

export default function AccessDenied() {

    const { setModal } = useModal();
    const { user } = useAuth();

    useEffect(() => {

        setModal(
            ErrorModal,
            {title : "Access Denied", message: "You do not have permission to view this page. Close this to return to the previous page (or the home page if you're logged out)."} as ModalDataError,
            () => {

                if (!user)
                    window.location.assign("/");
                else
                    window.history.back();
            }
        );

    }, []);
        

    const render = () => {

        return null;

    };

    return render();

}