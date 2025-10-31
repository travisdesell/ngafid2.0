// ngafid-frontend/src/app/components/providers/log_provider.tsx
import React, { createContext, useContext, useMemo } from "react";


/*
    Logger utility for consistent, styled
    logging across the site.

    Use by creating a new Logger at the
    top of a file, e.g.,

        const log = getLogger("MyComponent", "black", "Component");

    Then you can use:

        log("This is a log message");
        log.info("This is an info message");
        log.warn("This is a warning message");
        log.error("This is an error message");
        log.table("This is a table message", data);
*/


export type LogLevel = "log" | "info" | "warn" | "warning" | "error" | "table";
type LogMethod = (message: string, ...args: any[]) => void;
type TableColumns = readonly string[];
export type Logger = ((message: string, ...args: any[]) => void) & {
    log: LogMethod;
    info: LogMethod;
    warn: LogMethod;
    warning: LogMethod; //<-- Alias of 'warn'
    error: LogMethod;
    table: (message: string, data: any, columns?: TableColumns) => void;
};


const emit = (level: LogLevel, prefix: string, badge: string) =>
    (message: string, ...args: any[]) => {

        // Generate label string
        const label = `%c${prefix}%c ${message}`;

        // Handle table logging separately
        if (level === "table") {

            const [data, columns] = args as [any, TableColumns | undefined];

            console.groupCollapsed(label, badge, "");

            // Has specified columns
            if (columns && columns.length)
                console.table(data, [...columns]);

            // No specified columns
            else
                console.table(data);

            console.groupEnd();
            return;
        }

        const consoleLevel = level === "warning" ? "warn" : level;
        (console as any)[consoleLevel](label, badge, "", ...args);

    };


const makePrefixAndBadge = (name: string, color: string, type: LogComponentKey) => {

    const { prefix, styling } = LogComponentTypes[type];

    const label = `${prefix ? prefix + " - " : ""}${name}`;
    const badge = `color: ${color}; font-weight: bold; background-color: #eee; padding: 2px 6px; border-radius: 8px; ${styling}`;

    return { label, badge };

};


type LogContextValue = {
    createLogger: (opts: {
        name: string;
        color?: string;
        type?: LogComponentKey;
    }) => Logger;
};


type LogComponent = {
    prefix: string,
    styling?: string,
}
const LogComponentTypes = {
    Default: {
        prefix: "",
        styling: "",
    },
    Provider: {
        prefix: "🔽 Provider",
        styling: "background-color: lightblue;"
    },
    Modal: {
        prefix: "🔲 Modal",
        styling: "background-color: #F8F;"
    },
    Page: {
        prefix: "📄 Page",
        styling: "background-color: #AFA;"
    },
    Navbar: {
        prefix: "➖ Navbar",
        styling: "background-color: white;"
    },
    Component: {
        prefix: "🔧 Component",
        styling: "background-color: orange;"
    },
    Chart: {
        prefix: "📊 Chart",
        styling: "background-color: #F86;"
    }
} satisfies Record<string, LogComponent>;

export type LogComponentKey = keyof typeof LogComponentTypes;



let globalCreateLogger: LogContextValue["createLogger"] | null = null;

const defaultCreateLogger: LogContextValue["createLogger"] = ({
    name = "Unknown",
    color = "gray",
    type = "Default",
}) => {
    
    const { label, badge } = makePrefixAndBadge(name, color, type);

    // Base callable uses "log"
    const base = ((message: string, ...args: any[]) =>
        emit("log", label, badge)(message, ...args)) as Logger;

    // Methods
    base.log = emit("log", label, badge);
    base.info = emit("info", label, badge);
    base.warn = emit("warn", label, badge);
    base.warning = base.warn; //<-- Alias
    base.error = emit("error", label, badge);
    base.table = emit("table", label, badge);

    return base;

};


export function getLogger(name: string, color?: string, type?: LogComponentKey): Logger {

    const factory = (globalCreateLogger ?? defaultCreateLogger);
    return factory({ name, color, type });

}