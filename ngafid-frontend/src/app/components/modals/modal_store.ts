// ngafid-frontend/src/app/components/modals/modal_store.ts
import type { ModalComponent, ModalData } from "./types";

type AnyModalComponent = ModalComponent<ModalData>;
type AnyModalData = ModalData;

export type ModalStoreState = {
    modalType?: AnyModalComponent;
    modalData?: AnyModalData;
    onClose?: (data?: any) => void;
};

let state: ModalStoreState = {
    modalType: undefined,
    modalData: undefined,
    onClose: undefined,
};

const listeners = new Set<() => void>();

function notify() {

    for (const listener of listeners)
        listener();

}

export function getModalSnapshot(): ModalStoreState {
    return state;
}

export function subscribeModal(listener: () => void): () => void {

    listeners.add(listener);

    return () => {
        listeners.delete(listener);
    };

}

/**
 * Open a modal with type-safe data,
 * but store it in a type-erased global
 * state.
 */
export function openModal<D extends ModalData = ModalData>(component: ModalComponent<D>, data?: D, onClose?: (data?: any) => void) {

    state = {
        modalType: component as AnyModalComponent,  //<-- Type-erased to the base-modal component type
        modalData: (data ?? {}) as AnyModalData,    //<-- Type-erased to the base data shape
        onClose,
    };

    notify();
    
}

export function closeModal(data?: any) {

    const current = state;

    // Nothing open, exit
    if (!current.modalType)
        return;

    const handler = current.onClose;

    /*
        Clear state first so if handler
        opens another modal it starts
        from a clean state.
    */
    state = {
        modalType: undefined,
        modalData: undefined,
        onClose: undefined,
    };

    notify();

    // Call the onClose callback after notifying subscribers
    handler?.(data);

}
