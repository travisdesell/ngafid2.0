// ngafid-frontend/src/app/components/time_header/time_header.tsx
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import DatePicker from "../date-picker";
import React, { useEffect } from "react";
import { useTimeHeader } from "./time_header_provider";
import { Button } from "../ui/button";
import { Separator } from "../ui/separator";
import Ping from "@/components/pings/ping";
import TooltipIcon from "@/components/tooltip_icon";
import { Calendar, CircleQuestionMark } from "lucide-react";

type TimeHeaderProps = {
    children: React.ReactNode;
    onApply: () => void;
    dependencies?: any[];
    requireManualInitialApply?: boolean;
}

export default function TimeHeader({ children, onApply, dependencies, requireManualInitialApply=false }: TimeHeaderProps) {

    const { startDate, endDate, setStartDate, setEndDate, setReapplyTrigger, applyCurrentSelection } = useTimeHeader();

    //Snapshot last applied values (initialized to mount values)
    const appliedRef = React.useRef({
        start: startDate,
        end: endDate,
        depsKey: JSON.stringify(dependencies ?? []),
    });

    const depsKey = JSON.stringify(dependencies ?? []);

    //Derive allowReapply from current VS applied snapshot
    const allowReapply =
        startDate?.getTime() !== appliedRef.current.start?.getTime() ||
        endDate?.getTime() !== appliedRef.current.end?.getTime() ||
        depsKey !== appliedRef.current.depsKey;

    //Invalid range forces disabled
    const invalidDateRange = (startDate && endDate && startDate > endDate);
    const disabled = (!allowReapply || invalidDateRange);

    const applyTimeHeader = () => {

        //Update snapshot to current
        appliedRef.current = { start: startDate, end: endDate, depsKey };

        //Trigger onApply callback
        onApply();

        //Trigger reapply in context to notify children
        // setReapplyTrigger(n => n + 1);

        //Trigger context to apply current selection (update applied dates)
        applyCurrentSelection();

    };

    const mappedChildren = React.Children.map(children, child =>
        <div className="flex flex-row gap-6 items-center">
            {child}
            <Separator orientation="vertical" />
        </div>
    );
    
    const render = () => (

        <Card className="flex flex-row w-full justify-between items-center card-glossy">
            <CardHeader>
                <CardTitle>Time Range Selection</CardTitle>
                <CardDescription className="flex flex-row items-center gap-0">
                    <div>Select the time range to use for this page's content.</div>
                    <TooltipIcon
                        icon={CircleQuestionMark}
                        message="Click the Apply button to update the content after changing the dates. Some content may only use the selected years and months while defaulting to the first and last days of the selected months."
                    />
                </CardDescription>
            </CardHeader>
            <CardContent className="py-0 text-center flex flex-row space-x-6">

                {mappedChildren}

                {/* Time Range Selection */}
                <DatePicker labelText="Start Date" date={startDate} setDate={setStartDate} isInvalid={invalidDateRange} />
                <DatePicker labelText="End Date" date={endDate} setDate={setEndDate} isInvalid={invalidDateRange} />


                {/* Apply Button */}
                <Button variant={"outline"} disabled={disabled} className="self-center relative" onClick={applyTimeHeader}>
                    <span>Apply</span>
                    {
                        (!disabled) && <Ping />
                    }
                </Button>

            </CardContent>
        </Card>

    );

    return render();

}