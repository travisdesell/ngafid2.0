import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty";
import { Item, ItemActions, ItemContent, ItemDescription, ItemMedia } from "@/components/ui/item";
import { Bell, BellOff } from "lucide-react";
import { NGAFIDNotification } from "./notifications_data/types";
import { useNotifications } from "./notifications_provider";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

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
            <DropdownMenuTrigger>
                <Tooltip disableHoverableContent>
                    <TooltipTrigger asChild>
                        <Button
                            type="button"
                            variant="ghostMono"
                            className="h-8 p-1! aspect-square **:w-full! **:h-full! inline-flex items-center cursor-pointer"
                        >
                            <Bell />
                        </Button>
                    </TooltipTrigger>
                    <TooltipContent
                        leftAction="Open"
                    >
                        Notifications
                    </TooltipContent>
                </Tooltip>
            </DropdownMenuTrigger>
            <DropdownMenuContent className="from-muted to-background min-h-[400px] max-h-[400px] min-w-[275px] mr-4 bg-linear-to-b from-30% overflow-y-auto">
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
