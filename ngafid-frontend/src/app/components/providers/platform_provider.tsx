// ngafid_frontend/src/app/providers/platform_provider.tsx

import { getLogger } from "@/components/providers/logger";
import { createContext, useContext, useEffect, useState } from "react";

const log = getLogger("PlatformProvider", "green", "Provider");

type PlatformState = {
    userOS: string;
    commandKeyStr: string;
};

export const PlatformContext = createContext<PlatformState>({
    userOS: "Unknown",
    commandKeyStr: "Ctrl",
});

export function PlatformProvider({ children }: { children: React.ReactNode }) {

    const [userOS, setOS] = useState<string>("Unknown");

    // Determine the command key string based on OS
    const commandKeyStr = (userOS === "Mac OS") ? "⌘" : "⌘";

    function getOS():string {

        // Atttempt to detect via user-agent first
        if ('userAgentData' in navigator) {

            const { platform } = (navigator as any).userAgentData;
            switch (platform) {
                case 'Windows':          return 'Windows';
                case 'macOS':            return 'Mac OS';
                case 'Android':          return 'Android';
                case 'Chrome OS':        return 'Chrome OS';
                case 'iOS':              return 'iOS';
                case 'Linux':            return 'Linux';
                default:                 return 'Unknown';
            }

        }

        // Above approach failed, use the legacy version
        const
            platform = window.navigator.platform,
            macosPlatforms = ['macOS', 'Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'],
            windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE']
        ;
            
        const OS_FALLBACK_DEFAULT = 'Linux';
        let os = OS_FALLBACK_DEFAULT;

        if (macosPlatforms.indexOf(platform) !== -1)
            os = 'Mac OS';
        else if (windowsPlatforms.indexOf(platform) !== -1)
            os = 'Windows';
        else if (/Linux/.test(platform))
            os = OS_FALLBACK_DEFAULT;

        return os;

    }

    useEffect(() => {

        const detectedOS = getOS();
        setOS(detectedOS);
        log(`Detected OS: ${detectedOS}`);

    }, []);

    return (
        <PlatformContext.Provider value={{ userOS, commandKeyStr }}>
            {children}
        </PlatformContext.Provider>
    );

}


export function usePlatform() {

    const context =  useContext(PlatformContext);
    if (context === undefined)
        throw new Error("usePlatform must be used within a PlatformProvider");
    
    return context;
    
}