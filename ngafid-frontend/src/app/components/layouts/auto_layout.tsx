// src/app/components/layouts/auto_navbar_layout.tsx
import ProtectedNavbar from "@/components/navbars/protected_navbar";
import WelcomeNavbar from "@/components/navbars/welcome_navbar";
import { useAuth } from "@/components/providers/auth_provider";
import { LoaderCircle } from "lucide-react";
import { Outlet } from "react-router-dom";

/*
    Conditionally selects the
    appropriate navbar based on
    whether the user is logged in.

    Useful for routes that are
    accessible independent of
    whether the user is logged in
    (e.g. the Status page).
*/

export default function AutoLayout() {

    const { loading, isLoggedIn } = useAuth();

    if (loading)
        return <LoaderCircle className="animate-spin fixed top-1/2 left-1/2" size={48} />;

    return (
        <>
            {
                (isLoggedIn())
                ? <ProtectedNavbar />
                : <WelcomeNavbar />
            }
            <Outlet />
        </>
    );
    
}
