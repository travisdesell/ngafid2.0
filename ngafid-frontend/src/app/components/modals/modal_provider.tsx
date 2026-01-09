// ngafid-frontend/src/app/components/modals/modal_provider.tsx
import { ErrorBoundary } from "@/components/error_boundary";
import ErrorModal from "@/components/modals/error_modal";
import { ModalContext, useModal } from "@/components/modals/modal_context";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { CardAction, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { FocusTrap } from "focus-trap-react";
import { X } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import React, { useCallback, useEffect, useLayoutEffect, useSyncExternalStore } from "react";
import { createPortal } from "react-dom";
import { useLocation } from "react-router-dom";
import { closeModal, getModalSnapshot, modalIsOpen, openModal, subscribeModal } from "./modal_store";
import { SetModalFn } from "./types";

const log = getLogger("ModalProvider", "green", "Provider");

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

        // Modal is already open, open with 0ms delay
        if (modalIsOpen())
            setTimeout(() => openModal(component, data, onClose), 0);

        // Otherwise, open immediately
        else
            openModal(component, data, onClose);

    }, [close]);


    const renderModalHeader = useCallback((title: string, description: string, allowClose: boolean = true) => {

        return (
            <CardHeader className="grid gap-2">

                <div className="grid gap-2">
                    <CardTitle>{title}</CardTitle>
                    <CardDescription>{description}</CardDescription>
                </div>

                {
                    (allowClose)
                    &&
                    <CardAction>
                        <Tooltip>
                            <TooltipTrigger asChild className="w-12 h-9">
                                <Button
                                    type="button"
                                    variant="link"
                                    onClick={close}
                                    aria-label="Close modal"
                                    data-modal-close
                                >
                                    <X aria-hidden="true" />
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent keyboardAction="Esc">
                                Close
                            </TooltipContent>
                        </Tooltip>
                    </CardAction>
                }

            </CardHeader>
        );

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
    useLayoutEffect(() => {

        closeModal();

    }, [location.pathname, location.search]);

    const value = React.useMemo(() => ({
        modalType: undefined,
        modalData: undefined,
        onClose: undefined,
        setModal,
        close,
        renderModalHeader,
    }), [setModal, close, renderModalHeader]);

    return (
        <ModalContext.Provider value={value}>
            <ErrorBoundary onError={handleBoundaryError}>
                {children}
            </ErrorBoundary>
        </ModalContext.Provider>
    );

}

const TABBABLE_SELECTOR = [
    'a[href]',
    'area[href]',
    'button',
    'button:not([disabled])',
    'input:not([disabled]):not([type="hidden"])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    'iframe',
    'object',
    'embed',
    '[contenteditable="true"]',
    '[tabindex]:not([tabindex="-1"])',
].join(",");

function isTabbable(el: HTMLElement | null): el is HTMLElement {

    // No element -> False
    if (!el) return false;

    // Disabled, hidden, or negative tabindex -> False
    if (
        el.hasAttribute("disabled")
        || el.getAttribute("aria-hidden") === "true"
        || el.closest('[aria-hidden="true"]')
    )   
        return false;

    // Hidden -> False
    const style = window.getComputedStyle(el);
    if (style.display === "none" || style.visibility === "hidden")
        return false;

    // No client rects -> False
    if (el.getClientRects().length === 0)
        return false;

    const nonNegativeTabIndex = (el.tabIndex >= 0);
    return nonNegativeTabIndex;
    
}

function getTabbableElements(root: HTMLElement): HTMLElement[] {

    const nodes = Array.from(root.querySelectorAll<HTMLElement>(TABBABLE_SELECTOR));
    return nodes.filter(isTabbable);

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

    // Unique id per rendered modal instance
    const trapID = `ngafid-modal-trap-${modalKey}`;

    const focusTrapOptions = React.useMemo(() => {

        return {

            escapeDeactivates: false,   // <-- Already handled globally
            fallbackFocus: `#${trapID}`,    // <-- Focus the trap root as last resort

            /* 
                Initial focus logic:

                - Prefer 'data-modal-initial-focus' attribute
                - Otherwise, prefer first tabbable (except the Close button)
                - Otherwise, focus the Close button
                - Otherwise, focus the trap root
            */
            initialFocus: () => {

                const root = document.getElementById(trapID) as HTMLElement | null;

                // No root, nothing to focus
                if (!root)
                    return false;

                // Look for 'data-modal-initial-focus' attribute
                const initialFocusEl = root.querySelector<HTMLElement>("[data-modal-initial-focus]");
                if (isTabbable(initialFocusEl))
                    return initialFocusEl;

                const closeBtn = root.querySelector<HTMLElement>("[data-modal-close]");
                const tabbables = getTabbableElements(root);

                // Got a tabbable element that's not the close button, focus it
                const nonClose = tabbables.filter((el) => el !== closeBtn);
                if (nonClose.length > 0)
                    return nonClose[0];

                // Close button is tabbable, focus it
                if (isTabbable(closeBtn))
                    return closeBtn;

                // Otherwise, focus the root
                return root;

            },

            returnFocusOnDeactivate: true,  // <-- Return the focus to the opener when unmounted / deactivated

        };
    }, [trapID]);

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
                    <FocusTrap focusTrapOptions={focusTrapOptions}>
                        <div
                            id={trapID}
                            role="dialog"
                            aria-modal="true"
                            tabIndex={-1}
                            className="w-full h-full outline-none"
                        >
                            <Component data={modalData} setModal={setModal} />
                        </div>
                    </FocusTrap>
                </motion.div>
            }
        </AnimatePresence>,
        document.body
    );

}