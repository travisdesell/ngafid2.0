import React from 'react';
import { showErrorModal } from "./error_modal.js";

class TwoFactorPrompt extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            showPrompt: true,
            loading: false,
            user: null,
            is2FAEnabled: false,
            timeLeft: 30
        };
    }

    componentDidMount() {
        this.check2FAStatus();
        
        // Auto-hide the prompt after 30 seconds to be less invasive
        this.autoHideTimer = setTimeout(() => {
            this.setState({ showPrompt: false });
        }, 30000);
        
        // Update countdown every second
        this.countdownTimer = setInterval(() => {
            this.setState(prevState => ({
                timeLeft: Math.max(0, prevState.timeLeft - 1)
            }));
        }, 1000);
    }

    componentWillUnmount() {
        if (this.autoHideTimer) {
            clearTimeout(this.autoHideTimer);
        }
        if (this.countdownTimer) {
            clearInterval(this.countdownTimer);
        }
    }

    check2FAStatus() {
        $.ajax({
            type: 'GET',
            url: '/api/user/me',
            dataType: 'json',
            success: (response) => {
                // Check if user has already dismissed this prompt in this session
                const isDismissed = sessionStorage.getItem('2fa_prompt_dismissed') === 'true';
                
                this.setState({
                    user: response,
                    is2FAEnabled: response.twoFactorEnabled || false,
                    // Only show prompt if not dismissed and 2FA is not enabled
                    showPrompt: !isDismissed && !response.twoFactorEnabled
                });
            },
            error: (xhr, status, error) => {
                console.log('Failed to check 2FA status:', error);
            }
        });
    }

    initiate2FASetup() {
        this.setState({ loading: true });
        
        $.ajax({
            type: 'POST',
            url: '/api/auth/setup-2fa',
            dataType: 'json',
            success: (response) => {
                if (response.success) {
                    // Clear the dismissed state since user is now setting up 2FA
                    sessionStorage.removeItem('2fa_prompt_dismissed');
                    // Redirect to 2FA settings page to complete setup
                    window.location.href = "/two-factor-settings";
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

    dismissPrompt() {
        this.setState({ showPrompt: false });
        // Remember that user has dismissed this prompt in this session
        // This will persist until the browser tab is closed or session storage is cleared
        sessionStorage.setItem('2fa_prompt_dismissed', 'true');
    }

    render() {
        // Don't show if user dismissed it, if 2FA is already enabled, or if showPrompt is false
        if (!this.state.showPrompt || this.state.is2FAEnabled) {
            return null;
        }

        return (
            <div 
                className="fixed bottom-4 right-4 z-[9999] max-w-sm" 
                style={{ 
                    position: 'fixed', 
                    bottom: '16px', 
                    right: '16px',
                    zIndex: 9999,
                    pointerEvents: 'auto'
                }}
            >
                <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 p-4">
                    {/* Header */}
                    <div className="flex items-start justify-between mb-3">
                        <div className="flex items-center">
                            <h6 className="text-sm font-semibold text-gray-900 dark:text-white mb-0">
                                Protect Your Data
                            </h6>
                        </div>
                        <button
                            onClick={this.dismissPrompt.bind(this)}
                            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-sm"
                            title="Dismiss"
                        >
                            ×
                        </button>
                    </div>

                    {/* Message */}
                    <p className="text-xs text-gray-600 dark:text-gray-400 mb-4">
                        You can protect your account with two-factor authentication for enhanced security.
                        <br />
                        <br />
                        <small className="text-gray-500 dark:text-gray-500">
                            Setup 2-Factor Authentication later in Account → 2-Factor Auth
                        </small>
                    </p>

                    {/* Buttons */}
                    <div className="flex gap-2">
                        <button
                            onClick={this.initiate2FASetup.bind(this)}
                            disabled={this.state.loading}
                            className="flex-1 bg-blue-500 hover:bg-blue-600 disabled:bg-blue-300 text-white text-xs px-3 py-2 rounded-md transition-colors duration-200"
                        >
                            {this.state.loading ? (
                                <span>Setting up...</span>
                            ) : (
                                <span>Setup 2FA</span>
                            )}
                        </button>
                        <button
                            onClick={this.dismissPrompt.bind(this)}
                            className="flex-1 bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-xs px-3 py-2 rounded-md transition-colors duration-200"
                        >
                            Not Now
                        </button>
                    </div>
                </div>
            </div>
        );
    }
}

export default TwoFactorPrompt;
