// ngafid-frontend/src/app/components/modals/modal_provider.tsx
import { ErrorBoundary } from "@/components/error_boundary";
import ErrorModal from "@/components/modals/error_modal";
import { getLogger } from "@/components/providers/logger";
import { AnimatePresence, motion } from "motion/react";
import React from "react";
import { createPortal } from "react-dom";
import { useLocation } from "react-router-dom";
import { ModalComponent, ModalData, SetModalFn } from "./types";

const log = getLogger("ModalProvider", "green", "Provider");

type ModalContextValue = {
    modalType?: ModalComponent;
    modalData?: ModalData;
    setModal: SetModalFn;
    close: () => void;
    onClose?: (data?: any) => void;
};

const ModalContext = React.createContext<ModalContextValue | null>(null);

export function useModal() {
    const ctx = React.useContext(ModalContext);
    if (!ctx)
        throw new Error("useModal must be used within <ModalProvider>");
    return ctx;
}

export function ModalProvider({ children }: { children: React.ReactNode }) {

    const [modalType, setModalType] = React.useState<ModalComponent | undefined>();
    const [modalData, setModalData] = React.useState<ModalData | undefined>();
    const [modalOnClose, setModalOnClose] = React.useState<((data?: any) => void) | undefined>(undefined);

    const onCloseRef = React.useRef<((data?: any) => void) | undefined>(undefined);
    React.useEffect(() => { onCloseRef.current = modalOnClose }, [modalOnClose]);

    const close = React.useCallback(() => {

        log("Closing modal...");

        onCloseRef.current?.();

        setModalType(undefined);
        setModalData(undefined);
        setModalOnClose(undefined);
    }, []);

    const setModal = React.useCallback<SetModalFn>((component, data, onClose) => {

        log("Setting modal...");

        // No component is provided, close the modal
        if (!component) {
            log("No component provided, closing modal.");
            close();
            return;
        }
        
        /* @ts-ignore */
        setModalType(() => component);
        setModalOnClose(() => onClose);
        setModalData(data ?? {});

    }, [close]);


    /*
        ErrorBoundary handler that opens ErrorModal
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
        Close the modal on Escape key press
    */
    React.useEffect(() => {

        function onKeyDown(e: KeyboardEvent) {
            if (e.key === "Escape" && modalType)
                close();
        }

        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);

    }, [modalType, close]);


    /*
        Automatically close the modal on route/URL change
        so modals don't persist between pages.

        Shouldn't trigger when it's just the hash that
        changes.
    */
    const location = useLocation();
    React.useEffect(() => {

        if (modalType)
            close();

    }, [location.pathname, location.search]);

    const value = React.useMemo(
        () => ({ modalType, modalData, setModal, close, onClose: modalOnClose }),
        [modalType, modalData, setModal, close, modalOnClose]
    );

    return (
        <ModalContext.Provider value={value}>
            <ErrorBoundary onError={handleBoundaryError}>
                {children}
            </ErrorBoundary>
            {/* <ModalRoot /> */}
        </ModalContext.Provider>
    );
}


export function ModalOutlet() {

    const {
        modalType: Component,
        modalData,
        setModal
    } = useModal();
    const modalKey = Component?.__modalKey ?? Component?.displayName ?? Component?.name ?? "modal";

    return createPortal(
        <AnimatePresence mode="wait">
            {
                Component && (
                    <motion.div
                        key={modalKey}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="fixed inset-0 z-50 bg-black/75 w-screen h-screen"
                        tabIndex={-1}
                    >
                        <Component data={modalData} setModal={setModal} />
                    </motion.div>
                )
            }
        </AnimatePresence>,
        document.body
    );
}