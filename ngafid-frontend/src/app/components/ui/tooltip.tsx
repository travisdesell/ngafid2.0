// ngafid-frontend/src/app/components/ui/tooltip.tsx
"use client"

import { cn } from "@/lib/utils";
import * as TooltipPrimitive from "@radix-ui/react-tooltip";
import { Keyboard, MousePointerClick } from "lucide-react";
import * as React from "react";

type TooltipProps = React.ComponentProps<typeof TooltipPrimitive.Root>

function TooltipProvider({
    delayDuration = 0,
    ...props
}: React.ComponentProps<typeof TooltipPrimitive.Provider>) {
    return <TooltipPrimitive.Provider delayDuration={delayDuration} {...props} />
}

function Tooltip({ onOpenChange, open: openProp, defaultOpen, ...props }: TooltipProps) {
    
    const isControlled = (openProp !== undefined)

    const [open, setOpen] = React.useState<boolean>(defaultOpen ?? false)
    const closeTimerRef = React.useRef<number | null>(null)

    React.useEffect(() => {
        return () => {

            // Clean up timer on unmount
            if (closeTimerRef.current)
                window.clearTimeout(closeTimerRef.current)

        }
    }, [])

    const handleOpenChange = React.useCallback(

        (next: boolean) => {

            // Controlled, call the handler
            if (isControlled) {
                onOpenChange?.(next)
                return;
            }

            // Uncontrolled, manage state with delay on close
            if (closeTimerRef.current)
                window.clearTimeout(closeTimerRef.current)

            if (next) {

                setOpen(true)
                onOpenChange?.(true)

            } else {

                setOpen(false)
                onOpenChange?.(false)

            }
            
        },
        [isControlled, onOpenChange]
    )

    return (
        <TooltipPrimitive.Root
            {...props}
            open={isControlled ? openProp : open}
            onOpenChange={handleOpenChange}
        />
    )
}

function TooltipTrigger({
    className,
    ...props
}: React.ComponentProps<typeof TooltipPrimitive.Trigger>) {
    return (
        <TooltipPrimitive.Trigger
            {...props}
            className={cn("mx-0 p-0 w-fit select-text cursor-auto *:pointer-events-none!", className)}
        />
    )
}


type TooltipContentActionsProps = {
    leftAction?: string;
    rightAction?: string;
    keyboardAction?: string;
}
function TooltipContent({
    className,
    sideOffset = 0,
    leftAction=undefined,
    rightAction=undefined,
    keyboardAction=undefined,
    children,
    ...props
}: React.ComponentProps<typeof TooltipPrimitive.Content> & TooltipContentActionsProps) {
    return (
        <TooltipPrimitive.Portal>
            <TooltipPrimitive.Content
                sideOffset={sideOffset}
                updatePositionStrategy="always"
                className={cn(
                    `
                        bg-foreground text-background
                        z-1000 w-fit origin-(--radix-tooltip-content-transform-origin)
                        rounded-md px-3 py-1.5 text-xs text-balance max-w-lg
                        flex flex-col gap-1
                        pointer-events-none

                        data-[state=delayed-open]:animate-in data-[state=delayed-open]:fade-in-0 data-[state=delayed-open]:zoom-in-95
                        data-[state=instant-open]:animate-in data-[state=instant-open]:fade-in-0 data-[state=instant-open]:zoom-in-95

                        data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95
                        data-[state=closed]:animation-duration-[100ms] data-[state=closed]:[animation-timing-function:ease-in]

                        data-[side=bottom]:slide-in-from-top-2
                        data-[side=left]:slide-in-from-right-2
                        data-[side=right]:slide-in-from-left-2
                        data-[side=top]:slide-in-from-bottom-2
                    `,
                    className
                )}
                {...props}
            >
                {children}

                {/* Left Action */}
                {
                    (leftAction)
                    &&
                    <div className="opacity-50 flex items-center gap-1">
                        <MousePointerClick size={16}/>
                        <span>{leftAction}</span>
                    </div>
                }

                {/* Right Action */}
                {
                    (rightAction)
                    &&
                    <div className="opacity-50 flex items-center gap-1">
                        <MousePointerClick size={16} className="scale-x-[-1]"/>
                        <span>{rightAction}</span>
                    </div>
                }

                {/* Keyboard Action */}
                {
                    (keyboardAction)
                    &&
                    <div className="opacity-50 flex items-center gap-1">
                        <Keyboard size={16} className="scale-x-[-1]"/>
                        <span>{keyboardAction}</span>
                    </div>
                }
                <TooltipPrimitive.Arrow className="bg-foreground fill-foreground z-50 size-2.5 translate-y-[calc(-50%-2px)] rotate-45 rounded-[2px] pointer-events-none" />
            </TooltipPrimitive.Content>
        </TooltipPrimitive.Portal>
    )
}

export { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger };
