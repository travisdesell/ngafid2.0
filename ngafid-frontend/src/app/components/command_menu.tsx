import CommandModal from "@/components/modals/command_modal/command_modal";
import { useModal } from "@/components/modals/modal_context";
import { usePlatform } from "@/components/providers/platform_provider";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { LucideCommand } from "lucide-react";
import React, { useEffect } from "react";

function CommandMenu() {

    const { setModal } = useModal();
    const { commandKeyStr } = usePlatform();
    const [open, setOpen] = React.useState(false);
    const inputRef = React.useRef(null);

    React.useEffect(() => {
        
        const onKeyDown = (e: { key: string; metaKey: any; ctrlKey: any; target: any; preventDefault: () => void; }) => {

            const isK = (e.key?.toLowerCase() === "k");
            const isHotkey = isK && (e.metaKey || e.ctrlKey);

            // Hotkey not pressed, exit
            if (!isHotkey)
                return;

            // Make sure that the user isn't typing in an input field
            const el = e.target;
            const tag = el?.tagName;
            const isTypingTarget = (tag === "INPUT") || (tag === "TEXTAREA") || (tag === "SELECT") || el?.isContentEditable;

            // User is typing, exit
            if (isTypingTarget)
                return;

            e.preventDefault();
            setOpen((v) => !v);
        };

        document.addEventListener("keydown", onKeyDown);
        return () => document.removeEventListener("keydown", onKeyDown);

    }, []);

    React.useEffect(() => {

        // Closing the command menu, exit
        if (!open)
            return;

        // The input ref is not set, exit
        const inputRefCur = inputRef.current as unknown as HTMLElement;
        if (inputRefCur == null)
            return;

        requestAnimationFrame(() => inputRefCur.focus());

    }, [open]);

    useEffect(() => {

        // Not opening, exit
        if (!open)
            return;

        setModal(
            CommandModal,
            {inputRef},
            () => setOpen(false)
        );

    }, [open, setOpen]);

    const hotkey = `${commandKeyStr} + K`;
    return (
        <Tooltip disableHoverableContent>
            <TooltipTrigger asChild>
                <button type="button" className="h-6 inline-flex items-center cursor-pointer" onClick={()=>setOpen(true)}>
                    <LucideCommand />
                </button>
            </TooltipTrigger>
            <TooltipContent leftAction="Open" keyboardAction={hotkey} >
                Action Menu
            </TooltipContent>
        </Tooltip>
    );
}

export default CommandMenu;
