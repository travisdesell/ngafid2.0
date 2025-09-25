import React, { Component } from 'react';

import { useTheme } from "@/components/theme-provider"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Button } from "@/components/ui/button"
import { Moon, Sun } from 'lucide-react';


export function DarkModeToggle() {

    const { theme, setTheme } = useTheme();
    const isDarkTheme = (theme === "dark");

    return (
        <button
            className="cursor-pointer flex"
            onClick={() => setTheme(isDarkTheme ? "light" : "dark")}
        >
            {
                (isDarkTheme)
                ?
                <Sun/>
                :
                <Moon/>
            }
        </button>
    );
    
}