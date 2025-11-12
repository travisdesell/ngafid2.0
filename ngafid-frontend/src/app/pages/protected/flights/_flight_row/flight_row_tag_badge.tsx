// ngafid-frontend/src/app/pages/protected/flights/_flight_row/flight_row_tag_badge.tsx
"use client"

import { Badge } from "@/components/ui/badge";
import { Tag } from "lucide-react";

type TestTag = {
    name: string;
    color: string;
};

export default function FlightRowTagBadge({ tag }: { tag: TestTag }) {

    return (
        <Badge className="rounded-full flex items-center gap-2 truncate" variant="secondary">
            <Tag fill={tag.color} size={12}/>
            <span className="truncate">{tag.name}</span>
        </Badge>
    );

}