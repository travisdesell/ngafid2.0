// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search.tsx
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { FlightsPanelSearchGroup } from "./flights_panel_search_group";
import { ClipboardCopy, Ellipsis, Save, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useModal } from "@/components/modals/modal_provider";
import SuccessModal from "@/components/modals/success_modal";
import { useFlights } from "@/pages/protected/flights/flights";
import { Separator } from "@/components/ui/separator";
import { motion } from "motion/react";
import FilterEditModal from "@/components/modals/filter_edit_modal";
import { useFlightFilters } from "@/components/providers/flight_filters_provider";
import FilterListModal from "@/components/modals/filter_list_modal";

export default function FlightsPanelSearch() {

    const { allowSearchSubmit, filter } = useFlights();
    const { saveFilter, deleteFilterByName, filters } = useFlightFilters();
    const { setModal } = useModal();

    const renderSearchSubmitRow = () => {

        return <div className="flex flex-row gap-2 w-full p-2">

            {/* Copy Filter URL */}
            <Button onClick={() => setModal(SuccessModal, { title: "Filter URL Copied", message: "The URL linking to this filter has been copied to your clipboard." })}>
                <ClipboardCopy /> Copy Filter URL
            </Button>

            {/* Load a Saved Filter */}
            <Button onClick={() => setModal(FilterListModal, {filters, saveFilter, deleteFilterByName})}>
                <Ellipsis /> Load a Saved Filter
            </Button>

            {/* Save Current Filter */}
            <Button onClick={() => setModal(FilterEditModal, {saveFilter})}>
                <Save /> Save Current Filter
            </Button>

            {/* Submit Search */}
            <Button className="ml-auto" disabled={!allowSearchSubmit}>
                <Search /> Search Flights
            </Button>

        </div>

    }

    const render = () => {

        return (
            <Card className="w-full h-full min-h-0 card-glossy flex flex-col justify-between overflow-clip">

                {/* Search Filter */}
                <motion.div
                    layoutScroll
                    className="flex-1 min-h-0 w-full overflow-y-auto bg-muted "
                >

                    {/* Top-Level Search Group */}
                    <FlightsPanelSearchGroup depth={0} group={filter} indexPath={[]} />

                </motion.div>

                {/* Search Submit Row */}
                <CardFooter className="flex flex-col w-full p-0">
                    <Separator />
                    {renderSearchSubmitRow()}
                </CardFooter>

            </Card>
        );

    }

    return render();

}