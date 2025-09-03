import 'bootstrap';
import React from "react";
import { createRoot } from 'react-dom/client';
import { showErrorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

class TwoFactorSettings extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            user: null,
            is2FAEnabled: false,
            is2FASetupComplete: false,
            setupStep: 'initial',
            qrCodeUrl: '',
            secret: '',
            verificationCode: '',
            backupCodes: [],
            showBackupCodes: false,
            password: '',
            loading: false,
            initialLoading: true,
            successMessage: ''
        };
    }

    componentDidMount() {
        // Always load the latest user data from the API to get current 2FA status
        this.loadUserData();
        
        // Set up periodic polling of 2FA status (every 30 seconds)
        this.statusPollingInterval = setInterval(() => {
            this.refresh2FAStatus();
        }, 30000);
        
        // Also refresh when the page becomes visible (user switches back to tab)
        document.addEventListener('visibilitychange', this.handleVisibilityChange.bind(this));
    }
    
    handleVisibilityChange() {
        if (!document.hidden && this.state.setupStep === 'initial') {
            // Page became visible, refresh the status
            this.refresh2FAStatus();
        }
    }
    
    componentWillUnmount() {
        // Clean up the polling interval
        if (this.statusPollingInterval) {
            clearInterval(this.statusPollingInterval);
        }
        
        // Clean up the visibility change listener
        document.removeEventListener('visibilitychange', this.handleVisibilityChange.bind(this));
    }

    loadUserData() {
        // Always fetch the latest user data from the API to get current 2FA status
        $.ajax({
            type: 'GET',
            url: '/api/user/me',
            dataType: 'json',
            success: (response) => {
                console.log('Loaded current user data:', response);
                
                // Check if there's an incomplete 2FA setup and automatically cancel it
                if (response.twoFactorSecret && !response.twoFactorSetupComplete && !response.twoFactorEnabled) {
                    console.log('Detected incomplete 2FA setup, automatically cancelling...');
                    this.autoCancelIncompleteSetup(response);
                } else {
                    this.setState({
                        user: response,
                        is2FAEnabled: response.twoFactorEnabled || false,
                        is2FASetupComplete: response.twoFactorSetupComplete || false,
                        initialLoading: false
                    });
                }
            },
            error: (xhr, status, error) => {
                // If unauthorized, this might be a 2FA setup scenario
                if (xhr.status === 401) {
                    console.log('User not authenticated, setting up 2FA setup state');
                    this.setState({
                        user: null,
                        is2FAEnabled: false,
                        is2FASetupComplete: false,
                        setupStep: 'initial',
                        initialLoading: false
                    });
                } else {
                    console.error('Failed to load user data:', error);
                    showErrorModal("Failed to load user data", error);
                    
                    // Fallback to server-side data if available
                    if (typeof user !== 'undefined' && user !== null) {
                        console.log('Falling back to server-side user data');
                        this.setState({
                            user: user,
                            is2FAEnabled: user.twoFactorEnabled || false,
                            is2FASetupComplete: user.twoFactorSetupComplete || false,
                            initialLoading: false
                        });
                    } else {
                        // Set initialLoading to false even if we have no data
                        this.setState({ initialLoading: false });
                    }
                }
            }
        });
    }

    refresh2FAStatus() {
        // Only refresh if we're not in the middle of a setup process
        if (this.state.setupStep !== 'initial') {
            return;
        }
        
        $.ajax({
            type: 'GET',
            url: '/api/user/me',
            dataType: 'json',
            success: (response) => {
                const new2FAStatus = response.twoFactorEnabled || false;
                const new2FASetupStatus = response.twoFactorSetupComplete || false;
                
                // Only update state if the status has changed
                if (this.state.is2FAEnabled !== new2FAStatus || 
                    this.state.is2FASetupComplete !== new2FASetupStatus) {
                    
                    console.log(`2FA status updated: enabled=${new2FAStatus}, setupComplete=${new2FASetupStatus}`);
                    
                    this.setState({
                        user: response,
                        is2FAEnabled: new2FAStatus,
                        is2FASetupComplete: new2FASetupStatus
                    });
                }
            },
            error: (xhr, status, error) => {
                // Silently fail for polling - don't show error modal
                console.log("Failed to refresh 2FA status:", error);
            }
        });
    }

    initiate2FASetup() {
        this.setState({ loading: true });
        
        // Always start fresh - no need to check for incomplete setup
        $.ajax({
            type: 'POST',
            url: '/api/auth/setup-2fa',
            dataType: 'json',
            success: (response) => {
                if (response.success) {
                    this.setState({
                        setupStep: 'qr',
                        qrCodeUrl: response.qrCodeUrl,
                        secret: response.secret,
                        loading: false
                    });
                } else {
                    showErrorModal("Failed to initiate 2FA setup", response.message);
                    this.setState({ loading: false });
                }
            },
            error: (xhr, status, error) => {
                showErrorModal("Failed to initiate 2FA setup", error);
                this.setState({ loading: false });
            }
        });
    }

    verify2FASetup() {
        if (!this.state.verificationCode || this.state.verificationCode.length !== 6) {
            showErrorModal("Invalid Code", "Please enter a valid 6-digit verification code.");
            return;
        }

        this.setState({ loading: true });

        $.ajax({
            type: 'POST',
            url: '/api/auth/verify-2fa-setup',
            data: { code: this.state.verificationCode },
            dataType: 'json',
            success: (response) => {
                if (response.success) {
                    this.setState({
                        setupStep: 'complete',
                        backupCodes: response.backupCodes,
                        is2FAEnabled: true,
                        is2FASetupComplete: true,
                        loading: false
                    });
                    
                    this.setState({ successMessage: "Two-factor authentication has been enabled successfully!" });
                    
                    // Auto-clear success message after 5 seconds
                    setTimeout(() => this.setState({ successMessage: '' }), 5000);
                    
                    // Refresh the 2FA status to ensure consistency
                    setTimeout(() => this.refresh2FAStatus(), 1000);
                } else {
                    showErrorModal("Verification Failed", response.message);
                    this.setState({ loading: false });
                }
            },
            error: (xhr, status, error) => {
                showErrorModal("Verification failed", error);
                this.setState({ loading: false });
            }
        });
    }

    disable2FA() {
        if (!this.state.password) {
            showErrorModal("Password Required", "Please enter your password to disable 2FA.");
            return;
        }

        this.setState({ loading: true });

        $.ajax({
            type: 'POST',
            url: '/api/auth/disable-2fa',
            data: { password: this.state.password },
            dataType: 'json',
            success: (response) => {
                if (response.success) {
                    this.setState({
                        is2FAEnabled: false,
                        is2FASetupComplete: false,
                        setupStep: 'initial',
                        password: '',
                        loading: false
                    });
                    this.setState({ successMessage: "Two-factor authentication has been disabled." });
                    
                    // Auto-clear success message after 5 seconds
                    setTimeout(() => this.setState({ successMessage: '' }), 5000);
                    
                    // Refresh the 2FA status to ensure consistency
                    setTimeout(() => this.refresh2FAStatus(), 1000);
                } else {
                    showErrorModal("Failed to disable 2FA", response.message);
                    this.setState({ loading: false });
                }
            },
            error: (xhr, status, error) => {
                showErrorModal("Failed to disable 2FA", error);
                this.setState({ loading: false });
            }
        });
    }

    generateNewBackupCodes() {
        if (!this.state.password) {
            showErrorModal("Password Required", "Please enter your password to generate new backup codes.");
            return;
        }

        this.setState({ loading: true });

        $.ajax({
            type: 'POST',
            url: '/api/auth/generate-backup-codes',
            data: { password: this.state.password },
            dataType: 'json',
            success: (response) => {
                if (response.success) {
                    this.setState({
                        backupCodes: response.backupCodes,
                        showBackupCodes: true,
                        password: '',
                        loading: false
                    });
                    
                    showSuccessMessage("New backup codes have been generated successfully!");
                } else {
                    showErrorModal("Failed to generate backup codes", response.message);
                    this.setState({ loading: false });
                }
            },
            error: (xhr, status, error) => {
                showErrorModal("Failed to generate backup codes", error);
                this.setState({ loading: false });
            }
        });
    }

    autoCancelIncompleteSetup(userData) {
        // Automatically cancel incomplete setup without showing modal
        $.ajax({
            type: 'POST',
            url: '/api/auth/cancel-2fa-setup',
            dataType: 'json',
            success: (response) => {
                if (response.success) {
                    console.log('Automatically cancelled incomplete 2FA setup');
                    // Set state with cleaned up user data
                    this.setState({
                        user: { ...userData, twoFactorSecret: null },
                        is2FAEnabled: false,
                        is2FASetupComplete: false,
                        initialLoading: false
                    });
                } else {
                    console.error('Failed to auto-cancel incomplete setup:', response.message);
                    // Fall back to showing the incomplete setup
                    this.setState({
                        user: userData,
                        is2FAEnabled: false,
                        is2FASetupComplete: false,
                        initialLoading: false
                    });
                }
            },
            error: (xhr, status, error) => {
                console.error('Error auto-cancelling incomplete setup:', error);
                // Fall back to showing the incomplete setup
                this.setState({
                    user: userData,
                    is2FAEnabled: false,
                    is2FASetupComplete: false,
                    initialLoading: false
                });
            }
        });
    }



    renderInitialState() {
        const is2FAEnabled = this.state.is2FAEnabled || (typeof user !== 'undefined' && user && user.twoFactorEnabled);

        
        return (
            <div className="card">
                <div className="card-header">
                    <h5 className="mb-0">Two-Factor Authentication</h5>
                </div>
                
                {/* Success Message */}
                {this.state.successMessage && (
                    <div className="alert alert-success alert-dismissible fade show m-3" role="alert">
                        <strong>Success!</strong> {this.state.successMessage}
                        <button 
                            type="button" 
                            className="btn-close" 
                            onClick={() => this.setState({ successMessage: '' })}
                            aria-label="Close"
                        ></button>
                    </div>
                )}
                
                <div className="card-body">
                    <div className="mb-3">
                        <small className="text-muted">
                            Status: <strong>{is2FAEnabled ? 'Enabled' : 'Disabled'}</strong>
                        </small>
                    </div>
                    
                    {is2FAEnabled ? (
                        <div>
                            <div className="alert alert-success">
                                <strong>Two-factor authentication is enabled</strong>
                                <p className="mb-0">Your account is protected with an additional layer of security.</p>
                            </div>
                            <div className="d-flex gap-2">
                                <button 
                                    className="btn btn-danger" 
                                    onClick={() => this.setState({ setupStep: 'disable' })}
                                    disabled={this.state.loading}
                                >
                                    {this.state.loading ? 'Loading...' : 'Disable 2FA'}
                                </button>
                                <button 
                                    className="btn btn-secondary" 
                                    onClick={() => this.setState({ setupStep: 'backup' })}
                                    disabled={this.state.loading}
                                >
                                    Generate New Backup Codes
                                </button>
                            </div>
                        </div>
                    ) : (
                        <div>
                            <div className="alert alert-info">
                                <strong>Two-factor authentication is not enabled</strong>
                                <p className="mb-0">Enable 2FA to add an extra layer of security to your account.</p>
                            </div>
                            <button 
                                className="btn btn-primary" 
                                onClick={this.initiate2FASetup.bind(this)}
                                disabled={this.state.loading}
                            >
                                {this.state.loading ? 'Loading...' : 'Enable 2FA'}
                            </button>
                        </div>
                    )}
                </div>
            </div>
        );
    }

    renderQRSetup() {
        return (
            <div className="card">
                <div className="card-header">
                    <h5 className="mb-0">Set Up Two-Factor Authentication</h5>
                </div>
                <div className="card-body">
                    <div className="row">
                        <div className="col-md-6">
                            <h6>Step 1: Scan QR Code</h6>
                            <p>Use your authenticator app to scan this QR code:</p>
                            <div className="text-center mb-3">
                                <img 
                                    src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(this.state.qrCodeUrl)}`}
                                    alt="QR Code for 2FA"
                                    className="img-fluid border"
                                />
                            </div>
                            <p><small className="text-muted">
                                <strong>Manual entry:</strong> {this.state.secret}
                            </small></p>
                        </div>
                        <div className="col-md-6">
                            <h6>Step 2: Verify Setup</h6>
                            <p>Enter the 6-digit code from your authenticator app:</p>
                            <input
                                type="text"
                                className="form-control mb-3"
                                placeholder="000000"
                                maxLength="6"
                                value={this.state.verificationCode}
                                onChange={(e) => this.setState({ verificationCode: e.target.value })}
                                style={{ textAlign: 'center', fontSize: '18px', letterSpacing: '2px' }}
                            />
                            <button 
                                className="btn btn-primary me-2" 
                                onClick={this.verify2FASetup.bind(this)}
                                disabled={this.state.loading || this.state.verificationCode.length !== 6}
                            >
                                {this.state.loading ? 'Verifying...' : 'Verify & Enable'}
                            </button>
                            <button 
                                className="btn btn-secondary" 
                                onClick={() => this.setState({ setupStep: 'initial' })}
                                disabled={this.state.loading}
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    renderDisableConfirmation() {
        return (
            <div className="card">
                <div className="card-header">
                    <h5 className="mb-0">Disable Two-Factor Authentication</h5>
                </div>
                <div className="card-body">
                    <p>To disable two-factor authentication, please enter your password:</p>
                    <input
                        type="password"
                        className="form-control mb-3"
                        placeholder="Enter your password"
                        value={this.state.password}
                        onChange={(e) => this.setState({ password: e.target.value })}
                    />
                    <button 
                        className="btn btn-danger me-2" 
                        onClick={this.disable2FA.bind(this)}
                        disabled={this.state.loading || !this.state.password}
                    >
                        {this.state.loading ? 'Disabling...' : 'Disable 2FA'}
                    </button>
                    <button 
                        className="btn btn-secondary" 
                        onClick={() => {
                            this.setState({ setupStep: 'initial', password: '' });
                            // Refresh the 2FA status after going back
                            setTimeout(() => this.refresh2FAStatus(), 500);
                        }}
                        disabled={this.state.loading}
                    >
                        Cancel
                    </button>
                </div>
            </div>
        );
    }

    renderBackupCodes() {
        return (
            <div className="card">
                <div className="card-header">
                    <h5 className="mb-0">Backup Codes</h5>
                </div>
                <div className="card-body">
                    <div className="alert alert-info">
                        <strong>Backup Codes</strong>
                        <p className="mb-0">Save these codes in a secure location. You can use them to access your account if you lose your authenticator device.</p>
                    </div>
                    <div className="row">
                        {this.state.backupCodes.map((code, index) => (
                            <div key={index} className="col-md-3 col-sm-6 mb-2">
                                <code className="d-block p-2 bg-light border text-center">{code}</code>
                            </div>
                        ))}
                    </div>
                    <button 
                        className="btn btn-secondary" 
                        onClick={() => {
                            this.setState({ setupStep: 'initial' });
                            // Refresh the 2FA status after going back
                            setTimeout(() => this.refresh2FAStatus(), 500);
                        }}
                    >
                        Back to 2FA Settings
                    </button>
                </div>
            </div>
        );
    }

    renderGenerateBackupCodes() {
        return (
            <div className="card">
                <div className="card-header">
                    <h5 className="mb-0">Generate New Backup Codes</h5>
                </div>
                <div className="card-body">
                    <div className="alert alert-warning">
                        <strong>Warning:</strong> Generating new backup codes will invalidate your existing backup codes.
                    </div>
                    <p>To generate new backup codes, please enter your password:</p>
                    <input
                        type="password"
                        className="form-control mb-3"
                        placeholder="Enter your password"
                        value={this.state.password}
                        onChange={(e) => this.setState({ password: e.target.value })}
                    />
                    <button 
                        className="btn btn-primary me-2" 
                        onClick={this.generateNewBackupCodes.bind(this)}
                        disabled={this.state.loading || !this.state.password}
                    >
                        {this.state.loading ? 'Generating...' : 'Generate New Codes'}
                    </button>
                    <button 
                        className="btn btn-secondary" 
                        onClick={() => {
                            this.setState({ setupStep: 'initial', password: '' });
                            // Refresh the 2FA status after going back
                            setTimeout(() => this.refresh2FAStatus(), 500);
                        }}
                        disabled={this.state.loading}
                    >
                        Cancel
                    </button>
                </div>
            </div>
        );
    }

    render() {
        // Show loading while fetching initial data
        if (this.state.initialLoading) {
            return (
                <div>
                    <SignedInNavbar />
                    <div className="container mt-4">
                        <div className="row">
                            <div className="col-md-8 mx-auto">
                                <div className="text-center">
                                    <div className="spinner-border text-primary" role="status">
                                        <span className="visually-hidden">Loading...</span>
                                    </div>
                                    <p className="mt-2">Loading 2FA settings...</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            );
        }

        return (
            <div>
                <SignedInNavbar />
                <div className="container mt-4">
                    <div className="row">
                        <div className="col-md-8 mx-auto">
                            <h2>Security Settings</h2>
                            <hr />
                            
                            {this.state.setupStep === 'initial' && this.renderInitialState()}
                            {this.state.setupStep === 'qr' && this.renderQRSetup()}
                            {this.state.setupStep === 'disable' && this.renderDisableConfirmation()}
                            {this.state.setupStep === 'backup' && this.renderGenerateBackupCodes()}
                            {this.state.setupStep === 'complete' && this.renderBackupCodes()}
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

const container = document.querySelector("#two-factor-settings-content");
if (container) {
    const root = createRoot(container);
    root.render(<TwoFactorSettings />);
}

