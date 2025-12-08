// ngafid-frontend/src/app/components/providers/logger.tsx


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

        etc.

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

        // Logger globally disabled, exit
        if (!window.useLogger)
            return;

        // Generate label string
        const label = `%c${prefix}%c ${message}`;

        // Resolve CSS vars in badge
        const badgeResolved = resolveCSSVarsInString(badge);

        // Handle table logging separately
        if (level === "table") {

            const [data, columns] = args as [any, TableColumns | undefined];

            console.groupCollapsed(label, badgeResolved, "");

            // Has specified columns
            if (columns && columns.length)
                console.table(data, [...columns]);

            // No specified columns
            else
                console.table(data);

            console.groupEnd();
            return;
        }

        // Map "warning" to "warn"
        const consoleLevel = (level === "warning")
            ? "warn"
            : level;

        // Resolve CSS vars in any string args (covers %c style strings passed in)
        const resolvedArgs = args.map(arg =>

            (typeof arg === "string" && arg.includes("var("))
                ? resolveCSSVarsInString(arg)
                : arg

        );

        (console as any)[consoleLevel](label, badgeResolved, "", ...resolvedArgs);

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
    Context: {
        prefix: "🔗 Context",
        styling: "background-color: #6AF;"
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
        styling: "background-color: #ffd991;"
    },
    Utility: {
        prefix: "⚙️ Utility",
        styling: "background-color: #272733;"
    },
    Chart: {
        prefix: "📊 Chart",
        styling: "background-color: #F86;"
    },
    Main: {
        prefix: "🌳 Main",
        styling: "background-color: black;"
    },
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



// CSS styling support
const resolveCSSVar = (name: string, seen: Set<string> = new Set()): string => {

    // SSR / non-DOM, just return the var() expression
    if (typeof window === "undefined" || typeof document === "undefined")
        return `var(${name})`;

    // Got a circular reference, bail
    if (seen.has(name))
        return `var(${name})`;
    
    // Flag this variable as seen
    seen.add(name);

    const root = document.documentElement;
    const raw = getComputedStyle(root).getPropertyValue(name).trim();

    // Custom property itself is another var(...) reference, resolve it recursively
    const match = raw.match(/^var\((--[^)]+)\)$/);
    if (match)
        return resolveCSSVar(match[1], seen);

    // Got something non-empty, assume it's the final color (oklch, rgb, hex, etc.)
    if (raw)
        return raw;

    // Fallback: unresolved
    return `var(${name})`;

};

const resolveCSSVarsInString = (value: string): string => {

    if (!value.includes("var("))
        return value;

    return value.replace(/var\((--[^)]+)\)/g, (_full, name: string) => {
        return resolveCSSVar(name.trim());
    });

};




/*
    Functions to enable/disable the
    logger globally.

    e.g., to disable all logging,
    use 'DL()' in the browser console.
*/
declare global {
    interface Window {
        useLogger: boolean;
        EL: () => string;
        DL: () => string;
        TL: () => void;
    }
}

window.useLogger = true;
window.EL = () => {
    window.useLogger = true;
    return "Logger Enabled ✅";
}
window.DL = () => {
    window.useLogger = false;
    return "Logger Disabled ❌";
}
window.TL = () => {

    if (window.useLogger) 
        window.DL();
    else 
        window.EL();
}