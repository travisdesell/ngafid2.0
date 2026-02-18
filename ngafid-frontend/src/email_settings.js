import 'bootstrap';
import React from 'react';

import $ from 'jquery';

import InfoHint from './info_hint';

window.jQuery = $;
window.$ = $;

export {EmailSettingsTableUser, EmailSettingsTableManager};

import './index.css';          //<-- include Tailwind

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
            disableFetching: false
        };
    }

    componentDidMount() {
        this.getUserEmailPreferences();
    }

    //Update user email preferences
    updateUserEmailPreferences = (updatedSettings) => {
        this.setState({disableFetching: true});

        $.ajax({
            type: 'PUT',
            url: `/api/user/me/email-prefs`,
            data: updatedSettings,
            dataType: 'json',
            async: true,

            success: () => {
                console.log('Email preferences updated successfully!');
                this.setState({disableFetching: false});
            },

            error: (jqXHR, textStatus, errorThrown) => {
                console.log('Error updating email preferences:', errorThrown);
                this.setState({disableFetching: false});
            }
        });
    };

    //Fetch user email preferences
    getUserEmailPreferences = () => {

        $.ajax({
            type: 'GET',
            url: `/api/user/me/email-prefs`,
            async: true,
            success: (response) => {
                console.log("Got user pref response: ", response);

                let emailTypesIn = response.emailTypesKeys;
                const emailTypesUserIn = response.emailTypesUser;

                //Filter out email types marked as HIDDEN or FORCED
                emailTypesIn = emailTypesIn.filter(
                    (type) => !type.includes("HIDDEN") && !type.includes("FORCED")
                );

                if (this.props.isAdmin) {
                    const admin = emailTypesIn.filter((type) => type.includes("ADMIN"));
                    emailTypesIn = emailTypesIn.filter((type) => !type.includes("ADMIN")).concat(admin);
                } else {
                    emailTypesIn = emailTypesIn.filter((type) => !type.includes("ADMIN"));
                }

                const emailTypesUserInNormalized = Object.fromEntries(
                    emailTypesIn.map((t) => [t, !!emailTypesUserIn?.[t]])
                );


                this.setState({
                    emailTypes: emailTypesIn,
                    settings: emailTypesUserInNormalized
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("Error getting upset data:");
                console.log(errorThrown);
            },
        });
    };

    handleCheckboxChange = (type) => {
        this.setState(
            prevState => {
                const prevStateSettings = (prevState.settings || {});
                const updatedSettings = {
                    ...prevStateSettings,
                    [type]: !prevStateSettings[type]
                };
                return {settings: updatedSettings};
            },
            () => {
                this.updateUserEmailPreferences(this.state.settings);
            }
        );
    };

    render() {
        return (
            <div className="flex flex-row w-full gap-4 items-center justify-center">
            {
                this.state.emailTypes.map((type) => (

                    <div key={type} className="flex flex-row bg-[var(--c_tag_badge)] rounded-lg items-center p-2 pr-4">

                        {
                            //Admin Type
                            (type.includes("ADMIN") === true)
                            ?
                            <div className="mr-3 ml-2">
                                <div className="flex flex-row gap-2 font-bold items-center">
                                    <i className="fa fa-shield"/>
                                    Admin
                                </div>
                                {type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase()).replace("ADMIN", "")}
                            </div>

                            //Non-Admin Type
                            :
                            <div className="mr-3 ml-2">
                                <div className="flex flex-row gap-2 font-bold items-center">
                                    <i className="fa fa-user"/>
                                    User
                                </div>
                                {type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                            </div>
                        }

                        <input
                            type="checkbox"
                            checked={!!this.state.settings?.[type]}
                            onChange={this.state.disableFetching ? () => {} : () => this.handleCheckboxChange(type)}
                            className="ml-2 !scale-200"
                        />
                    </div>

                ))
            }
            </div>
        );
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

        const prefsByUserSeeded = Object.fromEntries(
            this.props.fleetUsers.map(user => [
                user.id,
                {
                    emailTypesUser: {},
                    emailTypesKeys: []
                }
            ])
        );

        this.state = {
            emailTypes : [],
            prefsByUser : prefsByUserSeeded,
            disableFetching : false
        };

    }

    componentDidMount() {
        this.getUserEmailPreferences();
    }

    updateUserEmailPreferences = (fleetUser, updatedSettings) => {
        const updatedEmailTypeSettingsTarget = updatedSettings[fleetUser.id].emailTypesUser;

        /*
            const submissionData = {
                handleUpdateType : "HANDLE_UPDATE_MANAGER",
                fleetUserID : fleetUser.id,
                fleetID : fleetUser.fleet.id,
                ...updatedEmailTypeSettingsTarget
            };
        */

        this.setState({ disableFetching: true });

        $.ajax({
            type: 'PUT',
            url: `/api/user/${fleetUser.id}/email-prefs`,
            data: updatedEmailTypeSettingsTarget,
            dataType: 'json',
            async: true,
            success: () => {
                this.setState({ disableFetching: false });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log('Error updating preferences:', errorThrown);
                this.setState({ disableFetching: false });
            }
        });

    };

    getUserEmailPreferences = () => {
        const fleetUsers = this.props.fleetUsers;


        fleetUsers.forEach((userTarget) => {
            $.ajax({
            type: "GET",
            url: `/api/user/${userTarget.id}/email-prefs`,
            async: true,
            success: (response) => {

                const EMAIL_TYPE_TAGS_EXCLUDED = [
                    "HIDDEN",
                    "FORCED",
                    "ADMIN"
                ];

                const rawKeys = response.emailTypesKeys.filter((t) =>
                    EMAIL_TYPE_TAGS_EXCLUDED.every((tag) => !t.includes(tag))
                );

                this.setState((prev) => {

                    //Make a global header for all known keys
                    const emailTypes = prev.emailTypes.length
                        ? Array.from(new Set([...prev.emailTypes, ...rawKeys]))
                        : rawKeys;

                    //Ensure every header key exists
                    const normalized = Object.fromEntries(
                        emailTypes.map((t) => [t, !!response.emailTypesUser?.[t]])
                    );

                    return {
                        emailTypes,
                        prefsByUser: {
                            ...prev.prefsByUser,
                            [userTarget.id]: {
                                emailTypesUser: normalized,
                                emailTypesKeys: rawKeys,
                            },
                        },
                    };
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("Error getting upset data:", errorThrown);
            },
            });
        });

        

    };

    handleCheckboxChange = (userTarget, type) => {

        this.setState(
            prevState => {

                const userTargetPrefs = prevState.prefsByUser[userTarget.id];

                //Target user's preferences aren't loaded, exit
                if (!userTargetPrefs || !userTargetPrefs.emailTypesUser)
                    return null;

                const prefsByUser = {
                    ...prevState.prefsByUser,
                    [userTarget.id]: {
                        ...prevState.prefsByUser[userTarget.id],
                        emailTypesUser: {
                            ...prevState.prefsByUser[userTarget.id].emailTypesUser,
                            [type]: !prevState.prefsByUser[userTarget.id].emailTypesUser[type]
                        }
                    }
                };
                return { prefsByUser };
            },
            () => this.updateUserEmailPreferences(userTarget, this.state.prefsByUser)
        );
    };

    refreshFleetUsers = (updatedUsers) => {
        this.setState({fleetUsers: updatedUsers});
    };

    bulkToggleColumn = (emailType, newValue) => {

        console.log("Bulk toggling email type column: ", emailType, newValue);

        const visibleUsers = this.props.fleetUsers
            .filter(u => this.props.showDeniedUsers || u.fleetAccess.accessType !== 'DENIED');

        this.setState(prev => {

            const prefsByUser = { ...prev.prefsByUser };

            visibleUsers.forEach(user => {
                prefsByUser[user.id] = {
                    ...prefsByUser[user.id],
                    emailTypesUser: {
                        ...prefsByUser[user.id].emailTypesUser,
                        [emailType]: newValue
                    }
                };
            });

            return { prefsByUser };

        }, () => {

            visibleUsers.forEach(u => this.updateUserEmailPreferences(u, this.state.prefsByUser));

        });

    };

    render() {

        const showDeniedUsers = this.props.showDeniedUsers;
        const fleetUsers = this.props.fleetUsers.filter(u => showDeniedUsers || u.fleetAccess.accessType !== 'DENIED');
        const prefsByUser = this.state.prefsByUser;

        return (
            <div className="w-full">

                {/* Mass-Toggle Instruction */}
                <InfoHint message={"Use the buttons next to each item to mass-toggle that email notification option for all users."} />

                {/* Email Settings Table */}
                <table className="table-hover rounded-lg w-full">
                    <thead className="leading-16 text-[var(--c_text)] border-b-1">
                        <tr>
                            <th className="pl-4">
                                Email
                            </th>
                            {
                                this.state.emailTypes.map((type, index) => (
                                    <th key={index}>
                                        <div style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                        }}>
                                            <ToggleButtonColumnManager
                                                emailTypes={this.state.emailTypes}
                                                emailTypeIndex={index}
                                                fleetUsers={fleetUsers}
                                                prefsByUser={prefsByUser}
                                                updateUserEmailPreferences={this.updateUserEmailPreferences}
                                                disableFetching={this.state.disableFetching}
                                                bulkToggleColumn={this.bulkToggleColumn}
                                                showDeniedUsers={showDeniedUsers}
                                            />
                                            {type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                                        </div>
                                    </th>
                                ))
                            }
                        </tr>
                    </thead>

                    <tbody className="text-[var(--c_text)] leading-16 before:content-['\A']">

                        {/* Empty spacer row */}
                        <tr className="pointer-none bg-transparent">
                            <td colSpan={3} className="h-6"/>
                        </tr>

                        {
                            fleetUsers
                                .map((userCurrent, settingIndex) => {

                                    const rowClassName = (userCurrent.fleetAccess.accessType==="DENIED" ? `italic opacity-50` : `${settingIndex%2 ? "bg-[var(--c_row_bg)]" : "bg-[var(--c_row_bg_alt)]"}`);
                                    const emailClassName = (userCurrent.fleetAccess.accessType==="DENIED" ? `italic opacity-50` : ``);

                                    return (
                                        <tr key={settingIndex} className={rowClassName}>

                                            {/* Email */}
                                            <td className={`pl-4 ${emailClassName}`}>
                                                {userCurrent.email}
                                            </td>

                                            {/* Email Types */}
                                            {
                                                this.state.emailTypes.map((type, typeIndex) => {
                                                    const buttonChecked = !!(prefsByUser[userCurrent.id]?.emailTypesUser?.[type]);
                                                    const buttonDisabled = this.state.disableFetching || !(type in (prefsByUser[userCurrent.id]?.emailTypesUser ?? {}));
                                                    return (
                                                        <td key={typeIndex}>

                                                            <div className="flex flex-row gap-2 items-center">
                                                                <div className="border-l-1 h-12 border-[var(--c_table_border)]"></div>
                                                                <input
                                                                    type="checkbox"
                                                                    checked={buttonChecked}
                                                                    disabled={buttonDisabled}
                                                                    onChange={this.state.disableFetching ? () => undefined : () => this.handleCheckboxChange(userCurrent, type)}
                                                                    className="ml-4 !scale-200"
                                                                />
                                                            </div>

                                                        </td>
                                                    );
                                                })
                                            }
                                        </tr>
                                    );
                                })
                        }

                        {/* Empty spacer row */}
                        <tr className="pointer-none bg-transparent">
                            <td colSpan={3} className="h-6"/>
                        </tr>

                    </tbody>
                </table>

            </div>
        );

    }

}


class ToggleButtonColumnManager extends React.Component {

    /*
        Button to toggle on/off ALL of the email types in
        a column for every user in the manager table.

        Directly uses and updates the state of the
        EmailSettingsTableManager component.

        Based on binary AND of all users' email types in
        the column.
    */

    constructor(props) {

        super(props);
        this.state = {
            isChecked: false
        };

    }

    componentDidMount() {

        const { emailTypes, emailTypeIndex, prefsByUser } = this.props;
        const emailType = emailTypes[emailTypeIndex];
        const visibleUsers = this.getVisibleUsers();

        //Check if all users have the same preference for this email type
        const allChecked =
            (visibleUsers.length > 0)
            && visibleUsers.every(u => prefsByUser[u.id]?.emailTypesUser[emailType]);

        this.setState({ isChecked: allChecked });

    }

    componentDidUpdate(prevProps) {

        if (prevProps.prefsByUser === this.props.prefsByUser)
            return;

        const emailType = this.props.emailTypes[this.props.emailTypeIndex];
        const visibleUsers = this.getVisibleUsers();
        const allOn =
            (visibleUsers.length > 0)
            && visibleUsers.every(u => this.props.prefsByUser[u.id]?.emailTypesUser[emailType]);

        if (allOn !== this.state.isChecked)
            this.setState({ isChecked: allOn });

    }

    getVisibleUsers = () => {

        return this.props.fleetUsers.filter(u => this.props.showDeniedUsers || u.fleetAccess.accessType !== 'DENIED');

    };

    handleToggle = () => {

        const { emailTypes, emailTypeIndex } = this.props;
        const newCheckedState = !this.state.isChecked;
        const emailType = emailTypes[emailTypeIndex];

        this.setState({ isChecked: newCheckedState });

        this.props.bulkToggleColumn(emailType, newCheckedState);

        const visibleUsers = this.getVisibleUsers();
        visibleUsers.forEach(user => {

            const updatedSettings = {
                [user.id]: {
                    emailTypesUser: {
                        ...this.props.prefsByUser[user.id].emailTypesUser,
                        [emailTypes[emailTypeIndex]]: newCheckedState
                    }
                }
            };

            this.props.updateUserEmailPreferences(user, updatedSettings);

        });

    };

    render() {

        const { disableFetching } = this.props;
        const { isChecked } = this.state;

        return (

            <button
                className="
                    ml-4 mr-2
                    scale-100 hover:scale-110 transition-transform duration-100
                    cursor-pointer
                "
                onClick={disableFetching ? () => undefined : this.handleToggle}
            >
                <div className="w-8 h-8 flex items-center justify-center rounded-lg bg-[var(--c_tag_badge)]">
                <i className={`fa fa-${isChecked ? 'times' : 'check'} text-[var(--c_text)]`}/>
                </div>
            </button>
        );
    }

}