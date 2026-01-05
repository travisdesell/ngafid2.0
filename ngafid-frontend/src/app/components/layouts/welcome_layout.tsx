// src/app/components/layouts/welcome_layout.tsx
import SuspenseLoading from "@/components/modals/suspense_loading";
import WelcomeNavbar from "@/components/navbars/welcome_navbar";
import React from "react";
import { Outlet } from "react-router-dom";

export default function WelcomeLayout() {
    return (
        <>
            <WelcomeNavbar />
            <React.Suspense fallback={SuspenseLoading()}>
                <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
                    <Outlet />
                </div>
            </React.Suspense>
        </>
    );
}