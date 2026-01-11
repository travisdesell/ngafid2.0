// ngafid-frontend/src/app/components/modals/charts_list_modal/charts_list_modal_badge.tsx

import { getLogger } from "@/components/providers/logger";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent } from "@/components/ui/tooltip";
import { TooltipTrigger } from "@radix-ui/react-tooltip";
import { MousePointerClick } from "lucide-react";
import { motion } from "motion/react";

const log = getLogger("ChartsListModalBadge", "black", "Component");

type Props = {
    label: string;
    isUniversal: boolean;
    onClick?: () => void;
};

export function ChartsListModalBadge({ label, isUniversal, onClick }: Props) {

    log("Rendering badge with label:", label, "isUniversal:", isUniversal);

    const removeMessage = (isUniversal)
        ? "Remove for all flights"
        : "Remove";

    return <Tooltip disableHoverableContent>

        <TooltipTrigger>

            <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                transition={{ duration: 0.2 }} 
            >
                <Badge
                    variant={isUniversal ? "default" : "outline"}
                    className="w-fit h-fit rounded-full flex gap-1 whitespace-nowrap cursor-pointer group relative select-none"
                    onClick={onClick}
                >
                    <span className="group-hover:opacity-50 ">{label}</span>
                    <hr className="absolute left-2 bottom-1/2 border-foreground border mt-0.5 w-[calc(100%-1rem)] hidden! group-hover:block!" />
                </Badge>
            </motion.div>

        </TooltipTrigger>

        <TooltipContent>
            <div className="opacity-50 flex items-center">
                <MousePointerClick size={16} className="mr-1"/>
                <span>{removeMessage}</span>
            </div>
        </TooltipContent>

    </Tooltip>

}