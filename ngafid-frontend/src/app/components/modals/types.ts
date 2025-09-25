// ngafid-frontend/src/app/components/modals/types.ts
export type ModalData = Record<string, unknown>;

export type SetModalFn = <D extends ModalData = ModalData>(
    component?: ModalComponent<D>,
    data?: D,
    onClose?: (data?: any) => void
) => void;

export type ModalProps<D extends ModalData = ModalData> = {
    setModal: SetModalFn;
    data?: D;
    onClose?: (data?: any) => void;
};

export type ModalComponent<D extends ModalData = ModalData>
    = React.ComponentType<ModalProps<D>> & { __modalKey?: string };