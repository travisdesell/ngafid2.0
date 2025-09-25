"use client"

import * as React from "react"
import { format } from "date-fns"
import { Calendar as CalendarIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover"
import { Label } from "@/components/ui/label"
import { OnSelectHandler } from "react-day-picker"

export type DatePickerProps = {
    labelText?: string,
    date: Date,
    setDate: OnSelectHandler<Date>,
    isInvalid?: boolean,
}

export default function DatePicker({ labelText, date, setDate, isInvalid }: DatePickerProps) {

    return (
        <div className="flex flex-col gap-2 justify-start items-start">
            <Label className="px-1">
                {labelText}
            </Label>
            <Popover>
                <PopoverTrigger asChild>
                    <Button
                        variant={isInvalid ? "destructive" : "outline"}
                        data-empty={!date}
                        className="data-[empty=true]:text-muted-foreground w-[280px] justify-start text-left font-normal"
                    >
                        <CalendarIcon />
                        {date ? format(date, "PPP") : <span>Pick a date</span>}
                    </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0">
                    <Calendar mode="single" required={true} selected={date} onSelect={setDate} />
                </PopoverContent>
            </Popover>
        </div>
    )
}