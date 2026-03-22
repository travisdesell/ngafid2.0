// ngafid-frontend/src/app/pages/protected/flights/_panels/_chart/flights_panel_chart_label_card.tsx

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { renderDateTime } from "@/pages/protected/flights/_flight_row/flight_row";
import { FlightLabelSection } from "@/pages/protected/flights/_flights_context_chart";
import { Download, FileUp, MapPinned, Trash } from "lucide-react";
import { ChangeEvent, useEffect, useRef, useState } from "react";

type Props = {
    flightId: number;
    pendingStartX: number | null;
    flightLabelSections: FlightLabelSection[];
    onFlightCsvDownload: () => void;
    onFleetCsvDownload: () => void;
    onImportCsv: (file: File) => void;
    onClose: () => void;
    position: { left: number; top: number };
    onPositionChange: (next: { left: number; top: number }) => void;
};

const LABELING_MARKER_COLORS = [
    "#e6194b", "#3cb44b", "#4363d8", "#f58231", "#911eb4", "#42d4f4", "#f032e6", "#bfef45",
    "#fabed4", "#469990", "#dcbeff", "#9a6324", "#fffac8", "#800000", "#aaffc3", "#808000", "#ffd8b1", "#000075",
];

const formatDateLocal = (date: Date): string => {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    const h = String(date.getHours()).padStart(2, "0");
    const min = String(date.getMinutes()).padStart(2, "0");
    const s = String(date.getSeconds()).padStart(2, "0");
    return `${y}-${m}-${d} ${h}:${min}:${s}`;
};

const formatElapsed = (secondsRaw: number): string => {
    const totalSeconds = Math.max(0, Math.floor(secondsRaw));
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;

    if (h > 0)
        return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;

    if (m > 0)
        return `${m}:${String(s).padStart(2, "0")}`;

    return `${s}s`;
};

const formatSectionDateTime = (rawTime: number): { datePart: string; timePart: string; } => {

    // Invalid number, display as empty
    if (!Number.isFinite(rawTime))
        return { datePart: "-", timePart: "" };

    // Unix timestamp seconds
    const EPOCH_THRESHOLD = 1e9;
    if (rawTime >= EPOCH_THRESHOLD) {

        const out = formatDateLocal(new Date(rawTime * 1000));
        const splitAt = out.indexOf(" ");
        if (splitAt > 0) {
            return {
                datePart: out.slice(0, splitAt),
                timePart: out.slice(splitAt + 1),
            };
        }
        return { datePart: out, timePart: "" };

    }

    return { datePart: formatElapsed(rawTime), timePart: "" };

};

const formatNumericValue = (value: number): string => {

    // Invalid number, display as dash
    if (!Number.isFinite(value))
        return "-";

    const FORMAT_PRECISION = 2;
    return value.toFixed(FORMAT_PRECISION);

};

