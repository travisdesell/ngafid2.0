// ngafid-frontend/src/app/pages/protected/flights/_panels/flights_panel_search.tsx
import { useModal } from "@/components/modals/modal_provider";
import { getLogger } from "@/components/providers/logger";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardFooter } from "@/components/ui/card";
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { ArrowDownWideNarrow, ArrowUpDown, Info, ListOrdered } from "lucide-react";
import { motion } from "motion/react";


const log = getLogger("FlightsPanelResults", "black", "Component");


export default function FlightsPanelResults() {

    const { setModal } = useModal();

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

        const flightsPerPageOptions = [10, 15, 25, 50, 100];

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

            {/* Sorting Select Dropdown */}
            <Select>
                <Button variant="outline" asChild>
                    <SelectTrigger className="min-w-[180px] w-fit">
                        <ArrowDownWideNarrow />
                        <SelectValue placeholder="Sort by" />
                    </SelectTrigger>
                </Button>
                <SelectContent>
                    <SelectItem value="newest">Newest</SelectItem>
                    <SelectItem value="oldest">Oldest</SelectItem>
                    <SelectItem value="a-z">A - Z</SelectItem>
                    <SelectItem value="z-a">Z - A</SelectItem>
                </SelectContent>
            </Select>

            {/* Sorting Order (Ascending/Descending) Select Dropdown */}
            <Select>
                <Button variant="outline" asChild>
                    <SelectTrigger className="min-w-[180px] w-fit">
                        <ArrowUpDown />
                        <SelectValue placeholder="Order" />
                    </SelectTrigger>
                </Button>
                <SelectContent>
                    <SelectItem value="ascending">Ascending</SelectItem>
                    <SelectItem value="descending">Descending</SelectItem>
                </SelectContent>
            </Select>

            {/* Flights Per Page Select Dropdown */}
            <Select>
                <Button variant="outline" asChild>
                    <SelectTrigger className="min-w-[180px] w-fit">
                        <ListOrdered />
                        <SelectValue placeholder="Flights per page" />
                    </SelectTrigger>
                </Button>
                <SelectContent>
                    {flightsPerPageOptions.map((option) => (
                        <SelectItem key={option} value={option.toString()}>
                            {option} per page
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>

        </div>

    }

    const render = () => {

        log("Rendering Flights Panel Results");

        return (
            <Card className="w-full h-full min-h-0 card-glossy flex flex-col justify-between overflow-clip">

                {/* Results List */}
                <motion.div
                    layoutScroll
                    className="flex-1 min-h-0 h-full w-full overflow-y-auto bg-muted "
                >

                    {/* Empty Results Message */}
                    {
                        (true)
                        &&
                        <div className="min-h-full flex items-center justify-center p-6">
                            {renderEmptyResultsMessage()}
                        </div>
                    }

                    {/* Top-Level Search Group */}
                    {/* <FlightsPanelSearchGroup depth={0} group={filter} indexPath={[]} /> */}

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