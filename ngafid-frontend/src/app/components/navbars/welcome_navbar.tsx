// ngafid-frontend/src/app/components/navbars/welcome_navbar.jsx

import { Button } from "@/components/ui/button";

import { DarkModeToggle } from "@/components/dark_mode_toggle";
import { LogIn, UserPlus } from 'lucide-react';

import { Link } from "react-router-dom";

import { useModal } from "@/components/modals/modal_context";
import { getLogger } from "@/components/providers/logger";
import LoginModal from '../modals/login_modal';
import RegisterModal from '../modals/register_modal';


const log = getLogger("WelcomeNavbar", "teal", "Navbar");


export default function WelcomeNavbar() {

    const { setModal } = useModal();

    const attemptLogIn = () => {
        log("Showing login modal...");
        setModal(LoginModal);
    };
    const attemptRegister = () => {
        log("Showing register modal...");
        setModal(RegisterModal);
    }

    const render = () => {

        return (
            <nav
                id='navbar'
                className="flex items-center justify-between p-2 pl-10 pr-4 bg-sidebar @container/navbar relative overflow-clip border-b"
            >

                {/* Left Elements */}
                <div className="relative inline-block overflow-visible">

                    {/* Background Shape (Edge) */}
                    <div className="
                        pointer-events-none
                        absolute left-0 top-1/2
                        w-[calc(100%*1.41421356+0.75rem)] aspect-square
                        -translate-x-1/2 -translate-y-1/2
                        rotate-45
                        bg-neutral-300 dark:bg-neutral-700
                        z-2
                    ">
                    </div>

                    {/* Background Shape */}
                    <div className="
                        pointer-events-none
                        absolute left-0 top-1/2
                        w-[calc(100%*1.41421356)] aspect-square
                        -translate-x-1/2 -translate-y-1/2
                        rotate-45
                        bg-neutral-200 dark:bg-neutral-800
                        z-5
                    ">
                    </div>

                    {/* Left Elements Content */}
                    <div className="relative z-10 inline-flex items-center gap-14 pr-10">

                        {/* Navbar Brand & Home Link */}
                        <Link className="font-semibold text-xl hover:underline decoration-dotted decoration-ring" to="/">
                            NGAFID
                        </Link>

                        {/* Dark Mode Toggle Button */}
                        <div className="ml-2">
                            <DarkModeToggle/>
                        </div>

                    </div>

                    
                </div>

                {/* Right Elements */}
                <div className="flex flex-row items-center justify-end gap-8">

                    {/* Login */}
                    <Button variant="ghost" onClick={attemptLogIn}>
                        <LogIn/>
                        Login
                    </Button>

                    {/* Register */}
                    <Button variant="ghost" onClick={attemptRegister}>
                        <UserPlus/>
                        Register
                    </Button>

                </div>

            </nav>
        );
    };

    return render();

}