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


// Import CSS
import '@/index.css';
import { TooltipProvider } from '@radix-ui/react-tooltip';
import Background from './components/background';



// Verify that Tailwind was imported correctly
const tailwindLoaded = !!document.querySelector('style[data-vite-dev-id*="src/app/index.css"]')
console.log("Tailwind CSS loaded:", tailwindLoaded);





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

function fileToRoute(filePath: string) {

    /*
        Convert a file path to a route URL and determine
        if it's protected.
    */

    let rel = filePath.replace(/^\.\//, "");   // "pages/..."
    rel = rel.replace(/^pages\//, "");         //<-- Drop "pages/"

    const isProtected = rel.startsWith("protected/");
    if (isProtected)
        rel = rel.slice("protected/".length);

    rel = rel.replace(/\.(t|j)sx$/, "");       //<-- Strip extension
    rel = collapseFolderNamedFile(rel);

    // Build a clean URL (avoid accidental "//")
    let url = "/" + rel.replace(/^\/+/, "");
    url = segmentizeParams(url);

    // Explicit root
    if (url === "/")
        url = "/";

    return { url, isProtected };

}

/* @ts-ignore */
const pageModules = import.meta.glob("./pages/**/*.tsx");   //<-- Lazy import all page modules

for (const [file, loader] of Object.entries(pageModules)) {

    // Target file is not a page candidate, skip
    if (!isPageCandidate(file))
        continue;

    const { url, isProtected } = fileToRoute(file);

    // Create route data, add to appropriate list
    const routeData = new RouteData(url, loader as any);
    (isProtected ? routesProtected : routes).push(routeData);

}

// Stabilize the output (root first)
const depth = (p: string) => (p === "/" ? 0 : p.split("/").length);
routes.sort((a, b) => depth(a.urlPath) - depth(b.urlPath));
routesProtected.sort((a, b) => depth(a.urlPath) - depth(b.urlPath));

// Automatically create route elements from the routes defined above
const routeElements = routes.map((route) => (
    <Route key={route.urlPath} path={route.urlPath} element={React.createElement(route.component)} />
));
const routeElementsProtected = routesProtected.map((route) => (
    <Route key={route.urlPath} path={`/protected${route.urlPath}`} element={React.createElement(route.component)} />
));

export const ROUTE_BASE = "";
export const ROUTE_DEFAULT_LOGGED_OUT = `${ROUTE_BASE}/`;
export const ROUTE_DEFAULT_LOGGED_IN = `${ROUTE_BASE}/protected/summary`;

export function openRoute(url: string, isProtected: boolean = false) {

    let targetURLFull: string;

    if (isProtected)
        targetURLFull = `${ROUTE_BASE}/protected/${url}`;
    else
        targetURLFull = `${ROUTE_BASE}/${url}`;

    console.log(`Navigating to ${targetURLFull} (Protected: ${isProtected})`);

    window.location.replace(targetURLFull);

}




// Provider composition
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
        <main>
            <React.Suspense fallback={null}>
                <Routes>

                    {/* Unprotected Routes (Default to Welcome page) */}
                    {routeElements}
                    <Route path="/" element={<Navigate to="/welcome" replace />} />

                    {/* Protected Routes (Default to Summary page) */}
                    <Route element={<RequireAuth />}>
                        {routeElementsProtected}
                        <Route path="/protected" element={<Navigate to="/protected/summary" replace />} />
                    </Route>

                    {/* 404 */}
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

console.log("Finished initializing main.tsx, rendering React tree...");

window.reactRoot.render(
    <BrowserRouter basename={ROUTE_BASE}>
        <AppProviders>
            <AppShell />
        </AppProviders>
    </BrowserRouter>
);

console.log("React tree rendered!");