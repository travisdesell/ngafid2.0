// ngafid-frontend/src/app/pages/protected/profile_preferences/EmailPreferenceItem.tsx

import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Shield, User } from "lucide-react";


const formatEmailType = (type: string) => (
    type
        .replace(/_/g, " ")
        .replace(/\b\w/g, (letter) => letter.toUpperCase())
        .replace("Admin", "")
        .trim()
);


export default function EmailPreferenceItem({ type, enabled, onToggle }: { type: string; enabled: boolean; onToggle: () => void }) {

    return (
        <div className="flex items-center justify-between dark:bg-background bg-muted px-4 py-3 rounded-lg border">
            <div className="flex flex-col gap-2">
                <div className="flex items-center gap-3">
                    {/* <span className="font-medium">{formatEmailType(type)}</span> */}

                    {
                        (type.includes("ADMIN"))
                        ? <Shield size={24} className="text-destructive" />
                        : <User size={24} className="text-foreground" />
                    }

                    <Badge className="text-md" variant={type.includes("ADMIN") ? "destructive" : "secondary"}>
                        {type.includes("ADMIN") ? "Admin" : "User"}
                    </Badge>
                </div>
                <span className="font-semibo text-lg">{formatEmailType(type)}</span>
            </div>
            <Switch checked={enabled} onCheckedChange={onToggle} />
        </div>
    );
}