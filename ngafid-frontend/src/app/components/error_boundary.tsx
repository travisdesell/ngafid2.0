// src/app/components/error_boundary.tsx
import { getLogger } from "@/components/providers/logger";
import React from "react";


const log = getLogger("ErrorBoundary", "white", "Utility");


type ErrorBoundaryProps = {
    onError?: (error: Error, info: React.ErrorInfo) => void;
    children: React.ReactNode;
};

type ErrorBoundaryState = {
    hasError: boolean;
};

export function ErrorBoundary({ onError, children }: ErrorBoundaryProps) {

    const [hasError, setHasError] = React.useState(false);

    const getDerivedStateFromError = (_: Error): ErrorBoundaryState => {

        log.error("An error was caught in ErrorBoundary:", _);

        setHasError(true);
        return { hasError: true };
    };

    const componentDidCatch = (error: Error, info: React.ErrorInfo) => {
        onError?.(error, info);
    };

    if (hasError)
        return null;

    return children;

}