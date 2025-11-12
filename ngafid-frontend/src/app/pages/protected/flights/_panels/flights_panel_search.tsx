// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search.tsx
import ErrorModal from "@/components/modals/error_modal";
import FilterEditModal from "@/components/modals/filter_edit_modal";
import FilterListModal from "@/components/modals/filter_list_modal";
import { useModal } from "@/components/modals/modal_provider";
import SuccessModal from "@/components/modals/success_modal";
import Ping from "@/components/pings/ping";
import { useFlightFilters } from "@/components/providers/flight_filters_provider";
import { getLogger } from "@/components/providers/logger";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardFooter } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { toWire, u8ToBase64url } from "@/pages/protected/flights/_filters/flights_filter_copy_helpers";
import { useFlights } from "@/pages/protected/flights/flights";
import { Bolt, ClipboardCopy, Ellipsis, Info, RotateCcw, Save, Search } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import pako from "pako";
import { FlightsPanelSearchGroup } from "./flights_panel_search_group";

const log = getLogger("FlightsPanelSearch", "black", "Component");

export default function FlightsPanelSearch() {

    const { filter, filterSearched, setFilterFromJSON, filterIsEmpty, filterIsValid, fetchFlightsWithFilter, revertFilter } = useFlights();
    const { filters, saveFilter, deleteFilterByName } = useFlightFilters();
    const { setModal } = useModal();


    const copyFilterURL = () => {

        /*
            Generates a URL encoding for the
            current filter from a JSON string.

            The URL is then copied to the user's
            clipboard.

            TODO: Compress the URL so it doesn't
            exceed the maximum URL length after
            just a few rules...
        */

        const wire = toWire(filter);
        if (!wire) {
            setModal(ErrorModal, { title: "Filter Empty", message: "Add at least one complete rule." });
            return;
        }

        // JSON -> deflate -> base64url
        const json = JSON.stringify(wire);
        const deflated = pako.deflate(json);
        const payload = u8ToBase64url(deflated);

        const fullURL = `${location.origin}${location.pathname}?f=${payload}`;

        log.info("Generated Filter URL: ", fullURL);

        // Clipboard unavailable
        if (!navigator.clipboard) {
            setModal(ErrorModal, { title: "Clipboard Unavailable", message: "Your browser does not support clipboard operations. You can try manually copying the URL below:", code: fullURL });
            return;
        }

        // Attempt to the URL to the clipboard
        navigator.clipboard.writeText(fullURL)
            .then(
                () => setModal(SuccessModal, { title: "Filter URL Copied", message: "The URL linking to this filter has been copied to your clipboard." })
            )
            .catch(
                () => setModal(ErrorModal, { title: "Error Copying Filter URL", message: "An error occurred while trying to copy the filter URL to your clipboard.", code: "kek!" })
            );

    }

    const renderSearchSubmitRow = () => {

        const displayRevert = (filterSearched && !filterIsEmpty(filterSearched));
        const allowRevert = (displayRevert && (JSON.stringify(filter) !== JSON.stringify(filterSearched)));

        const allowFilterURLCopyAndSave = filterIsValid(filter);

        const allowSearchSubmit = (displayRevert)
            ? (allowRevert && filterIsValid(filter))
            : filterIsValid(filter);

        return <div className="flex flex-row gap-2 w-full p-2">

            {/* Copy Filter URL */}
            <Button
                onClick={copyFilterURL}
                disabled={!allowFilterURLCopyAndSave}
            >
                <ClipboardCopy /> Copy Filter URL
            </Button>

            {/* Save Current Filter */}
            <Button
                onClick={() => setModal(FilterEditModal, {filter, saveFilter})}
                disabled={!allowFilterURLCopyAndSave}
            >
                <Save /> Save Current Filter
            </Button>

            {/* Load a Saved Filter */}
            <Button onClick={() => setModal(FilterListModal, {filters, saveFilter, deleteFilterByName, setFilterFromJSON})}>
                <Ellipsis /> Load a Saved Filter
            </Button>

            <div className="ml-auto flex flex-row gap-2">

                {/* Revert Filter */}
                {
                    (displayRevert)
                    &&
                    <motion.div
                        initial={{ opacity: 0, scale: 0 }}
                        animate={{ opacity: 1, scale: 1, transition: { duration: 0.25 } }}
                    >
                        <Button
                            onClick={revertFilter}
                            disabled={!allowRevert}
                        >
                            <RotateCcw /> Revert Filter
                        </Button>
                    </motion.div>
            }

                {/* Submit Search */}
                <Button
                    className="relative"
                    onClick={() => { void fetchFlightsWithFilter(filter, true) }}
                    disabled={!allowSearchSubmit}
                >
                    {
                        (allowSearchSubmit)
                        &&
                        <Ping />
                    }
                    <Search /> Search Flights
                </Button>

            </div>

        </div>

    }

    const renderEmptyFilterMessage = () => {

        log("Filter is currently empty, showing empty filter message.");

        return <motion.div
            key="empty-filter-message"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 *:text-nowrap"
        >
            <Info className=""/>

            <div className="flex flex-col">
                <AlertTitle>Filter Empty!</AlertTitle>
                <AlertDescription>
                    Your current filter does not contain any rules or groups.
                    <br />
                    <div className="flex">Try adding a rule with the <div className="flex items-center font-bold gap-1 mx-2"><Bolt size={16} />New Rule</div> button above.</div>
                </AlertDescription>
            </div>
        </motion.div>

    }

    const render = () => {

        return (
            <Card className="w-full h-full min-h-0 card-glossy flex flex-col justify-between overflow-clip">

                {/* Search Filter */}
                <motion.div
                    layoutScroll
                    className="flex-1 min-h-0 w-full overflow-y-auto relative"
                >

                    {/* Empty Filter Message */}
                    <AnimatePresence mode="wait" initial={false}>
                    {
                        (filterIsEmpty(filter))
                        &&
                        renderEmptyFilterMessage()
                    }
                    </AnimatePresence>

                    {/* Top-Level Search Group */}
                    <FlightsPanelSearchGroup depth={0} group={filter} indexPath={[]} />

                </motion.div>

                {/* Search Submit Row */}
                <CardFooter className="flex flex-col w-full p-0 bg-muted">
                    <Separator />
                    {renderSearchSubmitRow()}
                </CardFooter>

            </Card>
        );

    }

    return render();

}