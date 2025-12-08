// ngafoid-frontend/src/app/components/providers/system_ids_provider/system_ids_provider.tsx

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { useAuth } from "@/components/providers/auth_provider";
import { getLogger } from "@/components/providers/logger";
import { fetchJson } from "@/fetchJson";
import { createContext, useContext, useEffect, useState } from "react";

const log = getLogger("SystemIdsProvider", "red", "Provider");

type SystemIdsProviderState = {
    systemIds: string[];
    setSystemIds: (ids: string[]) => void;
}

type SystemIDResponseItem = {
    confirmed: boolean;
    fleetId: number;
    systemId: string;
    tail: string;
}

export const initialState: SystemIdsProviderState = {
    systemIds: [],
    setSystemIds: () => undefined,
}

export const SystemIdsProviderContext = createContext<SystemIdsProviderState>(initialState);

export function SystemIdsProvider({ children }: { children: React.ReactNode }) {

    const { isLoggedIn } = useAuth();
    const { setModal } = useModal();
    const [systemIds, setSystemIds] = useState<string[]>([]);

    // Fetch system IDs from API on mount
    useEffect(() => {

        const fetchSystemIds = async () => {

            try {

                const response = await fetchJson.get<SystemIDResponseItem[]>("/api/aircraft/system-id");
                log("Fetched system IDs:", response);
                setSystemIds(response.map(item => item.systemId));

            } catch (error) {

                const e = (error as Error);
                setModal(ErrorModal, { title: "Failed to fetch System IDs", message: "", code: e.toString() });

            }

        };

        // Logged in, fetch system IDs
        if (isLoggedIn())
            fetchSystemIds();

    }, []);

    return (
        <SystemIdsProviderContext.Provider value={{ systemIds, setSystemIds }}>
            {children}
        </SystemIdsProviderContext.Provider>
    );

}

export const useSystemIds = () => {
    
    const context = useContext(SystemIdsProviderContext);

    if (!context)
        throw new Error("useSystemIds must be used within a SystemIdsProvider");

    return context;

}