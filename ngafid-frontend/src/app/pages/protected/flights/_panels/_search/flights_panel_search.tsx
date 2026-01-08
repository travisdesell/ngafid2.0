// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search.tsx
import FilterEditModal from "@/components/modals/filter_edit_modal";
import FilterListModal from "@/components/modals/filter_list_modal/filter_list_modal";
import { useModal } from "@/components/modals/modal_context";
import Ping from "@/components/pings/ping";
import { CommandData, useRegisterCommands } from "@/components/providers/commands_provider";
import { useFlightFilters } from "@/components/providers/flight_filters_provider";
import { getLogger } from "@/components/providers/logger";
import { useSystemIds } from "@/components/providers/system_ids_provider/system_ids_provider";
import { useTags } from "@/components/providers/tags/tags_provider";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardFooter } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { createRules, RuleOptions } from "@/pages/protected/flights/_filters/flights_filter_rules";
import { useFlightsFilter, useFlightsSearchFilter } from "@/pages/protected/flights/_flights_context_search_filter";
import { Bolt, ClipboardCopy, Ellipsis, Info, RotateCcw, Save, Search } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { useEffect, useMemo, useRef } from "react";
import { FlightsPanelSearchGroup } from "./flights_panel_search_group";

const log = getLogger("FlightsPanelSearch", "black", "Component");

export default function FlightsPanelSearch() {

    const { filter, filterSearched, setFilterFromJSON, filterIsEmpty, filterIsValid, revertFilter, copyFilterURL } = useFlightsFilter();
    const { fetchFlightsWithFilter } = useFlightsSearchFilter();
    const { filters, saveFilter, deleteFilterByName } = useFlightFilters();
    const { setModal } = useModal();
    const { fleetTags } = useTags();
    const { systemIds } = useSystemIds();


    const filterRef = useRef(filter);
    useEffect(() => { filterRef.current = filter; }, [filter]);

    const commands = useMemo<CommandData[]>(() => ([
        {
            id: "flights.copyFilterUrl",
            name: "Copy Filter URL",
            Icon: ClipboardCopy,
            hotkey: "Ctrl+Shift+C",
            command: () => copyFilterURL(filterRef.current),
            disabled: () => filterIsEmpty(filterRef.current),
        },
        {
            id: "flights.saveCurrentFilter",
            name: "Save Current Filter",
            Icon: Save,
            hotkey: "Ctrl+Shift+S",
            command: () => setModal(FilterEditModal, {filter: filterRef.current, saveFilter}),
            disabled: () => filterIsEmpty(filterRef.current),
        },
        {
            id: "flights.loadSavedFilter",
            name: "Load Saved Filter",
            Icon: Ellipsis,
            hotkey: "Ctrl+Shift+O",
            command: () => setModal(FilterListModal, {filters, saveFilter, deleteFilterByName, setFilterFromJSON, copyFilterURL}),
        },
        {
            id: "flights.revertFilter",
            name: "Revert Filter",
            Icon: RotateCcw,
            hotkey: "Ctrl+Shift+R",
            command: () => revertFilter(),
            disabled: () => {
                const hasPreviousSearch = (!!filterSearched && !filterIsEmpty(filterSearched));
                const filterDiffersFromLastSearch = (hasPreviousSearch && (filterRef.current !== filterSearched));
                return !(hasPreviousSearch && filterDiffersFromLastSearch);
            },
        },
        {
            id: "flights.submitSearch",
            name: "Search Flights",
            Icon: Search,
            hotkey: "Ctrl+Shift+Enter",
            command: () => {
                if (!filterIsValid(filterRef.current))
                    return;
                void fetchFlightsWithFilter(filterRef.current, true);
            },
            disabled: () => {
                const isFilterValid = filterIsValid(filterRef.current);
                const hasPreviousSearch = (!!filterSearched && !filterIsEmpty(filterSearched));
                const filterDiffersFromLastSearch = (hasPreviousSearch && (filterRef.current !== filterSearched));
                return !(isFilterValid && (!hasPreviousSearch || filterDiffersFromLastSearch));
            }
        }
    ]), [copyFilterURL, filterIsEmpty, saveFilter, setModal, deleteFilterByName, filters, setFilterFromJSON]);

    useRegisterCommands(commands);


    const ruleDefinitions = useMemo(() => {
        
        const systemIDNames = systemIds.map(id => id.toString())
            .sort((a, b) => a.localeCompare(b));

        const tagNames = fleetTags
            .map(t => t.name)
            .sort((a, b) => a.localeCompare(b));

        const ruleOptions: RuleOptions = {
            airframes: [],              // TODO
            systemIds: systemIDNames,
            tailNumbers: [],            // TODO
            timeZones: ["UTC", "Local"],
            doubleTimeSeriesNames: [],  // TODO
            visitedAirports: [],        // TODO
            visitedRunways: [],         // TODO
            eventNames: [],             // TODO
            tagNames,
        };

        return createRules(ruleOptions);

    }, [fleetTags, systemIds, /* TODO dependencies for other options */]);


    const isFilterValid = useMemo(
        () => filterIsValid(filter),
        [filter, filterIsValid],
    );

    const hasPreviousSearch = (!!filterSearched && !filterIsEmpty(filterSearched));
    const filterDiffersFromLastSearch = (hasPreviousSearch && (filter !== filterSearched));

    const displayRevert = hasPreviousSearch;
    const allowRevert = (displayRevert && (filter !== filterSearched));

    const allowFilterURLCopyAndSave = isFilterValid;

    const allowSearchSubmit = (isFilterValid)
        && (!hasPreviousSearch || filterDiffersFromLastSearch);

    const handleSearchClick = () => {

        if (!filterIsValid(filter))
            return;

        void fetchFlightsWithFilter(filter, true);

    }

    const renderSearchSubmitRow = () => {

        return <div className="flex flex-row gap-2 w-full p-2 @container">

            {/* Copy Filter URL */}
            <Button
                onClick={() => copyFilterURL(filter)}
                disabled={!allowFilterURLCopyAndSave}
                className="@4xl:after:content-['Copy_Filter_URL']"
            >
                <ClipboardCopy />
            </Button>

            {/* Save Current Filter */}
            <Button
                onClick={() => setModal(FilterEditModal, {filter, saveFilter})}
                disabled={!allowFilterURLCopyAndSave}
                className="@2xl:after:content-['Save_Current_Filter']"
            >
                <Save />
            </Button>

            {/* Load a Saved Filter */}
            <Button
                onClick={() => setModal(FilterListModal, {filters, saveFilter, deleteFilterByName, setFilterFromJSON, copyFilterURL})}
                className="@2xl:after:content-['Load_Saved_Filter']"
            >
                <Ellipsis />
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
                            className="@4xl:after:content-['Revert_Filter']"
                        >
                            <RotateCcw />
                        </Button>
                    </motion.div>
            }

                {/* Submit Search */}
                <Button
                    className="relative @2xl:after:content-['Search_Flights']"
                    onClick={handleSearchClick}
                    disabled={!allowSearchSubmit}
                >
                    {
                        (allowSearchSubmit)
                        &&
                        <Ping />
                    }
                    <Search />
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
                    animate={{overflow: "clip"}}
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
                    <FlightsPanelSearchGroup
                        depth={0}
                        group={filter}
                        indexPath={[]}
                        ruleDefinitions={ruleDefinitions}
                    />

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