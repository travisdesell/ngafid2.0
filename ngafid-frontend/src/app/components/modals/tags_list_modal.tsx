// ngafid-frontend/src/app/components/modals/tags_list_modal.tsx
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { motion } from "motion/react";

import { ColorPicker, randomHexColor } from '@/components/color_picker';
import ConfirmModal from '@/components/modals/confirm_modal';
import ErrorModal from '@/components/modals/error_modal';
import { useModal } from "@/components/modals/modal_context";
import { getLogger } from "@/components/providers/logger";
import { TagData, useTags } from "@/components/providers/tags/tags_provider";
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Textarea } from '@/components/ui/textarea';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { fetchJson } from '@/fetchJson';
import '@/index.css';
import { Link, Pencil, Plus, Tag, Trash, Unlink, X } from 'lucide-react';
import { JSX, useEffect, useState } from 'react';
import type { ModalData, ModalProps } from "./types";


const log = getLogger("TagsListModal", "black", "Modal");


export type ModalDataTagsList = ModalData & {
    flightTags: TagData[];
    flightId: number;
    onTagsUpdate: (updatedTags: TagData[]) => void;
}

export default function TagsListModal({ data }: ModalProps<ModalDataTagsList>) {

    const { close, setModal } = useModal();
    const { fleetTags, associateTagWithFlight, unassociateTagWithFlight, editTag, updateFleetTag, addFleetTag, deleteFleetTag } = useTags();
    const { flightId, flightTags, onTagsUpdate } = (data as ModalDataTagsList);

    const [currentFlightTags, setCurrentFlightTags] = useState<TagData[]>(flightTags ?? []);
    const [editingTagID, setEditingTagID] = useState<number | null>(null);

    const associatedTags = fleetTags.filter((tag) => currentFlightTags.some((ft) => ft.hashId === tag.hashId));
    const unassociatedTags = fleetTags.filter((tag) => !currentFlightTags.some((ft) => ft.hashId === tag.hashId));

    useEffect(() => {
        log.table("Associated Tags: ", associatedTags);
        log.table("Unassociated Tags: ", unassociatedTags);
        log.table("All Fleet Tags: ", fleetTags);
    }, []);



    const [colorPickerValue, setColorPickerValue] = useState<string>(randomHexColor());
    const [nameInputValue, setNameInputValue] = useState<string>("");
    const [descriptionInputValue, setDescriptionInputValue] = useState<string>("");
    

    const renderTagsAssociated = () => {

        const renderTagRowAssociated = (tag: TagData, index: number) => {

            const unassociateThisTag = async () => {

                log("Unassociating tag from flight: ", tag);

                await unassociateTagWithFlight(tag.hashId.toString(), flightId);
                setCurrentFlightTags((prev) => {
                    const next = prev.filter((t) => t.hashId !== tag.hashId);
                    onTagsUpdate(next);
                    return next;
                });

            }

            return <div key={index} className="flex flex-col p-2 pl-4 border-b last:border-b-0 gap-1 hover:bg-background">

                <div className="flex flex-row items-center w-full gap-1">

                    {/* Tag Color Box */}
                    <Tag size={24} className="mr-3" fill={tag.color} />

                    {/* Tag Name */}
                    <div className="font-medium truncate select-all">{tag.name}</div>

                    {/* Tag Edit Button */}
                    <Tooltip disableHoverableContent>
                        <TooltipTrigger asChild>
                            <Button
                                variant="ghost"
                                className="aspect-square ml-auto"
                                onClick={() => startTagEdit(tag)}
                            >
                                <Pencil size={16} />
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                            Edit Tag
                        </TooltipContent>
                    </Tooltip>

                    {/* Tag Unassociate Button */}
                    <Tooltip disableHoverableContent>
                        <TooltipTrigger asChild>
                            <Button
                                variant="ghostDestructive"
                                className="aspect-square"
                                onClick={() => unassociateThisTag()}
                            >
                                <Unlink size={16} />
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                            Unassociate Tag
                        </TooltipContent>
                    </Tooltip>

                </div>

                <div className="text-xs text-muted-foreground">
                    {tag.description}
                </div>

            </div>

        }

        return <>

            {/* Associated Tags List */}
            {
                (associatedTags.length === 0)
                ?
                <div className="text-center text-muted-foreground">
                    No tags associated with this flight.
                </div>
                :
                <div className="max-h-128 overflow-y-auto border rounded">
                    {associatedTags.map((tag, index) => renderTagRowAssociated(tag, index))}
                </div>
            }

        </>

    }

    const renderTagsUnassociated = () => {

        const renderTagRowUnassociated = (tag: TagData, index: number) => {

            const associateThisTag = async () => {

                log("Associating tag with flight: ", tag);

                await associateTagWithFlight(tag.hashId.toString(), flightId);
                setCurrentFlightTags((prev) => {
                    const next = [...prev, tag];
                    onTagsUpdate(next);
                    return next;
                });

            };

            const deleteThisTag = async () => {

                const onConfirmDelete = async () => {

                    log("Deleting tag: ", tag);

                    try {

                        await deleteFleetTag(tag.hashId.toString());
                        log("Deleted tag: ", tag);

                        // Remove the tag from the current flight tags list
                        setCurrentFlightTags((prev) => prev.filter((t) => t.hashId !== tag.hashId));
                        onTagsUpdate(currentFlightTags.filter((t) => t.hashId !== tag.hashId));

                        // Remove the tag from the fleet tags list
                        updateFleetTag(tag);

                    } catch (error) {

                        log.error("Error deleting tag: ", error);
                        setModal(ErrorModal, {
                            title: "Error Deleting Tag",
                            message: "An error occurred while deleting the tag. Please try again.",
                        });

                    }

                }

                log("Confirming deletion of tag: ", tag);

                setModal(ConfirmModal, {
                    title: "Confirm Tag Deletion",
                    message: `Are you sure you want to delete the tag "${tag.name}"? The tag will be removed from all flights. This action cannot be undone.`,
                    onConfirm: onConfirmDelete,
                });

            }

            return <div key={index} className="flex flex-col p-2 pl-4 border-b last:border-b-0 gap-1 hover:bg-background">

                <div className="flex flex-row items-center w-full gap-1">

                    {/* Tag Color Box */}
                    <Tag size={24} className="mr-3" fill={tag.color} />

                    {/* Tag Name */}
                    <div className="font-medium truncate select-all">{tag.name}</div>

                    {/* Tag Delete Button */}
                    <Tooltip disableHoverableContent>
                        <TooltipTrigger asChild>
                            <Button
                                variant="ghostDestructive"
                                className="aspect-square ml-auto"
                                onClick={() => deleteThisTag()}
                            >
                                <Trash size={16} />
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                            Delete Tag
                        </TooltipContent>
                    </Tooltip>

                    {/* Tag Edit Button */}
                    <Tooltip disableHoverableContent>
                        <TooltipTrigger asChild>
                            <Button
                                variant="ghost"
                                className="aspect-square"
                                onClick={() => startTagEdit(tag)}
                            >
                                <Pencil size={16} />
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                            Edit Tag
                        </TooltipContent>
                    </Tooltip>

                    {/* Tag Associate Button */}
                    <Tooltip disableHoverableContent>
                        <TooltipTrigger asChild>
                            <Button
                                variant="ghost"
                                className="aspect-square"
                                onClick={() => associateThisTag()}
                            >
                                <Link size={16} />
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                            Associate Tag
                        </TooltipContent>
                    </Tooltip>

                </div>

                <div className="text-xs text-muted-foreground">
                    {tag.description}
                </div>

            </div>

        }

        return <>

            {
                (unassociatedTags.length === 0)
                ?
                <div className="text-center text-muted-foreground">
                    No unassociated tags available.
                </div>
                :
                <div className="max-h-128 overflow-y-auto border rounded">
                    {unassociatedTags.map((tag, index) => renderTagRowUnassociated(tag, index))}
                </div>
            }

        </>


    }

    const renderTagsCreate = () => {

        const allowCreate = (nameInputValue.trim() !== "" && descriptionInputValue.trim() !== "");

        const createThisTag = async () => {

            const RESPONSE_TAG_ALREADY_EXISTS = "ALREADY_EXISTS";

            const params = new URLSearchParams();
            params.set("name", nameInputValue);
            params.set("description", descriptionInputValue);
            params.set("color", colorPickerValue);
            params.set("id", String(flightId));

            log("Creating tag with body: ", {
                name: nameInputValue,
                description: descriptionInputValue,
                color: colorPickerValue,
                id: flightId,
            });

            const newTag = await fetchJson.post<TagData | string>("/api/tag", params)
                .then((data) => {

                    // Got string 'ALREADY_EXISTS', show error modal
                    if (data === RESPONSE_TAG_ALREADY_EXISTS) {

                        setModal(ErrorModal, {
                            title: "Tag Already Exists",
                            message: "A tag with that name already exists. Please choose a different name.",
                        });
                        throw new Error("Tag already exists");
                    }

                    return data as TagData;
                    
                })
                .catch((error) => {

                    log.error("Error during tag creation: ", error);
                    return null;

                });

            // Failed to create tag, show error modal
            if (!newTag) {

                log.error("Failed to create tag.");
                return;

            }

            log(`Created tag, attempting to associate with flight ID ${flightId}: `, newTag);

            // Associate the new tag with the flight
            setCurrentFlightTags((prev) => {
                const next = [...prev, newTag];
                onTagsUpdate(next);
                return next;
            });

            // Add the new tag to the fleet tags list
            addFleetTag(newTag);
            
            // Move back to the 'Associated' tab
            setModalTab("associated");
        };

        return <div className="grid gap-2">

            {/* Color & Name Row */}
            <div className="flex w-full">

                {/* Color Picker */}
                <ColorPicker value={colorPickerValue} onChange={setColorPickerValue} />

                {/* Name Input */}
                <Input className="ml-4" placeholder="Tag Name" value={nameInputValue} onChange={(e) => setNameInputValue(e.target.value)} />

            </div>

            {/* Description Input */}
            <Textarea
                className="w-full"
                placeholder="Tag Description"
                value={descriptionInputValue}
                onChange={(e) => setDescriptionInputValue(e.target.value)}
            />

            {/* Create Button */}
            <Button
                className="self-end"
                disabled={!allowCreate}
                onClick={() => createThisTag()}
            >
                <Plus size={16} /> Create Tag
            </Button>

        </div>

    }

    const renderTagsEdit = () => {

        const allowUpdate = (nameInputValue.trim() !== "" && descriptionInputValue.trim() !== "");

        const editThisTag = async () => {

            if (editingTagID == null) {

                setModal(ErrorModal, {
                    title: "No Tag Selected",
                    message: "Please select a tag to edit.",
                });
                
                return;
            }

            log("Editing tag with ID: ", editingTagID, "and body: ", {
                name: nameInputValue,
                description: descriptionInputValue,
                color: colorPickerValue,
            });

            const updatedTag = await editTag(editingTagID.toString(), {
                name: nameInputValue,
                description: descriptionInputValue,
                color: colorPickerValue,
            });

            log("Updated tag: ", updatedTag);

            // Update the tag in the current flight tags list
            setCurrentFlightTags((prev) => {
                const next = prev.map((t) => (t.hashId === updatedTag.hashId) ? updatedTag : t);
                onTagsUpdate(next);
                return next;
            });

            // Update the tag in the fleet tags list
            updateFleetTag(updatedTag);

            // Move back to the 'Associated' tab
            setModalTab("associated");

        }

        return <div className="grid gap-2">

            {/* Color & Name Row */}
            <div className="flex w-full">

                {/* Color Picker */}
                <ColorPicker value={colorPickerValue} onChange={setColorPickerValue} />

                {/* Name Input */}
                <Input className="ml-4" placeholder="Tag Name" value={nameInputValue} onChange={(e) => setNameInputValue(e.target.value)} />

            </div>

            {/* Description Input */}
            <Textarea
                className="w-full"
                placeholder="Tag Description"
                value={descriptionInputValue}
                onChange={(e) => setDescriptionInputValue(e.target.value)}
            />

            {/* Update Button */}
            <Button
                className="self-end"
                disabled={!allowUpdate}
                onClick={() => editThisTag()}
            >
                <Pencil size={16} /> Update Tag
            </Button>

        </div>

    }

    const startTagCreate = () => {

        /*
            Move to the 'Create' tab and
            reset the form to default values.
        */

        setModalTab("create");
        setColorPickerValue(randomHexColor());
        setNameInputValue("");
        setDescriptionInputValue("");

    }


    type ModalTab = {
        name: string;
        content: () => JSX.Element;
        onOpen?: () => void;
        disabled?: boolean;
        description?: string;
    };

    const MODAL_TABS: Record<string, ModalTab> = {
        associated: {
            name: "Associated",
            content: renderTagsAssociated,
            description: "View tags currently associated with this flight.",
        },
        unassociated: {
            name: "Unassociated",
            content: renderTagsUnassociated,
            description: "View tags not currently associated with this flight.",
        },
        create: {
            name: "Create",
            content: renderTagsCreate,
            onOpen: startTagCreate,
            description: "Create a new tag and associate it with this flight.",
        },
        edit: {
            name: "Edit",
            content: renderTagsEdit,
            disabled: true,
            description: "Modify an existing tag.",
        },
    };


    const MODAL_TABS_KEYS = Object.keys(MODAL_TABS) as (keyof typeof MODAL_TABS)[];
    const MODAL_TABS_VALUES = Object.values(MODAL_TABS);


    const [modalTab, setModalTab] = useState<keyof typeof MODAL_TABS>("associated");
    useEffect(() => {

        const currentTab = MODAL_TABS[modalTab];

        // New tab has an onOpen function, call it
        if (currentTab.onOpen)
            currentTab.onOpen();

    }, [modalTab]);


    const updateTab = (tab: keyof typeof MODAL_TABS) => {

        if (!MODAL_TABS_KEYS.includes(tab)) {
            log.error(`Invalid tab: ${tab}`);
            return;
        }

        log("Updating tab: ", tab);
        setModalTab(tab);
        
    }

    const startTagEdit = (tag: TagData) => {

        /*
            Move to the 'Edit' tab and
            populate the form with the
            tag's current data.
        */

        setModalTab("edit");
        setEditingTagID(tag.hashId);
        setColorPickerValue(tag.color);
        setNameInputValue(tag.name);
        setDescriptionInputValue(tag.description);

    }


    // log.table("Rendering tags list: ", tags);
    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                <CardHeader className="grid gap-2">

                    <div className="grid gap-2">
                        <CardTitle>Flight Tags</CardTitle>
                        <CardDescription>
                            {/* Choose which tags to associate with this flight. */}
                            {MODAL_TABS[modalTab].description}
                        </CardDescription>
                    </div>

                    <CardAction>
                        <Button variant="link" onClick={close}>
                            <X/>
                        </Button>
                    </CardAction>

                </CardHeader>
                <CardContent className="space-y-8">

                    <Tabs
                        id="tags-list-modal-tabs"
                        defaultValue={modalTab}
                        value={modalTab}
                        onValueChange={(e)=>updateTab(e as keyof typeof MODAL_TABS)}
                    >

                        {/* Modal Tab Options */}
                        <TabsList className="w-full justify-between" id="tags-list-modal-tabs">
                            {
                                MODAL_TABS_KEYS.map((key) => (
                                    <TabsTrigger key={key} value={key} disabled={MODAL_TABS[key].disabled}>
                                        <span className="capitalize">{key}</span>
                                    </TabsTrigger>
                                ))
                            }
                        </TabsList>

                        {/* Modal Content */}
                        {
                            MODAL_TABS_KEYS.map((key) => (
                            <TabsContent
                                key={key}
                                value={key}
                                className="w-full min-h-48 pt-8"
                            >
                                {MODAL_TABS[key].content()}
                            </TabsContent>
                        ))}

                    </Tabs>

                </CardContent>
            </Card>
        </motion.div>
    );
}