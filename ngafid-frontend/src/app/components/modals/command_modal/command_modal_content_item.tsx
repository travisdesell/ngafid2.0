// ngafid-frontend/src/app/components/modals/command_modal/command_modal_content_item.tsx

import { getLogger } from "@/components/providers/logger";
import { usePlatform } from "@/components/providers/platform_provider";
import { Command } from "cmdk";
import { LucideIcon, Slash } from "lucide-react";
import { useEffect } from "react";

const log = getLogger("CommandModalContentItem", "black", "Component");

type CommandModalContentItemProps = {
    submitCommand: (command: any) => void;
    command: () => void;
    name: string;
    Icon: LucideIcon;
    hotkey?: string;
    toggleState?: boolean;
    disabled?: boolean;
}

export default function CommandModalContentItem(props: CommandModalContentItemProps) {

    const { commandKeyStr } = usePlatform();
    const { submitCommand, name, Icon, hotkey, disabled=false } = props;
    
    const doSubmit = () => {

        if (disabled) {
            log.warn(`Command "${name}" is disabled, not executing.`);
            return;
        }

        submitCommand(() => {

            log(`Executing command: ${name}`);
            props.command();           
            
        });

    }

    const hotkeySplit = hotkey?.split("+") || [];

    const parseHotkey = (hotkey: string) => {

        const parts = hotkey.split("+").map(p => p.toLowerCase().trim());
        const spec = {
            ctrl: parts.includes("ctrl"),
            alt: parts.includes("alt"),
            shift: parts.includes("shift"),
            meta: parts.includes("meta"),
            key: parts.find(p => !["ctrl", "alt", "shift", "meta"].includes(p)) ?? "",
        };

        // Normalize common key names if you ever use them
        if (spec.key === "esc")
            spec.key = "escape";

        return spec;

    };

    // Generate hotkey from string
    useEffect(() => {
        
        // No hotkey, exit
        if (!hotkey)
            return;

        log(`Generating Hotkey for command "${name}": ${hotkey}`);

        const spec = parseHotkey(hotkey);

        const onKeyDown = (e: KeyboardEvent) => {

            // Got repeated event, ignore
            if (e.repeat)
                return;

            const key = e.key.toLowerCase();

            // Non-modifier key doesn't match, exit
            if (spec.key && key !== spec.key)
                return;

            // Match modifiers (required must be true, non-required must be false)
            if (spec.ctrl) {
                const ctrlOrMeta = (e.ctrlKey !== e.metaKey); // <-- Treat ctrl/meta as interchangeable
                if (!ctrlOrMeta)
                    return;
            }
            if (e.altKey !== spec.alt) return;
            if (e.shiftKey !== spec.shift) return;

            e.preventDefault();
            doSubmit();

        };

        document.addEventListener("keydown", onKeyDown);
        return () => document.removeEventListener("keydown", onKeyDown);

    }, [hotkey, name]);

    // Build display name
    let displayName:string = name;
    if (props.toggleState !== undefined) {

        const stateStr = (props.toggleState) ? "Disable" : "Enable";
        displayName = `${stateStr} ${name}`;

    }

    const renderHotkeyName = (keyIn: string) => {

        switch (keyIn.toLowerCase()) {
            case "ctrl": return commandKeyStr;
            case "alt": return "Alt";
            case "shift": return "Shift";
            case "meta": return "Meta";
            case "arrowup": return "↑";
            case "arrowdown": return "↓";
            case "arrowleft": return "←";
            case "arrowright": return "→";
            case "escape": return "Esc";
            case " ": return "Space";
            default: return keyIn.toUpperCase();
        }

    };

    return (
        <Command.Item onSelect={doSubmit} className={`flex gap-1 items-center ${disabled ? "opacity-25! cursor-not-allowed! border-black/0!" : ""}`}>
            
            {/* Icon */}
            {
                (props.toggleState === undefined)
                ?
                <Icon className="mx-2"/>
                :
                <div className="relative h-8 w-8 mx-1">
                    <Icon className={`absolute left-1/2 -translate-x-1/2 top-1/2 -translate-y-1/2 ${props.toggleState ? "opacity-100" : "opacity-25"}`}/>
                    <Slash className={`absolute left-1/2 -translate-x-1/2 top-1/2 -translate-y-1/2 ${props.toggleState ? "opacity-0" : "opacity-100"}`}/>
                </div>
            }

            <span>{displayName}</span>
            {
                (hotkey && hotkeySplit.length > 0)
                &&
                <span className="ml-auto flex gap-1 mr-2">
                    {
                        hotkeySplit.map((part, index) => (
                            <kbd className="bg-muted px-1 rounded" key={index}>
                                {renderHotkeyName(part)}
                            </kbd>
                        ))
                    }
                </span>

            }
        </Command.Item>
    )

}