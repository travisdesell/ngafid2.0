// ngafid-frontend/src/app/components/modals/flights_selected_modal/flights_selected_modal_badge.tsx

import { getLogger } from "@/components/providers/logger";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { MousePointerClick } from "lucide-react";
import { motion } from "motion/react";

const log = getLogger("FlightsSelectedModalBadge", "black", "Component");

type Props = {
    label: string;
    kind: "universal" | "universal-missing" | "per-flight";
    onClick?: () => void;
};

export function FlightsSelectedModalBadge({ label, kind, onClick }: Props) {

    const removeMessage = (kind === "universal" || kind === "universal-missing")
        ? "Remove for all flights"
        : "Remove";

    const badgeVariant = (kind === "per-flight")
        ? "outline"
        : "default";    

    const warningClasses = (kind === "universal-missing")
        ? "bg-[var(--warning)] text-[var(--warning-foreground)] border-[var(--warning)] hover:bg-[var(--warning)]"
        : "";

    log("Rendering badge with label:", label, "kind:", kind);

    return <Tooltip disableHoverableContent>

        <TooltipTrigger className="group cursor-pointer" onClick={onClick}>
            <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                transition={{ duration: 0.2 }}
            >
                <Badge
                    variant={badgeVariant as any}
                    className={`w-fit h-fit rounded-full flex gap-1 whitespace-nowrap relative select-none ${warningClasses}`}
                >
                    <span className="group-hover:opacity-50">{label}</span>
                    <hr className="absolute left-2 bottom-1/2 border-foreground border mt-0.5 w-[calc(100%-1rem)] hidden! group-hover:block!" />
                </Badge>
            </motion.div>
        </TooltipTrigger>

        <TooltipContent>
            <div className="opacity-50 flex items-center">
                <MousePointerClick size={16} className="mr-1" />
                <span>{removeMessage}</span>
            </div>
        </TooltipContent>
        
    </Tooltip>
    

}
