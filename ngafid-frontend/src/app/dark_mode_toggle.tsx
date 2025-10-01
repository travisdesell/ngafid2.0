import React, { Component } from 'react';

import { useTheme } from "@/components/theme-provider"
import { Moon, Sun } from 'lucide-react';
import PingHalfLeft from './components/pings/ping_half_left';
import PingHalfRight from './components/pings/ping_half_right';
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuTrigger,
} from "@/components/ui/context-menu"
import { Checkbox } from './components/ui/checkbox';
import { Separator } from './components/ui/separator';
import { useAuth } from './auth';


export function DarkModeToggle() {

    const didToggleThemeBefore = (localStorage.getItem("didToggleTheme") !== null);
    const didOpenThemeContextMenuBefore = (localStorage.getItem("didOpenThemeContextMenu") !== null);

    const { isLoggedIn } = useAuth();
    const { theme, setTheme, useHighContrastCharts, setUseHighContrastCharts } = useTheme();
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
                    {/* <Checkbox checked={isDarkTheme} className="ml-auto pointer-events-none"/> */}
                </ContextMenuItem>
                <Separator />

                {/* Toggle High Contrast Charts */}
                <ContextMenuItem
                    className='p-3'
                    onClick={() => setUseHighContrastCharts(!useHighContrastCharts)}
                >
                    {useHighContrastCharts ? "Disable" : "Enable"} High Contrast Charts
                    <Checkbox checked={useHighContrastCharts} className="ml-auto pointer-events-none"/>
                </ContextMenuItem>
                {/* <Separator /> */}

            </ContextMenuContent>
        }
    </ContextMenu>
    );
    
}