import { ChevronLeft, ChevronRight, MoreHorizontal } from "lucide-react";
import * as React from "react";

import { Button, ButtonProps, buttonVariants } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";

const Pagination = ({ className, ...props }: React.ComponentProps<"nav">) => (
    <nav
        role="navigation"
        aria-label="pagination"
        className={cn("mx-auto flex w-full justify-center", className)}
        {...props}
    />
);
Pagination.displayName = "Pagination";

const PaginationContent = React.forwardRef<
    HTMLUListElement,
    React.ComponentProps<"ul">
>(({ className, ...props }, ref) => (
    <ul
        ref={ref}
        className={cn("flex flex-row items-center gap-1", className)}
        {...props}
    />
));
PaginationContent.displayName = "PaginationContent";

const PaginationItem = React.forwardRef<
    HTMLLIElement,
    React.ComponentProps<"li">
>(({ className, ...props }, ref) => (
    <li ref={ref} className={cn("", className)} {...props} />
));
PaginationItem.displayName = "PaginationItem";

type PaginationLinkProps = {
    isActive?: boolean
} & Pick<ButtonProps, "size"> &
    React.ComponentProps<"a">

const PaginationLink = ({
    className,
    isActive,
    size = "icon",
    ...props
}: PaginationLinkProps) => (
    <a
        aria-current={isActive ? "page" : undefined}
        className={cn(
            "cursor-pointer select-none",
            buttonVariants({
                variant: isActive ? "outline" : "ghost",
                size,
            }),
            className
        )}
        {...props}
    />
);
PaginationLink.displayName = "PaginationLink";

const PaginationPrevious = ({
    className,
    ...props
}: React.ComponentProps<typeof PaginationLink>) => (
    <PaginationLink
        aria-label="Go to previous page"
        size="default"
        className={cn("gap-1 pl-2.5", className)}
        {...props}
    >
        <ChevronLeft className="h-4 w-4" />
        <span>Previous</span>
    </PaginationLink>
);
PaginationPrevious.displayName = "PaginationPrevious";

const PaginationNext = ({
    className,
    ...props
}: React.ComponentProps<typeof PaginationLink>) => (
    <PaginationLink
        aria-label="Go to next page"
        size="default"
        className={cn("gap-1 pr-2.5", className)}
        {...props}
    >
        <span>Next</span>
        <ChevronRight className="h-4 w-4" />
    </PaginationLink>
);
PaginationNext.displayName = "PaginationNext";

type PaginationEllipsisProps = {
    page: number
    pages: number
    onPageChange: (page: number) => void
} & React.ComponentProps<"span">

const PaginationEllipsis = ({
    className,
    page,
    pages,
    onPageChange,
    ...props
}: PaginationEllipsisProps) => {

    const [targetPage, setTargetPage] = React.useState("");
    const hasPages = pages > 0;
    const maxPage = Math.max(0, pages - 1);

    const clampPage = (value: number) => Math.min(Math.max(value, 0), maxPage);

    const goToPage = (value: number) => {

        // No pages available, exit
        if (!hasPages)
            return;

        const clamped = clampPage(value);
        onPageChange(clamped);

    };

    const commitTargetPage = () => {

        // No pages available, exit
        if (!hasPages)
            return;

        const cleaned = targetPage.trim();

        // Empty input, exit
        if (!cleaned)
            return;

        const parsed = Number(cleaned);

        // Invalid number, exit
        if (!Number.isFinite(parsed))
            return;

        const zeroBased = Math.floor(parsed) - 1;
        const clamped = clampPage(zeroBased);
        onPageChange(clamped);
        setTargetPage(String(clamped + 1));

    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghostMono" size="icon" disabled={!hasPages}>
                    <span
                        aria-hidden
                        className={cn("flex h-9 w-9 items-center justify-center", className)}
                        {...props}
                    >
                        <MoreHorizontal className="h-4 w-4" />
                        <span className="sr-only">More pages</span>
                    </span>
                </Button>
            </DropdownMenuTrigger>

            {/* Page Number Selection */}
            <DropdownMenuContent>

                {/* All Pages */}
                <div className="max-h-60 overflow-y-auto">
                    {
                        hasPages
                            ? Array.from({ length: pages }, (_, index) => (
                                <DropdownMenuItem
                                    key={index}
                                    onSelect={() => goToPage(index)}
                                    className={index === page ? "font-semibold" : ""}
                                >
                                    Page {index + 1}
                                </DropdownMenuItem>
                            ))
                            : (
                                <DropdownMenuItem disabled>
                                    No pages available
                                </DropdownMenuItem>
                            )
                    }
                </div>

                {/* First / Last Page */}
                <Separator className="my-1" />
                <DropdownMenuGroup>
                    <DropdownMenuItem
                        disabled={!hasPages || page === 0}
                        onSelect={() => goToPage(0)}
                    >
                        First Page
                    </DropdownMenuItem>
                    <DropdownMenuItem
                        disabled={!hasPages || page === maxPage}
                        onSelect={() => goToPage(maxPage)}
                    >
                        Last Page
                    </DropdownMenuItem>
                </DropdownMenuGroup>

                {/* Specific Page (from user) */}
                <Separator className="my-1" />
                <div className="flex gap-2">
                    <Input
                        type="number"
                        min={1}
                        max={pages}
                        placeholder="Page #"
                        value={targetPage}
                        onChange={(event) => setTargetPage(event.target.value)}
                        onKeyDown={(event) => {
                            if (event.key === "Enter") {
                                event.preventDefault();
                                commitTargetPage();
                            }
                        }}
                        className="w-full"
                    />
                    <Button type="button" onClick={commitTargetPage} disabled={!hasPages}>
                        Go
                    </Button>
                </div>

            </DropdownMenuContent>
        </DropdownMenu>
    );
};
PaginationEllipsis.displayName = "PaginationEllipsis";

export {
    Pagination,
    PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious
};

