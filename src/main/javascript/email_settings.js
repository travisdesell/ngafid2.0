import 'bootstrap';
import React, { useState, useEffect } from 'react';
import ReactDOM from "react-dom";


import $ from 'jquery';
import { redraw } from 'plotly.js';
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
        $.ajax({
            type: 'GET',
            url: '/protected/email_preferences/HANDLE_FETCH_USER',
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
            
                if (this.props.isAdmin) {   //For admins...
            
                    //...sort the ADMIN email types to the end of the list
                    let emailTypesAdmin = emailTypesIn.filter(
                        type => (type.includes("ADMIN") === true)
                    );
            
                    emailTypesIn = emailTypesIn.filter(
                        type => (type.includes("ADMIN") !== true)
                    );
            
                    emailTypesIn = emailTypesIn.concat(emailTypesAdmin);
            
                } else {    //For non-admins
            
                    //...filter out the ADMIN email types
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

                //Map over the previous settings and make a new array
                let prevStateSettings = (prevState.settings || []);
                const updatedSettings = {
                    ...prevStateSettings,
                    [type]: !prevStateSettings[type]
                };

                return { settings: updatedSettings };

            },
            () => {
                //Deliver updated preferences
                this.updateUserEmailPreferences(this.state.settings);
            }

        );

    };

    render() { 

        return (
            <table style={{
                minWidth: "85%",
                maxWidth: "85%",
                paddingLeft: "15%",
                textAlign: "center",
                borderCollapse: "separate",
                borderSpacing: "16px 16px",
                color: "var(--c_text)"
            }}>
                <thead>
                    <tr>
                    {
                        this.state.emailTypes.map((type, index) => (
                            <th key={index} style={{textAlign:"center"}}>
                            {
                                type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
                            }
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
                                    checked={
                                        (this.state.settings && this.state.settings[type])
                                        ? this.state.settings[type]
                                        : false
                                    }
                                    onChange={
                                        this.state.disableFetching
                                        ? () => undefined
                                        : () => this.handleCheckboxChange(type)
                                    }
                                    style={{ scale: "2.00" }}
                                />
                            </td>
                        ))
                    }
                    </tr>
                </tbody>

            </table>
        );

    }

};




/*
----------------------------------------
            MANAGER SETTINGS            
----------------------------------------
*/

