import { useEffect, useRef } from "react";

/*
    Extended version of the useEffect
    hook that provides the previous value
    of the dependency.
*/

export function useEffectPrev<T>(value: T, effect: (prev: T | undefined) => void) {

    const hasMounted = useRef(false);
    const prevRef = useRef<T | undefined>(undefined);

    useEffect(() => {

        const prev = (hasMounted.current)
            ? prevRef.current
            : undefined;

        effect(prev);

        hasMounted.current = true;
        prevRef.current = value;

    }, [value]);

}