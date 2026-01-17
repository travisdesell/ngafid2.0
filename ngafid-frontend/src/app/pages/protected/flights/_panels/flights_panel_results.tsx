// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search.tsx
import { getLogger } from "@/components/providers/logger";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardFooter } from "@/components/ui/card";
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { SORTABLE_COLUMN_NAMES, SORTABLE_COLUMN_VALUES, SORTABLE_COLUMNS } from "@/pages/protected/flights/_filters/flights_filter_rules";
import FlightRow from "@/pages/protected/flights/_flight_row/flight_row";
import { useFlightsResults } from "@/pages/protected/flights/_flights_context_results";
import { useFlightsSearchFilter } from "@/pages/protected/flights/_flights_context_search_filter";
import { FLIGHTS_PER_PAGE_OPTIONS, isValidSortingDirection } from "@/pages/protected/flights/types";
import { useVirtualizer } from "@tanstack/react-virtual";
import { ArrowDownWideNarrow, ArrowUpDown, Info, ListOrdered, Loader2 } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { memo, useRef } from "react";

const log = getLogger("FlightsPanelResults", "black", "Component");


function FlightsPanelResultsInner() {

    const { flights, totalFlights } = useFlightsResults();
    const {
        isFilterSearchLoading,
        isFilterSearchLoadingManual,
        currentPage, setCurrentPage,
        sortingColumn, setSortingColumn,
        sortingDirection, setSortingDirection,
        pageSize, setPageSize
    } = useFlightsSearchFilter();

    const scrollParentRef = useRef<HTMLDivElement | null>(null);
    const FLIGHT_ROW_HEIGHT_ESTIMATE_PX = 144;

    const rowVirtualizer = useVirtualizer({
        count: flights.length,
        getScrollElement: () => scrollParentRef.current,
        estimateSize: () => FLIGHT_ROW_HEIGHT_ESTIMATE_PX,  //<-- Estimated height of each row in pixels
        overscan: 2,    //<-- Extra rows rendered beyond the visible area
    });

    const updateSortingColumn = (value: string) => {

        const isValidSortingColumn = (value: string): boolean => {
            return SORTABLE_COLUMN_VALUES.includes(value);
        }

        if (!isValidSortingColumn(value)) {
            log.error("Invalid sorting column value:", value);
            return;
        }

        setSortingColumn(value);
        log("Updated sorting column to:", value);

    }

    const updateSortingDirection = (value: string) => {

        if (!isValidSortingDirection(value)) {
            log.error("Invalid sorting direction value:", value);
            return;
        }

        setSortingDirection(value);
        log("Updated sorting direction to:", value);

    }

    const updatePageSize = (value: string) => {
        
        const newSize = parseInt(value);

        if (isNaN(newSize)) {
            log.error("Invalid page size value:", value);
            return;
        }

        setPageSize(newSize);
        log("Updated flights per page to:", newSize);

    }

    const renderEmptyResultsMessage = () => {

        log("No flight results to display, showing empty results message.");

        return <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap"
        >
            <Info className=""/>

            <div className="flex flex-col">
                <AlertTitle>No Results!</AlertTitle>
                <AlertDescription>
                    Flights matching the current search criteria will appear here.
                    <br />
                    Try adjusting your search filters to find more flights.
                </AlertDescription>
            </div>
        </motion.div>

    }

    const renderPaginationRow = () => {

        const pagePreviousAvailable:boolean = (currentPage > 0);
        const pageNextAvailable:boolean = (() => {

            const maxPage = Math.ceil(totalFlights / (pageSize || FLIGHTS_PER_PAGE_OPTIONS[0])) - 1;
            return (currentPage < maxPage);

        })();

        const pageButtonEnabled = `cursor-pointer`;
        const pageButtonDisabled = `opacity-50 pointer-events-none`;

        const pagePrevious = () => {

            if (!pagePreviousAvailable) {
                log("Already on first page, cannot navigate to previous page.");
                return;
            }

            setCurrentPage(currentPage - 1);
            log("Navigating to previous page:", currentPage - 1);
        }

        const pageNext = () => {

            // const maxPage = Math.ceil(totalFlights / (pageSize || FLIGHTS_PER_PAGE_OPTIONS[0])) - 1;
            if (!pageNextAvailable) {
                log("Already on last page, cannot navigate to next page.");
                return;
            }

            setCurrentPage(currentPage + 1);
            log("Navigating to next page:", currentPage + 1);
        }

        return <div className="flex flex-row gap-2 w-full p-2 @container">

            {/* Pagination Controls */}
            <Pagination className="w-fit m-0 mr-auto">
                <PaginationContent>

                    {/* Previous Page Button */}
                    <PaginationItem>
                        <PaginationPrevious onClick={pagePrevious} className={pagePreviousAvailable ? pageButtonEnabled : pageButtonDisabled} />
                    </PaginationItem>

                    {/* Current Page */}
                    <PaginationItem>
                        <PaginationLink >
                            {currentPage+1}
                        </PaginationLink>
                    </PaginationItem>

                    {/* ... */}
                    <PaginationItem>
                        <PaginationEllipsis />
                    </PaginationItem>

                    {/* Next Page Button */}
                    <PaginationItem>
                        <PaginationNext onClick={pageNext} className={pageNextAvailable ? pageButtonEnabled : pageButtonDisabled} />
                    </PaginationItem>

                </PaginationContent>
            </Pagination>

            {/* Flight Count Badge */}
            {
                (totalFlights > 0)
                &&
                <motion.div
                    initial={{ opacity: 0, scale: 0 }}
                    animate={{ opacity: 1, scale: 1 }}
                >
                    <Badge variant="outline" className="text-center bg-background rounded-full px-4 mr-2 h-full text-nowrap @5xl:after:content-['_Found'] whitespace-pre select-none">
                        {(totalFlights).toLocaleString()} Flight{totalFlights !== 1 ? "s" : ""}
                    </Badge>
                </motion.div>
            }

            {/* Sorting Select Dropdown */}
            <Select onValueChange={(value) => updateSortingColumn(value)} value={sortingColumn || ""}>
                <Button variant="outline" asChild>
                    <SelectTrigger className="min-w-16 w-fit">
                        <ArrowDownWideNarrow />
                        <div className="@max-4xl:hidden!">
                            <SelectValue placeholder="Sort by" />
                        </div>
                    </SelectTrigger>
                </Button>
                <SelectContent>
                    {
                        SORTABLE_COLUMN_NAMES.map((columnName) => (
                            <SelectItem key={columnName} value={SORTABLE_COLUMNS[columnName]}>
                                {columnName}
                            </SelectItem>
                        ))
                    }
                </SelectContent>
            </Select>

            {/* Sorting Order (Ascending/Descending) Select Dropdown */}
            <Select onValueChange={(value) => updateSortingDirection(value)} value={sortingDirection}>
                <Button variant="outline" asChild>
                    <SelectTrigger className="min-w-16 w-fit">
                        <ArrowUpDown />
                        <div className="@max-4xl:hidden!">
                            <SelectValue placeholder="Order" />
                        </div>
                    </SelectTrigger>
                </Button>
                <SelectContent>
                    <SelectItem value="Ascending">Ascending</SelectItem>
                    <SelectItem value="Descending">Descending</SelectItem>
                </SelectContent>
            </Select>

            {/* Flights Per Page Select Dropdown */}
            <Select onValueChange={(value) => updatePageSize(value)} value={pageSize?.toString()}>
                <Button variant="outline" asChild>
                    <SelectTrigger className="min-w-16 w-fit">
                        <ListOrdered />
                        <div className="@max-4xl:hidden!">
                            <SelectValue placeholder="Flights per page" />
                        </div>
                    </SelectTrigger>
                </Button>
                <SelectContent>
                    {
                        FLIGHTS_PER_PAGE_OPTIONS.map((option) => (
                            <SelectItem key={option} value={option.toString()}>
                                {option} per page
                            </SelectItem>
                        ))
                    }
                </SelectContent>
            </Select>

        </div>

    }

    const render = () => {

        log("Rendering Flights Panel Results");

        const displayFlightsEmpty = (flights.length === 0);
        const getFlightRowAnimationDelay = (index: number): number => {

            const MAX_DELAY = 0.50;
            const DELAY_INCREMENT = 0.03;

            let delay = index * DELAY_INCREMENT;
            if (delay > MAX_DELAY)
                delay = MAX_DELAY;

            return delay;

        }

        return (
            <Card className="w-full h-full min-h-0 card-glossy flex flex-col justify-between overflow-clip relative">

                {/* Results List */}
                <motion.div
                    ref={scrollParentRef}
                    className="flex-1 min-h-0 h-full w-full overflow-y-auto"
                >

                    {/* Loading Spinner */}
                    <AnimatePresence>
                    {
                        (isFilterSearchLoadingManual || isFilterSearchLoading)
                        &&
                        <motion.div
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="absolute w-full h-full bg-muted/80 z-1 overflow-clip"
                        >
                            <AnimatePresence>
                            <motion.div
                                initial={{ scale: 0.0 }}
                                animate={{ scale: 1.0 }}
                                exit={{ scale: 0.0 }}
                                className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2"
                            >
                                <Loader2 className="animate-spin ml-auto" size={64} />
                            </motion.div>
                            </AnimatePresence>
                        </motion.div>
                    }
                    </AnimatePresence>

                    {/* Empty Results Message */}
                    {
                        (displayFlightsEmpty)
                        &&
                        <div className="min-h-full flex items-center justify-center p-6">
                            {renderEmptyResultsMessage()}
                        </div>
                    }

                    {/* Virtualized Flight Results List */}
                    {
                        (!displayFlightsEmpty)
                        &&
                        <div
                            id="flights-results-list-virtualized"
                            className="w-full relative"
                            style={{height: rowVirtualizer.getTotalSize()}}
                        >
                                {
                                    rowVirtualizer.getVirtualItems().map((virtualRow) => {
                                        const flight = flights[virtualRow.index];

                                        return <div
                                                key={virtualRow.key}
                                                className="absolute top-0 left-0 w-full"
                                                style={{
                                                    transform: `translateY(${virtualRow.start}px)`,
                                                }}
                                            >
                                                <motion.div
                                                    initial={{ opacity: 0, y: 0 }}
                                                    animate={{ opacity: 1, y: 0 }}
                                                >
                                                    <FlightRow flight={flight} />
                                                </motion.div>
                                            </div>
                                    })
                                }
                                        
                        </div>

                    }

                </motion.div>

                {/* Pagination Row */}
                <CardFooter className="flex flex-col w-full p-0 bg-muted">
                    <Separator />
                    {renderPaginationRow()}
                </CardFooter>

            </Card>
        );

    }

    return render();

}

export const FlightsPanelResults = memo(FlightsPanelResultsInner);