export default function FlightsPanelChartLabelCard({
    flightId,
    pendingStartX,
    flightLabelSections,
    onFlightCsvDownload,
    onFleetCsvDownload,
    onImportCsv,
    onClose,
    position,
    onPositionChange,
}: Props) {

    const labelImportInputRef = useRef<HTMLInputElement | null>(null);
    const [dragging, setDragging] = useState(false);
    const dragStartRef = useRef<{ mouseX: number; mouseY: number; left: number; top: number } | null>(null);

    useEffect(() => {
        if (!dragging)
            return;

        const handleMouseMove = (e: MouseEvent) => {
            const start = dragStartRef.current;
            if (!start)
                return;

            const dx = e.clientX - start.mouseX;
            const dy = e.clientY - start.mouseY;

            onPositionChange({
                left: Math.max(0, start.left + dx),
                top: Math.max(0, start.top + dy),
            });
        };

        const handleMouseUp = () => {
            setDragging(false);
            dragStartRef.current = null;
        };

        window.addEventListener("mousemove", handleMouseMove);
        window.addEventListener("mouseup", handleMouseUp);

        return () => {
            window.removeEventListener("mousemove", handleMouseMove);
            window.removeEventListener("mouseup", handleMouseUp);
        };
    }, [dragging, onPositionChange]);

    const handleImportLabels = (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        e.target.value = "";

        if (!file)
            return;

        onImportCsv(file);
    };

    const handleDragStart = (e: React.MouseEvent<HTMLDivElement>) => {
        if (e.button !== 0)
            return;

        setDragging(true);
        dragStartRef.current = {
            mouseX: e.clientX,
            mouseY: e.clientY,
            left: position.left,
            top: position.top,
        };
    };

    const renderTable = () => {

        return <table className="text-xs w-full">
            <thead>
                <tr className="*:py-2 text-left *:pl-2">
                    <th className="invisible">{/*Show*/}</th>
                    {/* <th ></th> */}
                    <th >Time Range</th>
                    {/* <th >End time</th> */}
                    <th >Value Range</th>
                    {/* <th >Val end</th> */}
                    <th >Parameters</th>
                    <th >Label</th>
                    <th ></th>
                </tr>
            </thead>
            <tbody>
                {
                    flightLabelSections.map((section, i) => {
                        const showVal = (section.parameterNames?.length ?? 0) === 1;

                        return (
                            <tr
                                key={`${flightId}-${section.id ?? i}`}
                                className="*:select-auto! border-t *:px-2 *:py-1 *:min-w-12  *:pt-2"
                            >

                                {/* Chart Visibility Toggle */}
                                <td className="align-middle text-center min-w-0! pl-4! relative overflow-clip">
                                    <Checkbox
                                        checked={section.visibleOnChart !== false}
                                        disabled
                                        title={section.visibleOnChart !== false ? "Visible on chart" : "Hidden on chart"}
                                        className="m-0 p-0"
                                    />

                                    {/* Label Marker */}
                                    <span
                                        className="absolute -left-2 -top-2 h-4 aspect-square rounded-none rotate-45 border border-background opacity-75"
                                        style={{
                                            background: LABELING_MARKER_COLORS[i % LABELING_MARKER_COLORS.length],
                                        }}
                                        title={`Section ${i + 1}`}
                                    />
                                </td>

                                {/* Start/End Date & Time */}
                                <td className="align-middle *:text-nowrap">
                                    <div>{renderDateTime(section.startTime, "N/A")}</div>
                                    <div>{renderDateTime(section.endTime, "N/A")}</div>
                                </td>

                                {/* Start/End Values */}
                                <td>
                                    {showVal ? formatNumericValue(section.startValue) : "-"}
                                    &nbsp;-&nbsp;
                                    {showVal ? formatNumericValue(section.endValue) : "-"}
                                </td>

                                {/* Parameter Names */}
                                <td>{(section.parameterNames && section.parameterNames.length > 0) ? section.parameterNames.join(", ") : "-"}</td>

                                {/* Label Selection */}
                                <td style={{minWidth: 120 }}>
                                    {section.labelText || "-"}

                                </td>

                                {/* Remove Section Button */}
                                <td className="align-middle text-center">
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        className="p-0 aspect-square opacity-50 cursor-not-allowed"
                                        style={{ minWidth: 28 }}
                                        title="Remove section (coming soon)"
                                        disabled
                                    >
                                        <Trash />
                                    </Button>
                                </td>
                            </tr>
                        );
                    })
                }
            </tbody>
        </table>

    }

    const renderDownloadImportButtons = () => {

        return <>

            {/* Download Buttons */}
            <Button
                size="sm"
                variant="ghost"
                className="h-8 px-2 text-xs"
                onClick={onFlightCsvDownload}
            >
                <Download size={14} />
                Flight CSV
            </Button>

            <Button
                size="sm"
                variant="ghost"
                className="h-8 px-2 text-xs"
                onClick={onFleetCsvDownload}
            >
                <Download size={14} />
                Fleet CSV
            </Button>


            {/* Import Button */}
            <Button
                size="sm"
                variant="ghost"
                className="h-8 px-2 text-xs"
                onClick={() => labelImportInputRef.current?.click()}
            >
                <FileUp size={14} />
                Import CSV
            </Button>

            <input
                ref={labelImportInputRef}
                type="file"
                accept=".csv"
                className="hidden"
                onChange={handleImportLabels}
            />
        </>;

    }

    const render = () => {

        return (
            <Card
                className="absolute w-2xl bg-background/75 backdrop-blur-xs border shadow-sm z-20"
                style={{ left: position.left, top: position.top }}
            >
                <CardHeader className="flex justify-between flex-row cursor-grab active:cursor-grabbing pb-4" onMouseDown={handleDragStart}>

                    <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                            <MapPinned size={16} />
                            <div className="font-semibold text-sm">Labeling Tool - Flight {flightId}</div>
                        </div>
                        <div className="text-xs text-muted-foreground">
                            Label Count: {flightLabelSections.length}
                        </div>
                    </div>


                    <Button
                        size="sm"
                        variant="ghost"
                        className="h-8 px-2 text-xs w-fit"
                        onMouseDown={(e) => e.stopPropagation()}
                        onClick={onClose}
                    >
                        Close
                    </Button>
                </CardHeader>

                <CardContent className="pb-4 px-0 **:border-x-0">

                    <div className="overflow-auto max-h-85 border rounded-sm">
                        {renderTable()}
                    </div>
                </CardContent>

                <CardFooter className="flex gap-2 flex-wrap pb-4">

                    {renderDownloadImportButtons()}

                </CardFooter>

            </Card>
        );

    }

    return render();

}