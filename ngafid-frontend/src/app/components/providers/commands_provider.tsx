// ngafid-frontend/src/app/components/providers/commands_provider.tsx

import { getLogger } from "@/components/providers/logger";
import { LucideIcon } from "lucide-react";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";

const log = getLogger("CommandsProvider", "blue", "Provider");

export type CommandData = {
    id: string;
    name: string;
    command: () => void;
    Icon: LucideIcon;
    hotkey: string;
    toggleState?: boolean;
    disabled?: boolean | (() => boolean);
}

export const PAGE_COMMAND_SEPARATOR = undefined;

type PageCommands = CommandData | typeof PAGE_COMMAND_SEPARATOR;
type CommandsState = {
    pageCommands: PageCommands[];
    setSourceCommands: (sourceId: string, commands: CommandData[]) => void;
    clearSourceCommands: (sourceId: string) => void;
}

export const CommandsContext = createContext<CommandsState>({
    pageCommands: [],
    setSourceCommands: () => { },
    clearSourceCommands: () => { },
});

export function CommandsProvider({ children }: { children: React.ReactNode }) {

    const [bySource, setBySource] = useState<Record<string, CommandData[]>>({});
    const [sourceOrder, setSourceOrder] = useState<string[]>([]);

    const setSourceCommands = useCallback((sourceId: string, cmds: CommandData[]) => {

        setBySource(prev => {

            // No change -> Preserve reference
            if (prev[sourceId] === cmds)
                return prev;

            return { ...prev, [sourceId]: cmds };

        });

        setSourceOrder(prev => (prev.includes(sourceId) ? prev : [sourceId, ...prev]));

    }, []);


    const clearSourceCommands = useCallback((sourceId: string) => {

        setBySource(prev => {
            const next = { ...prev };
            delete next[sourceId];
            return next;
        });

        setSourceOrder(prev => prev.filter(id => id !== sourceId));
        
    }, []);

    const pageCommands = useMemo<PageCommands[]>(() => {

        const out: PageCommands[] = [];
        const seen = new Set<string>(); // <-- Dedupe by command.id

        for (const sourceId of sourceOrder) {

            const commands = (bySource[sourceId] ?? []);
            const visible = commands.filter(c => !seen.has(c.id));

            // No commands to add from this source, skip
            if (visible.length === 0)
                continue;

            // Already have commands, add a separator
            if (out.length > 0)
                out.push(PAGE_COMMAND_SEPARATOR);

            // Add visible commands
            for (const c of visible) {
                seen.add(c.id);
                out.push(c);
            }

        }

        return out;

    }, [bySource, sourceOrder]);

    const value = useMemo(() => ({
        pageCommands,
        setSourceCommands,
        clearSourceCommands,
    }), [pageCommands, setSourceCommands, clearSourceCommands]);

    return <CommandsContext.Provider value={value}>
        {children}
    </CommandsContext.Provider>;
    
}

export function useCommands() {

    /*
        Used for reading the current commands context.

        Do not use this to register commands.
    */

    const context = useContext(CommandsContext);
    if (!context)
        throw new Error("useCommands must be used within a CommandsProvider");

    return context;

}

function useStableSourceId(prefix: string) {

    /*
        Generates and returns a stable unique source ID for command registration.

        The ID is generated once per component instance and remains constant
        across re-renders.

    */

    const ref = useRef<string | null>(null);

    // No ID yet, generate one
    if (!ref.current)
        ref.current = `${prefix}:${crypto.randomUUID?.() ?? Math.random().toString(36).slice(2)}`;
    
    return ref.current;

}

export function useRegisterCommands(commands: CommandData[]) {

    /*
        Registers the given commands with the CommandsProvider under a unique source ID.
        
        The commands are registered when the component mounts and unregistered when it unmounts.
    */

    const { setSourceCommands, clearSourceCommands } = useContext(CommandsContext);
    const sourceId = useStableSourceId("commands");

    useEffect(() => {
        setSourceCommands(sourceId, commands);
        return () => clearSourceCommands(sourceId);
    }, [sourceId, setSourceCommands, clearSourceCommands, commands]);

}
