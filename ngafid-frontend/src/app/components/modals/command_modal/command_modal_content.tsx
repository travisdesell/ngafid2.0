// ngafid-frontend/src/app/components/modals/command_modal/command_modal_content.tsx

import BugReportModal from "@/components/modals/bug_report_modal";
import CommandModalContentItem from "@/components/modals/command_modal/command_modal_content_item";
import { useModal } from "@/components/modals/modal_context";
import { useAuth } from "@/components/providers/auth_provider";
import { useCommands } from "@/components/providers/commands_provider";
import { getLogger } from "@/components/providers/logger";
import { useTheme } from "@/components/providers/theme-provider";
import { Input } from "@/components/ui/input";
import { Command } from "cmdk";
import { Bug, ChartArea, Flame, Grid2X2Plus, Home, Image, Info, ListOrdered, LogOut, LucideIcon, Menu, Moon, Plane, Rows3, Search, Upload, User, Users } from "lucide-react";
import { useEffect, useState } from "react";

const log = getLogger("CommandModalContent", "black", "Modal");

type CommandModalContentProps = {
    submitCommand: (command: any) => void;
    inputRef: React.RefObject<HTMLInputElement> | undefined;
};

export type CommandData = {
    name: string;
    command: () => void;
    Icon: LucideIcon;
    hotkey: string;
    toggleState?: boolean;
}

