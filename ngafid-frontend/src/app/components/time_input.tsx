// ngafid-frontend/src/app/components/time_input.tsx
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import React from 'react'

type TimeInputProps = React.InputHTMLAttributes<HTMLInputElement> & { label?: string }

const TimeInput = ({ label, id, ...inputProps }: TimeInputProps) => {

    const fallbackId = React.useId();
    const inputId = (id ?? `time-picker-${fallbackId}`);

    return (
        <div className="w-full max-w-xs space-y-2">
            {
                (label)
                &&
                <Label htmlFor={inputId} className="px-1">
                    {label}
                </Label>
            }
            <Input
                type="time"
                id={inputId}
                step="1"
                className="cursor-text bg-background appearance-none [&::-webkit-calendar-picker-indicator]:hidden [&::-webkit-calendar-picker-indicator]:appearance-none"
                {...inputProps}
            />
        </div>
    );

}

export default TimeInput