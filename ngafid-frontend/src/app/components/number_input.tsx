// ngafid-frontend/src/app/components/number_input.tsx
import { ChevronDown, ChevronUp } from "lucide-react";
import React, {
    forwardRef,
    useCallback,
    useEffect,
    useState,
} from "react";
import { Button } from "./ui/button";
import { Input } from "./ui/input";

export interface NumberInputProps
    extends Omit<React.InputHTMLAttributes<HTMLInputElement>,
        "value" | "defaultValue" | "onChange" | "type"> {
    stepper?: number;
    min?: number;
    max?: number;
    value?: number;
    defaultValue?: number;
    onValueChange?: (value: number | undefined) => void;
}

export const NumberInput = forwardRef<HTMLInputElement, NumberInputProps>(
    (
        {
            stepper = 1,
            min = -Infinity,
            max = Infinity,
            value: controlledValue,
            defaultValue,
            onValueChange,
            className,
            ...inputProps
        },
        ref
    ) => {

        const isControlled = (controlledValue !== undefined);

        const [inner, setInner] = useState<string>(() => {

            // Controlled value is set, use it
            if (controlledValue !== undefined)
                return String(controlledValue);

            // Otherwise, attempt to use default value
            if (defaultValue !== undefined)
                return String(defaultValue);

            return "";

        });

        useEffect(() => {

            // Not controlled, do nothing
            if (!isControlled)
                return;

            const next = (controlledValue === undefined)
                ? ""
                : String(controlledValue);

            setInner(prev => (prev === next ? prev : next));

        }, [isControlled, controlledValue]);

        const parse = (s: string): number | undefined => {

            const trimmed = s.trim();

            // Got empty string -> undefined
            if (!trimmed)
                return undefined;

            const n = Number(trimmed);

            // Not a number -> undefined
            if (Number.isNaN(n))
                return undefined;

            return n;

        };

        const clamp = (n: number): number => {
            let r = n;
            if (r < min)
                r = min;
            if (r > max)
                r = max;
            return r;
        };

        const handleChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
            const s = e.target.value;
            setInner(s);

            const n = parse(s);
            onValueChange?.(n);
        };

        const handleBlur: React.FocusEventHandler<HTMLInputElement> = (e) => {

            const s = e.target.value;
            let n = parse(s);

            // Empty or invalid, reset to empty
            if (n === undefined) {
                setInner("");
                onValueChange?.(undefined);
                return;
            }

            n = clamp(n);
            const s2 = String(n);
            setInner(s2);
            onValueChange?.(n);

        };

        const bump = useCallback((direction: 1 | -1) => {

            const current = parse(inner) ?? 0;
            const n = clamp(current + direction * (stepper ?? 1));
            const s = String(n);

            setInner(s);
            onValueChange?.(n);

        }, [inner, stepper, min, max, onValueChange]);

        const handleKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (e) => {

            const KEY_INCREMENT = "ArrowUp";
            const KEY_DECREMENT = "ArrowDown";

            if (e.key === KEY_INCREMENT) {
                e.preventDefault();
                bump(1);
            } else if (e.key === KEY_DECREMENT) {
                e.preventDefault();
                bump(-1);
            }

        };

        const numeric = parse(inner);
        const atMin = (numeric !== undefined && numeric <= min);
        const atMax = (numeric !== undefined && numeric >= max);

        return (
            <div className="flex items-center bg-background">
                <Input
                    {...inputProps}
                    ref={ref}
                    type="number"
                    value={inner}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    onKeyDown={handleKeyDown}
                    min={Number.isFinite(min) ? min : undefined}
                    max={Number.isFinite(max) ? max : undefined}
                    className={`[appearance:textfield] [&::-webkit-outer-spin-button]:webkit-appearance-none [&::-webkit-inner-spin-button]:appearance-none rounded-r-none relative ${className ?? ""}`}
                />

                <div className="flex flex-col">
                    <Button
                        aria-label="Increase value"
                        className="px-2 h-fit rounded-none! rounded-l-none rounded-br-none border-input border-l-0  border-t-[1.25px] focus-visible:relative"
                        variant="outline"
                        onClick={() => bump(1)}
                        disabled={atMax}
                    >
                        <ChevronUp className="absolute scale-75" />
                    </Button>
                    <Button
                        aria-label="Decrease value"
                        className="px-2 h-fit rounded-none! rounded-l-none rounded-tr-none border-input border-l-0 border-b-[1.25px] focus-visible:relative"
                        variant="outline"
                        onClick={() => bump(-1)}
                        disabled={atMin}
                    >
                        <ChevronDown className="absolute scale-75" />
                    </Button>
                </div>
            </div>
        );
    }
);
