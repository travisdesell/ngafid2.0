// ngafid-frontend/src/app/main.tsx
import { ModalOutlet, ModalProvider } from '@/components/modals/modal_provider';
import { AirframesProvider } from '@/components/providers/airframes_provider';
import { AuthProvider, RequireAuth } from '@/components/providers/auth_provider';
import { FlightFiltersProvider } from '@/components/providers/flight_filters_provider';
import { ThemeProvider } from '@/components/providers/theme-provider';
import NotFound from '@/pages/page_missing/page_missing';
import React, { JSX } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { NotificationsProvider } from './components/providers/notifications/notifications_provider';
import { TimeHeaderProvider } from './components/providers/time_header/time_header_provider';


// Import CSS
import AutoLayout from '@/components/layouts/auto_layout';
import ProtectedLayout from '@/components/layouts/protected_layout';
import { getLogger } from '@/components/providers/logger';
import { SystemIdsProvider } from '@/components/providers/system_ids_provider/system_ids_provider';
import { TagsProvider } from '@/components/providers/tags/tags_provider';
import '@/index.css';
import { ROUTE_BASE } from '@/lib/route_utils';
import { TooltipProvider } from '@radix-ui/react-tooltip';
import Background from './components/background';


const log = getLogger("Main", "white", "Main");

// Verify that Tailwind was imported correctly
const tailwindLoaded = !!document.querySelector('style[data-vite-dev-id*="src/app/index.css"]')
log("Tailwind CSS loaded:", tailwindLoaded);





// Define application routes
class RouteData {
    urlPath: string;
    componentPath?: string;
    component: React.LazyExoticComponent<() => JSX.Element>;

    constructor(
        urlPath: string,
        moduleOrPath: string | (() => Promise<{ default: React.ComponentType<any> }>)
    ) {
        this.urlPath = urlPath;

        // Path is a string, use dynamic import with React.lazy
        if (typeof moduleOrPath === "string") {
            this.componentPath = moduleOrPath;
            this.component = React.lazy(() => import(/* @vite-ignore */ moduleOrPath));

        // Otherwise, it's a direct module import
        } else {
            this.componentPath = undefined;
            this.component = React.lazy(moduleOrPath as any);
        }

    }

}




/*
    Automatically discover pages in the ngafid-frontend/src/app/pages
    directory and create routes for them.

    Files in the /protected subdirectory will be protected routes.

    Other files will be public routes.
*/
const routes: RouteData[] = [];
const routesProtected: RouteData[] = [];
const routesAuto: RouteData[] = [];

function segmentizeParams(params: string) {

    /*
        Convert Next.js style params to React Router style params:

        [id] -> :id
        [...rest] -> *
    */

    // Catch-all first: [...rest] -> *
    params = params.replace(/\[\.{3}([^\]]+)\]/g, "*");

    // Normal params: [id] -> :id
    params = params.replace(/\[([^\]]+)\]/g, ":$1");

    return params;

}

