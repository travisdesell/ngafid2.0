// src/app/components/layouts/welcome_layout.tsx
import WelcomeNavbar from "@/components/navbars/welcome_navbar";
import { Outlet } from "react-router-dom";

export default function WelcomeLayout() {
    return (
        <>
            <WelcomeNavbar />
            <Outlet />
        </>
    );
}