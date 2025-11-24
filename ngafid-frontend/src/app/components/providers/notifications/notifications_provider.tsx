// ngafid-frontend/src/app/components/misc_providers/notifications_provider.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { getLogger } from "@/components/providers/logger";
import { createContext, useContext, useState } from "react";
import { makeNotifQuantity } from "./notifications_data/notification_quantity";
import { NGAFIDNotification } from "./notifications_data/types";


const log = getLogger("NotificationsProvider");


type NotificationsData = {      //[EX]
    notifications: Array<{
        count: number;
        message: string;
        badgeType: string;
        name: string | undefined;
    }>;
}

type NotificationsProviderState = {
    notifications: NGAFIDNotification[],
};

type ThemeProviderProps = {
    children: React.ReactNode
};

const NotificationsProviderContext = createContext<NotificationsProviderState | null>(null);

export function NotificationsProvider({children}: ThemeProviderProps) {

    const { setModal } = useModal();

    const TEST_NOTIFICATION = makeNotifQuantity(
        "1",
        "This is a sample notification.",
        "info",
        1,
    )
    const [notifications, setNotifications] = useState<NGAFIDNotification[]>([
        TEST_NOTIFICATION,
    ]);



    const fetchNotificationStatistics = () => {

        log("Fetching notification statistics...");

        const NOTIFICATION_TYPES = [
            'waitingUserCount',
            'unconfirmedTailsCount'
        ];

        for (const type of NOTIFICATION_TYPES) {

            fetch(`/api/notifications/statistics?type=${type}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
            .then(response => response.json())
            .then(data => {

                log(`Fetched notification statistics for type ${type}:`, data);

                const newNotification = {
                    count: data.count,
                    message: data.message,
                    badgeType: data.badgeType,
                    name: data.name
                };

                log("New notification data:", newNotification);

                setNotifications(prev => {
                    if (prev) {
                        return {
                            notifications: [...prev.notifications, newNotification]
                        };
                    } else {
                        return {
                            notifications: [newNotification]
                        };
                    }
                });

            })
            .catch(error => {
                setModal(ErrorModal, { title: "Error fetching notifications", message: error.toString() });
            });

        }

    };


    // useEffect(() => {
    //     fetchNotificationStatistics();
    // }, []);


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