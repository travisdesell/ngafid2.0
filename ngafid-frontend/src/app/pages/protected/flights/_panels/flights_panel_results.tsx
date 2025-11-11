// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search.tsx
import { useModal } from "@/components/modals/modal_provider";
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
import { FLIGHTS_PER_PAGE_OPTIONS, isValidSortingDirection, useFlights } from "@/pages/protected/flights/flights";
import { ArrowDownWideNarrow, ArrowUpDown, Info, ListOrdered } from "lucide-react";
import { motion } from "motion/react";

const log = getLogger("FlightsPanelResults", "black", "Component");


export default function FlightsPanelResults() {

    const { setModal } = useModal();
    const { flights, totalFlights, sortingColumn, setSortingColumn, sortingDirection, setSortingDirection, pageSize, setPageSize } = useFlights();

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

        return <div className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center">
            <Info className=""/>

            <div className="flex flex-col">
                <AlertTitle>No Results!</AlertTitle>
                <AlertDescription>
                    Flights matching the current search criteria will appear here.
                    <br />
                    Try adjusting your search filters to find more flights.
                </AlertDescription>
            </div>
        </div>

    }

    const renderPaginationRow = () => {

        return <div className="flex flex-row gap-2 w-full p-2">

            {/* Pagination Controls */}
            <Pagination className="w-fit m-0 mr-auto">
                <PaginationContent>
                    <PaginationItem>
                        <PaginationPrevious href="#" />
                    </PaginationItem>
                    <PaginationItem>
                        <PaginationLink href="#">1</PaginationLink>
                    </PaginationItem>
                    <PaginationItem>
                        <PaginationEllipsis />
                    </PaginationItem>
                    <PaginationItem>
                        <PaginationNext href="#" />
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
                    <Badge variant="outline" className="text-center bg-background rounded-full px-4 mr-4 h-full">
                        {(totalFlights).toLocaleString()} Flight{totalFlights !== 1 ? "s" : ""} Found
                    </Badge>
                </motion.div>
            }

            {/* Sorting Select Dropdown */}
            <Select onValueChange={(value) => updateSortingColumn(value)} value={sortingColumn || ""}>
                <Button variant="outline" asChild>
                    <SelectTrigger className="min-w-[180px] w-fit">
                        <ArrowDownWideNarrow />
                        <SelectValue placeholder="Sort by" />
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
                    <SelectTrigger className="min-w-[180px] w-fit">
                        <ArrowUpDown />
                        <SelectValue placeholder="Order" />
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
                    <SelectTrigger className="min-w-[180px] w-fit">
                        <ListOrdered />
                        <SelectValue placeholder="Flights per page" />
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
            <Card className="w-full h-full min-h-0 card-glossy flex flex-col justify-between overflow-clip">

                {/* Results List */}
                <motion.div
                    layoutScroll
                    className="flex-1 min-h-0 h-full w-full overflow-y-auto bg-muted "
                >

                    {/* Empty Results Message */}
                    {
                        (displayFlightsEmpty)
                        &&
                        <div className="min-h-full flex items-center justify-center p-6">
                            {renderEmptyResultsMessage()}
                        </div>
                    }

                    {/* Top-Level Search Group */}
                    <div id="flights-results-list" className="flex flex-col w-full">
                    {
                        flights.map((flight, index) => (
                            <motion.div key={flight.id}
                                initial={{ opacity: 0, }}
                                animate={{ opacity: 1, }}
                                transition={{ duration: 0.20, delay: getFlightRowAnimationDelay(index) }}
                            >
                                <FlightRow flight={flight} key={flight.id} />
                            </motion.div>
                        ))
                    }
                    </div>

                </motion.div>

                {/* Pagination Row */}
                <CardFooter className="flex flex-col w-full p-0">
                    <Separator />
                    {renderPaginationRow()}
                </CardFooter>

            </Card>
        );

    }

    return render();

}