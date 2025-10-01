// ngafid-frontend/src/app/components/navbars/home_navbar.jsx
import React from "react";

import {Button} from "@/components/ui/button";

import {DarkModeToggle} from "@/dark_mode_toggle";
import { LogIn, UserPlus } from 'lucide-react';

import { Link } from "react-router-dom";

import LoginModal from '../modals/login_modal';
import RegisterModal from '../modals/register_modal';
import { useModal } from "../modals/modal_provider";


export default function HomeNavbar() {

    const { setModal } = useModal();

    const attemptLogIn = () => {
        console.log("Showing login modal...");
        setModal(LoginModal);
    };
    const attemptRegister = () => {
        console.log("Showing register modal...");
        setModal(RegisterModal);
    }

    const render = () => {

        return (
            <nav
                id='navbar'
                className="navbar navbar-expand-lg navbar-light flex! flex-row! items-center justify-between! p-2 px-4 bg-(--sidebar)"
                style={{zIndex: "999", opacity: "1.0"}}
            >

                {/* Left Elements */}
                <div>
                    
                    {/* Navbar Brand & Home Link */}
                    <Link className="font-semibold text-xl" to="/">
                        NGAFID
                    </Link>
                    
                </div>

                {/* Right Elements */}
                <div className="flex flex-row items-center justify-end gap-8">

                    {/* Login */}
                    <Button asChild variant="ghost" onClick={attemptLogIn}>
                        <Link to="/#login">
                            <LogIn/>
                            Login
                        </Link>
                    </Button>

                    {/* Register */}
                    <Button asChild variant="ghost" onClick={attemptRegister}>
                        <Link to="/#register">
                            <UserPlus/>
                            Register
                        </Link>
                    </Button>

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