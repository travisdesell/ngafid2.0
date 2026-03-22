// ngafid-frontend/src/app/pages/protected/flights/_panels/_chart/flights_panel_chart_label_card.tsx

import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { renderDateTime } from "@/pages/protected/flights/_flight_row/flight_row";
import { FlightLabelSection } from "@/pages/protected/flights/_flights_context_chart";
import { Download, FileUp, MapPinned, Minus, Trash } from "lucide-react";
import { ChangeEvent, useEffect, useRef, useState } from "react";

type Props = {
    flightId: number;
    pendingStartX: number | null;
    flightLabelSections: FlightLabelSection[];
    onFlightCsvDownload: () => void;
    onFleetCsvDownload: () => void;
    onImportCsv: (file: File) => void;
    onRemoveSection: (sectionIndex: number) => void;
    labelDefinitions: FleetLabelDefinition[];
    onUpdateSectionLabel: (sectionIndex: number, labelText: string) => void;
    onCreateLabelDefinition: (labelText: string) => Promise<boolean>;
    onClose: () => void;
    position: { left: number; top: number };
    onPositionChange: (next: { left: number; top: number }) => void;
};

type FleetLabelDefinition = {
    id: number;
    labelText: string;
    displayOrder: number;
};

const ADD_NEW_LABEL_VALUE = "__add_new__";
const NONE_LABEL_VALUE = "__none__";

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
    onRemoveSection,
    labelDefinitions,
    onUpdateSectionLabel,
    onCreateLabelDefinition,
    onClose,
    position,
    onPositionChange,
}: Props) {

    const cardRef = useRef<HTMLDivElement | null>(null);
    const labelImportInputRef = useRef<HTMLInputElement | null>(null);
    const [dragging, setDragging] = useState(false);
    const [localPosition, setLocalPosition] = useState(position);
    const localPositionRef = useRef(position);
    const dragStartRef = useRef<{ mouseX: number; mouseY: number; left: number; top: number } | null>(null);

    useEffect(() => {
        if (dragging)
            return;

        setLocalPosition(position);
        localPositionRef.current = position;
    }, [position, dragging]);

    useEffect(() => {
        if (!dragging)
            return;

        const handleMouseMove = (e: MouseEvent) => {
            const start = dragStartRef.current;
            if (!start)
                return;

            const dx = e.clientX - start.mouseX;
            const dy = e.clientY - start.mouseY;

            let nextLeft = Math.max(0, start.left + dx);
            let nextTop = Math.max(0, start.top + dy);

            const cardElement = cardRef.current;
            const containerElement = cardElement?.parentElement;

            if (cardElement && containerElement) {
                const maxLeft = Math.max(0, containerElement.clientWidth - cardElement.offsetWidth);
                const maxTop = Math.max(0, containerElement.clientHeight - cardElement.offsetHeight);

                nextLeft = Math.min(nextLeft, maxLeft);
                nextTop = Math.min(nextTop, maxTop);
            }

            const nextPosition = {
                left: nextLeft,
                top: nextTop,
            };

            localPositionRef.current = nextPosition;
            setLocalPosition(nextPosition);
        };

        const handleMouseUp = () => {
            onPositionChange(localPositionRef.current);
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
            left: localPositionRef.current.left,
            top: localPositionRef.current.top,
        };
    };

    const handleLabelSelectionChange = async (sectionIndex: number, nextValue: string) => {
        if (nextValue !== ADD_NEW_LABEL_VALUE) {
            onUpdateSectionLabel(sectionIndex, nextValue);
            return;
        }

        const text = window.prompt("New label text (e.g. 40-my cluster name):");
        if (text == null || String(text).trim() === "")
            return;

        const labelText = String(text).trim();
        const created = await onCreateLabelDefinition(labelText);
        if (!created)
            return;

        onUpdateSectionLabel(sectionIndex, labelText);
    };

    const renderTable = () => {

        // No Labels Yet
        if (!flightLabelSections || flightLabelSections.length === 0) {

            return <div className="p-4 text-center text-sm text-muted-foreground">
                No labels for this flight exist yet.
            </div>;

        }

        return <table className="text-xs w-full">
            <thead>
                <tr className="*:py-2 text-left *:pl-2">
                    <th className="invisible">{/*Show*/}</th>
                    <th >Time Range</th>
                    <th >Value Range</th>
                    <th >Parameters</th>
                    <th className="pl-6!">Label</th>
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
                                className="*:select-auto! border-t *:px-2 *:min-w-12 *:py-2"
                            >

                                {/* Chart Visibility Toggle */}
                                <td className="text-center min-w-0! pl-4! pt-3! relative overflow-clip">
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
                                    <div className="flex w-fit gap-2">
                                        {showVal ? formatNumericValue(section.startValue) : "-"}
                                        <Minus size={16} className="opacity-25"/>
                                        {showVal ? formatNumericValue(section.endValue) : "-"}
                                    </div>
                                </td>

                                {/* Parameter Names */}
                                <td>
                                    {
                                        (section.parameterNames && section.parameterNames.length > 0)
                                            ? section.parameterNames.join(", ")
                                            : "-"
                                    }
                                </td>

                                {/* Label Selection */}
                                <td className="align-middle py-0!">
                                    <Select
                                        value={section.labelText || NONE_LABEL_VALUE}
                                        onValueChange={(value) => {
                                            const normalized = (value === NONE_LABEL_VALUE) ? "" : value;
                                            void handleLabelSelectionChange(i, normalized);
                                        }}
                                    >
                                        <SelectTrigger
                                            className={cn(
                                                buttonVariants({ variant: "ghost" }),
                                                "text-white w-42 text-xs border-none shadow-none"
                                            )}
                                            onMouseDown={(e) => e.stopPropagation()}
                                            title="Select label or add new"
                                        >
                                            <SelectValue placeholder="-" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectGroup>
                                                <SelectItem value={NONE_LABEL_VALUE}>-</SelectItem>
                                                {
                                                    labelDefinitions
                                                        .slice()
                                                        .sort((a, b) => (a.displayOrder - b.displayOrder) || a.labelText.localeCompare(b.labelText))
                                                        .map((labelDef) => (
                                                            <SelectItem key={labelDef.id} value={labelDef.labelText}>
                                                                {labelDef.labelText}
                                                            </SelectItem>
                                                        ))
                                                }
                                                {
                                                    section.labelText
                                                        && !labelDefinitions.some((def) => def.labelText === section.labelText)
                                                        && (
                                                            <SelectItem value={section.labelText}>{section.labelText}</SelectItem>
                                                        )
                                                }
                                                <SelectItem value={ADD_NEW_LABEL_VALUE}>+ Add new label...</SelectItem>
                                            </SelectGroup>
                                        </SelectContent>
                                    </Select>
                                </td>

                                {/* Remove Section Button */}
                                <td className="align-middle text-center">
                                    <Button
                                        variant="ghostDestructive"
                                        size="sm"
                                        className="p-0 aspect-square"
                                        style={{ minWidth: 28 }}
                                        title="Remove section"
                                        onClick={() => onRemoveSection(i)}
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
                ref={cardRef}
                className="absolute w-2xl bg-background/75 backdrop-blur-xs border shadow-sm z-20"
                style={{ left: localPosition.left, top: localPosition.top }}
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