const ToggleButtonColumnManager = ({disableFetching, updateUserEmailPreferences, fleetUsers, refreshFleetUsers, emailTypes, emailTypeIndex}) => {

    //Set default state
    const [doClear, setDoClear] = useState(true);
    const emailTypeTarget = emailTypes[emailTypeIndex];

    useEffect(() => {

        let clear = true;
        for(let i = 0 ; i < fleetUsers.length ; i++) {

            let userCurrent = fleetUsers[i];
            let userEmailPreferenceCurrent = userCurrent.emailTypesUser;

            //Preferences aren't defined, continue
            if (!userEmailPreferenceCurrent) {
                continue;
            }

            //Found unchecked, iterate count
            if (userEmailPreferenceCurrent[emailTypeTarget] !== true) {
                clear = false;
                break;
            }

        }

        setDoClear( clear );

    }, [fleetUsers, emailTypeTarget, emailTypeIndex]);

    //Ensure the checkboxes reflect their new states
    const applyToggle = () => {

        const updatedUsers = fleetUsers.map(user => {

            return {
                ...user,
                emailTypesUser: {
                    ...user.emailTypesUser,
                    [emailTypeTarget]: !doClear
                }
            };

        });

        refreshFleetUsers(updatedUsers);

        updatedUsers.forEach(user => {
            updateUserEmailPreferences(user, updatedUsers);
        });

    };

    //Clearing --> Render Unchecking Box
    if (doClear) {
        return (
            <div>
                <button onClick={disableFetching ? ()=>undefined : applyToggle} style={{borderRadius:"4px", width:"28px", height:"28px", padding:"0"}}>
                    <i  className="fa fa-square-o fa-lg"></i>
                </button>
            </div>
        );
    }

    //Not Clearing --> Render Checking Box
    return (
        <div>
            <button onClick={disableFetching ? ()=> undefined : applyToggle} style={{borderRadius:"4px", width:"28px", height:"28px", padding:"0"}}>
                <i style={{color:"#2e7ce2"}} className="fa fa-check-square fa-lg"></i>
            </button>
        </div>
    );

}


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

    //Update user email preferences
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

            success: (response) => {
                console.log('Preferences updated successfully!'/*, response*/);
                this.setState({ disableFetching: false });  
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log('Error updating preferences:', errorThrown);
                this.setState({ disableFetching: false });  
            }

        });
    
    }

    //Fetch user email preferences
    getUserEmailPreferences = () => {

        let fleetUsers = this.state.fleetUsers;
        let fleetUsersEmailSettings = [];

        fleetUsers.forEach(userTarget => {
            $.ajax({
                type: 'GET',
                url: '/protected/email_preferences/HANDLE_FETCH_MANAGER/' + userTarget.id + '/' + userTarget.fleet.id,
                async: true,

                success : (response) => {

                    console.log("got user pref response");

                    let emailTypesUserIn = response.emailTypesUser;
                    let emailTypesKeysIn = response.emailTypesKeys;

                    //Filter out email types marked as HIDDEN or FORCED
                    emailTypesKeysIn = emailTypesKeysIn.filter(
                        type => (
                            (type.includes("HIDDEN") !== true)
                            && (type.includes("FORCED") !== true)
                        )
                    );

                    if (userTarget.isAdmin) {   //For admins...

                        /*

                        ----------------------------------------------------------
                        Management for Admin email types will be disabled for now.
                        ----------------------------------------------------------

                        //...sort the ADMIN email types to the end of the list
                        let emailTypesKeysAdmin = emailTypesKeys.filter(
                            type => (type.includes("ADMIN") === true)
                        );

                        emailTypesIn = emailTypesIn.filter(
                            type => (type.includes("ADMIN") !== true)
                        );

                        emailTypesIn = emailTypesIn.concat(emailTypesKeysAdmin);

                        */

                    } else {    //For non-admins

                        //...filter out the ADMIN email types
                        emailTypesKeysIn = emailTypesKeysIn.filter(
                            type => (type.includes("ADMIN") !== true)
                        );

                    }

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

                    fleetUsersEmailSettings.push({
                        id: userTarget.id,
                        emailTypesUser: emailTypesUserIn,
                        emailTypesKeys: emailTypesKeysIn
                    });

                },
                error: (jqXHR, textStatus, errorThrown) => {
                    console.log("Error getting upset data:");
                    console.log(errorThrown);
                },

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
            () => {
                //Deliver updated preferences
                this.updateUserEmailPreferences(userTarget, this.state.fleetUsers);
            }
        
        );

    };

    refreshFleetUsers = (updatedUsers) => {
        this.setState({ fleetUsers: updatedUsers });
    };

    render() {

        return (
            <table style={{
            width:"100%",
            borderCollapse: "collapse",
            color:"var(--c_text)"
            }}>
                <thead>
                    <tr>
                        <th style={{padding:"0 12px"}}>Email</th>
                        {
                            this.state.emailTypes.map((type, index) => (
                                <th key={index}>
                                    <div style={{display: 'flex', alignItems: 'center', padding:"20px 0px", margin:"-7px", gap:"8px", width:"95%"}}>
                                        <ToggleButtonColumnManager
                                            disableFetching={this.state.disableFetching}
                                            updateUserEmailPreferences={this.updateUserEmailPreferences}
                                            fleetUsers={this.state.fleetUsers}
                                            refreshFleetUsers = {this.refreshFleetUsers}
                                            emailTypes={this.state.emailTypes}
                                            emailTypeIndex={index}
                                        />
                                        {
                                            type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
                                        }
                                    </div>
                                </th>
                            ))
                        }
                    </tr>
                </thead>

                <tbody>
                <tr style={{border:"solid", borderColor:"var(--c_table_border)", borderWidth: "2px 0px"}}/>
                {
                    this.state.fleetUsers.map((userCurrent, settingIndex) => (
                        <tr key={settingIndex} style={{border:"solid", borderColor:"var(--c_table_border)", borderWidth: "1px 0px"}} className={(settingIndex%2 === 0) ? "row-bg-solid-B" : "row-bg-solid-A"}>
                            <td style={{padding:"16px 12px"}}>{userCurrent.email}</td>
                            {
                                this.state.emailTypes.map((type, typeIndex) => (
                                    <td key={typeIndex}>
                                        <input
                                            type="checkbox"
                                            checked={
                                                (userCurrent.emailTypesUser && userCurrent.emailTypesUser[type])
                                                ? userCurrent.emailTypesUser[type]
                                                : false
                                            }
                                            onChange={
                                                this.state.disableFetching
                                                ? () => undefined
                                                : () => this.handleCheckboxChange(userCurrent, type)
                                            }
                                        />
                                    </td>
                                ))
                            }
                        </tr>
                    ))
                }
                </tbody>

            </table>
        );
    }

};