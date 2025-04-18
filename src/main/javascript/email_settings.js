import 'bootstrap';
import React, { useState, useEffect } from 'react';
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;

export { EmailSettingsTableUser, EmailSettingsTableManager }

/*
------------------------------------
            USER SETTINGS
------------------------------------
*/

class EmailSettingsTableUser extends React.Component {

    constructor(props) {
        super(props);
        console.log("Generating User Email Settings Table...");
        this.state = {
            emailTypes: [],
            settings: {},
            disableFetching : false
        };
    }

    componentDidMount() {
        this.getUserEmailPreferences();
    }

    //Update user email preferences
    updateUserEmailPreferences = (updatedSettings) => {

        var submissionData = {
            handleUpdateType : "HANDLE_UPDATE_USER",
            ...updatedSettings
        };

        this.setState({ disableFetching: true });

        $.ajax({
            type: 'POST',
            url: '/protected/update_email_preferences',
            data: submissionData,
            dataType: 'json',
            async: true,

            success: (response) => {
                console.log('Email preferences updated successfully!');
                this.setState({ disableFetching: false });
            },

            error: (jqXHR, textStatus, errorThrown) => {
                console.log('Error updating email preferences:', errorThrown);
                this.setState({ disableFetching: false });
            }
        });
    }

    //Fetch user email preferences
    getUserEmailPreferences = () => {

        let submissionData = {
            handleFetchType : "HANDLE_FETCH_USER"
        };

        $.ajax({
            type: 'GET',
            url: '/protected/email_preferences',
            data: submissionData,
            dataType: 'json',
            async: true,

            success: (response) => {

                console.log("got user pref response");
                console.log(response);

                let emailTypesIn = response.emailTypesKeys;
                let emailTypesUserIn = response.emailTypesUser;

                //Filter out email types marked as HIDDEN or FORCED
                emailTypesIn = emailTypesIn.filter(
                    type => (
                        (type.includes("HIDDEN") !== true)
                        && (type.includes("FORCED") !== true)
                    )
                );

                if (this.props.isAdmin) {
                    let emailTypesAdmin = emailTypesIn.filter(
                        type => (type.includes("ADMIN") === true)
                    );
                    emailTypesIn = emailTypesIn.filter(
                        type => (type.includes("ADMIN") !== true)
                    );
                    emailTypesIn = emailTypesIn.concat(emailTypesAdmin);
                } else {
                    emailTypesIn = emailTypesIn.filter(
                        type => (type.includes("ADMIN") !== true)
                    );
                }

                this.setState({
                    emailTypes: emailTypesIn,
                    settings: emailTypesUserIn
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("Error getting upset data:");
                console.log(errorThrown);
            },
        });
    }

    handleCheckboxChange = (type) => {
        this.setState(
            prevState => {
                let prevStateSettings = (prevState.settings || []);
                const updatedSettings = {
                    ...prevStateSettings,
                    [type]: !prevStateSettings[type]
                };
                return { settings: updatedSettings };
            },
            () => {
                this.updateUserEmailPreferences(this.state.settings);
            }
        );
    };

    render() {
        return (
            <table style={{
                width: "100%",
                textAlign: "center",
                borderCollapse: "separate",
                borderSpacing: "16px 16px"
            }}>
                <thead>
                <tr>
                    {
                        this.state.emailTypes.map((type, index) => (
                            <th key={index} style={{textAlign: "center"}}>
                                {type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                            </th>
                        ))
                    }
                </tr>
                </thead>
                <tbody>
                <tr>
                    {
                        this.state.emailTypes.map((type, typeIndex) => (
                            <td key={typeIndex} style={{ textAlign: "center" }}>
                                <input
                                    type="checkbox"
                                    checked={(this.state.settings && this.state.settings[type]) ? this.state.settings[type] : false}
                                    onChange={this.state.disableFetching ? () => undefined : () => this.handleCheckboxChange(type)}
                                    style={{ scale: "2.00" }}
                                />
                            </td>
                        ))
                    }
                </tr>
                </tbody>
            </table>
        )
    }
};

/*
----------------------------------------
            MANAGER SETTINGS
----------------------------------------
*/

class EmailSettingsTableManager extends React.Component {

    constructor(props) {
        super(props);
        console.log("Generating Manager Email Settings Table...");
        this.state = {
            emailTypes : [],
            fleetUsers : props.fleetUsers,
            disableFetching : false
        };
    }

    componentDidMount() {
        this.getUserEmailPreferences();
    }

    updateUserEmailPreferences = (fleetUser, updatedSettings) => {
        let updatedEmailTypeSettingsTarget = updatedSettings.find(setting => setting.id === fleetUser.id).emailTypesUser;

        var submissionData = {
            handleUpdateType : "HANDLE_UPDATE_MANAGER",
            fleetUserID : fleetUser.id,
            fleetID : fleetUser.fleet.id,
            ...updatedEmailTypeSettingsTarget
        };

        this.setState({ disableFetching: true });

        $.ajax({
            type: 'POST',
            url: '/protected/update_email_preferences',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: () => this.setState({ disableFetching: false }),
            error: (_, __, errorThrown) => {
                console.log('Error updating preferences:', errorThrown);
                this.setState({ disableFetching: false });
            }
        });
    }

    getUserEmailPreferences = () => {
        let fleetUsers = this.state.fleetUsers;

        fleetUsers.forEach(userTarget => {
            let submissionData = {
                handleFetchType : "HANDLE_FETCH_MANAGER",
                fleetUserID : userTarget.id,
                fleetID: userTarget.fleet.id
            };

            $.ajax({
                type: 'GET',
                url: '/protected/email_preferences',
                data: submissionData,
                dataType : 'json',
                async: true,
                success : (response) => {
                    let emailTypesUserIn = response.emailTypesUser;
                    let emailTypesKeysIn = response.emailTypesKeys.filter(type =>
                        !type.includes("HIDDEN") && !type.includes("FORCED") && !type.includes("ADMIN")
                    );

                    this.setState(prevState => {
                        const fleetUsers = prevState.fleetUsers.map(userCurrent => {
                            if (userCurrent.id === userTarget.id) {
                                return {
                                    ...userCurrent,
                                    emailTypesUser : emailTypesUserIn,
                                    emailTypesKeys : emailTypesKeysIn
                                };
                            }
                            return userCurrent;
                        });

                        return {
                            emailTypes : emailTypesKeysIn,
                            fleetUsers
                        };
                    });
                },
                error: (_, __, errorThrown) => console.log("Error getting upset data:", errorThrown)
            });
        });
    }

    handleCheckboxChange = (userTarget, type) => {
        this.setState(
            prevState => {
                const fleetUsers = prevState.fleetUsers.map(user => {
                    if (user.id === userTarget.id) {
                        return {
                            ...user,
                            emailTypesUser: {
                                ...user.emailTypesUser,
                                [type]: (user.emailTypesUser ? !user.emailTypesUser[type] : false)
                            }
                        };
                    }
                    return user;
                });
                return { fleetUsers };
            },
            () => this.updateUserEmailPreferences(userTarget, this.state.fleetUsers)
        );
    };

    refreshFleetUsers = (updatedUsers) => {
        this.setState({ fleetUsers: updatedUsers });
    };

    render() {
        return (
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                <tr>
                    <th style={{padding: "0 12px"}}>Email</th>
                    {
                        this.state.emailTypes.map((type, index) => (
                            <th key={index}>
                                <div style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    padding: "20px 0px",
                                    margin: "-7px",
                                    gap: "8px",
                                    width: "95%"
                                }}>
                                    <ToggleButtonColumnManager
                                        disableFetching={this.state.disableFetching}
                                        updateUserEmailPreferences={this.updateUserEmailPreferences}
                                        fleetUsers={this.state.fleetUsers}
                                        refreshFleetUsers={this.refreshFleetUsers}
                                        emailTypes={this.state.emailTypes}
                                        emailTypeIndex={index}
                                    />
                                    {type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                                </div>
                            </th>
                        ))
                    }
                </tr>
                </thead>
                <tbody>
                <tr style={{border: "solid", borderColor: "#00004411", borderWidth: "2px 0px"}}></tr>
                {
                    this.state.fleetUsers
                        .filter(userCurrent => userCurrent.fleetAccess.accessType !== "DENIED")
                        .map((userCurrent, settingIndex) => {
                            const rowStyle = {
                                backgroundColor: (settingIndex % 2 === 0) ? '#FFFFFF00' : '#00000009'
                            };

                            return (
                                <tr key={settingIndex} style={{...rowStyle, border: "solid", borderColor: "#00004411", borderWidth: "1px 0px"}}>
                                    <td style={{padding: "16px 12px"}}>{userCurrent.email}</td>
                                    {
                                        this.state.emailTypes.map((type, typeIndex) => (
                                            <td key={typeIndex}>
                                                <input
                                                    type="checkbox"
                                                    checked={(userCurrent.emailTypesUser && userCurrent.emailTypesUser[type]) ? userCurrent.emailTypesUser[type] : false}
                                                    onChange={this.state.disableFetching ? () => undefined : () => this.handleCheckboxChange(userCurrent, type)}
                                                    style={{scale: "2.00"}}
                                                />
                                            </td>
                                        ))
                                    }
                                </tr>
                            );
                        })
                }
                </tbody>
            </table>
        );
    }
}
