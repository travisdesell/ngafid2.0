// ngafid-frontend/src/app/components/providers/theme-provider.tsx
import { getLogger } from "@/components/providers/logger";
import { createContext, useContext, useEffect, useState } from "react";
import { useLocalStorage } from "@uidotdev/usehooks";


/*

    NOTE: Theme fields are currently modified
    by the user via the dark_mode_toggle.tsx
    component, which could probably use a
    better name than that.

*/

const log = getLogger("ThemeProvider", "fuchsia", "Provider");

type Theme = "dark" | "light" | "system"

interface ThemeProviderProps {
    children: React.ReactNode
    defaultTheme?: Theme
    storageKey?: string
}

interface ThemeProviderState {
    theme: Theme
    setTheme: (theme: Theme) => void,
    useHighContrastCharts: boolean, setUseHighContrastCharts: (useHighContrast: boolean) => void,
    invertBackgroundImage: boolean, setInvertBackgroundImage: (invertBackgroundImage: boolean) => void,
    blurBackgroundImage: boolean, setBlurBackgroundImage: (blurBackgroundImage: boolean) => void,
    useNavbarPageNames: boolean, setUseNavbarPageNames: (useNavbarPageNames: boolean) => void,
}

const initialState: ThemeProviderState = {
    theme: "system",
    setTheme: () => null,
    useHighContrastCharts: false, setUseHighContrastCharts: () => null,
    invertBackgroundImage: true, setInvertBackgroundImage: () => null,
    blurBackgroundImage: false, setBlurBackgroundImage: () => null,
    useNavbarPageNames: true, setUseNavbarPageNames: () => null,
};

const ThemeProviderContext = createContext<ThemeProviderState>(initialState);

export function ThemeProvider({
    children,
    defaultTheme = "system",
    ...props
}: ThemeProviderProps) {

    const [theme, setTheme] = useLocalStorage<Theme>("ngafid-theme", "light");
    const [useHighContrastCharts, setUseHighContrastCharts] = useLocalStorage<boolean>("ngafid-use-high-contrast-charts", true);
    const [invertBackgroundImage, setInvertBackgroundImage] = useLocalStorage<boolean>("ngafid-invert-background-image", false);
    const [blurBackgroundImage, setBlurBackgroundImage] = useLocalStorage<boolean>("ngafid-blur-background-image", true);
    const [useNavbarPageNames, setUseNavbarPageNames] = useLocalStorage<boolean>("ngafid-use-navbar-page-names", true);

    useEffect(() => {

        const root = window.document.documentElement;

        const systemTheme = (window.matchMedia("(prefers-color-scheme: dark)").matches)
            ? "dark"
            : "light";

        const resolved = (theme === "system")
            ? systemTheme
            : theme;

        root.classList.remove("light", "dark");
        root.classList.add(resolved);

        root.style.colorScheme = resolved;

        log("Theme set to:", resolved);
        
    }, [theme]);


    const value = {
        theme,
        setTheme,
        useHighContrastCharts, setUseHighContrastCharts,
        invertBackgroundImage, setInvertBackgroundImage,
        blurBackgroundImage, setBlurBackgroundImage,
        useNavbarPageNames, setUseNavbarPageNames,
    };

    return (
        <ThemeProviderContext.Provider {...props} value={value}>
            {children}
        </ThemeProviderContext.Provider>
    );
}

export const useTheme = () => {
    const context = useContext(ThemeProviderContext);

    if (context === undefined)
        throw new Error("useTheme must be used within a ThemeProvider");

    return context;
};