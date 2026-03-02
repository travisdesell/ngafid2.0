"use client"

import { format } from "date-fns"
import { Calendar as CalendarIcon } from "lucide-react"
import * as React from "react"

import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Label } from "@/components/ui/label"
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover"
import { OnSelectHandler } from "react-day-picker"

export type DatePickerProps = {
    labelText?: string,
    date: Date,
    setDate: OnSelectHandler<Date>,
    isInvalid?: boolean,
}

export default function DatePicker({ labelText, date, setDate, isInvalid }: DatePickerProps) {

    const [open, setOpen] = React.useState(false);
    const [displayMonth, setDisplayMonth] = React.useState<Date>(date);

    React.useEffect(() => {
        if (open)
            setDisplayMonth(date);
    }, [open, date]);

    return (
        <div className="flex flex-col gap-2 justify-start items-start">
            {
                (labelText?.length)
                &&
                <Label className="px-1">
                    {labelText}
                </Label>
            }
            <Popover open={open} onOpenChange={setOpen}>
                <PopoverTrigger asChild>
                    <Button
                        variant={isInvalid ? "destructive" : "outline"}
                        data-empty={!date}
                        className="data-[empty=true]:text-muted-foreground w-70 justify-start text-left font-normal"
                    >
                        <CalendarIcon />
                        {date ? format(date, "PPP") : <span>Pick a date</span>}
                    </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0">
                    <Calendar
                        mode="single"
                        required={true}
                        selected={date}
                        onSelect={setDate}
                        month={displayMonth}
                        onMonthChange={setDisplayMonth}
                    />
                </PopoverContent>
            </Popover>
        </div>
    )
}