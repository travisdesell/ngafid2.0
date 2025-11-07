// src/app/components/layouts/auto_navbar_layout.tsx
import ProtectedLayout from "@/components/layouts/protected_layout";
import WelcomeLayout from "@/components/layouts/welcome_layout";
import { useAuth } from "@/components/providers/auth_provider";
import { LoaderCircle } from "lucide-react";

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
                ? <ProtectedLayout />
                : <WelcomeLayout />
            }
        </>
    );
    
}
