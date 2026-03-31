// ngafid-frontend/src/app/components/navbars/protected_navbar.jsx
import React from "react";

import { Button } from "@/components/ui/button";

import { DarkModeToggle } from "@/components/dark_mode_toggle";
import { CalendarCog, ChevronDown, Home, Info, Plane, Search, Upload, User } from 'lucide-react';

import { Link } from "react-router-dom";

import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "../ui/dropdown-menu";


import CommandMenu from "@/components/command_menu";
import { useModal } from "@/components/modals/modal_context";
import { useNavbarSlot } from "@/components/navbars/navbar_slot";
import { useAuth } from "@/components/providers/auth_provider";
import { getLogger } from "@/components/providers/logger";
import { useTheme } from "@/components/providers/theme-provider";
import { ROUTE_DEFAULT_LOGGED_IN } from "@/lib/route_utils";
import { motion } from "framer-motion";
import BugReportModal from "../modals/bug_report_modal";
import Notifications from "../providers/notifications/notifications";
import MultifleetSelect from "@/components/navbars/multifleet_select";

const log = getLogger("ProtectedNavbar", "teal", "Navbar");

export default function ProtectedNavbar({ children }: { children?: React.ReactNode }) {

    const { setModal } = useModal();
    const { user, attemptLogOut } = useAuth();
    const { useNavbarPageNames } = useTheme();
    const { extras } = useNavbarSlot();

    const extraItems = React.useMemo(
        () => React.Children.toArray(extras).filter(Boolean),
        [extras]
    );

    const render = () => {

        log(`Rendering with user = `, user);

        const buttonLinkClass = "hover:[&_*]:underline";
        const pageNameLinkClass = (useNavbarPageNames ? "block group-hover:underline @max-[100rem]/navbar:hidden!" : "hidden!");
        const renderPageNameLink = (pageName: string) => <span className={pageNameLinkClass}>{pageName}</span>;

        return (
            <nav
                id='navbar'
                className="flex items-center justify-between p-2 pl-10 pr-4 bg-sidebar @container/navbar relative overflow-clip border-b"
            >

                {/* Left Elements */}
                <div className="flex items-center gap-14 *:z-10 relative">

                    {/* Background Shape (Edge) */}
                    <div className="
                        absolute left-0 right-0
                        aspect-square scale-138 -mr-2
                        bg-neutral-300 dark:bg-neutral-700
                        z-2
                        rotate-45 transform -translate-x-1/2
                    ">
                    </div>

                    {/* Background Shape */}
                    <div className="
                        absolute left-0 right-0
                        aspect-square scale-138
                        bg-neutral-200 dark:bg-neutral-800
                        z-5
                        rotate-45 transform -translate-x-1/2
                    ">
                    </div>

                    {/* Navbar Brand & Home Link */}
                    <Link className="font-semibold text-xl hover:underline decoration-dotted decoration-ring" to={ROUTE_DEFAULT_LOGGED_IN}>
                        NGAFID
                    </Link>

                    {/* Multifleet Select */}
                    <MultifleetSelect />

                    {/* Action Button */}
                    <div className="flex items-center ml-auto gap-8">

                        {/* Notifications */}
                        <Notifications />

                        {/* Command Menu */}
                        <CommandMenu />

                        {/* Dark Mode Toggle Button */}
                        <DarkModeToggle />

                    </div>

                    {/* Child Elements */}
                    <div className="flex items-center gap-2">
                        {
                            extraItems.map((child, index) => {

                                const key = (React.isValidElement(child) && child.key != null)
                                    ? child.key
                                    : `navbar-child-${index}`;

                                return (
                                    <motion.div
                                        className="gap-2"
                                        key={key}
                                        initial={{ opacity: 0 }}
                                        animate={{ opacity: 1 }}
                                        transition={{ duration: 0.5, delay: 0.05 * index }}
                                    >
                                        {child}
                                    </motion.div>
                                );

                            })
                        }
                    </div>

                </div>

                {/* Right Elements */}
                <div className="flex items-center gap-8">

                    {/* Summary */}
                    <Button asChild variant="ghost" className={buttonLinkClass}>
                        <Link to="/protected/summary">
                            <Home />
                            {renderPageNameLink("Summary")}
                        </Link>
                    </Button>

                    {/* Status */}
                    <Button asChild variant="ghost" className={buttonLinkClass}>
                        <Link to="/protected/status">
                            <Info />
                            {renderPageNameLink("Status")}
                        </Link>
                    </Button>

                    {/* Events */}
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className={buttonLinkClass}>
                                <CalendarCog />
                                {renderPageNameLink("Events")}
                                <ChevronDown />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>

                            {/* Event Analysis */}
                            <DropdownMenuItem asChild>
                                <Link to="/protected/trends">
                                    Trends
                                </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/severities">
                                    Severities
                                </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/heat_map">
                                    Heat Map
                                </Link>
                            </DropdownMenuItem>

                            {/* Event Info */}
                            <DropdownMenuSeparator />
                            <DropdownMenuItem asChild>
                                <Link to="/protected/statistics">
                                    Statistics
                                </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/event_definitions">
                                    Definitions
                                </Link>
                            </DropdownMenuItem>

                        </DropdownMenuContent>
                    </DropdownMenu>

                    {/* Analysis */}
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className={buttonLinkClass}>
                                <Search />
                                {renderPageNameLink("Analysis")}
                                <ChevronDown />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/turn_to_final">
                                    Turn to Final
                                </Link>
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>

                    {/* Flights */}
                    <Button asChild variant="ghost" className={buttonLinkClass}>
                        <Link to="/protected/flights">
                            <Plane />
                            {renderPageNameLink("Flights")}
                        </Link>
                    </Button>

                    {/* Uploads */}
                    <Button asChild variant="ghost" className={buttonLinkClass}>
                        <Link to="/protected/uploads">
                            <Upload />
                            {renderPageNameLink("Uploads")}
                        </Link>
                    </Button>

                    {/* Account */}
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className={buttonLinkClass}>
                                <User />
                                {renderPageNameLink("Account")}
                                <ChevronDown />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>

                            {/* User Name */}
                            <DropdownMenuLabel>
                                {
                                    (user?.email)
                                        ?
                                        user?.email
                                        :
                                        <i>
                                            Unknown User
                                        </i>
                                }
                            </DropdownMenuLabel>
                            <DropdownMenuSeparator />

                            {/* Fleet Management */}
                            <DropdownMenuItem asChild>
                                <Link to="/protected/manage_fleet">
                                    Manage Fleet
                                </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/system_ids">
                                    Manage Tail Numbers
                                </Link>
                            </DropdownMenuItem>

                            {/* Account Management */}
                            <DropdownMenuSeparator />
                            <DropdownMenuItem asChild>
                                <Link to="/protected/preferences">
                                    My Preferences
                                </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/update_profile">
                                    Update Profile
                                </Link>
                            </DropdownMenuItem>

                            {/* Other */}
                            <DropdownMenuSeparator />
                            <DropdownMenuItem asChild>
                                <Button
                                    className="w-full items-center justify-start"
                                    variant="ghost"
                                    onClick={() => setModal(BugReportModal, { user: user! })}
                                >
                                    Report a Bug
                                </Button>
                            </DropdownMenuItem>

                            {/* Log Out */}
                            <DropdownMenuSeparator />
                            <DropdownMenuItem asChild>
                                <button onClick={attemptLogOut} className="button-generic w-full text-left">
                                    Log Out
                                </button>
                            </DropdownMenuItem>

                        </DropdownMenuContent>
                    </DropdownMenu>

                </div>

            </nav>
        );
    };

    return render();

}