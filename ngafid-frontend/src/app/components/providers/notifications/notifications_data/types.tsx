// ngafid-frontend/src/app/components/providers/notifications/notifications_data/types.tsx
import { JSX } from "react";

export interface NGAFIDNotification { 
    id: string;
    message: string;
    type: "info" | "warning" | "error";
    render?: () => JSX.Element;
};