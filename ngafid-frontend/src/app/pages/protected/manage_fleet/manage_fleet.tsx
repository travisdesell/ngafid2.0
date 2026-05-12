// ngafid-frontend/src/app/pages/protected/manage_fleet/manage_fleet.tsx

import { setPageTitle } from "@/components/page_title";
import { useAuth } from "@/components/providers/auth_provider";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ManageFleetFleetUsersContent from "@/pages/protected/manage_fleet/_manage_fleet_fleet_users_content";
import ManageFleetTailNumbersContent from "@/pages/protected/manage_fleet/_manage_fleet_tail_numbers_content";
import { JSX, useState } from "react";
import { useSearchParams } from "react-router-dom";

type ManageFleetTab = {
    name: string;
    content: JSX.Element;
};

type ManageFleetTabKey = "fleet-users" | "tail-numbers";

const MANAGE_FLEET_TABS: Record<ManageFleetTabKey, ManageFleetTab> = {
    "fleet-users": {
        name: "Fleet Users",
        content: <ManageFleetFleetUsersContent />,
    },
    "tail-numbers": {
        name: "Tail Numbers",
        content: <ManageFleetTailNumbersContent />,
    },
};

const MANAGE_FLEET_TAB_KEYS = Object.keys(MANAGE_FLEET_TABS) as ManageFleetTabKey[];

export default function ManageFleet() {

    const { user } = useAuth();
    const [searchParams, setSearchParams] = useSearchParams();
    const [manageFleetTab, setManageFleetTab] = useState<ManageFleetTabKey>("fleet-users");

    setPageTitle("Manage Fleet");

    if (!user)
        return <p>Loading...</p>;

    return (
        <div className="page-content-thin gap-2">

            <Card className="card-glossy flex-1 min-h-0 overflow-hidden flex flex-col relative">

                <CardHeader className="shrink-0">
                    <CardTitle>Manage Fleet</CardTitle>
                    <CardDescription>Manage fleet users, access levels, email settings, and tail number mappings.</CardDescription>
                </CardHeader>

                <CardContent className="flex-1 min-h-0 p-0">
                    <Tabs
                        id="manage-fleet-tabs"
                        defaultValue={manageFleetTab}
                        value={manageFleetTab}
                        onValueChange={(value) => setManageFleetTab(value as ManageFleetTabKey)}
                        className="flex flex-col min-h-0 h-full"
                    >

                        <div className="mx-auto">
                            <TabsList className="w-md justify-between" id="manage-fleet-tabs-list">
                                {MANAGE_FLEET_TAB_KEYS.map((key) => (
                                    <TabsTrigger key={key} value={key}>
                                        <span>{MANAGE_FLEET_TABS[key].name}</span>
                                    </TabsTrigger>
                                ))}
                            </TabsList>
                        </div>

                        {MANAGE_FLEET_TAB_KEYS.map((key) => (
                            <TabsContent
                                key={key}
                                value={key}
                                className="w-full min-h-0 h-full pt-8 flex-1 overflow-y-auto p-4"
                            >
                                {MANAGE_FLEET_TABS[key].content}
                            </TabsContent>
                        ))}

                    </Tabs>
                </CardContent>

            </Card>
        </div>
    );
}
