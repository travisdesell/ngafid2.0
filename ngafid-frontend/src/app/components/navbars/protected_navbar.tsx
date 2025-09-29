// ngafid-frontend/src/app/components/navbars/protected_navbar.jsx
import React from "react";

import {Button} from "@/components/ui/button";

import {DarkModeToggle} from "../../../dark_mode_toggle";
import { CalendarCog, ChevronDown, CloudDownload, Home, Info, LogIn, Plane, Search, Upload, User, UserPlus } from 'lucide-react';

import { Link } from "react-router-dom";

import { useModal } from "../modals/modal_provider";
import { DropdownMenu, DropdownMenuItem, DropdownMenuLabel, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuSeparator} from "../ui/dropdown-menu";


import { ROUTE_DEFAULT_LOGGED_IN, ROUTE_DEFAULT_LOGGED_OUT } from "@/main";
import ErrorModal from "../modals/error_modal";
import { useAuth } from "@/auth";


export default function ProtectedNavbar() {

    const { setModal } = useModal();
    const { user } = useAuth();

    const attemptLogOut = () => {

        console.log("Logging out...");

        fetch("/api/auth/logout", {
            method: "POST",
            credentials: "include"
        }).then((response) => {

            if (!response.ok)
                setModal(ErrorModal, {title: "Error", message: "An error occurred while logging out. Please try again."} );
            
        }).catch((error) => {
            setModal(ErrorModal, {title: "Error", message: error.toString()} );
        }).finally(() => {
            window.location.assign(ROUTE_DEFAULT_LOGGED_OUT);
        });

    }

    const render = () => {

        return (
            <nav
                id='navbar'
                className="navbar navbar-expand-lg navbar-light flex! flex-row! items-center justify-between! p-2 px-4 bg-(--sidebar)"
            >

                {/* Left Elements */}
                <div>
                    
                    {/* Navbar Brand & Home Link */}
                    <Link className="font-semibold text-xl" to={ROUTE_DEFAULT_LOGGED_IN}>
                        NGAFID
                    </Link>
                    
                </div>

                {/* Right Elements */}
                <div className="flex flex-row items-center justify-end gap-8">

                    {/* Summary */}
                    <Button asChild variant="ghost">
                        <Link to="/protected/summary">
                            <Home/>
                            Summary
                        </Link>
                    </Button>

                    {/* Status */}
                    <Button asChild variant="ghost">
                        <Link to="/status">
                            <Info/>
                            Status
                        </Link>
                    </Button>

                    {/* Events */}
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost">
                                <CalendarCog/>
                                Events
                                <ChevronDown/>
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
                            <DropdownMenuSeparator/>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/event_statistics">
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
                            <Button variant="ghost">
                                <Search/>
                                Analysis
                                <ChevronDown/>
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/trends">
                                    Turn to Final
                                </Link>
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>

                    {/* Flights */}
                    <Button asChild variant="ghost">
                        <Link to="/protected/flights">
                            <Plane/>
                            Flights
                        </Link>
                    </Button>

                    {/* Imports */}
                    <Button asChild variant="ghost">
                        <Link to="/protected/imports">
                            <CloudDownload/>
                            Imports
                        </Link>
                    </Button>

                    {/* Uploads */}
                    <Button asChild variant="ghost">
                        <Link to="/protected/uploads">
                            <Upload/>
                            Uploads
                        </Link>
                    </Button>

                    {/* Account */}
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost">
                                <User/>
                                Account
                                <ChevronDown/>
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>

                            {/* User Name */}
                            <DropdownMenuLabel>
                                {user?.email}
                            </DropdownMenuLabel>
                            <DropdownMenuSeparator/>

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
                            <DropdownMenuSeparator/>
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
                            <DropdownMenuSeparator/>
                            <DropdownMenuItem asChild>
                                <Link to="/protected/bug_report">
                                    Report a Bug
                                </Link>
                            </DropdownMenuItem>

                            {/* Log Out */}
                            <DropdownMenuSeparator/>
                            <DropdownMenuItem asChild>
                                <button onClick={attemptLogOut} className="button-generic w-full text-left">
                                    Log Out
                                </button>
                            </DropdownMenuItem>

                        </DropdownMenuContent>
                    </DropdownMenu>

                    {/* Dark Mode Toggle Button */}
                    <div className="ml-2">
                        <DarkModeToggle/>
                    </div>
                </div>

            </nav>
        );
    };

    return render();

}