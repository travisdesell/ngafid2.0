// ngafid-frontend/src/app/components/time_header/time_header.tsx
import DatePicker from "@/components/date-picker";
import Ping from "@/components/pings/ping";
import TooltipIcon from "@/components/tooltip_icon";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { CircleQuestionMark } from "lucide-react";
import React, { useEffect } from "react";
import { useTimeHeader } from "./time_header_provider";

/**
 * - Automatic: Automatically apply immediately when mounted
 * - Manual: Wait for user to click apply
 * - Require-dep-change: Disable apply until a change in dependencies is detected (then require manual apply)
 */
export type TimeHeaderInitialApply =
    | 'manual'
    | 'automatic' 
    | 'require-dep-change'
;

interface TimeHeaderProps {
    children: React.ReactNode;
    onApply: () => void;
    dependencies?: Array<any>;
    initialApply?: TimeHeaderInitialApply;
}

export default function TimeHeader({ children, onApply, dependencies, initialApply = 'manual' }: TimeHeaderProps) {

    const { startDate, endDate, setStartDate, setEndDate, applyCurrentSelection } = useTimeHeader();

    const hasAppliedRef = React.useRef(false);

    const depsInnate = [startDate, endDate];
    const depsFull = () => (dependencies)
        ? [...dependencies, ...depsInnate]
        : depsInnate;

    // Snapshot last applied values (initialized to mount values)
    const appliedRef = React.useRef({
        start: startDate,
        end: endDate,
        depsKey: JSON.stringify(depsFull())
    });
    const depsKey = JSON.stringify(depsFull());
    const depsChanged = (depsKey !== appliedRef.current.depsKey);

    // Derive allowReapply from current VS applied snapshot
    let allowReapply = depsChanged;
    if (!hasAppliedRef.current) {

        if (initialApply === 'manual')
            allowReapply = true;

        if (initialApply === 'require-dep-change')
            allowReapply = depsChanged;

    }

    // Invalid range forces disabled
    const invalidDateRange = (startDate && endDate && startDate > endDate);
    const disabled = (!allowReapply || invalidDateRange);

    const applyTimeHeader = React.useCallback(() => {

        // Update snapshot to current
        appliedRef.current = { start: startDate, end: endDate, depsKey };
        hasAppliedRef.current = true;

        // Trigger onApply callback
        onApply();

        // Trigger context to apply current selection (update applied dates)
        applyCurrentSelection();

    }, [applyCurrentSelection, depsKey, endDate, onApply, startDate]);


    useEffect(() => {

        if (initialApply !== 'automatic')
            return;

        if (hasAppliedRef.current || invalidDateRange)
            return;

        applyTimeHeader();

    }, [applyTimeHeader, initialApply, invalidDateRange]);


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
                    <div>Select the time range to use for this page&apos;s content.</div>
                    <TooltipIcon
                        icon={CircleQuestionMark}
                        message="Click the Apply button to update the content after changing the dates. Some content may only use the selected years and months while defaulting to the first and last days of the selected months."
                    />
                </CardDescription>
            </CardHeader>
            <CardContent className="py-0 text-center flex flex-row space-x-6">

                {mappedChildren}

                {/* Time Range Selection */}
                <div className="flex items-center space-x-6">
                    <DatePicker labelText="Start Date" date={startDate} setDate={setStartDate} isInvalid={invalidDateRange} />
                    <DatePicker labelText="End Date" date={endDate} setDate={setEndDate} isInvalid={invalidDateRange} />
                </div>


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
