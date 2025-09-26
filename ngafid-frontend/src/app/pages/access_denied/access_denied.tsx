// ngafid-frontend/src/app/pages/access_denied/access_denied.tsx
import ErrorModal, { ModalDataError } from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import { useEffect } from "react";

export default function AccessDenied() {

    const { setModal } = useModal();

    useEffect(() => {

        setModal(
            ErrorModal,
            {title : "Access Denied", message: "You do not have permission to view this page. Close this to return to the previous page."} as ModalDataError,
            () => { window.history.back(); }
        );

    }, []);
        

    const render = () => {

        return null;

    };

    return render();

}