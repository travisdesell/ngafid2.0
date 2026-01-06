// ngafid-frontend/src/app/components/providers/commands_provider.tsx

import { CommandData } from "@/components/modals/command_modal/command_modal_content";
import { getLogger } from "@/components/providers/logger";
import { createContext, useContext, useEffect, useState } from "react";
import { useLocation } from "react-router-dom";

const log = getLogger("CommandsProvider", "blue", "Provider");

type CommandsState = {
    pageCommands: CommandData[];
    setCommands?: (commands: CommandData[]) => void;
}

export const CommandsContext = createContext<CommandsState>({
    pageCommands: [],
});

export function CommandsProvider({ children }: { children: React.ReactNode }) {

    const [commands, setCommands] = useState<CommandData[]>([]);
    const location = useLocation();

    // Clear the commands when the page changes
    useEffect(() => {

        log("Clearing commands due to page change...");

        setCommands([]);

    }, [location.pathname]);

    return (
        <CommandsContext.Provider value={{ pageCommands: commands, setCommands }}>
            {children}
        </CommandsContext.Provider>
    );

}

export function useCommands() {

    const context = useContext(CommandsContext);
    if (!context)
        throw new Error("useCommands must be used within a CommandsProvider");

    return context;
    
}