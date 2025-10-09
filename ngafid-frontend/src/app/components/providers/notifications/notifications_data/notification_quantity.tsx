// ngafid-frontend/src/app/components/providers/notifications/notifications_data/notification_quantity.tsx
import { NGAFIDNotification } from "./types";

export function makeNotifQuantity(
    id: string,
    message: string,
    type: "info" | "warning" | "error",
    quantity: number
): NGAFIDNotification {

    return {
        id,
        message,
        type,
        render: () => (
            <div className="flex justify-between w-full">
                <div>{message}</div>
                <div className="font-bold">{quantity}</div>
            </div>
        ),
    };

}
