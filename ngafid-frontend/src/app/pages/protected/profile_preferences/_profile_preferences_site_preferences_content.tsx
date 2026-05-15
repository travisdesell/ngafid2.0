// ngafid-frontend/src/app/pages/protected/profile_preferences/_profile_preferences_site_preferences_content.tsx
import ConfirmModal from "@/components/modals/confirm_modal";
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { fleetSelectable } from "@/components/navbars/multifleet_select";
import { useAuth } from "@/components/providers/auth_provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Form } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { fetchJson } from "@/fetchJson";
import EmailPreferenceItem from "@/pages/protected/profile_preferences/EmailPreferenceItem";
import { DoorOpen, Info, Users, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import type { MultifleetInvite } from "src/types/types";

interface MetricPreferencesResponse {
    flightMetrics?: Array<string>;
    decimalPrecision?: number;
}

interface EmailPreferencesResponse {
    emailTypesKeys?: Array<string>;
    emailTypesUser?: Record<string, boolean>;
}

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

const FLEET_NAME_LENGTH_LIMIT = 256;
const FLEET_NAME_REGEX = /^[a-zA-Z0-9 @#$%^&*()_+!.,/\\'-]+$/;




export default function ProfilePreferencesSitePreferencesContent() {

    const { setModal } = useModal();
    const { user, fleetLoading, refreshFleetData } = useAuth();
    const isAdmin = Boolean(user?.isAdmin);
    const currentFleet = user?.fleet ?? null;
    const fleetAccess = user?.fleetAccess ?? [];

    const [invites, setInvites] = useState<Array<MultifleetInvite>>([]);
    const [invitesLoading, setInvitesLoading] = useState(true);
    const [inviteAction, setInviteAction] = useState<string | null>(null);

    const [selectedFleetId, setSelectedFleetId] = useState<string>("");

    const [newFleetName, setNewFleetName] = useState("");
    const [isCreatingFleet, setIsCreatingFleet] = useState(false);
    const [isLeavingFleet, setIsLeavingFleet] = useState(false);

    const [allMetrics, setAllMetrics] = useState<Array<string>>([]);
    const [selectedMetrics, setSelectedMetrics] = useState<Array<string>>([]);
    const [decimalPrecision, setDecimalPrecision] = useState<number>(1);
    const [metricsLoading, setMetricsLoading] = useState(true);
    const [metricsSaving, setMetricsSaving] = useState(false);
    const [metricToAdd, setMetricToAdd] = useState<string>("");

    const [emailTypes, setEmailTypes] = useState<Array<string>>([]);
    const [emailSettings, setEmailSettings] = useState<Record<string, boolean>>({});
    const [emailLoading, setEmailLoading] = useState(true);
    const [emailSaving, setEmailSaving] = useState(false);

    const [airSyncTimeout, setAirSyncTimeout] = useState<string>("");
    const [airSyncSaving, setAirSyncSaving] = useState(false);

    const displayInvites:boolean = invites.some((invite) => !invite.fleetId || invite.fleetId !== currentFleet?.id);

    const trimmedFleetName = newFleetName.trim();
    const isFleetNameValid =
        (trimmedFleetName.length > 0)
        && (trimmedFleetName.length < FLEET_NAME_LENGTH_LIMIT)
        && FLEET_NAME_REGEX.test(trimmedFleetName);

    const selectedFleetAccess = fleetAccess.find((fleet) => fleet.fleetId === currentFleet?.id);
    const managesSelectedFleet = selectedFleetAccess?.accessType === "MANAGER";
    const otherFleets = fleetAccess.filter((fleet) => fleet.fleetId !== currentFleet?.id);
    const allOtherFleetsDeniedOrWaiting = otherFleets.every((fleet) => fleet.accessType === "DENIED" || fleet.accessType === "WAITING");
    const leaveDisabled = isLeavingFleet || !currentFleet || managesSelectedFleet || allOtherFleetsDeniedOrWaiting;
    const leaveTitle =
        !currentFleet ? "No fleet selected."
        : managesSelectedFleet ? "Unable to leave a fleet you manage."
        : allOtherFleetsDeniedOrWaiting ? "No other non-waiting/non-denied fleets to select."
        : "Leave the current fleet.";

    useEffect(() => {
        setSelectedFleetId(currentFleet?.id ? String(currentFleet.id) : "");
    }, [currentFleet?.id]);

    useEffect(() => {
        const fetchInvites = async () => {
            setInvitesLoading(true);
            const response = await fetchJson.get<Array<MultifleetInvite>>("/api/user/multifleet-invites").catch((error: Error) => {
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
        const fetchMetrics = async () => {
            setMetricsLoading(true);
            const [preferences, series] = await Promise.all([
                fetchJson.get<MetricPreferencesResponse>("/api/user/me/metric-prefs").catch((error: Error) => {
                    setModal(ErrorModal, { title: "Error fetching metric preferences", message: error.message });
                    return null;
                }),
                fetchJson.get<{ names: Array<string> }>("/api/flight/double-series").catch((error: Error) => {
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

            if (action === "accept")
                await refreshFleetData();
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

    const createFleet = async () => {

        // Already creating fleet / Fleet name is invalid, exit
        if (isCreatingFleet || !isFleetNameValid)
            return;

        setIsCreatingFleet(true);

        const payload = new URLSearchParams({ fleetName: trimmedFleetName });

        try {
            await fetchJson.post("/api/user/create-fleet", payload);
            window.location.reload();
        } catch (error) {
            setModal(ErrorModal, { title: "Error Creating Fleet", message: (error as Error).message });
            setIsCreatingFleet(false);
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

        const response = await fetchJson.patch<Array<string>>("/api/user/me/metric-prefs", payload).catch((error: Error) => {
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

    const leaveCurrentFleet = async () => {

        setModal(
            ConfirmModal,
            {
                title: "Leave Current Fleet",
                message: "Are you sure you want to leave the current fleet? You will not be able to access it unless you are re-invited.",
                onConfirm: confirmLeaveCurrentFleet,
            }
        );

    };

    const confirmLeaveCurrentFleet = async () => {

        // Not allowed to leave, exit
        if (leaveDisabled)
            return;

        setIsLeavingFleet(true);

        try {
            const response = await fetch("/api/user/leave-fleet", { method: "PUT" });

            if (!response.ok) {
                const message = await response.text().catch(() => "");
                throw new Error(message || "Failed to leave current fleet.");
            }

            window.location.reload();
        } catch (error) {
            setModal(ErrorModal, { title: "Error Leaving Fleet", message: (error as Error).message });
            setIsLeavingFleet(false);
        }

    };

    return (
        <div className="flex flex-col gap-6 pb-6">
            <Card className="card-glossy">
                <CardHeader className="flex flex-row items-center! justify-between">

                    <div className="flex flex-col gap-1.5 m-0">
                        <CardTitle className="flex items-center gap-2">
                            <Users size={16} className="text-foreground" />
                            <span>Fleet Access</span>
                        </CardTitle>
                        <CardDescription>Manage fleet invitations and switch between fleets you can access.</CardDescription>
                    </div>

                    {/* Leave Current Fleet */}
                    <Button
                        variant="ghostDestructive"
                        onClick={leaveCurrentFleet}
                        disabled={leaveDisabled}
                        title={leaveTitle}
                    >
                        <DoorOpen size={16} />
                        Leave Fleet
                    </Button>

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
                                    <SelectItem
                                        key={fleet.fleetId}
                                        value={String(fleet.fleetId)}
                                        disabled={!fleetSelectable(currentFleet?.id ?? -1, fleet.fleetId, fleet)}
                                    >
                                        {fleet.fleetName}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <Separator />

                    {/* New Fleet Creation */}
                    <div className="flex flex-col gap-2">
                        <Label>Create New Fleet</Label>

                        {/* Input + Button */}
                        <form onSubmit={(e) => {
                            e.preventDefault();
                            createFleet();
                        }}>
                            <div className="flex gap-2">
                                <Input
                                    name="fleetName"
                                    placeholder="Fleet name"
                                    disabled={fleetLoading || isCreatingFleet}
                                    maxLength={FLEET_NAME_LENGTH_LIMIT}
                                    value={newFleetName}
                                    onChange={(event) => setNewFleetName(event.target.value)}
                                />
                                <Button
                                    type="submit"
                                    disabled={fleetLoading || isCreatingFleet || !isFleetNameValid}
                                >
                                    {isCreatingFleet ? "Creating..." : "Create"}
                                </Button>
                            </div>
                        </form>

                        {/* Invalid Fleet Name Notice */}
                        {
                            (trimmedFleetName.length > 0 && !isFleetNameValid)
                            &&
                            <div className="text-sm text-destructive">
                                Fleet name must be 1-{FLEET_NAME_LENGTH_LIMIT} characters and may only include letters,
                                numbers, spaces, and @#$%^&amp;*()_+!/\\.,'-
                            </div>
                        }

                        {/* Notice */}
                        <div className="text-sm text-muted-foreground flex items-center gap-1 mt-2">
                            <Info size={12} className="inline mb-0.5" />
                            Created fleets cannot be deleted. A fleet cannot be left unless another user can manage it and another valid fleet is available to switch to.
                        </div>

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
                            
                            {
                                (!invitesLoading && invites.length > 0)
                                &&
                                <>
                                    <Separator />
                                    <div className="grid grid-cols-3">
                                        {invites.map((invite) => (
                                            <div key={invite.fleetName} className="flex items-center justify-between gap-2 bg-background rounded-lg border border-border p-3">
                                                <div className="flex flex-col gap-1">
                                                    <span className="font-medium">{invite.fleetName}</span>
                                                    <span className="text-xs text-muted-foreground">Invited by {invite.inviteEmail}</span>
                                                </div>
                                                <div className="flex flex-wrap gap-2">
                                                    <Button
                                                        variant={"ghost"}
                                                        disabled={inviteAction === `accept-${invite.fleetName}`}
                                                        onClick={() => submitInviteAction(invite.fleetName, "accept")}
                                                    >
                                                        Accept
                                                    </Button>
                                                    <Button
                                                        variant="ghostDestructive"
                                                        disabled={inviteAction === `decline-${invite.fleetName}`}
                                                        onClick={() => submitInviteAction(invite.fleetName, "decline")}
                                                    >
                                                        Decline
                                                    </Button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </>
                            }
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
