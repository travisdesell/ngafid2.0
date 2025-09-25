// ngafid-frontend/src/app/components/modals/modal_provider.tsx
import React from "react";
import { createPortal } from "react-dom";
import { AnimatePresence, motion } from "motion/react";
import { ModalComponent, ModalData, SetModalFn } from "./types";
import { useLocation } from "react-router-dom";

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

        onCloseRef.current?.();

        setModalType(undefined);
        setModalData(undefined);
        setModalOnClose(undefined);
    }, []);

    const setModal = React.useCallback<SetModalFn>((component, data, onClose) => {
        if (!component) {
            close();
            return;
        }
        
        /* @ts-ignore */
        setModalType(() => component);
        setModalOnClose(() => onClose);
        setModalData(data ?? {});
    }, [close]);


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
            {children}
            <ModalRoot />
        </ModalContext.Provider>
    );
}

function ModalRoot() {
    const { modalType: Component, modalData, setModal } = useModal();
    const modalKey =
        Component?.__modalKey ?? Component?.displayName ?? Component?.name ?? "modal";

    // Render at <body> level so you don’t fight page layout/z-index
    return createPortal(
        <AnimatePresence mode="wait">
            {Component && (
                <motion.div
                    key={modalKey}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="fixed inset-0 z-50 bg-black/75 w-screen h-screen"
                >
                    <Component data={modalData} setModal={setModal} />
                </motion.div>
            )}
        </AnimatePresence>,
        document.body
    );
}
