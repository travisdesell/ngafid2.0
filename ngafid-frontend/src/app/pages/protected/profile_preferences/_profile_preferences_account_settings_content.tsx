// ngafid-frontend/src/app/pages/protected/profile_preferences/_profile_preferences_account_settings_content.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { fetchJson } from "@/fetchJson";
import { validatePasswordChange } from "@/lib/password_validation";
import { type FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";

type ErrorResponse = {
    errorTitle?: string;
    errorMessage?: string;
};

type TwoFactorStatusResponse = {
    twoFactorEnabled?: boolean;
    twoFactorSetupComplete?: boolean;
    twoFactorSecret?: string | null;
};

type TwoFactorSetupResponse = {
    success: boolean;
    qrCodeUrl?: string;
    secret?: string;
    message?: string;
};

type TwoFactorVerifyResponse = {
    success: boolean;
    backupCodes?: string[];
    message?: string;
};

type TwoFactorSimpleResponse = {
    success: boolean;
    message?: string;
};

type SetupStep = "initial" | "qr" | "disable" | "backup" | "complete";

export default function ProfilePreferencesAccountSettingsContent() {

    const { setModal } = useModal();

    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordSaving, setPasswordSaving] = useState(false);
    const [passwordSuccess, setPasswordSuccess] = useState("");

    const [setupStep, setSetupStep] = useState<SetupStep>("initial");
    const [is2FAEnabled, setIs2FAEnabled] = useState(false);
    const [is2FASetupComplete, setIs2FASetupComplete] = useState(false);
    const [qrCodeUrl, setQrCodeUrl] = useState("");
    const [secret, setSecret] = useState("");
    const [verificationCode, setVerificationCode] = useState("");
    const [backupCodes, setBackupCodes] = useState<string[]>([]);
    const [twoFactorPassword, setTwoFactorPassword] = useState("");
    const [twoFactorLoading, setTwoFactorLoading] = useState(false);
    const [twoFactorLoadingInitial, setTwoFactorLoadingInitial] = useState(true);

    const setupStepRef = useRef<SetupStep>(setupStep);
    useEffect(() => {
        setupStepRef.current = setupStep;
    }, [setupStep]);


    const loadUserData = useCallback(async () => {
        setTwoFactorLoadingInitial(true);
        const response = await fetchJson.get<TwoFactorStatusResponse>("/api/user/me").catch((error: Error) => {
            setModal(ErrorModal, { title: "Failed to load user data", message: error.message });
            return null;
        });

        if (!response) {
            setTwoFactorLoadingInitial(false);
            return;
        }

        const hasIncompleteSetup = Boolean(response.twoFactorSecret) && !response.twoFactorSetupComplete && !response.twoFactorEnabled;
        if (hasIncompleteSetup) {
            const cancelResponse = await fetchJson.post<TwoFactorSimpleResponse>("/api/auth/cancel-2fa-setup").catch(() => null);
            if (!cancelResponse?.success) {
                setIs2FAEnabled(Boolean(response.twoFactorEnabled));
                setIs2FASetupComplete(Boolean(response.twoFactorSetupComplete));
                setTwoFactorLoadingInitial(false);
                return;
            }
        }

        setIs2FAEnabled(Boolean(response.twoFactorEnabled));
        setIs2FASetupComplete(Boolean(response.twoFactorSetupComplete));
        setTwoFactorLoadingInitial(false);
    }, [setModal]);

    const refresh2FAStatus = useCallback(async () => {
        if (setupStepRef.current !== "initial")
            return;

        const response = await fetchJson.get<TwoFactorStatusResponse>("/api/user/me").catch(() => null);
        if (!response)
            return;

        const enabled = Boolean(response.twoFactorEnabled);
        const setupComplete = Boolean(response.twoFactorSetupComplete);

        if (enabled !== is2FAEnabled || setupComplete !== is2FASetupComplete) {
            setIs2FAEnabled(enabled);
            setIs2FASetupComplete(setupComplete);
        }
    }, [is2FAEnabled, is2FASetupComplete]);

    useEffect(() => {
        loadUserData();

        const interval = window.setInterval(() => refresh2FAStatus(), 30000);
        const onVisibilityChange = () => {
            if (!document.hidden)
                refresh2FAStatus();
        };

        document.addEventListener("visibilitychange", onVisibilityChange);

        return () => {
            window.clearInterval(interval);
            document.removeEventListener("visibilitychange", onVisibilityChange);
        };
    }, [loadUserData, refresh2FAStatus]);

    const passwordValidation = useMemo(
        () => validatePasswordChange(currentPassword, newPassword, confirmPassword),
        [confirmPassword, currentPassword, newPassword],
    );

    const updatePassword = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!passwordValidation.valid || passwordSaving)
            return;

        setPasswordSaving(true);
        const payload = new URLSearchParams({
            currentPassword,
            newPassword,
            confirmPassword,
        });

        const response = await fetchJson.patch("/api/auth/change-password", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Submitting Account Information", message: error.message });
            return null;
        });

        if (!response) {
            setPasswordSaving(false);
            return;
        }

        const errorResponse = response as ErrorResponse;
        if (errorResponse.errorTitle) {
            setModal(ErrorModal, { title: errorResponse.errorTitle, message: errorResponse.errorMessage ?? "" });
            setPasswordSaving(false);
            return;
        }

        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
        setPasswordSuccess("Password updated successfully.");
        setPasswordSaving(false);

        window.setTimeout(() => setPasswordSuccess(""), 4000);
    };

    const initiate2FASetup = async () => {
        setTwoFactorLoading(true);

        const response = await fetchJson.post<TwoFactorSetupResponse>("/api/auth/setup-2fa").catch((error: Error) => {
            setModal(ErrorModal, { title: "Failed to initiate 2FA setup", message: error.message });
            return null;
        });

        if (!response || !response.success) {
            setTwoFactorLoading(false);
            return;
        }

        setSetupStep("qr");
        setQrCodeUrl(response.qrCodeUrl ?? "");
        setSecret(response.secret ?? "");
        setTwoFactorLoading(false);
    };

    const verify2FASetup = async () => {
        if (verificationCode.trim().length !== 6) {
            setModal(ErrorModal, { title: "Invalid Code", message: "Please enter a valid 6-digit verification code." });
            return;
        }

        setTwoFactorLoading(true);
        const payload = new URLSearchParams({ code: verificationCode.trim() });
        const response = await fetchJson.post<TwoFactorVerifyResponse>("/api/auth/verify-2fa-setup", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Verification failed", message: error.message });
            return null;
        });

        if (!response?.success) {
            setModal(ErrorModal, { title: "Verification failed", message: response?.message ?? "Please try again." });
            setTwoFactorLoading(false);
            return;
        }

        setBackupCodes(response.backupCodes ?? []);
        setIs2FAEnabled(true);
        setIs2FASetupComplete(true);
        setSetupStep("complete");
        setVerificationCode("");
        setTwoFactorLoading(false);
        showSuccessMessage("Two-factor authentication has been enabled successfully.");

        window.setTimeout(() => refresh2FAStatus(), 1000);
    };

    const disable2FA = async () => {
        if (!twoFactorPassword) {
            setModal(ErrorModal, { title: "Password Required", message: "Please enter your password to disable 2FA." });
            return;
        }

        setTwoFactorLoading(true);
        const payload = new URLSearchParams({ password: twoFactorPassword });
        const response = await fetchJson.post<TwoFactorSimpleResponse>("/api/auth/disable-2fa", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Failed to disable 2FA", message: error.message });
            return null;
        });

        if (!response?.success) {
            setModal(ErrorModal, { title: "Failed to disable 2FA", message: response?.message ?? "Please try again." });
            setTwoFactorLoading(false);
            return;
        }

        setIs2FAEnabled(false);
        setIs2FASetupComplete(false);
        setSetupStep("initial");
        setTwoFactorPassword("");
        setTwoFactorLoading(false);
        showSuccessMessage("Two-factor authentication has been disabled.");

        window.setTimeout(() => refresh2FAStatus(), 1000);
    };

    const generateNewBackupCodes = async () => {
        if (!twoFactorPassword) {
            setModal(ErrorModal, { title: "Password Required", message: "Please enter your password to generate new backup codes." });
            return;
        }

        setTwoFactorLoading(true);
        const payload = new URLSearchParams({ password: twoFactorPassword });
        const response = await fetchJson.post<TwoFactorVerifyResponse>("/api/auth/generate-backup-codes", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Failed to generate backup codes", message: error.message });
            return null;
        });

        if (!response?.success) {
            setModal(ErrorModal, { title: "Failed to generate backup codes", message: response?.message ?? "Please try again." });
            setTwoFactorLoading(false);
            return;
        }

        setBackupCodes(response.backupCodes ?? []);
        setSetupStep("complete");
        setTwoFactorPassword("");
        setTwoFactorLoading(false);
        showSuccessMessage("New backup codes have been generated successfully.");
    };

    const cancelQrSetup = async () => {
        setTwoFactorLoading(true);
        await fetchJson.post<TwoFactorSimpleResponse>("/api/auth/cancel-2fa-setup").catch(() => null);
        setTwoFactorLoading(false);
        setSetupStep("initial");
        setVerificationCode("");
        setQrCodeUrl("");
        setSecret("");
        window.setTimeout(() => refresh2FAStatus(), 500);
    };

    const renderTwoFactorContent = () => {
        if (twoFactorLoadingInitial) {
            return <div className="text-sm text-muted-foreground">Loading 2FA settings...</div>;
        }

        if (setupStep === "qr") {
            return (
                <div className="grid gap-6 md:grid-cols-2">
                    <div className="flex flex-col gap-3">
                        <h4 className="font-semibold">Step 1: Scan QR Code</h4>
                        <p className="text-sm text-muted-foreground">Use your authenticator app to scan this QR code.</p>
                        <div className="flex items-center justify-center rounded-lg border border-border dark:bg-background bg-muted p-4">
                            {qrCodeUrl ? (
                                <img
                                    src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(qrCodeUrl)}`}
                                    alt="QR Code for 2FA"
                                    className="h-48 w-48"
                                />
                            ) : (
                                <span className="text-xs text-muted-foreground">QR code unavailable</span>
                            )}
                        </div>
                        <div className="text-xs text-muted-foreground">
                            Manual entry: <span className="font-mono">{secret}</span>
                        </div>
                    </div>

                    <div className="flex flex-col gap-3">
                        <h4 className="font-semibold">Step 2: Verify Setup</h4>
                        <p className="text-sm text-muted-foreground">Enter the 6-digit code from your authenticator app.</p>
                        <Input
                            value={verificationCode}
                            onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, ""))}
                            maxLength={6}
                            placeholder="000000"
                            className="text-center tracking-[0.3em]"
                        />
                        <div className="flex flex-wrap gap-2">
                            <Button
                                onClick={verify2FASetup}
                                disabled={twoFactorLoading || verificationCode.length !== 6}
                            >
                                {twoFactorLoading ? "Verifying..." : "Verify & Enable"}
                            </Button>
                            <Button variant="outline" onClick={cancelQrSetup} disabled={twoFactorLoading}>
                                Cancel
                            </Button>
                        </div>
                    </div>
                </div>
            );
        }

        if (setupStep === "disable") {
            return (
                <div className="flex flex-col gap-4">
                    <p className="text-sm text-muted-foreground">Enter your password to disable two-factor authentication.</p>
                    <Input
                        type="password"
                        placeholder="Enter your password"
                        value={twoFactorPassword}
                        onChange={(event) => setTwoFactorPassword(event.target.value)}
                    />
                    <div className="flex flex-wrap gap-2">
                        <Button variant="destructive" onClick={disable2FA} disabled={twoFactorLoading || !twoFactorPassword}>
                            {twoFactorLoading ? "Disabling..." : "Disable 2FA"}
                        </Button>
                        <Button
                            variant="outline"
                            onClick={() => {
                                setSetupStep("initial");
                                setTwoFactorPassword("");
                                window.setTimeout(() => refresh2FAStatus(), 500);
                            }}
                            disabled={twoFactorLoading}
                        >
                            Cancel
                        </Button>
                    </div>
                </div>
            );
        }

        if (setupStep === "backup") {
            return (
                <div className="flex flex-col gap-4">
                    <div className="rounded-lg border border-border bg-muted/30 p-3 text-sm">
                        Generating new backup codes will invalidate your existing codes.
                    </div>
                    <Input
                        type="password"
                        placeholder="Enter your password"
                        value={twoFactorPassword}
                        onChange={(event) => setTwoFactorPassword(event.target.value)}
                    />
                    <div className="flex flex-wrap gap-2">
                        <Button onClick={generateNewBackupCodes} disabled={twoFactorLoading || !twoFactorPassword}>
                            {twoFactorLoading ? "Generating..." : "Generate New Codes"}
                        </Button>
                        <Button
                            variant="outline"
                            onClick={() => {
                                setSetupStep("initial");
                                setTwoFactorPassword("");
                                window.setTimeout(() => refresh2FAStatus(), 500);
                            }}
                            disabled={twoFactorLoading}
                        >
                            Cancel
                        </Button>
                    </div>
                </div>
            );
        }

        if (setupStep === "complete") {
            return (
                <div className="flex flex-col gap-4">
                    <span className=" text-sm">
                        Save these backup codes in a secure location. Each code can be <b>used once!</b> These codes <b>cannot be viewed again.</b>
                    </span>
                    <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-5">
                        {backupCodes.map((code) => (
                            <div key={code} className="rounded border border-border bg-background px-3 py-2 text-center font-mono text-sm">
                                {code}
                            </div>
                        ))}
                    </div>
                    <Button
                        variant="outline"
                        onClick={() => {
                            setSetupStep("initial");
                            window.setTimeout(() => refresh2FAStatus(), 500);
                        }}
                    >
                        Back to 2FA Settings
                    </Button>
                </div>
            );
        }

        return (
            <div className="flex flex-col gap-4">
                {/* <div className="text-sm text-muted-foreground">
                    Status: <span className="font-semibold text-foreground">{is2FAEnabled ? "Enabled" : "Disabled"}</span>
                </div> */}

                {is2FAEnabled ? (
                    <div className="flex flex-col gap-3">
                        <span className="text-sm">
                            Two-factor authentication is <b>enabled</b> for your account.
                        </span>
                        <div className="flex flex-wrap gap-2">
                            <Button
                                variant="outline"
                                onClick={() => setSetupStep("backup")}
                                disabled={twoFactorLoading}
                            >
                                Generate New Backup Codes
                            </Button>
                            <Button
                                variant="destructive"
                                onClick={() => setSetupStep("disable")}
                                disabled={twoFactorLoading}
                            >
                                Disable 2FA
                            </Button>
                        </div>
                    </div>
                ) : (
                    <div className="flex flex-col gap-3">
                        <div className="rounded-lg border border-border dark:bg-background bg-muted p-3 text-sm">
                            Two-factor authentication is <b>not enabled</b>. Enable it to protect your account.
                        </div>
                        <Button onClick={initiate2FASetup} disabled={twoFactorLoading}>
                            {twoFactorLoading ? "Loading..." : "Enable 2FA"}
                        </Button>
                    </div>
                )}
            </div>
        );
    };

    return (
        <div className="flex flex-col gap-6 pb-6">
            <Card className="card-glossy">
                <CardHeader>
                    <CardTitle>Change Password</CardTitle>
                    <CardDescription>Update your password and confirm changes.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={updatePassword} className="flex flex-col gap-4">
                        <div className="grid gap-4 md:grid-cols-2">
                            <div className="flex flex-col gap-2 md:col-span-2">
                                <Label htmlFor="current-password">Current Password</Label>
                                <Input
                                    id="current-password"
                                    type="password"
                                    value={currentPassword}
                                    onChange={(event) => setCurrentPassword(event.target.value)}
                                    autoComplete="current-password"
                                />
                            </div>
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="new-password">New Password</Label>
                                <Input
                                    id="new-password"
                                    type="password"
                                    value={newPassword}
                                    onChange={(event) => setNewPassword(event.target.value)}
                                    autoComplete="new-password"
                                />
                            </div>
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="confirm-password">Confirm New Password</Label>
                                <Input
                                    id="confirm-password"
                                    type="password"
                                    value={confirmPassword}
                                    onChange={(event) => setConfirmPassword(event.target.value)}
                                    autoComplete="new-password"
                                />
                            </div>
                        </div>

                        {(passwordSuccess || !passwordValidation.valid) && (
                            <div className={passwordValidation.valid ? "text-sm text-green-600" : "text-sm text-red-500"}>
                                {passwordValidation.valid ? passwordSuccess : passwordValidation.message}
                            </div>
                        )}

                        <div className="flex justify-end">
                            <Button type="submit" disabled={!passwordValidation.valid || passwordSaving}>
                                {passwordSaving ? "Updating..." : "Update Password"}
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>

            <Card className="card-glossy">
                <CardHeader>
                    <CardTitle>Two-Factor Authentication</CardTitle>
                    <CardDescription>Protect your account with an extra layer of security.</CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-4">
                    {renderTwoFactorContent()}
                </CardContent>
            </Card>
        </div>
    );
}
