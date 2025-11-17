// ngafid-frontend/src/app/components/color_picker.tsx
// Via: https://github.com/nightspite/shadcn-color-picker/blob/master/src/components/ui/color-picker.tsx
'use client';

import type { ButtonProps } from '@/components/ui/button';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover';
import { cn } from '@/lib/utils';
import type { ColorResult } from '@uiw/color-convert';
import Colorful from '@uiw/react-color-colorful';
import { Check } from 'lucide-react';
import { forwardRef, useMemo, useState } from 'react';


export const randomHexColor = () => {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * 16)];
    }

    return color;
}


interface ColorPickerProps {
    value: string;
    onChange: React.Dispatch<React.SetStateAction<string>> | ((value: string) => void);
    onBlur?: () => void;
}

const ColorPicker = forwardRef<
  HTMLInputElement,
  Omit<ButtonProps, 'value' | 'onChange' | 'onBlur'> & ColorPickerProps
>(
    (
        { disabled, value, onChange, onBlur, name, className, size, ...props },
        forwardedRef
    ) => {
        const ref = forwardedRef;
        const [open, setOpen] = useState(false);

        const parsedValue = useMemo(() => {
            return value || '#FFFFFF';
        }, [value]);

        return (
            <Popover onOpenChange={setOpen} open={open}>
                <PopoverTrigger asChild disabled={disabled} onBlur={onBlur}>
                    <Button
                        {...props}
                        className={cn('block', className)}
                        name={name}
                        onClick={() => {
                            setOpen(true);
                        }}
                        size={size}
                        style={{
                            backgroundColor: parsedValue,
                        }}
                        variant='outline'
                    >
                        <div />
                    </Button>
                </PopoverTrigger>
                <PopoverContent className='w-full flex flex-col items-center gap-4'>
                    <Colorful disableAlpha color={parsedValue} onChange={(c: ColorResult) => onChange(c.hex)} />

                    <div className="flex flex-row gap-2 items-center">
                        <Input
                            maxLength={7}
                            onChange={(e) => {
                                onChange(e?.currentTarget?.value);
                            }}
                            ref={ref}
                            value={parsedValue}
                        />

                        <Button
                            className='aspect-square'
                            variant='outline'
                            onClick={() => {
                                setOpen(false);
                            }}
                        >
                            <Check />
                        </Button>
                    </div>
                </PopoverContent>
            </Popover>
        );
    }
);
ColorPicker.displayName = 'ColorPicker';

export { ColorPicker };