function isPageCandidate(file: string) {

    /*
        Ignore some files that are not pages
        that might end up in the pages directory.
    */

    //Has leading underscore, ignore
    if (/\/_/.test(file))
        return false;

    //In certain directories, ignore
    if (/\/(components|ui|shared|hooks|lib)\//.test(file))
        return false;

    //In __tests__ directory, ignore
    if (/\/__tests__\//.test(file))
        return false;

    //Is a test/spec file, ignore
    if (/\.(test|spec)\.(t|j)sx$/.test(file))
        return false;

    return true;

}

function collapseFolderNamedFile(relNoExt: string) {

    /*
        Convert folder-named files to the folder route:

        /x/x -> /x
        /x/index -> /x
        /x/y/x -> /x/y
        /x/y/index -> /x/y
    */

    // "/x/index" -> "/x"
    relNoExt = relNoExt.replace(/\/index$/, "");

    // Also handle "x/x" at any depth ("", "nested/x/x" -> "", "nested/x")
    relNoExt = relNoExt.replace(/(^|\/)([^/]+)\/\2$/, "$1$2");

    return relNoExt;

}

type RouteInfo = {
    url: string;
    isProtected: boolean;
    isAuto: boolean;
};
function fileToRoute(filePath: string): RouteInfo {

    /*
        Convert a file path to a route URL and determine
        if it's protected.
    */

    let rel = filePath.replace(/^\.\//, "");   // "pages/..."
    rel = rel.replace(/^pages\//, "");         //<-- Drop "pages/"

    const isProtected = rel.startsWith("protected/");
    if (isProtected)
        rel = rel.slice("protected/".length);

    const isAuto = rel.startsWith("auto/");
    if (isAuto)
        rel = rel.slice("auto/".length);

    rel = rel.replace(/\.(t|j)sx$/, "");       //<-- Strip extension
    rel = collapseFolderNamedFile(rel);

    // Build a clean URL (avoid accidental "//")
    let url = "/" + rel.replace(/^\/+/, "");
    url = segmentizeParams(url);

    // Explicit root
    if (url === "/")
        url = "/";

    return { url, isProtected, isAuto };

}

/* @ts-ignore */
const pageModules = import.meta.glob([
    "./pages/**/*.tsx",
    "!./pages/**/_*/**",
    "!./pages/**/_*.tsx",
]);

for (const [file, loader] of Object.entries(pageModules)) {

    // Target file is not a page candidate, skip
    if (!isPageCandidate(file))
        continue;

    const { url, isProtected, isAuto } = fileToRoute(file);

    // Create route data, add to appropriate list
    const routeData = new RouteData(url, loader as any);
    let routeListTarget = (isAuto)
        ? routesAuto
        : (isProtected)
            ? routesProtected
            : routes;

    routeListTarget.push(routeData);

}

// Stabilize the output (root first)
const depth = (p: string) => (p === "/" ? 0 : p.split("/").length);
routes.sort((a, b) => depth(a.urlPath) - depth(b.urlPath));
routesProtected.sort((a, b) => depth(a.urlPath) - depth(b.urlPath));
routesAuto.sort((a, b) => depth(a.urlPath) - depth(b.urlPath));

log.table("Discovered Routes:", [...routes, ...routesProtected, ...routesAuto].map(r => ({
    url: r.urlPath,
    protected: routesProtected.includes(r),
    auto: routesAuto.includes(r),
    componentPath: r.componentPath || "Direct Import",
})));

// Automatically create route elements from the routes defined above
const routeElements = routes.map((route) => (
    <Route key={route.urlPath} path={route.urlPath} element={React.createElement(route.component)} />
));
const routeElementsProtected = routesProtected.map((route) => (
    <Route key={route.urlPath} path={`/protected${route.urlPath}`} element={React.createElement(route.component)} />
));
const routeElementsAuto = routesAuto.map((route) => (
    <Route key={route.urlPath} path={`/auto${route.urlPath}`} element={React.createElement(route.component)} />
));





// Provider composition
type ProviderEntry<P = any> =
    [React.ComponentType<React.PropsWithChildren<P>>, P];

const providerTree: ProviderEntry[] = [
    [ThemeProvider, { defaultTheme: "dark", storageKey: "theme" }],
    [TooltipProvider, {}],
    [ModalProvider, {}],
    [AuthProvider, {}],
    [TagsProvider, {}],
    [AirframesProvider, {}],
    [SystemIdsProvider, {}],
    [TimeHeaderProvider, {}],
    [NotificationsProvider, {}],
    [FlightFiltersProvider, {}],
];

// Helper to nest providers in order
const AppProviders: React.FC<React.PropsWithChildren> = ({ children }) => {

    return providerTree.reduceRight<React.ReactNode>((acc, [Comp, props]) => {
        return React.createElement(Comp, props, acc);
    }, children) as JSX.Element;

};



// App content that lives inside the providers
const AppShell = () => (
    <>
        <Background />
        <main className="min-h-0 overflow-hidden">
            <Routes>

                {/* Public Routes */}
                {routeElements}
                <Route path="/" element={<Navigate to="/welcome" replace />} />

                {/* Auto Routes (Valid regardless of authentication) */}
                <Route element={<AutoLayout />}>
                    {routeElementsAuto}
                    <Route path="/auto" element={<Navigate to="/welcome" replace />} />
                </Route>

                {/* Protected Routes */}
                <Route element={<RequireAuth />}>
                    <Route element={<ProtectedLayout />}>
                        {routeElementsProtected}
                        <Route path="/protected" element={<Navigate to="/protected/summary" replace />} />
                    </Route>
                </Route>

                {/* 404 */}
                <Route path="*" element={<NotFound />} />

            </Routes>
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

log("Finished initializing main.tsx, rendering React tree...");

window.reactRoot.render(
    <BrowserRouter basename={ROUTE_BASE}>
        <AppProviders>
            <AppShell />
            <ModalOutlet />
        </AppProviders>
    </BrowserRouter>
);

log("React tree rendered!");