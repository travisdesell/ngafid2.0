// ngafid-frontend/src/app/components/time_input.tsx
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import React from 'react'

type TimeInputProps = React.InputHTMLAttributes<HTMLInputElement> & { label?: string }
const TimeInput = (props: TimeInputProps) => {
    return (
        <div className='w-full max-w-xs space-y-2'>

            {
                (props.label)
                &&
                <Label htmlFor='time-picker' className='px-1'>
                    {props.label}
                </Label>
            }
            <Input
                type='time'
                id='time-picker'
                step='1'
                defaultValue='12:00:00'
                className='cursor-text bg-background appearance-none [&::-webkit-calendar-picker-indicator]:hidden [&::-webkit-calendar-picker-indicator]:appearance-none'
            />
        </div>
    )
}

export default TimeInput