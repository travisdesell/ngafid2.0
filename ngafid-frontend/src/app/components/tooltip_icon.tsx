import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import * as TooltipPrimitive from '@radix-ui/react-tooltip';
import { Circle as CircleIcon, LucideIcon } from "lucide-react";

const TOOLTIP_MESSAGE_DEFAULT = "This is a placeholder message...";

type TooltipIconProps = {
    icon?: LucideIcon;
    message?: string|Array<string>;
    className?: string;
    iconClassName?: string;
};

export default function TooltipIcon({
    icon: Icon = CircleIcon,
    message = TOOLTIP_MESSAGE_DEFAULT,
    className = "ml-1 inline-block align-middle",
    iconClassName = "inline-block h-4 w-4 align-middle text-(--muted-foreground) mb-0.5",
}: TooltipIconProps) {

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className={className}>
                    <Icon className={iconClassName} aria-hidden="true" />
                </span>
            </TooltipTrigger>
            <TooltipContent className="max-w-xs">
                {message}
                <TooltipPrimitive.Arrow className="fill-primary pb-[1px] transition-all" />
            </TooltipContent>
        </Tooltip>
    );
    
}
