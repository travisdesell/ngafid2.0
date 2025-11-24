// ngafid-frontend/src/app/components/providers/auth_provider.tsx
import ErrorModal, { ModalDataError } from '@/components/modals/error_modal';
import { useModal } from '@/components/modals/modal_context';
import { getLogger } from '@/components/providers/logger';
import { LoaderCircle } from 'lucide-react';
import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { NGAFIDUser } from 'src/types';

// const log = getLogger({ color: "blue",  type: "Provider" });
const log = getLogger("AuthProvider", "blue", "Provider");

type IsLoggedInFn = () => boolean;
type AuthState = {
    loading: boolean;
    user: NGAFIDUser|null,
};

type AuthContextValue = AuthState & { isLoggedIn: IsLoggedInFn };

const AuthContext = React.createContext<AuthContextValue>({
    loading: true,
    user: null,
    isLoggedIn: () => false,
});



export function AuthProvider({ children }: { children: React.ReactNode }) {
    
    const { setModal } = useModal();

    const [state, setState] = React.useState<AuthState>({
        loading: true,
        user: null,
    });
    const isLoggedIn = () => (state.user !== null);


    React.useEffect(() => {

        log("Checking authentication status...");

        fetch('/api/user/me', {
            credentials: 'include'
        })
        .then(async (r) => (
            r.ok
                ? setState({ loading: false, user: await r.json() })
                : setState({ loading: false, user: null })
            )
        )
        .catch((error) => {
            setState({ loading: false, user: null });
            setModal(ErrorModal, {title : "Error during login submission", message: error.toString()} as ModalDataError);
        });

    }, []);


    const render = () => {

        log("Rendering, loading =", state.loading, ", user =", state.user);

        return (
            <AuthContext.Provider value={{ ...state, isLoggedIn }}>
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
