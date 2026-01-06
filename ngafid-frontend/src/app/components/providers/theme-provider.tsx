// ngafid-frontend/src/app/components/providers/theme-provider.tsx
import { getLogger } from "@/components/providers/logger";
import { createContext, useContext, useEffect, useState } from "react";

const log = getLogger("ThemeProvider", "fuchsia", "Provider");

type Theme = "dark" | "light" | "system"

type ThemeProviderProps = {
    children: React.ReactNode
    defaultTheme?: Theme
    storageKey?: string
}

type ThemeProviderState = {
    theme: Theme
    setTheme: (theme: Theme) => void,
    useHighContrastCharts: boolean, setUseHighContrastCharts: (useHighContrast: boolean) => void,
    useBackgroundImage: boolean, setUseBackgroundImage: (useBackgroundImage: boolean) => void,
    useNavbarPageNames: boolean, setUseNavbarPageNames: (useNavbarPageNames: boolean) => void,
}

const initialState: ThemeProviderState = {
    theme: "system",
    setTheme: () => null,
    useHighContrastCharts: false, setUseHighContrastCharts: () => null,
    useBackgroundImage: true, setUseBackgroundImage: () => null,
    useNavbarPageNames: true, setUseNavbarPageNames: () => null,
}

const ThemeProviderContext = createContext<ThemeProviderState>(initialState)

export function ThemeProvider({
    children,
    defaultTheme = "system",
    storageKey = "vite-ui-theme",
    ...props
}: ThemeProviderProps) {

    const themeDefault = (localStorage.getItem(storageKey) as Theme) || defaultTheme;
    const [theme, setTheme] = useState<Theme>(themeDefault);
    const [useHighContrastCharts, setUseHighContrastCharts] = useState<boolean>(false);
    const [useBackgroundImage, setUseBackgroundImage] = useState<boolean>(true);
    const [useNavbarPageNames, setUseNavbarPageNames] = useState<boolean>(true);

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
        setTheme: (theme: Theme) => {
            localStorage.setItem(storageKey, theme)
            setTheme(theme)
        },
        useHighContrastCharts, setUseHighContrastCharts,
        useBackgroundImage, setUseBackgroundImage,
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
}