// ngafid-frontend/src/app/pages/protected/flights/_flight_row/flight_row_tag_badge.tsx
"use client"

import type { TagData } from "@/components/providers/tags/tags_provider";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Tag } from "lucide-react";


export default function FlightRowTagBadge({ tag }: { tag: TagData }) {
    return (
        <Tooltip>
            <TooltipTrigger>
                <Badge className="rounded-full flex items-center gap-2 truncate" variant="secondary">
                    <Tag fill={tag.color} size={12}/>
                    <span className="truncate">{tag.name}</span>
                </Badge>
            </TooltipTrigger>
            <TooltipContent>
                {tag.description}
            </TooltipContent>
        </Tooltip>
    );

}