// ngafid-frontend/src/app/components/modals/modal_provider.tsx
import { ErrorBoundary } from "@/components/error_boundary";
import ErrorModal from "@/components/modals/error_modal";
import { getLogger } from "@/components/providers/logger";
import { AnimatePresence, motion } from "motion/react";
import React, { useCallback, useEffect, useSyncExternalStore } from "react";
import { createPortal } from "react-dom";
import { useLocation } from "react-router-dom";
import { getModalSnapshot, subscribeModal } from "./modal_store";
import { SetModalFn } from "./types";

const log = getLogger("ModalProvider", "green", "Provider");

import { ModalContext, useModal } from "@/components/modals/modal_context";
import { closeModal, openModal } from "./modal_store";

export function ModalProvider({ children }: { children: React.ReactNode }) {

    const location = useLocation();


    const close = useCallback(() => {

        log("Closing modal...");
        closeModal();

    }, []);


    const setModal = useCallback<SetModalFn>((component, data, onClose) => {

        log("Setting modal...");

        // No component -> close
        if (!component) {
            close();
            return;
        }

        openModal(component, data, onClose);

    }, [close]);


    /*
        ErrorBoundary handler that opens ErrorModal
        using the global modal store.
    */
    const handleBoundaryError = React.useCallback((error: Error, info: React.ErrorInfo) => {

        log.error("Caught error in ErrorBoundary:", error, info);

        const code =
            (error.stack ?? "") +
            (info.componentStack ? `\n\nReact component stack:\n${info.componentStack}` : "");

        setModal(ErrorModal, {
            title: "An unexpected error occurred",
            message: error.message || "An unexpected error occurred. Please try again.",
            code,
        });

    }, [setModal]);


    /*
        Close modal on Escape key.
    */
    useEffect(() => {

        function onKeyDown(e: KeyboardEvent) {

            if (e.key === "Escape")
                closeModal();

        }

        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);

    }, []);


    /*
        Close modal on route/URL change.
    */
    useEffect(() => {
        
        closeModal();

    }, [location.pathname, location.search]);


    const value = React.useMemo(() => ({
        modalType: undefined,
        modalData: undefined,
        onClose: undefined,
        setModal,
        close,
    }), [setModal, close]);

    return (
        <ModalContext.Provider value={value}>
            <ErrorBoundary onError={handleBoundaryError}>
                {children}
            </ErrorBoundary>
        </ModalContext.Provider>
    );

}



export function ModalOutlet() {

    const { setModal } = useModal();
    const { modalType: Component, modalData } = useSyncExternalStore(
        subscribeModal,
        getModalSnapshot,
        getModalSnapshot,
    );

    const modalKey = (Component?.__modalKey)
        ?? (Component?.displayName)
        ?? (Component?.name)
        ?? "modal";

    return createPortal(
        <AnimatePresence mode="wait">
            {
                (Component)
                &&
                <motion.div
                    key={modalKey}
                    initial={{ opacity: 0.25 }}
                    animate={{ opacity: 1.00 }}
                    exit={{ opacity: 0.00 }}
                    className="fixed inset-0 z-50 bg-black/75 w-screen h-screen"
                    tabIndex={-1}
                >
                    <Component data={modalData} setModal={setModal} />
                </motion.div>
            }
        </AnimatePresence>,
        document.body
    );

}