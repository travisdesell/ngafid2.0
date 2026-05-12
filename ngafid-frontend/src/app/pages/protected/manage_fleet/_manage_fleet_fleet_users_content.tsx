// ngafid-frontend/src/app/pages/protected/manage_fleet/_manage_fleet_fleet_users_content.tsx

import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { useAuth } from "@/components/providers/auth_provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Switch } from "@/components/ui/switch";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { fetchJson } from "@/fetchJson";
import { useLocalStorage } from "@uidotdev/usehooks";
import { Check, Loader2, Send, UserPlus } from "lucide-react";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

type AccessType = "MANAGER" | "UPLOAD" | "VIEW" | "WAITING" | "DENIED";

type FleetAccess = {
    fleetId: number;
    accessType: AccessType;
};

type FleetUser = {
    id: number;
    email: string;
    firstName?: string;
    lastName?: string;
    fleetAccess: FleetAccess;
    originalAccessType: AccessType;
};

type Fleet = {
    id: number;
    name: string;
};

type ErrorResponse = {
    errorTitle?: string;
    errorMessage?: string;
    message?: string;
};

type EmailType = {
    name: string;
    displayName?: string;
    display?: string;
};

type EmailPreferencesResponse = {
    emailTypesKeys?: (string | EmailType)[];
    emailTypesUser?: Record<string, boolean>;
};

type EmailPreferencesByUser = Record<number, Record<string, boolean>>;

const ACCESS_TYPES: AccessType[] = ["MANAGER", "UPLOAD", "VIEW", "WAITING", "DENIED"];
const EMAIL_TYPE_ORDER = [
    "upload_process_start",
    "import_processed_receipt",
    "airsync_update_report",
];

const ACCESS_LABELS: Record<AccessType, string> = {
    MANAGER: "Manager",
    UPLOAD: "Upload",
    VIEW: "View",
    WAITING: "Waiting",
    DENIED: "Denied",
};

function isFleetAccess(value: unknown): value is FleetAccess {
    return Boolean(
        value
        && typeof value === "object"
        && "accessType" in value
        && "fleetId" in value
    );
}

function normalizeFleetUser(user: any, fallbackFleetId?: number): FleetUser {
    const accessType = isFleetAccess(user.fleetAccess)
        ? user.fleetAccess.accessType
        : (user.fleetAccess as AccessType | undefined) ?? "VIEW";

    const fleetId = isFleetAccess(user.fleetAccess)
        ? user.fleetAccess.fleetId
        : fallbackFleetId ?? user.fleet?.id ?? user.fleetId ?? -1;

    return {
        id: user.id,
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        fleetAccess: {
            accessType,
            fleetId,
        },
        originalAccessType: accessType,
    };
}

function sortUsers(users: FleetUser[]) {
    return [...users].sort((a, b) => {
        if (a.fleetAccess.accessType === "DENIED" && b.fleetAccess.accessType !== "DENIED")
            return 1;
        if (a.fleetAccess.accessType !== "DENIED" && b.fleetAccess.accessType === "DENIED")
            return -1;

        return a.email.localeCompare(b.email);
    });
}

function getUserName(user: FleetUser) {
    const name = `${user.firstName ?? ""} ${user.lastName ?? ""}`.trim();
    return name.length > 0 ? name : "-";
}

function normalizeEmailType(type: string | EmailType): EmailType {
    if (typeof type === "string")
        return { name: type };

    return type;
}

function getEmailTypeName(type: EmailType) {
    return type.displayName ?? type.display ?? formatEmailType(type.name);
}

function formatEmailType(type: string) {
    return type
        .replace(/_/g, " ")
        .replace(/\b\w/g, (letter) => letter.toUpperCase())
        .replace("Admin", "")
        .trim();
}

function compareEmailTypes(a: EmailType, b: EmailType) {
    const aIndex = EMAIL_TYPE_ORDER.indexOf(a.name);
    const bIndex = EMAIL_TYPE_ORDER.indexOf(b.name);

    if (aIndex !== -1 || bIndex !== -1) {
        if (aIndex === -1)
            return 1;
        if (bIndex === -1)
            return -1;

        return aIndex - bIndex;
    }

    return getEmailTypeName(a).localeCompare(getEmailTypeName(b));
}

function readResponseMessage(response: unknown, fallback: string) {
    const error = response as ErrorResponse | null;
    return error?.errorMessage ?? error?.message ?? fallback;
}

