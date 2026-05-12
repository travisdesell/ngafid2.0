// ngafid-frontend/src/app/components/providers/auth_provider.tsx
import ErrorModal, { ModalDataError } from '@/components/modals/error_modal';
import { useModal } from '@/components/modals/modal_context';
import { getLogger } from '@/components/providers/logger';
import { fetchJson } from '@/fetchJson';
import { ROUTE_DEFAULT_LOGGED_OUT } from '@/lib/route_utils';
import { LoaderCircle } from 'lucide-react';
import React, { useCallback, useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Fleet, FleetAccess, NGAFIDUser } from 'src/types';

// const log = getLogger({ color: "blue",  type: "Provider" });
const log = getLogger("AuthProvider", "blue", "Provider");

type IsLoggedInFn = () => boolean
type AttemptLogOutFn = () => void;
type AuthState = {
    loading: boolean;
    user: NGAFIDUser|null,
    fleetLoading: boolean;
    fleetLoaded: boolean;
};

type AuthContextValue = AuthState & {
    isLoggedIn: IsLoggedInFn,
    attemptLogOut: AttemptLogOutFn,
    refreshFleetData: () => Promise<void>,
};

const AuthContext = React.createContext<AuthContextValue>({
    loading: true,
    user: null,
    fleetLoading: false,
    fleetLoaded: false,
    isLoggedIn: () => false,
    attemptLogOut: () => {},
    refreshFleetData: async () => {},
});



export function AuthProvider({ children }: { children: React.ReactNode }) {
    
    const { setModal } = useModal();

    const [state, setState] = React.useState<AuthState>({
        loading: true,
        user: null,
        fleetLoading: false,
        fleetLoaded: false,
    });
    const isLoggedIn = () => (state.user !== null);
    const attemptLogOut = () => {

        log("Logging out...");

        fetch("/api/auth/logout", {
            method: "POST",
            credentials: "include"
        }).then((response) => {

            if (!response.ok)
                setModal(ErrorModal, { title: "Error", message: "An error occurred while logging out. Please try again." });

        }).catch((error) => {
            setModal(ErrorModal, { title: "Error", message: error.toString() });
        }).finally(() => {
            window.location.assign(ROUTE_DEFAULT_LOGGED_OUT);
        });

    }

    const refreshFleetData = useCallback(async () => {

        log("Refreshing auth fleet data...");
        setState((prev) => ({ ...prev, fleetLoading: true }));

        try {

            const [fleet, fleetAccess] = await Promise.all([
                fetchJson.get<Fleet>("/api/fleet"),
                fetchJson.get<FleetAccess[]>("/api/user/fleet-access"),
            ]);

            setState((prev) => {

                if (!prev.user)
                    return { ...prev, fleetLoading: false, fleetLoaded: true };

                return {
                    ...prev,
                    fleetLoading: false,
                    fleetLoaded: true,
                    user: {
                        ...prev.user,
                        fleet,
                        fleetAccess,
                    },
                };

            });

        } catch (error) {

            setState((prev) => ({ ...prev, fleetLoading: false }));
            setModal(ErrorModal, { title: "Error fetching fleet information", message: String(error) });

        }

    }, [setModal]);


    useEffect(() => {

        log("Checking authentication status...");

        fetch('/api/user/me', {
            credentials: 'include'
        })
        .then(async (r) => {

            if (!r.ok) {
                setState({ loading: false, user: null, fleetLoading: false, fleetLoaded: false });
                return;
            }

            const user = await r.json() as NGAFIDUser;
            setState({
                loading: false,
                fleetLoading: false,
                fleetLoaded: Boolean(user.fleet && user.fleetAccess?.length),
                user: {
                    ...user,
                    fleet: user.fleet ?? null,
                    fleetAccess: user.fleetAccess ?? [],
                },
            });

        })
        .catch((error) => {
            setState({ loading: false, user: null, fleetLoading: false, fleetLoaded: false });
            setModal(ErrorModal, {title : "Error during login submission", message: error.toString()} as ModalDataError);
        });

        log("Finished checking authentication status: loading =", state.loading, ", user =", state.user);

    }, []);


    useEffect(() => {

        // No user, exit
        if (!state.user)
            return;

        // User already has fleet data, exit
        if (state.fleetLoaded || state.fleetLoading)
            return;

        void refreshFleetData();

    }, [state.user, state.fleetLoaded, state.fleetLoading, refreshFleetData]);

    const render = () => {

        log("Rendering, loading =", state.loading, ", user =", state.user);

        return (
            <AuthContext.Provider value={{ ...state, isLoggedIn, attemptLogOut, refreshFleetData }}>
                {children}
            </AuthContext.Provider>
        );

    };

    return render();

}


export function useAuth() {
    return React.useContext(AuthContext);
}


export function RequireAuth() {
    
    const { loading, user } = useAuth();
    const location = useLocation();

    //Loading, show loading spinner
    if (loading)
        return <LoaderCircle className="animate-spin fixed top-1/2 left-1/2" size={48} />;

    //Not logged in, redirect to home page
    if (!user) {
        log.warn("Auth: Not logged in, redirecting to /access_denied");
        return <Navigate to="/access_denied" replace state={{ from: location }} />;
    }

    //Logged in, show the requested page
    log("Logged in, showing protected page");
    return <Outlet />;

}
