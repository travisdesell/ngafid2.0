// ngafid-frontend/src/app/pages/protected/profile_preferences/_profile_preferences_site_preferences_content.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { useAuth } from "@/components/providers/auth_provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { fetchJson } from "@/fetchJson";
import EmailPreferenceItem from "@/pages/protected/profile_preferences/EmailPreferenceItem";
import { Users, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

type FleetAccess = {
    fleetName: string;
    fleetId: number;
    accessType: string;
    userId: number;
};

type Fleet = {
    id: number;
    name: string;
};

type MultifleetInvite = {
    inviteEmail: string;
    fleetName: string;
    fleetId?: number;
};

type MetricPreferencesResponse = {
    flightMetrics?: string[];
    decimalPrecision?: number;
};

type EmailPreferencesResponse = {
    emailTypesKeys?: string[];
    emailTypesUser?: Record<string, boolean>;
};

const AIRSYNC_OPTIONS = [
    "72 Hours",
    "48 Hours",
    "24 Hours",
    "12 Hours",
    "6 Hours",
    "3 Hours",
    "1 Hour",
    "30 Minutes",
];

const METRIC_EXEMPT_COLS = new Set(["LOC-I Index", "Stall Index"]);




export default function ProfilePreferencesSitePreferencesContent() {

    const { setModal } = useModal();
    const { user } = useAuth();
    const isAdmin = Boolean(user?.isAdmin);

    const [invites, setInvites] = useState<MultifleetInvite[]>([]);
    const [invitesLoading, setInvitesLoading] = useState(true);
    const [inviteAction, setInviteAction] = useState<string | null>(null);

    const [fleetAccess, setFleetAccess] = useState<FleetAccess[]>([]);
    const [currentFleet, setCurrentFleet] = useState<Fleet | null>(null);
    const [selectedFleetId, setSelectedFleetId] = useState<string>("");
    const [fleetLoading, setFleetLoading] = useState(true);

    const [allMetrics, setAllMetrics] = useState<string[]>([]);
    const [selectedMetrics, setSelectedMetrics] = useState<string[]>([]);
    const [decimalPrecision, setDecimalPrecision] = useState<number>(1);
    const [metricsLoading, setMetricsLoading] = useState(true);
    const [metricsSaving, setMetricsSaving] = useState(false);
    const [metricToAdd, setMetricToAdd] = useState<string>("");

    const [emailTypes, setEmailTypes] = useState<string[]>([]);
    const [emailSettings, setEmailSettings] = useState<Record<string, boolean>>({});
    const [emailLoading, setEmailLoading] = useState(true);
    const [emailSaving, setEmailSaving] = useState(false);

    const [airSyncTimeout, setAirSyncTimeout] = useState<string>("");
    const [airSyncSaving, setAirSyncSaving] = useState(false);

    const displayInvites:boolean = invites.some((invite) => !invite.fleetId || invite.fleetId !== currentFleet?.id);

    useEffect(() => {
        const fetchInvites = async () => {
            setInvitesLoading(true);
            const response = await fetchJson.get<MultifleetInvite[]>("/api/user/multifleet-invites").catch((error: Error) => {
                setModal(ErrorModal, { title: "Error fetching invites", message: error.message });
                return null;
            });

            if (response)
                setInvites(response);

            setInvitesLoading(false);
        };

        fetchInvites();
    }, [setModal]);

    useEffect(() => {
        const fetchFleets = async () => {
            setFleetLoading(true);
            const [fleetResponse, accessResponse] = await Promise.all([
                fetchJson.get<Fleet>("/api/fleet").catch((error: Error) => {
                    setModal(ErrorModal, { title: "Error fetching fleet", message: error.message });
                    return null;
                }),
                fetchJson.get<FleetAccess[]>("/api/user/fleet-access").catch((error: Error) => {
                    setModal(ErrorModal, { title: "Error fetching fleet access", message: error.message });
                    return null;
                }),
            ]);

            if (fleetResponse) {
                setCurrentFleet(fleetResponse);
                setSelectedFleetId(String(fleetResponse.id));
            }
            if (accessResponse)
                setFleetAccess(accessResponse);

            setFleetLoading(false);
        };

        fetchFleets();
    }, [setModal]);

    useEffect(() => {
        const fetchMetrics = async () => {
            setMetricsLoading(true);
            const [preferences, series] = await Promise.all([
                fetchJson.get<MetricPreferencesResponse>("/api/user/me/metric-prefs").catch((error: Error) => {
                    setModal(ErrorModal, { title: "Error fetching metric preferences", message: error.message });
                    return null;
                }),
                fetchJson.get<{ names: string[] }>("/api/flight/double-series").catch((error: Error) => {
                    setModal(ErrorModal, { title: "Error fetching metric list", message: error.message });
                    return null;
                }),
            ]);

            if (series?.names)
                setAllMetrics(series.names);

            if (preferences) {
                setSelectedMetrics(preferences.flightMetrics ?? []);
                setDecimalPrecision(preferences.decimalPrecision ?? 1);
            }

            setMetricsLoading(false);
        };

        fetchMetrics();
    }, [setModal]);

    useEffect(() => {
        const fetchEmailPreferences = async () => {
            setEmailLoading(true);
            const response = await fetchJson.get<EmailPreferencesResponse>("/api/user/me/email-prefs").catch((error: Error) => {
                setModal(ErrorModal, { title: "Error fetching email preferences", message: error.message });
                return null;
            });

            if (!response) {
                setEmailLoading(false);
                return;
            }

            let emailTypesIn = response.emailTypesKeys ?? [];
            const emailTypesUser = response.emailTypesUser ?? {};

            emailTypesIn = emailTypesIn.filter((type) => !type.includes("HIDDEN") && !type.includes("FORCED"));

            if (isAdmin) {
                const admin = emailTypesIn.filter((type) => type.includes("ADMIN"));
                emailTypesIn = emailTypesIn.filter((type) => !type.includes("ADMIN")).concat(admin);
            } else {
                emailTypesIn = emailTypesIn.filter((type) => !type.includes("ADMIN"));
            }

            const normalized = Object.fromEntries(
                emailTypesIn.map((type) => [type, Boolean(emailTypesUser[type])])
            );

            setEmailTypes(emailTypesIn);
            setEmailSettings(normalized);
            setEmailLoading(false);
        };

        fetchEmailPreferences();
    }, [isAdmin, setModal]);

    const metricOptions = useMemo(() => (
        allMetrics
            .filter((metric) => !selectedMetrics.includes(metric))
            .filter((metric) => !METRIC_EXEMPT_COLS.has(metric))
    ), [allMetrics, selectedMetrics]);

    const submitInviteAction = async (fleetName: string, action: "accept" | "decline") => {
        setInviteAction(`${action}-${fleetName}`);
        try {
            const response = await fetch(`/api/user/multifleet-invites/${action}`, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({ fleetName }).toString(),
            });

            if (!response.ok) {
                const message = await response.text().catch(() => "");
                throw new Error(message || `Failed to ${action} invite.`);
            }

            setInvites((prev) => prev.filter((invite) => invite.fleetName !== fleetName));

            if (action === "accept") {
                const accessResponse = await fetchJson.get<FleetAccess[]>("/api/user/fleet-access").catch(() => null);
                if (accessResponse)
                    setFleetAccess(accessResponse);
            }
        } catch (error) {
            setModal(ErrorModal, { title: "Invite update failed", message: (error as Error).message });
        } finally {
            setInviteAction(null);
        }
    };

    const switchToFleet = async (fleetId: number) => {
        if (!fleetId || fleetId === currentFleet?.id)
            return;

        try {
            const response = await fetch("/api/user/select-fleet", {
                method: "PUT",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({ fleetIdSelected: String(fleetId) }).toString(),
            });

            if (!response.ok) {
                const message = await response.text().catch(() => "");
                throw new Error(message || "Failed to switch fleets.");
            }

            window.location.reload();
        } catch (error) {
            setModal(ErrorModal, { title: "Error switching fleets", message: (error as Error).message });
        }
    };

    const updateMetric = async (metricName: string, modificationType: "addition" | "deletion") => {
        if (!metricName)
            return;

        setMetricsSaving(true);
        const payload = new URLSearchParams({
            metricName,
            modificationType,
        });

        const response = await fetchJson.patch<string[]>("/api/user/me/metric-prefs", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Updating User Preferences", message: error.message });
            return null;
        });

        if (response)
            setSelectedMetrics(response);

        setMetricsSaving(false);
        setMetricToAdd("");
    };

    const updatePrecision = async (value: string) => {
        const precisionValue = Number(value);
        if (Number.isNaN(precisionValue))
            return;

        setDecimalPrecision(precisionValue);
        const payload = new URLSearchParams({ decimal_precision: String(precisionValue) });

        await fetchJson.put("/api/user/me/metric-prefs/precision", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Updating User Preferences", message: error.message });
        });
    };

    const updateEmailPreference = async (emailType: string) => {
        const updated = {
            ...emailSettings,
            [emailType]: !emailSettings[emailType],
        };

        setEmailSettings(updated);
        setEmailSaving(true);

        const payload = new URLSearchParams();
        Object.entries(updated).forEach(([key, value]) => payload.set(key, String(value)));

        await fetchJson.put("/api/user/me/email-prefs", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Updating Email Preferences", message: error.message });
        });

        setEmailSaving(false);
    };

    const updateAirSyncTimeout = async (value: string) => {
        if (!value)
            return;

        setAirSyncTimeout(value);
        setAirSyncSaving(true);

        const payload = new URLSearchParams({ timeout: value });

        await fetchJson.patch("/api/airsync/timeout", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Updating AirSync Settings", message: error.message });
        });

        setAirSyncSaving(false);
    };

    return (
        <div className="flex flex-col gap-6 pb-6">
            <Card className="card-glossy">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Users size={16} className="text-foreground" />
                        <span>Fleet Access</span>
                    </CardTitle>
                    <CardDescription>Manage fleet invitations and switch between fleets you can access.</CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-6">

                    {/* Fleet Selection */}
                    <div className="flex flex-col gap-2">
                        <Label>Switch Fleet</Label>
                        <Select
                            value={selectedFleetId}
                            onValueChange={(value) => {
                                setSelectedFleetId(value);
                                switchToFleet(Number(value));
                            }}
                            disabled={fleetLoading || fleetAccess.length === 0}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder={fleetLoading ? "Loading..." : "Select fleet"} />
                            </SelectTrigger>
                            <SelectContent>
                                {fleetAccess.map((fleet) => (
                                    <SelectItem key={fleet.fleetId} value={String(fleet.fleetId)}>
                                        {fleet.fleetName}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Fleet Invitations */}
                    {
                        (displayInvites)
                        &&
                        <div className="flex flex-col gap-3">
                            {
                                (invitesLoading)
                                ?
                                <>
                                    <Label>Invitations</Label>
                                    <div className="text-sm text-muted-foreground">Loading invites...</div>
                                </>
                                :
                                null
                            }
                            {/* {!invitesLoading && invites.length === 0 && (
                                <div className="text-sm text-muted-foreground">No pending invitations.</div>
                            )} */}
                            {!invitesLoading && invites.length > 0 && (
                                <div className="flex flex-col gap-3">
                                    {invites.map((invite) => (
                                        <div key={invite.fleetName} className="flex flex-col gap-2 rounded-lg border border-border p-3">
                                            <div className="flex flex-col gap-1">
                                                <span className="font-medium">{invite.fleetName}</span>
                                                <span className="text-xs text-muted-foreground">Invited by {invite.inviteEmail}</span>
                                            </div>
                                            <div className="flex flex-wrap gap-2">
                                                <Button
                                                    size="sm"
                                                    disabled={inviteAction === `accept-${invite.fleetName}`}
                                                    onClick={() => submitInviteAction(invite.fleetName, "accept")}
                                                >
                                                    Accept
                                                </Button>
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    disabled={inviteAction === `decline-${invite.fleetName}`}
                                                    onClick={() => submitInviteAction(invite.fleetName, "decline")}
                                                >
                                                    Decline
                                                </Button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    }
                </CardContent>
            </Card>

            <Card className="card-glossy">
                <CardHeader>
                    <CardTitle>Metric Viewer Preferences</CardTitle>
                    <CardDescription>Choose which metrics appear by default and set precision.</CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-4">
                    {metricsLoading ? (
                        <div className="text-sm text-muted-foreground">Loading metric preferences...</div>
                    ) : (
                        <>
                            <div className="flex flex-col gap-2">
                                <Label>Selected Metrics</Label>
                                <div className="flex flex-wrap gap-2">
                                    {selectedMetrics.length === 0 && (
                                        <span className="text-sm text-muted-foreground">No metrics selected.</span>
                                    )}
                                    {selectedMetrics.map((metric) => (
                                        <Button
                                            key={metric}
                                            type="button"
                                            size="sm"
                                            variant="ghostMono"
                                            onClick={() => updateMetric(metric, "deletion")}
                                            disabled={metricsSaving}
                                            className="flex items-center gap-2 hover:underline"
                                        >
                                            <span>{metric}</span>
                                            {/* <span className="text-xs">x</span> */}
                                            <X size={12} className="text-muted-foreground" />
                                        </Button>
                                    ))}
                                </div>
                            </div>

                            <div className="flex flex-col gap-2">
                                <Label>Add Metric</Label>
                                <Select
                                    value={metricToAdd}
                                    onValueChange={(value) => {
                                        setMetricToAdd(value);
                                        updateMetric(value, "addition");
                                    }}
                                    disabled={metricsSaving || metricOptions.length === 0}
                                >
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select a metric" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {metricOptions.map((metric) => (
                                            <SelectItem key={metric} value={metric}>{metric}</SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="flex flex-col gap-2">
                                <Label>Decimal Precision</Label>
                                <Select
                                    value={String(decimalPrecision)}
                                    onValueChange={updatePrecision}
                                >
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select precision" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {Array.from({ length: 7 }, (_, index) => (
                                            <SelectItem key={index} value={String(index)}>{index}</SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </>
                    )}
                </CardContent>
            </Card>

            <Card className="card-glossy">
                <CardHeader>
                    <CardTitle>Email Preferences</CardTitle>
                    <CardDescription>Control which email notifications you receive.</CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-4">
                    {emailLoading ? (
                        <div className="text-sm text-muted-foreground">Loading email preferences...</div>
                    ) : (
                        <div className="grid grid-cols-3 gap-6">
                            {emailTypes.map((type) => (
                                <EmailPreferenceItem
                                    key={type}
                                    type={type}
                                    enabled={emailSettings[type]}
                                    onToggle={() => updateEmailPreference(type)}
                                />
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>

            {isAdmin && (
                <Card className="card-glossy">
                    <CardHeader>
                        <CardTitle>AirSync Settings</CardTitle>
                        <CardDescription>Set how frequently NGAFID checks for new AirSync flights.</CardDescription>
                    </CardHeader>
                    <CardContent className="flex flex-col gap-2">
                        <Label>Sync Frequency</Label>
                        <Select
                            value={airSyncTimeout}
                            onValueChange={updateAirSyncTimeout}
                            disabled={airSyncSaving}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="Select frequency" />
                            </SelectTrigger>
                            <SelectContent>
                                {AIRSYNC_OPTIONS.map((option) => (
                                    <SelectItem key={option} value={option}>{option}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <span className="text-xs text-muted-foreground">
                            Choose a new interval to apply across the fleet.
                        </span>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}