export default function CommandModalContent({ submitCommand, inputRef }: CommandModalContentProps) {

    const { pageCommands } = useCommands();
    const { theme, setTheme, useHighContrastCharts, setUseHighContrastCharts, useBackgroundImage, setUseBackgroundImage, useNavbarPageNames, setUseNavbarPageNames } = useTheme();
    const { setModal } = useModal();
    const { user, attemptLogOut } = useAuth();
    const [previousCommandData, setPreviousCommandData] = useState<null|CommandData>(null);

    // Focus the input on mount
    useEffect(() => {

        const inputRefCur = inputRef?.current as unknown as HTMLElement;
        log(`Focusing inputRefCur: ${inputRefCur}`);

        if (inputRefCur)
            inputRefCur.focus();

    }, [inputRef]);

    const renderPreviousActionGroup = () => {

        // No previous command, skip
        if (!previousCommandData)
            return null;

        return (
            <>
                <Command.Group heading="Previous Action">
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name={previousCommandData.name}
                        command={previousCommandData.command}
                        Icon={previousCommandData.Icon}
                        hotkey={previousCommandData.hotkey}
                        toggleState={previousCommandData.toggleState}
                    />
                </Command.Group>
                <Command.Separator />
            </>
        )

    }

    return (
        <Command label="Global Command Menu">

            <Command.Input ref={inputRef} placeholder="Type a command..." asChild>
                <Input />
            </Command.Input>

            <Command.List className="max-h-128">

                {/* No Results Message */}
                <Command.Empty>
                    <Info />
                    <span>No results found.</span>
                </Command.Empty>

                {/* Previous Action */}
                {renderPreviousActionGroup()}

                {/* Special Per-Page Actions */}
                {
                    (pageCommands.length > 0)
                    &&
                    <>
                        <Command.Group heading="Page Actions">
                        {
                            pageCommands.map((commandData, index) => (
                                <CommandModalContentItem
                                    key={index}
                                    submitCommand={submitCommand}
                                    name={commandData.name}
                                    command={commandData.command}
                                    Icon={commandData.Icon}
                                    hotkey={commandData.hotkey}
                                    toggleState={commandData.toggleState}
                                />
                            ))
                        }
                        </Command.Group>
                        <Command.Separator />
                    </>
                }

                <Command.Group heading="Pages">

                    {/* Open Page - Summary */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Go to Summary Page"
                        command={()=>window.location.href="/protected/summary"}
                        Icon={Home}
                        hotkey="Ctrl+S"
                    />

                    {/* Open Page - Status */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Go to Status Page"
                        command={()=>window.location.href="/protected/status"}
                        Icon={Info}
                        hotkey="Ctrl+A"
                    />

                    {/* Open Page - Events - Trends */}
                    <CommandModalContentItem
                        disabled
                        submitCommand={submitCommand}
                        name="Go to Events — Trends Page"
                        command={()=>window.location.href="/protected/trends"}
                        Icon={ChartArea}
                        hotkey="Ctrl+E"
                    />

                    {/* Open Page - Events - Severities */}
                    <CommandModalContentItem
                        disabled
                        submitCommand={submitCommand}
                        name="Go to Events — Severities Page"
                        command={()=>window.location.href="/protected/severities"}
                        Icon={ChartArea}
                        hotkey="Ctrl+I"
                    />

                    {/* Open Page - Events - Heat Map */}
                    <CommandModalContentItem
                        disabled
                        submitCommand={submitCommand}
                        name="Go to Events — Heat Map Page"
                        command={()=>window.location.href="/protected/heat_map"}
                        Icon={Flame}
                        hotkey="Ctrl+H"
                    />

                    {/* Open Page - Events - Statistics */}
                    <CommandModalContentItem
                        disabled
                        submitCommand={submitCommand}
                        name="Go to Events — Statistics Page"
                        command={()=>window.location.href="/protected/event_statistics"}
                        Icon={Grid2X2Plus}
                        hotkey="Ctrl+C"
                    />

                    {/* Open Page - Events - Definitions */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Go to Events — Definitions Page"
                        command={()=>window.location.href="/protected/event_definitions"}
                        Icon={Rows3}
                        hotkey="Ctrl+D"
                    />

                    {/* Open Page - Analysis - Turn to Final */}
                    <CommandModalContentItem
                        disabled
                        submitCommand={submitCommand}
                        name="Go to Analysis — Turn to Final Page"
                        command={()=>window.location.href="/protected/turn_to_final"}
                        Icon={Search}
                        hotkey="Ctrl+O"
                    />

                    {/* Open Page - Flights */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Go to Flights Page"
                        command={()=>window.location.href="/protected/flights"}
                        Icon={Plane}
                        hotkey="Ctrl+F"
                    />

                    {/* Open Page - Uploads */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Go to Uploads Page"
                        command={()=>window.location.href="/protected/uploads"}
                        Icon={Upload}
                        hotkey="Ctrl+U"
                    />

                    {/* Open Page - Account - Manage Fleet */}
                    <CommandModalContentItem
                        disabled
                        submitCommand={submitCommand}
                        name="Go to Account - Manage Fleet Page"
                        command={()=>window.location.href="/protected/manage_fleet"}
                        Icon={Users}
                        hotkey="Ctrl+M"
                    />

                    {/* Open Page - Account - Manage Tail Numbers */}
                    <CommandModalContentItem
                        disabled
                        submitCommand={submitCommand}
                        name="Go to Account - Manage Tail Numbers Page"
                        command={()=>window.location.href="/protected/manage_tail_numbers"}
                        Icon={ListOrdered}
                        hotkey="Ctrl+G"
                    />

                    {/* Open Page - Account - My Preferences */}
                    <CommandModalContentItem
                        disabled
                        submitCommand={submitCommand}
                        name="Go to Account - My Preferences Page"
                        command={()=>window.location.href="/protected/my_preferences"}
                        Icon={User}
                        hotkey="Ctrl+P"
                    />
                </Command.Group>
                <Command.Separator />

                <Command.Group heading="Theming">

                    {/* Toggle Dark/Light Theme */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Dark Theme"
                        command={() => setTheme(theme === "dark" ? "light" : "dark")}
                        Icon={Moon}
                        hotkey="Ctrl+Alt+D"
                        toggleState={theme === "dark"}
                    />

                    {/* Toggle High-Contrast Charts */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="High-Contrast Charts"
                        command={() => setUseHighContrastCharts(!useHighContrastCharts)}
                        Icon={ChartArea}
                        hotkey="Ctrl+Alt+H"
                        toggleState={useHighContrastCharts}
                    />

                    {/* Toggle Background Image */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Toggle Background Image"
                        command={() => setUseBackgroundImage(!useBackgroundImage)}
                        Icon={Image}
                        hotkey="Ctrl+Alt+B"
                        toggleState={useBackgroundImage}
                    />

                    {/* Toggle Navbar Page Names */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Toggle Navbar Page Names"
                        command={() => setUseNavbarPageNames(!useNavbarPageNames)}
                        Icon={Menu}
                        hotkey="Ctrl+Alt+N"
                        toggleState={useNavbarPageNames}
                    />

                </Command.Group>
                <Command.Separator />


                <Command.Group heading="Other">

                    {/* Report a Bug */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Report a Bug"
                        command={() => {
                            setModal(BugReportModal, { user: user! });
                        }}
                        Icon={Bug}
                        hotkey="Ctrl+Alt+P"
                    />

                    {/* Log Out */}
                    <CommandModalContentItem
                        submitCommand={submitCommand}
                        name="Log Out"
                        command={attemptLogOut}
                        Icon={LogOut}
                        hotkey="Ctrl+Alt+L"
                    />

                </Command.Group>

            </Command.List>
        </Command>
    );
    
}