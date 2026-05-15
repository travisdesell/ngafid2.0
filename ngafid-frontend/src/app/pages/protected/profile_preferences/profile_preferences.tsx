// ngafid-frontend/src/app/pages/protected/profile_preferences/profile_preferences.tsx

import { setPageTitle } from "@/components/page_title";
import { useAuth } from "@/components/providers/auth_provider";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ProfilePreferencesAccountSettingsContent from "@/pages/protected/profile_preferences/_profile_preferences_account_settings_content";
import ProfilePreferencesProfileInformationContent from "@/pages/protected/profile_preferences/_profile_preferences_profile_information_content";
import ProfilePreferencesSitePreferencesContent from "@/pages/protected/profile_preferences/_profile_preferences_site_preferences_content";
import { JSX, useState } from "react";


interface ProfilePreferenceTab {
    name: string;
    content: JSX.Element;
}

const PROFILE_PREFERENCES_TABS: Record<string, ProfilePreferenceTab> = {
    "Profile Information": {
        name: "Profile Information",
        content: <ProfilePreferencesProfileInformationContent />,
    },
    "Site Preferences": {
        name: "Site Preferences",
        content: <ProfilePreferencesSitePreferencesContent />,
    },
    "Account Security": {
        name: "Account Security",
        content: <ProfilePreferencesAccountSettingsContent />,
    },
};

const PROFILE_PREFERENCES_TABS_KEYS = Object.keys(PROFILE_PREFERENCES_TABS) as Array<keyof typeof PROFILE_PREFERENCES_TABS>;


export default function ProfilePreferences() {

    const { user } = useAuth();

    setPageTitle("Profile Preferences");

    // User data loading state
    if (!user)
        return <p>Loading...</p>;

    
    const [profilePreferenceTab, setProfilePreferenceTab] = useState<keyof typeof PROFILE_PREFERENCES_TABS>("Profile Information");
    return (
        <div className="page-content-thin gap-2">

            <Card className="card-glossy flex-1 min-h-0 overflow-hidden flex flex-col relative">

                <CardHeader className="shrink-0">
                    <CardTitle>Profile Preferences</CardTitle>
                    <CardDescription>Manage your profile details, site preferences, and account security settings.</CardDescription>
                </CardHeader>

                <CardContent className="flex-1 min-h-0 p-0">
                    <Tabs
                        id="profile-preferences-tabs"
                        defaultValue={profilePreferenceTab}
                        value={profilePreferenceTab}
                        onValueChange={(value) => setProfilePreferenceTab(value as keyof typeof PROFILE_PREFERENCES_TABS)}
                        className="flex flex-col min-h-0 h-full"
                    >

                        <div className="mx-auto">
                            <TabsList className="w-xl justify-between" id="profile-preferences-tabs-list">
                                {PROFILE_PREFERENCES_TABS_KEYS.map((key) => (
                                    <TabsTrigger key={key} value={key}>
                                        <span className="capitalize">{key}</span>
                                    </TabsTrigger>
                                ))}
                            </TabsList>
                        </div>

                        {PROFILE_PREFERENCES_TABS_KEYS.map((key) => (
                            <TabsContent
                                key={key}
                                value={key}
                                className="w-full min-h-0 h-full pt-8 flex-1 overflow-y-auto p-4"
                            >
                                {PROFILE_PREFERENCES_TABS[key].content}
                            </TabsContent>
                        ))}

                    </Tabs>
                </CardContent>

            </Card>
        </div>
    );

};