// ngafid-frontend/src/app/components/dark_mode_toggle.tsx
import React from 'react';

import { useTheme } from "@/components/providers/theme-provider";
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuTrigger,
} from "@/components/ui/context-menu";
import { Moon, Sun } from 'lucide-react';
import PingHalfLeft from './pings/ping_half_left';
import PingHalfRight from './pings/ping_half_right';
import { useAuth } from './providers/auth_provider';
import { Checkbox } from './ui/checkbox';
import { Separator } from './ui/separator';


export function DarkModeToggle() {

    const didToggleThemeBefore = (localStorage.getItem("didToggleTheme") !== null);
    const didOpenThemeContextMenuBefore = (localStorage.getItem("didOpenThemeContextMenu") !== null);

    const { isLoggedIn } = useAuth();
    const {
        theme, setTheme,
        useHighContrastCharts, setUseHighContrastCharts,
        useBackgroundImage, setUseBackgroundImage,
        useNavbarPageNames, setUseNavbarPageNames,
    } = useTheme();
    const isDarkTheme = (theme === "dark");

    const [didToggle, setDidToggle] = React.useState(false);
    const [didOpenContextMenu, setDidOpenContextMenu] = React.useState(false);


    /*
        Side effects to store whether user has toggled theme
        or opened context menu before.

        Used to hide the ping animation prompts after first use.
    */
    React.useEffect(() => {
        if (didToggle && !didToggleThemeBefore)
            localStorage.setItem("didToggleTheme", "true");
    }, [didToggle, didToggleThemeBefore]);
    React.useEffect(() => {
        if (didOpenContextMenu && !didOpenThemeContextMenuBefore)
            localStorage.setItem("didOpenThemeContextMenu", "true");
    }, [didOpenContextMenu, didOpenThemeContextMenuBefore]);
    

    const toggleThemeManual = () => {
        
        // Flag as having toggled the theme at least once
        setDidToggle(true);

        // Toggle theme
        setTheme(isDarkTheme ? "light" : "dark");

    }

    return (

        <ContextMenu
            onOpenChange={()=>setDidOpenContextMenu(true)}
        >
            <ContextMenuTrigger>
                <button
                    className="cursor-pointer flex relative"
                    onClick={toggleThemeManual}
                >
                {/* Theme Toggle Icon */}
                {
                    (isDarkTheme)
                    ?
                    <Sun/>
                    :
                    <Moon/>
                }

                {
                    (isLoggedIn())
                    &&
                    <>
                        {/* Left Ping (Prompt to toggle theme) */}
                        {
                            (!didToggle && !didToggleThemeBefore)
                            &&
                            <PingHalfLeft/>
                        }

                        {/* Right Ping (Prompt to open theme context window) */}
                        {
                            (!didOpenContextMenu && !didOpenThemeContextMenuBefore)
                            &&
                            <PingHalfRight/>
                        }
                    </>
                }

                </button>
            </ContextMenuTrigger>

        {
            (isLoggedIn())
            &&
            <ContextMenuContent className='p-0! min-w-[260px]'>


                {/* Toggle Theme */}
                <ContextMenuItem
                    className='p-3'
                    onClick={toggleThemeManual}
                >
                    Switch to {isDarkTheme ? "Light" : "Dark"} Theme
                </ContextMenuItem>
                <Separator />


                {/* Toggle High Contrast Charts */}
                <ContextMenuItem
                    className='p-3'
                    onClick={() => setUseHighContrastCharts(!useHighContrastCharts)}
                >
                    Use High Contrast Charts
                    <Checkbox checked={useHighContrastCharts} className="ml-auto pointer-events-none"/>
                </ContextMenuItem>
                <Separator />


                {/* Toggle Background Image */}
                <ContextMenuItem
                    className='p-3'
                    onClick={() => setUseBackgroundImage(!useBackgroundImage)}
                >
                    Use Background Image
                    <Checkbox checked={useBackgroundImage} className="ml-auto pointer-events-none"/>
                </ContextMenuItem>
                <Separator />
                

                {/* Toggle Navbar Page Names */}
                <ContextMenuItem
                    className='p-3'
                    onClick={() => setUseNavbarPageNames(!useNavbarPageNames)}
                >
                    Show Navbar Page Names
                    <Checkbox checked={useNavbarPageNames} className="ml-auto pointer-events-none"/>
                </ContextMenuItem>


            </ContextMenuContent>
        }
    </ContextMenu>
    );
    
}