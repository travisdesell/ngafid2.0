// ngafid-frontend/src/app/lib/useEffectPrev.ts
import { useEffect, useRef } from "react";

/*
    Extended version of the useEffect
    hook that provides the previous value
    of the dependency.
*/

export function useEffectPrev<T>(value: T, effect: (prev: T | undefined) => void) {

    const hasMounted = useRef(false);
    const prevRef = useRef<T | undefined>(undefined);
    const effectRef = useRef(effect);

    // Update effect ref on each render
    useEffect(() => {
        effectRef.current = effect;
    }, [effect]);

    useEffect(() => {

        const prev = (hasMounted.current)
            ? prevRef.current
            : undefined;

        effectRef.current(prev);
        hasMounted.current = true;
        prevRef.current = value;

    }, [value]);

}