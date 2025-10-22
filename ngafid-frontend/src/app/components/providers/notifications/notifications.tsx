import { Bell, BellOff } from "lucide-react";
import { useNotifications } from "./notifications_provider";
import { NGAFIDNotification } from "./notifications_data/types";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Item, ItemActions, ItemContent, ItemDescription, ItemFooter, ItemHeader, ItemMedia, ItemTitle } from "@/components/ui/item";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";

export default function Notifications() {

    const { notifications } = useNotifications();

    const renderNotification = (notification: NGAFIDNotification) => {

        const notifRenderMethod = (notification as any).render ? (notification as any).render() : null;

        return (
            <Item key={notification.id}>
                <ItemMedia />
                    <ItemContent>

                        {

                            /* Has custom render method */
                            (notifRenderMethod)
                            ?
                            <>
                                {notifRenderMethod}
                            </>

                            /* Otherwise, use standard rendering */
                            :
                            <ItemDescription>{notification.message}</ItemDescription>

                        }

                    </ItemContent>
                <ItemActions />
            </Item>
        );


    };

    const render = () => (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <button className="cursor-pointer flex relative">
                    <Bell />
                </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent className="from-muted to-background min-h-[400px] max-h-[400px] min-w-[275px] mr-4 bg-gradient-to-b from-30% overflow-y-auto">
                {
                    (notifications.length === 0)
                    ?
                    <Empty>
                        <EmptyHeader>
                            <EmptyMedia variant="icon">
                                <BellOff />
                            </EmptyMedia>
                            <EmptyTitle>
                                No Notifications
                            </EmptyTitle>
                            <EmptyDescription>
                                You&apos;re all caught up. New notifications will appear here.
                            </EmptyDescription>
                        </EmptyHeader>
                    </Empty>
                    :
                    notifications.map((notification) => (
                        renderNotification(notification)
                    ))
                }
            </DropdownMenuContent>
        </DropdownMenu>
    );

    return render();

}
