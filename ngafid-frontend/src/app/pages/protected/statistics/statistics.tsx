// ngafid-frontend/src/app/pages/protected/statistics/statistics.tsx

import { setPageTitle } from "@/components/page_title";
import PanelAlert from "@/components/panel_alert";
import Ping from "@/components/pings/ping";
import { ALL_AIRFRAMES_ID, useAirframes } from "@/components/providers/airframes_provider";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { AIRFRAME_NAMES_IGNORED } from "@/lib/airframe_names_ignored";
import AirframeStatisticsContent from "@/pages/protected/statistics/statistics_content";
import { AirframeCardData } from "@/pages/protected/statistics/types";
import { Loader2 } from "lucide-react";
import { useMemo, useState } from "react";


const log = getLogger("Statistics", "black", "Page");

const GENERIC_AIRFRAME_ID = 0;
const GENERIC_AIRFRAME_NAME = "Generic";


export default function StatisticsPage() {

    const { airframes } = useAirframes();

    const [isLoading, setIsLoading] = useState(false);
    const [airframeIDSelected, setAirframeIDSelected] = useState<number|null>(null);
    const [showNoDataAlert, setShowNoDataAlert] = useState(false);
    const hasSelectedAirframe = (airframeIDSelected !== null);

    setPageTitle("Event Statistics");


    const allAirframeCards = useMemo<AirframeCardData[]>(() => {
        const output: AirframeCardData[] = [];
        const seenIds = new Set<number>();

        const pushCard = (id: number, name: string) => {
            if (seenIds.has(id))
                return;

            seenIds.add(id);
            output.push({ id, name });
        };

        pushCard(GENERIC_AIRFRAME_ID, GENERIC_AIRFRAME_NAME);

        for (const airframe of airframes) {

            // ...
            if (airframe.id === ALL_AIRFRAMES_ID)
                continue;

            // ...
            if (AIRFRAME_NAMES_IGNORED.includes(airframe.name))
                continue;
    
            pushCard(airframe.id, airframe.name);
        }

        return output;

    }, [airframes]);


    const updateSelectedAirframeID = (id: number|null) => {

        log("Selected airframe ID:", id);

        // Update state with new selection
        setAirframeIDSelected(id);
        setShowNoDataAlert(false);

        // If "All Airframes" selected, load all airframe statistics
        if (id === ALL_AIRFRAMES_ID) {
            log("Selected 'All Airframes', loading statistics for all airframes.");
            setIsLoading(true);
        }
        else {
            log("Selected airframe ID:", id);
            setIsLoading(true);
        }

    }

    const disableSelection = (airframes.length === 0 || isLoading);

    log("Rendering Statistics page with airframes:", allAirframeCards);

    return (
        <div className="page-content-thin gap-2">

            <Card className="card-glossy flex-1 min-h-0 overflow-hidden flex flex-col relative">

                {/* Event Statistics Header */}
                <CardHeader className="flex flex-row justify-between items-start gap-2">

                    {/* Header Text */}
                    <div className="flex flex-col gap-1.5">
                        <CardTitle>Event Statistics</CardTitle>
                        <CardDescription>View the event statistics for the selected airframe type.</CardDescription>
                    </div>

                    {/* Airframe Selection */}
                    <div className="flex flex-col gap-2 justify-start items-start">
   
                        <Select
                            disabled={disableSelection}
                            value={undefined}
                            onValueChange={(value) => {
                                const id = parseInt(value);
                                updateSelectedAirframeID(id);
                            }}
                        >
                            <Button asChild variant="outline" className="relative">
                                <SelectTrigger className="w-50" defaultValue={"Test"}>

                                    {/* Initial Selection Ping */}
                                    {
                                        (!hasSelectedAirframe)
                                        &&
                                        <Ping />
                                    }

                                    {/* Selected Value */}
                                    <SelectValue placeholder="Select Airframe" />

                                </SelectTrigger>
                            </Button>
                            <SelectContent>
                                {
                                    allAirframeCards.map((pair) => (
                                        <SelectItem key={pair.id} value={pair.id.toString()}>{pair.name}</SelectItem>
                                    ))
                                }
                            </SelectContent>
                        </Select>
                    </div>

                </CardHeader>

                <CardContent className="flex-1 min-h-0 p-0 overflow-y-auto">

                    {/* No Airframe Selected Message */}
                    {
                        (!hasSelectedAirframe)
                        &&
                        <PanelAlert title={"No Airframe Selected"} description={"Select an airframe from the dropdown menu to view its event statistics."} />
                    }

                    {/* Selected Airframe Statistics */}
                    {
                        (hasSelectedAirframe)
                        &&
                        showNoDataAlert
                        &&
                        <PanelAlert
                            title={"No Event Statistics Available"}
                            description={"No event statistics are available for the selected airframe."}
                        />
                    }

                    {/* Selected Airframe Statistics */}
                    {
                        (hasSelectedAirframe)
                        &&
                        allAirframeCards
                            .filter((airframe) => airframe.id === airframeIDSelected)
                            .map((airframe) => (
                                <AirframeStatisticsContent
                                    key={airframe.id}
                                    airframeId={airframe.id}
                                    airframeName={airframe.name}
                                    setIsLoading={setIsLoading}
                                    setShowNoDataAlert={setShowNoDataAlert}
                                />
                            ))
                    }
                </CardContent>

                {
                    (isLoading)
                    &&
                    <div className="absolute inset-0 flex items-center justify-center">
                        <Loader2 size={128} className="animate-spin text-gray-500" />
                    </div>
                }

            </Card>

        </div>

    );

}