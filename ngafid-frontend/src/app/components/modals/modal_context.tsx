// ngafid-frontend/src/app/components/modals/modal_context.tsx

import { getLogger } from "@/components/providers/logger";
import React from "react";
import type { ModalComponent, ModalData, SetModalFn } from "./types";

const log = getLogger("ModalContext", "blue", "Context");

export type ModalContextValue = {
    modalType?: ModalComponent;
    modalData?: ModalData;
    setModal: SetModalFn;
    close: () => void;
    onClose?: (data?: any) => void;
};

export const ModalContext = React.createContext<ModalContextValue | null>(null);

const devFallbackModalContext: ModalContextValue = {
    modalType: undefined,
    modalData: undefined,
    setModal: (...args: any[]) => {
        log.warn("setModal called without <ModalProvider> (dev fallback). Args:", args);
    },
    close: () => {
        log.warn("close called without <ModalProvider> (dev fallback).");
    },
    onClose: undefined,
};

export function useModal(): ModalContextValue {

    const context = React.useContext(ModalContext);

    // Got context, return it
    if (context)
        return context;

    // Otherwise...
    if (import.meta.env.DEV) {

        const globalCtx = (window as any).__ModalContextInstance;
        console.warn("useModal: missing ModalProvider", {
            ModalContextEqualsGlobal: globalCtx === ModalContext,
            ModalContext,
            GlobalContext: globalCtx,
        });

        return devFallbackModalContext;

    }

    throw new Error("useModal must be used within <ModalProvider>");

}