function FleetEmailSettingsTable({
    users,
    visibleUsers,
}: {
    users: FleetUser[];
    visibleUsers: FleetUser[];
}) {

    const { setModal } = useModal();

    const [emailTypes, setEmailTypes] = useState<EmailType[]>([]);
    const [preferencesByUser, setPreferencesByUser] = useState<EmailPreferencesByUser>({});
    const [loading, setLoading] = useState(false);
    const [savingKey, setSavingKey] = useState("");

    const userIdsKey = useMemo(
        () => users.map((user) => user.id).sort((a, b) => a - b).join(","),
        [users],
    );

    const visibleUserIds = useMemo(() => new Set(visibleUsers.map((user) => user.id)), [visibleUsers]);

    const loadEmailPreferences = useCallback(async () => {
        if (users.length === 0) {
            setEmailTypes([]);
            setPreferencesByUser({});
            setLoading(false);
            return;
        }

        setLoading(true);

        try {
            const responses = await Promise.all(
                users.map(async (user) => {
                    const response = await fetchJson.get<EmailPreferencesResponse>(`/api/user/${user.id}/email-prefs`).catch((error: Error) => {
                        setModal(ErrorModal, { title: "Error fetching email preferences", message: error.message });
                        return null;
                    });

                    return { user, response };
                }),
            );

            const nextTypes = new Map<string, EmailType>();
            const nextPreferences: EmailPreferencesByUser = {};

            for (const { user, response } of responses) {
                if (!response)
                    continue;

                const prefs = response.emailTypesUser ?? {};
                nextPreferences[user.id] = prefs;

                for (const rawType of response.emailTypesKeys ?? []) {
                    const type = normalizeEmailType(rawType);

                    if (type.name.includes("HIDDEN") || type.name.includes("FORCED") || type.name.includes("ADMIN"))
                        continue;

                    nextTypes.set(type.name, type);
                }
            }

            setEmailTypes(Array.from(nextTypes.values()).sort(compareEmailTypes));
            setPreferencesByUser(nextPreferences);
        } finally {
            setLoading(false);
        }
    }, [setModal, userIdsKey]);

    useEffect(() => {
        loadEmailPreferences();
    }, [loadEmailPreferences]);

    const updateUserPreference = async (userId: number, typeName: string, checked: boolean) => {
        const existing = preferencesByUser[userId] ?? {};
        const payload = new URLSearchParams();

        for (const type of emailTypes)
            payload.set(type.name, String(type.name === typeName ? checked : Boolean(existing[type.name])));

        const key = `${userId}:${typeName}`;
        setSavingKey(key);

        const response = await fetchJson.put<EmailPreferencesResponse>(`/api/user/${userId}/email-prefs`, payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Updating Email Preferences", message: error.message });
            return null;
        });

        if (response) {
            setPreferencesByUser((current) => ({
                ...current,
                [userId]: response.emailTypesUser ?? {
                    ...existing,
                    [typeName]: checked,
                },
            }));
        }

        setSavingKey("");
    };

    const updateColumnPreference = async (typeName: string, checked: boolean) => {
        const targetUsers = users.filter((user) => visibleUserIds.has(user.id));

        setSavingKey(`column:${typeName}`);
        for (const user of targetUsers)
            await updateUserPreference(user.id, typeName, checked);
        setSavingKey("");
    };

    if (loading)
        return <p className="text-sm text-muted-foreground">Loading email settings...</p>;

    if (emailTypes.length === 0)
        return <p className="text-sm text-muted-foreground">No configurable fleet email settings are available.</p>;

    return (
        <div className="overflow-x-auto rounded-md border bg-background">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead className="min-w-56">Email</TableHead>
                        {emailTypes.map((type) => {
                            const allVisibleEnabled = visibleUsers.length > 0
                                && visibleUsers.every((user) => Boolean(preferencesByUser[user.id]?.[type.name]));

                            return (
                                <TableHead key={type.name} className="min-w-36">
                                    <div className="flex items-center gap-2">
                                        <span>{getEmailTypeName(type)}</span>

                                        <Tooltip disableHoverableContent>
                                            <TooltipTrigger>
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="icon"
                                                    className="size-7"
                                                    disabled={savingKey.length > 0}
                                                    onClick={() => updateColumnPreference(type.name, !allVisibleEnabled)}
                                                    title={allVisibleEnabled ? "Disable for visible users" : "Enable for visible users"}
                                                >
                                                    {savingKey === `column:${type.name}` ? <Loader2 className="animate-spin" /> : <Check />}
                                                </Button>
                                            </TooltipTrigger>
                                            <TooltipContent>
                                                {allVisibleEnabled ? "Disable this email setting for all visible users" : "Enable this email setting for all visible users"}
                                            </TooltipContent>
                                        </Tooltip>
                                    </div>
                                </TableHead>
                            );
                        })}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {visibleUsers.map((user) => (
                        <TableRow key={user.id}>
                            <TableCell className="font-medium">{user.email}</TableCell>
                            {emailTypes.map((type) => {
                                const checked = Boolean(preferencesByUser[user.id]?.[type.name]);
                                const key = `${user.id}:${type.name}`;

                                return (
                                    <TableCell key={type.name}>
                                        <Checkbox
                                            checked={checked}
                                            disabled={savingKey.length > 0}
                                            onCheckedChange={(value) => updateUserPreference(user.id, type.name, Boolean(value))}
                                            aria-label={`${getEmailTypeName(type)} for ${user.email}`}
                                        />
                                        {savingKey === key && <Loader2 className="ml-2 inline size-4 animate-spin" />}
                                    </TableCell>
                                );
                            })}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
}

export default function ManageFleetFleetUsersContent() {

    const { user: currentUser } = useAuth();
    const { setModal } = useModal();

    const [fleet, setFleet] = useState<Fleet | null>(null);
    const [users, setUsers] = useState<FleetUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [inviteEmail, setInviteEmail] = useState("");
    const [inviteSaving, setInviteSaving] = useState(false);
    const [showDeniedUsers, setShowDeniedUsers] = useLocalStorage("ngafid-manage-fleet-show-denied-users", true);

    const loadFleetData = useCallback(async () => {
        setLoading(true);

        const [fleetResponse, usersResponse] = await Promise.all([
            fetchJson.get<Fleet>("/api/fleet").catch((error: Error) => {
                setModal(ErrorModal, { title: "Error fetching fleet", message: error.message });
                return null;
            }),
            fetchJson.get<any[]>("/api/user").catch((error: Error) => {
                setModal(ErrorModal, { title: "Error fetching fleet users", message: error.message });
                return null;
            }),
        ]);

        if (fleetResponse)
            setFleet(fleetResponse);

        if (usersResponse)
            setUsers(sortUsers(usersResponse.map((fleetUser) => normalizeFleetUser(fleetUser, fleetResponse?.id))));

        setLoading(false);
    }, [setModal]);

    useEffect(() => {
        loadFleetData();
    }, [loadFleetData]);

    const visibleUsers = useMemo(
        () => users.filter((fleetUser) => fleetUser.id !== currentUser?.id && (showDeniedUsers || fleetUser.fleetAccess.accessType !== "DENIED")),
        [currentUser?.id, showDeniedUsers, users],
    );

    const deniedUsersCount = useMemo(
        () => users.filter((fleetUser) => fleetUser.id !== currentUser?.id && fleetUser.fleetAccess.accessType === "DENIED").length,
        [currentUser?.id, users],
    );

    const emailSettingsUsers = useMemo(
        () => users.filter((fleetUser) => fleetUser.id !== currentUser?.id),
        [currentUser?.id, users],
    );

    const updateUserAccess = async (fleetUser: FleetUser, accessType: AccessType) => {
        const fleetId = fleetUser.fleetAccess.fleetId > 0 ? fleetUser.fleetAccess.fleetId : fleet?.id;

        if (!fleetId || accessType === fleetUser.fleetAccess.accessType)
            return;

        const previousAccessType = fleetUser.fleetAccess.accessType;

        setUsers((current) => sortUsers(current.map((user) => (
            user.id === fleetUser.id
                ? {
                    ...user,
                    fleetAccess: {
                        ...user.fleetAccess,
                        accessType,
                    },
                }
                : user
        ))));

        const payload = new URLSearchParams({
            fleetUserId: String(fleetUser.id),
            fleetId: String(fleetId),
            accessType,
        });

        const response = await fetchJson.patch<ErrorResponse>(`/api/user/${fleetUser.id}/fleet-access`, payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Updating Fleet Access", message: error.message });
            return null;
        });

        if (!response) {
            setUsers((current) => sortUsers(current.map((user) => (
                user.id === fleetUser.id
                    ? {
                        ...user,
                        fleetAccess: {
                            ...user.fleetAccess,
                            accessType: previousAccessType,
                        },
                    }
                    : user
            ))));
            return;
        }

        if (response.errorTitle) {
            setModal(ErrorModal, { title: response.errorTitle, message: response.errorMessage ?? "" });
            setUsers((current) => sortUsers(current.map((user) => (
                user.id === fleetUser.id
                    ? {
                        ...user,
                        fleetAccess: {
                            ...user.fleetAccess,
                            accessType: previousAccessType,
                        },
                    }
                    : user
            ))));
            return;
        }

        setUsers((current) => sortUsers(current.map((user) => (
            user.id === fleetUser.id
                ? { ...user, originalAccessType: accessType }
                : user
        ))));

    };

    const inviteUser = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!fleet || inviteSaving)
            return;

        const email = inviteEmail.trim();
        if (!email.includes("@")) {
            setModal(ErrorModal, { title: "Invalid Email", message: "Please enter a valid email address." });
            return;
        }

        setInviteSaving(true);

        const payload = new URLSearchParams({
            email,
            fleetName: fleet.name,
            fleetId: String(fleet.id),
        });

        const response = await fetchJson.post<ErrorResponse>("/api/user/invite", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Inviting User", message: error.message });
            return null;
        });

        if (!response) {
            setInviteSaving(false);
            return;
        }

        if (response.errorTitle) {
            setModal(ErrorModal, { title: response.errorTitle, message: readResponseMessage(response, "") });
            setInviteSaving(false);
            return;
        }

        setInviteEmail("");
        setInviteSaving(false);
    };

    // Loading, show loader circle
    if (loading)
        return <Loader2 size={128} className="animate-spin mr-2 text-gray-500 absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2" />

    return (
        <div className="flex flex-col gap-6 pb-6">

            {/* Fleet Invitations */}
            <Card className="card-glossy">
                <CardHeader>
                    <CardTitle>{fleet ? `Manage ${fleet.name} Users` : "Manage Fleet Users"}</CardTitle>
                    <CardDescription>Send access invitations to new fleet users.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={inviteUser} className="flex flex-col gap-4">
                        <div className="grid gap-4 md:grid-cols-[1fr_auto] md:items-end">
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="fleet-invite-email">Invite User</Label>
                                <Input
                                    id="fleet-invite-email"
                                    type="email"
                                    value={inviteEmail}
                                    onChange={(event) => setInviteEmail(event.target.value)}
                                    placeholder="name@example.com"
                                    autoComplete="email"
                                />
                            </div>
                            <Button type="submit" disabled={!fleet || inviteSaving}>
                                {inviteSaving ? <Loader2 className="animate-spin" /> : <Send />}
                                Send Invite
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>

            {/* Fleet Access Levels */}
            <Card className="card-glossy">
                <CardHeader className="gap-4 sm:flex-row sm:items-start sm:justify-between sm:space-y-0">
                    <div className="flex flex-col space-y-1.5">
                        <CardTitle>Fleet Access Levels</CardTitle>
                        <CardDescription>
                            {"Review and update user access for this fleet."}
                        </CardDescription>
                    </div>

                    <div className="flex items-center gap-2 pt-1">
                        <Switch
                            id="show-denied-users"
                            checked={showDeniedUsers}
                            onCheckedChange={setShowDeniedUsers}
                            disabled={deniedUsersCount === 0}
                        />
                        <Label htmlFor="show-denied-users" className="text-sm">
                            Show denied users{deniedUsersCount > 0 ? ` (${deniedUsersCount})` : ""}
                        </Label>
                    </div>
                </CardHeader>

                <CardContent>
                    <div className="overflow-x-auto rounded-md border bg-background">
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead className="min-w-64">Email</TableHead>
                                    <TableHead className="min-w-44">Name</TableHead>
                                    <TableHead className="min-w-[34rem]">Access Level</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {visibleUsers.map((fleetUser) => {

                                    return (
                                        <TableRow key={fleetUser.id}>
                                            <TableCell className="font-medium">{fleetUser.email}</TableCell>
                                            <TableCell>{getUserName(fleetUser)}</TableCell>
                                            <TableCell>
                                                <RadioGroup
                                                    value={fleetUser.fleetAccess.accessType}
                                                    onValueChange={(value) => updateUserAccess(fleetUser, value as AccessType)}
                                                    className="grid grid-cols-5 gap-4"
                                                >
                                                    {ACCESS_TYPES.map((accessType) => (
                                                        <div key={accessType} className="flex items-center gap-2">
                                                            <RadioGroupItem
                                                                value={accessType}
                                                                id={`fleet-user-${fleetUser.id}-${accessType}`}
                                                            />
                                                            <Label
                                                                htmlFor={`fleet-user-${fleetUser.id}-${accessType}`}
                                                                className="text-sm"
                                                            >
                                                                {ACCESS_LABELS[accessType]}
                                                            </Label>
                                                        </div>
                                                    ))}
                                                </RadioGroup>
                                            </TableCell>
                                        </TableRow>
                                    );
                                })}

                                {visibleUsers.length === 0 && (
                                    <TableRow>
                                        <TableCell colSpan={3} className="h-24 text-center text-muted-foreground">
                                            No fleet users to show.
                                        </TableCell>
                                    </TableRow>
                                )}
                            </TableBody>
                        </Table>
                    </div>
                </CardContent>
            </Card>

            {/* Fleet Email Settings */}
            <Card className="card-glossy">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        {/* <UserPlus className="size-5" /> */}
                        Fleet Email Settings
                    </CardTitle>
                    <CardDescription>Manage notification settings for fleet users.</CardDescription>
                </CardHeader>
                <CardContent>
                    <FleetEmailSettingsTable users={emailSettingsUsers} visibleUsers={visibleUsers} />
                </CardContent>
            </Card>

        </div>
    );
}
