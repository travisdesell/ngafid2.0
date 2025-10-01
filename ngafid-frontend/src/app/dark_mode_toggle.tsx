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

    const { isLoggedIn } = useAuth();
    const { theme, setTheme, useHighContrastCharts, setUseHighContrastCharts } = useTheme();
    const isDarkTheme = (theme === "dark");

    const [didToggle, setDidToggle] = React.useState(false);
    const [didOpenContextMenu, setDidOpenContextMenu] = React.useState(false);
    
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
                            (!didToggle)
                            &&
                            <PingHalfLeft/>
                        }

                        {/* Right Ping (Prompt to open theme context window) */}
                        {
                            (!didOpenContextMenu)
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
                    <Checkbox checked={!isDarkTheme} className="ml-auto"/>
                </ContextMenuItem>
                <Separator />

                {/* Toggle High Contrast Charts */}
                <ContextMenuItem
                    className='p-3'
                    onClick={() => setUseHighContrastCharts(!useHighContrastCharts)}
                >
                    {useHighContrastCharts ? "Disable" : "Enable"} High Contrast Charts
                    <Checkbox checked={useHighContrastCharts} className="ml-auto"/>
                </ContextMenuItem>
                {/* <Separator /> */}

            </ContextMenuContent>
        }
    </ContextMenu>
    );
    
}