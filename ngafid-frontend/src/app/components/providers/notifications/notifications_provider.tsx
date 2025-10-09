// ngafid-frontend/src/app/components/misc_providers/notifications_provider.tsx
import { createContext, useContext, useState } from "react";
import { NGAFIDNotification } from "./notifications_data/types";
import { makeNotifQuantity } from "./notifications_data/notification_quantity";

type NotificationsProviderState = {
    notifications: NGAFIDNotification[],
};

type ThemeProviderProps = {
    children: React.ReactNode
};

const NotificationsProviderContext = createContext<NotificationsProviderState | null>(null);

export function NotificationsProvider({children}: ThemeProviderProps) {

    const TEST_NOTIFICATION = makeNotifQuantity(
        "1",
        "This is a sample notification.",
        "info",
        1,
    )
    const [notifications, setNotifications] = useState<NGAFIDNotification[]>([
        TEST_NOTIFICATION,
    ]);



    const value = {
        notifications,
    };
    return (
        <NotificationsProviderContext.Provider value={value}>
            {children}
        </NotificationsProviderContext.Provider>
    )

}


export function useNotifications() {

    const ctx = useContext(NotificationsProviderContext);
    if (!ctx)
        throw new Error("useNotifications must be used within <NotificationsProvider>");

    return ctx;

}