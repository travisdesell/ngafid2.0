// ngafid-frontend/src/app/components/providers/tags/tags_provider.tsx

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { useAuth } from "@/components/providers/auth_provider";
import { getLogger } from "@/components/providers/logger";
import { fetchJson } from "@/fetchJson";
import { createContext, useContext, useEffect, useState, useTransition } from "react";

const log = getLogger("TagsProvider", "gray", "Provider");

export type TagData = {
    hashId: number;
    fleetId: number;
    name: string;
    description: string;
    color: string;
}


type TagsProviderState = {
    fleetTags: TagData[];
    isFetchingTags: boolean;
    setFleetTags: (tags: TagData[]) => void;
    addFleetTag: (tag: TagData) => void;
    updateFleetTag: (tag: TagData) => void;
    deleteFleetTag: (tagId: string) => void;
    associateTagWithFlight: (tagId: string, flightId: number) => Promise<void>;
    unassociateTagWithFlight: (tagId: string, flightId: number) => Promise<void>;
    editTag: (tagId: string, body: { name: string; description: string; color: string }) => Promise<TagData>;
}

export const initialState: TagsProviderState = {
    fleetTags: [],
    isFetchingTags: false,
    setFleetTags: () => undefined,
    addFleetTag: () => undefined,
    updateFleetTag: () => undefined,
    deleteFleetTag: () => undefined,
    associateTagWithFlight: async (_tagId: string, _flightId: number) => {},
    unassociateTagWithFlight: async (_tagId: string, _flightId: number) => {},
    editTag: async (_tagId: string, _body: { name: string; description: string; color: string }) => {
        return {
            hashId: 0,
            fleetId: 0,
            name: "",
            description: "",
            color: "",
        };
    }
}

export const TagsProviderContext = createContext<TagsProviderState>(initialState);

export function TagsProvider({ children }: { children: React.ReactNode }) {

    const { isLoggedIn } = useAuth();
    const { setModal } = useModal();
    const [fleetTags, setFleetTags] = useState<TagData[]>([]);
    const [isFetchingTags, startTransition] = useTransition();
    const [hasFetched, setHasFetched] = useState(false);

    useEffect(() => {

        // Not logged in yet or already fetched, exit
        if (!isLoggedIn() || hasFetched)
            return;

        let cancelled = false;

        const fetchFleetTags = async () => {
            
            log("Fetching fleet tags...");

            try {

                const response = await fetch("/api/tag");
                log("Received response for fleet tags:", response);

                const text = await response.text();
                const data = JSON.parse(text) as TagData[];
                log("Parsed fleet tags data:", data);

                // Response not OK, throw error
                if (!response.ok)
                    throw new Error("Failed to fetch fleet tags");

                // Operation cancelled, exit
                if (cancelled)
                    return;

                // Update state with fetched tags
                startTransition(() => { setFleetTags(data); });
                setHasFetched(true);

            } catch (error) {
                
                const e = (error as Error);
                setModal(ErrorModal, { title: "Failed to fetch fleet tags", message: "", code: e.toString() });

            }

        };

        fetchFleetTags();

        return () => {
            cancelled = true;
        };

    }, [isLoggedIn, hasFetched]);


    const addFleetTag = (tag: TagData) => {
        setFleetTags((prev) => [...prev, tag]);
    }

    const updateFleetTag = (updatedTag: TagData) => {
        setFleetTags((prev) => prev.map(tag => tag.hashId === updatedTag.hashId ? updatedTag : tag));
    }

    const deleteFleetTag = (tagId: string) => {

        /*
            DELETE: /api/tag/{tid}
        */

        fetch(`/api/tag/${tagId}`, {
            method: "DELETE",
        }).then((response) => {

            if (!response.ok)
                throw new Error(`Failed to delete tag: ${response.statusText}`);
            
            startTransition(() => {
                setFleetTags((prev) => prev.filter(tag => tag.hashId.toString() !== tagId));
            });

        })

    }

    const associateTagWithFlight = async (_tagId: string, _flightId: number): Promise<void> => {

        /*
            PUT: /api/flight/{fid}/tag/{tagId}
        */

        try {

            const response = await fetch(`/api/flight/${_flightId}/tag/${_tagId}`, {
                method: "PUT",
            });

            if (!response.ok) {
                throw new Error(`Failed to associate tag with flight: ${response.statusText}`);
            }

        } catch (error) {
            log("Error associating tag with flight:", error);
            throw error;
        }

    }

    const unassociateTagWithFlight = async (_tagId: string, _flightId: number): Promise<void> => {

        /*
            DELETE: /api/flight/{fid}/tag/{tagId}
        */

        try {

            const response = await fetch(`/api/flight/${_flightId}/tag/${_tagId}`, {
                method: "DELETE",
            });

            if (!response.ok) {
                throw new Error(`Failed to unassociate tag with flight: ${response.statusText}`);
            }
            
        } catch (error) {
            log("Error unassociating tag with flight:", error);
            throw error;
        }

    }

    const editTag = async (tagId: string, body: { name: string; description: string; color: string }): Promise<TagData> => {

        const params = new URLSearchParams({
            name: body.name,
            description: body.description,
            color: body.color,
        });

        log("Attempting to edit tag with ID:", tagId, "and body:", body);

        const response = await fetchJson.patch<TagData | string>(`/api/tag/${tagId}`, params)
            .catch((error) => {
                log("Error editing tag:", error);
                throw error;
            });

        // Backend may return a string in some cases
        if (typeof response === "string") {

            if (response === "ALREADY_EXISTS")
                throw new Error("Tag name already exists");

            // Unexpected string payload
            throw new Error(`Unexpected response from /api/tag/${tagId}: ${response}`);

        }

        return response;
        
    };


    const value = {
        fleetTags,
        isFetchingTags,
        setFleetTags,
        addFleetTag,
        updateFleetTag,
        deleteFleetTag,
        associateTagWithFlight,
        unassociateTagWithFlight,
        editTag,
    };
    return (
        <TagsProviderContext.Provider value={value}>
            {children}
        </TagsProviderContext.Provider>
    );
}

export const useTags = () => {

    const context = useContext(TagsProviderContext);
    if (!context)
        throw new Error("useTags must be used within a TagsProvider");
    
    return context;
}