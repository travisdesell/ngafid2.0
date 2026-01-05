// src/app/components/layouts/protected_layout.tsx
import SuspenseLoading from "@/components/modals/suspense_loading";
import { NavbarSlotProvider } from "@/components/navbars/navbar_slot";
import ProtectedNavbar from "@/components/navbars/protected_navbar";
import React from "react";
import { Outlet } from "react-router-dom";

export default function ProtectedLayout() {
    return (
        <NavbarSlotProvider>
            <div className="flex flex-col h-dvh min-h-0 overflow-hidden">
                <ProtectedNavbar />
                <React.Suspense fallback={SuspenseLoading()}>
                    <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
                        <Outlet />
                    </div>
                </React.Suspense>
            </div>
        </NavbarSlotProvider>
    );
}
