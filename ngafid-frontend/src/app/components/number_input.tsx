// ngafid-frontend/src/app/components/number_input.tsx
import { ChevronDown, ChevronUp } from 'lucide-react';
import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
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


        const inputRef = useRef<HTMLInputElement>(null);

        // Expose the inner input to parents
        useImperativeHandle(ref, () => inputRef.current!);

        const [value, setValue] = useState<number | undefined>(
            controlledValue ?? defaultValue
        );



        useEffect(() => {

            if (controlledValue !== undefined)
                setValue(prev => (prev === controlledValue ? prev : controlledValue));

        }, [controlledValue]);



        const fromUserRef = useRef(false);
        useEffect(() => {

            // No change from user, exit
            if (!fromUserRef.current)
                return;

            fromUserRef.current = false;
            onValueChange?.(value);

        }, [value, onValueChange]);

        const setFromUser = useCallback((next: number | undefined | ((p: number | undefined) => number | undefined)) => {

            fromUserRef.current = true;
            setValue(prev => (
                (typeof next === 'function')
                    ? (next as any)(prev)
                    : next
            ));

        }, []);


        const handleIncrement = useCallback(() => {

            const step = (stepper ?? 1);
            setFromUser(prev =>
                (prev === undefined)
                    ? step
                    : Math.min(prev + step, max)
            );

        }, [stepper, max, setFromUser]);

        const handleDecrement = useCallback(() => {

            const step = (stepper ?? 1);
            setFromUser(prev =>
                (prev === undefined)
                    ? -step
                    : Math.max(prev - step, min)
            );

        }, [stepper, min, setFromUser]);


        const handleChange = (values: { value: string; floatValue: number | undefined }) => {

            const newValue = (values.floatValue === undefined)
                ? undefined
                : values.floatValue;

            setFromUser(newValue)

        }

        const handleBlur = () => {

            // Value is undefined, exit
            if (value === undefined)
                return

            // Get clamped value
            let clamped = value;
            if (value < min)
                clamped = min;
            else if (value > max)
                clamped = max;

            // Clamped value differs from current value...
            if (clamped !== value) {

                // ...Set and notify
                setFromUser(clamped)

                // ...Update input display
                if (inputRef.current)
                    inputRef.current.value = String(clamped)

            }

        }


        const handleKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (e) => {
            if (e.key === 'ArrowUp') {
                e.preventDefault();
                handleIncrement();
            } else if (e.key === 'ArrowDown') {
                e.preventDefault();
                handleDecrement();
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
                    valueIsNumericString={false}
                    onBlur={handleBlur}
                    onKeyDown={handleKeyDown}
                    max={max}
                    min={min}
                    suffix={suffix}
                    prefix={prefix}
                    customInput={Input}
                    placeholder={placeholder}
                    className="[appearance:textfield] [&::-webkit-outer-spin-button]:webkit-appearance-none [&::-webkit-inner-spin-button]:appearance-none rounded-r-none relative"
                    getInputRef={(el: HTMLInputElement) => { inputRef.current = el as HTMLInputElement }}
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
                        <ChevronUp className="absolute scale-75" />
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
