// ngafid-frontend/src/app/pages/waiting/waiting.tsx
import { useModal } from "@/components/modals/modal_context";
import WaitingModal from "@/components/modals/waiting_modal";
import { useAuth } from "@/components/providers/auth_provider";
import { useEffect } from "react";

export default function Waiting() {

    const { setModal } = useModal(); 
    const { attemptLogOut } = useAuth();

    // Display waiting modal
    useEffect(() => {

        setModal(
            WaitingModal,
            {},
            attemptLogOut
        );

    }, [setModal]);
   
    
    // Render nothing
    return null;

}