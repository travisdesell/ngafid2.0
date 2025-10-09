// ngafid-frontend/src/app/components/time_header/time_header_provider.tsx
import { Calendar } from "lucide-react";
import React, { JSX } from "react";
import { OnSelectHandler } from "react-day-picker";

type TimeHeaderContextValue = {
    //Selected (non-applied)
    startDate: Date,
    endDate: Date,
    setStartDate: OnSelectHandler<Date>,
    setEndDate: OnSelectHandler<Date>,

    //Applied (last committed)
    appliedStartDate: Date,
    appliedEndDate: Date,

    //Derived from applied
    endpointStartDate: string,
    endpointEndDate: string,

    //Reapply trigger + apply API
    reapplyTrigger: number,
    setReapplyTrigger: React.Dispatch<React.SetStateAction<number>>,
    applyCurrentSelection: () => void,

    //UI helpers (reflect applied dates)
    renderDateRangeNumeric: () => JSX.Element,
    renderDateRangeMonthly: () => JSX.Element,
};

const TimeHeaderContext = React.createContext<TimeHeaderContextValue | null>(null);

export function useTimeHeader() {
    const ctx = React.useContext(TimeHeaderContext);
    if (!ctx)
        throw new Error("useTimeHeader must be used within <TimeHeader>");
    return ctx;
}




//Start Date: January 1st of current year
const dateDefaultStart = new Date(new Date().getFullYear(), 0, 1);

//End Date: Current Date
const dateDefaultEnd = new Date();


export function TimeHeaderProvider({ children }: { children: React.ReactNode }) {

    //Selected (non-applied)
    const [startDate, setStartDate] = React.useState<Date>(dateDefaultStart);
    const [endDate, setEndDate] = React.useState<Date>(dateDefaultEnd);

    //Applied (committed)
    const [appliedStartDate, setAppliedStartDate] = React.useState<Date>(dateDefaultStart);
    const [appliedEndDate, setAppliedEndDate] = React.useState<Date>(dateDefaultEnd);

    const [reapplyTrigger, setReapplyTrigger] = React.useState(0);

    const buildEndpointStartDate = React.useCallback(() => {

        const y = appliedStartDate.getFullYear();
        const m = appliedStartDate.getMonth() + 1;
        const mm = m < 10 ? `0${m}` : `${m}`;

        return `${y}-${mm}-01`;

    }, [appliedStartDate]);


    const buildEndpointEndDate = React.useCallback(() => {

        const y = appliedEndDate.getFullYear();
        const m = appliedEndDate.getMonth() + 1;
        const mm = m < 10 ? `0${m}` : `${m}`;
        const lastDay = new Date(y, m, 0).getDate();

        return `${y}-${mm}-${lastDay}`;

    }, [appliedEndDate]);

    const endpointStartDate = buildEndpointStartDate();
    const endpointEndDate = buildEndpointEndDate();

    //Commit the current selection as 'applied'
    const applyCurrentSelection = React.useCallback(() => {

        setAppliedStartDate(startDate);
        setAppliedEndDate(endDate);
        setReapplyTrigger(n => n + 1);

    }, [startDate, endDate]);


    const renderDateRangeNumeric = React.useCallback(() => {

        return (
            <div className="flex flex-row items-center gap-1 mt-1 text-sm text-[var(--c_text_subtle)] opacity-50">
                <Calendar size={16} className="mb-0.5" /> {endpointStartDate} — {endpointEndDate}
            </div>
        );

    }, [appliedStartDate, appliedEndDate, endpointStartDate, endpointEndDate]);

    const renderDateRangeMonthly = React.useCallback(() => {

        const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'short' };
        const startString = appliedStartDate.toLocaleDateString(undefined, options);
        const endString = appliedEndDate.toLocaleDateString(undefined, options);

        return (
            <div className="flex flex-row items-center gap-1 text-xs font-medium text-[var(--c_text_subtle)] opacity-50">
                <Calendar size={16} /> {startString} — {endString}
            </div>
        );

    }, [appliedStartDate, appliedEndDate]);


    const value = React.useMemo(() => ({
        //Selected (non-applied)
        startDate,
        endDate,
        setStartDate,
        setEndDate,

        //Applied (last committed)
        appliedStartDate,
        appliedEndDate,

        //Derived from applied
        endpointStartDate,
        endpointEndDate,

        //Reapply trigger + apply API
        reapplyTrigger,
        setReapplyTrigger,
        applyCurrentSelection,

        //UI helpers (reflect applied dates)
        renderDateRangeNumeric,
        renderDateRangeMonthly,
    }), [
        startDate, endDate,
        appliedStartDate, appliedEndDate,
        endpointStartDate, endpointEndDate,
        reapplyTrigger,
        applyCurrentSelection,
        renderDateRangeNumeric, renderDateRangeMonthly,
    ]);

    return (
        <TimeHeaderContext.Provider value={value}>
            {children}
        </TimeHeaderContext.Provider>
    );

}