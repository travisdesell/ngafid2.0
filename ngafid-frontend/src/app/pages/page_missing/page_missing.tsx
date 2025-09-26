// ngafid-frontend/src/app/pages/page_missing/page_missing.tsx
import ErrorModal, { ModalDataError } from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_provider";
import { useEffect } from "react";

export default function PageMissing() {

    const { setModal } = useModal();

    useEffect(() => {

        setModal(
            ErrorModal,
            {title : "Page Not Found", message: "The page you are looking for does not exist. Close this to return to the previous page."} as ModalDataError,
            () => { window.history.back(); }
        );

    }, []);
        

    const render = () => {

        return null;

    };

    return render();

}