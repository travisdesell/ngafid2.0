// src/app/components/layouts/welcome_layout.tsx
import SuspenseLoading from "@/components/modals/suspense_loading";
import { NavbarSlotProvider } from "@/components/navbars/navbar_slot";
import WelcomeNavbar from "@/components/navbars/welcome_navbar";
import React from "react";
import { Outlet } from "react-router-dom";

export default function WelcomeLayout() {

    return (
        <NavbarSlotProvider>
            <div className="flex flex-col h-dvh min-h-0 overflow-hidden">
                <WelcomeNavbar />
                <React.Suspense fallback={SuspenseLoading()}>
                    <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
                        <Outlet />
                    </div>
                </React.Suspense>
            </div>
        </NavbarSlotProvider>
    );
    
}