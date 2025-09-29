// ngafid-frontend/src/app/main.tsx
import React, { JSX } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider } from '@/components/theme-provider';
import { ModalProvider } from '@/components/modals/modal_provider';
import { AuthProvider, RequireAuth } from '@/auth';
import { TimeHeaderProvider } from '@/components/time_header/time_header_provider';
import { AirframesProvider } from '@/airframes_provider';
import NotFound from '@/pages/page_missing/page_missing';


//Import CSS
import '@/index.css';
import { TooltipProvider } from '@radix-ui/react-tooltip';
import Background from './components/background';

//Verify that Tailwind was imported correctly
const tailwindLoaded = !!document.querySelector('style[data-vite-dev-id*="src/app/index.css"]')
console.log("Tailwind CSS loaded:", tailwindLoaded);





//Define application routes
class RouteData {

    urlPath: string;
    componentPath: string;
    component: React.LazyExoticComponent<() => JSX.Element>;


    constructor (urlPath:string, componentPath: string) {
        this.urlPath = urlPath;
        this.componentPath = componentPath;

        this.component = React.lazy(() => import(/* @vite-ignore */ componentPath));
    }
}

//Define all NON-PROTECTED routes here (URL first, then component path)
const routes: RouteData[] = [
    new RouteData("/", "./pages/welcome/welcome"),
    new RouteData("/status", "./pages/status/status"),
    new RouteData("/access_denied", "./pages/access_denied/access_denied"),
];

//Define all PROTECTED routes here (URL first, then component path. Will automatically prepend /protected)
const routesProtected: RouteData[] = [
    new RouteData("/summary", "./pages/summary/summary"),
    new RouteData("/waiting", "./pages/waiting/waiting"),
    new RouteData("/event_definitions", "./pages/event_definitions/event_definitions"),
];

//Automatically create route elements from the routes defined above
const routeElements = routes.map((route) => (
    <Route key={route.urlPath} path={route.urlPath} element={React.createElement(route.component)} />
));
const routeElementsProtected = routesProtected.map((route) => (
    <Route key={route.urlPath} path={`/protected${route.urlPath}`} element={React.createElement(route.component)} />
));


export const ROUTE_BASE = "";
export const ROUTE_DEFAULT_LOGGED_OUT = `${ROUTE_BASE}/`;
export const ROUTE_DEFAULT_LOGGED_IN = `${ROUTE_BASE}/protected/summary`;


console.log("Main.tsx loaded...");


export function openRoute(url: string, isProtected: boolean = false) {

    let targetURLFull: string;

    if (isProtected)
        targetURLFull = `${ROUTE_BASE}/protected/${url}`;
    else
        targetURLFull = `${ROUTE_BASE}/${url}`;

    console.log(`Navigating to ${targetURLFull} (Protected: ${isProtected})`);

    window.location.replace(targetURLFull);

}


//Provider composition
type ProviderEntry<P = any> =
    [React.ComponentType<React.PropsWithChildren<P>>, P];

const providerTree: ProviderEntry[] = [
    [ThemeProvider, { defaultTheme: "dark", storageKey: "theme" }],
    [TooltipProvider, {}],
    [ModalProvider, {}],
    [AuthProvider, {}],
    [AirframesProvider, {}],
    [TimeHeaderProvider, {}],
];

//Helper to nest providers in order
const AppProviders: React.FC<React.PropsWithChildren> = ({ children }) => {

    return providerTree.reduceRight<React.ReactNode>((acc, [Comp, props]) => {
        return React.createElement(Comp, props, acc);
    }, children) as JSX.Element;
    
};



// ---- App content that should live inside providers ----
const AppShell = () => (
    <>
        <Background />
        <main>
                <React.Suspense fallback={null}>
                    <Routes>
                        {routeElements}
                        <Route element={<RequireAuth />}>
                            {routeElementsProtected}
                        </Route>
                        <Route path="*" element={<NotFound />} />
                    </Routes>
                </React.Suspense>
        </main>
    </>
);

//The global React root doesn't already exist, create it
if (!window.reactRoot) {

    const rootElement = document.getElementById("root");
    if (!rootElement)
        throw new Error("#root not found");

    window.reactRoot = createRoot(rootElement);

}

window.reactRoot.render(
    <BrowserRouter basename={ROUTE_BASE}>
        <AppProviders>
            <AppShell />
        </AppProviders>
    </BrowserRouter>
);