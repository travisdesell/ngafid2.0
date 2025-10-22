// ngafid-frontend/src/app/components/number_input.tsx
import { ChevronDown, ChevronUp } from 'lucide-react';
import { forwardRef, useCallback, useEffect, useState } from 'react';
import { NumericFormat, NumericFormatProps } from 'react-number-format';
import { Button } from './ui/button';
import { Input } from './ui/input';

export interface NumberInputProps
    extends Omit<NumericFormatProps, 'value' | 'onValueChange'> {
    stepper?: number;
    thousandSeparator?: string;
    placeholder?: string;
    defaultValue?: number;
    min?: number;
    max?: number;
    value?: number; // Controlled value
    suffix?: string;
    prefix?: string;
    onValueChange?: (value: number | undefined) => void;
    fixedDecimalScale?: boolean;
    decimalScale?: number;
}

export const NumberInput = forwardRef<HTMLInputElement, NumberInputProps>(
    (
        {
            stepper,
            thousandSeparator,
            placeholder,
            defaultValue,
            min = -Infinity,
            max = Infinity,
            onValueChange,
            fixedDecimalScale = false,
            decimalScale = 0,
            suffix,
            prefix,
            value: controlledValue,
            ...props
        },
        ref
    ) => {
        const [value, setValue] = useState<number | undefined>(
            controlledValue ?? defaultValue
        );

        const handleIncrement = useCallback(() => {
            setValue((prev) =>
                prev === undefined ? stepper ?? 1 : Math.min(prev + (stepper ?? 1), max)
            );
        }, [stepper, max]);

        const handleDecrement = useCallback(() => {
            setValue((prev) =>
                prev === undefined
                    ? -(stepper ?? 1)
                    : Math.max(prev - (stepper ?? 1), min)
            );
        }, [stepper, min]);

        useEffect(() => {
            const handleKeyDown = (e: KeyboardEvent) => {
                if (
                    document.activeElement ===
                    (ref as React.RefObject<HTMLInputElement>).current
                ) {
                    if (e.key === 'ArrowUp') {
                        handleIncrement();
                    } else if (e.key === 'ArrowDown') {
                        handleDecrement();
                    }
                }
            };

            window.addEventListener('keydown', handleKeyDown);

            return () => {
                window.removeEventListener('keydown', handleKeyDown);
            };
        }, [handleIncrement, handleDecrement, ref]);

        useEffect(() => {
            if (controlledValue !== undefined) {
                setValue(controlledValue);
            }
        }, [controlledValue]);

        const handleChange = (values: {
            value: string;
            floatValue: number | undefined;
        }) => {
            const newValue =
                values.floatValue === undefined ? undefined : values.floatValue;
            setValue(newValue);
            if (onValueChange) {
                onValueChange(newValue);
            }
        };

        const handleBlur = () => {
            if (value !== undefined) {
                if (value < min) {
                    setValue(min);
                    (ref as React.RefObject<HTMLInputElement>).current!.value =
                        String(min);
                } else if (value > max) {
                    setValue(max);
                    (ref as React.RefObject<HTMLInputElement>).current!.value =
                        String(max);
                }
            }
        };

        return (
            <div className="flex items-center bg-background">
                <NumericFormat
                    value={value}
                    onValueChange={handleChange}
                    thousandSeparator={thousandSeparator}
                    decimalScale={decimalScale}
                    fixedDecimalScale={fixedDecimalScale}
                    allowNegative={min < 0}
                    valueIsNumericString
                    onBlur={handleBlur}
                    max={max}
                    min={min}
                    suffix={suffix}
                    prefix={prefix}
                    customInput={Input}
                    placeholder={placeholder}
                    className="[appearance:textfield] [&::-webkit-outer-spin-button]:webkit-appearance-none [&::-webkit-inner-spin-button]:appearance-none rounded-r-none relative"
                    getInputRef={ref}
                    {...props}
                />

                <div className="flex flex-col">
                    <Button
                        aria-label="Increase value"
                        className="px-2 h-fit rounded-none! rounded-l-none rounded-br-none border-input border-l-0  border-t-[1.25px] focus-visible:relative"
                        variant="outline"
                        onClick={handleIncrement}
                        disabled={value === max}
                    >
                        <ChevronUp size={"xs"} className="absolute scale-75" />
                    </Button>
                    <Button
                        aria-label="Decrease value"
                        className="px-2 h-fit rounded-none! rounded-l-none rounded-tr-none border-input border-l-0 border-b-[1.25px] focus-visible:relative"
                        variant="outline"
                        onClick={handleDecrement}
                        disabled={value === min}
                    >
                        <ChevronDown className="absolute scale-75" />
                    </Button>
                </div>
            </div>
        );
    }
);
