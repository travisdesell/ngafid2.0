import 'bootstrap';
import React, { useEffect, useState } from "react";
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

const EmailSettingsTableUser = ({ isAdmin }) => {
 
    //Update user email preferences
    function updateUserEmailPreferences(updatedSettings) {

        var submissionData = {
            handleUpdateType : "HANDLE_UPDATE_USER",
            ...updatedSettings
        }

        $.ajax({
            type: 'POST',
            url: '/protected/update_email_preferences',
            data: submissionData,
            dataType: 'json',
            async: false,

            success: function(response) {
                // console.log('Email preferences updated successfully!', response);
                console.log('Email preferences updated successfully!');
            },

            error: function(jqXHR, textStatus, errorThrown) {
                console.log('Error updating email preferences:', errorThrown);
            }
        
        });

    }

    //Fetch user email preferences
    function getUserEmailPreferences() {

        let resultsOut = "No results found.";

        let submissionData = {
            handleFetchType : "HANDLE_FETCH_USER"
        };

        $.ajax({
            type: 'GET',
            url: '/protected/email_preferences',
            data: submissionData,
            dataType: 'json',
            async: false,

            success : function(response) {
                console.log("got user pref response");
                 console.log(response);
                resultsOut = response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log("Error getting upset data:");
                console.log(errorThrown);
            },

        });
            
        return resultsOut;

    }

    let resultsIn = getUserEmailPreferences();
    let emailTypes = resultsIn.emailTypesKeys;

    //Filter out email types marked as HIDDEN or FORCED
    emailTypes = emailTypes.filter(
        type => (
            (type.includes("HIDDEN") !== true)
            && (type.includes("FORCED") !== true)
        )
    );

    //For admins...
    if (isAdmin) {

        //...sort the ADMIN email types to the end of the list
        let emailTypesAdmin = emailTypes.filter(
            type => (type.includes("ADMIN") === true)
        );

        emailTypes = emailTypes.filter(
            type => (type.includes("ADMIN") !== true)
        );

        emailTypes = emailTypes.concat(emailTypesAdmin);

    }

    //For non-admins
    else {

        //...filter out the ADMIN email types
        emailTypes = emailTypes.filter(
            type => (type.includes("ADMIN") !== true)
        );

    }


    const [settings, setSettings] = useState(resultsIn.emailTypesUser);
    

    const handleCheckboxChange = (type) => {

        setSettings(prevSettings => {

            //Map over the previous settings and make a new array
            const updatedSettings = {
                ...prevSettings,
                [type]: !prevSettings[type]
            };

            //Deliver updated preferences
            updateUserEmailPreferences(updatedSettings);

            return updatedSettings;

        });

    };

    return (
        <table style={{
            minWidth: "85%",
            maxWidth: "85%",
            paddingLeft: "15%",
            textAlign: "center",
            borderCollapse: "separate",
            borderSpacing: "16px 16px",
        }}>

            <thead>
                <tr>
                {
                    emailTypes.map((type, index) => (
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
                    emailTypes.map((type, typeIndex) => (
                        <td key={typeIndex} style={{ textAlign: "center" }}>
                            <input
                                type="checkbox"
                                checked={settings[type] || false}
                                onChange={() => handleCheckboxChange(type)}
                                style={{ scale: "2.00" }}
                            />
                        </td>
                    ))
                }
                </tr>
            </tbody>

        </table>
    );

};




/*
----------------------------------------
            MANAGER SETTINGS            
----------------------------------------
*/

const ToggleButtonColumnManager = ({updateUserEmailPreferences, setSettings, usersList, emailTypes, emailTypeIndex}) => {

    //Set default state
    let doClear = true;
    let emailTypeTarget = emailTypes[emailTypeIndex];

    for(let i = 0 ; i < usersList.length ; i++) {

        let userCurrent = usersList[i];
        let emailDataList = userCurrent.emailTypesUser;

        //Found unchecked, iterate count
        if (emailDataList[emailTypeTarget] !== true) {
            doClear = false;
            break;
        }

    }

    //Ensure the checkboxes reflect their new states
    let applyToggle = () => {

        const updatedSettings = usersList.map(
            user =>
            ({
                ...user,
                emailTypesUser: {
                    ...user.emailTypesUser,
                    [emailTypeTarget]: !doClear
                    }
            })
        );

        setSettings(updatedSettings);

        updatedSettings.forEach(user => {
            updateUserEmailPreferences(user, updatedSettings);
        });

    };

    //Clearing --> Render Unchecking Box
    if (doClear) {
        return (
            <div>
                <button onClick={applyToggle} style={{borderRadius:"4px", width:"28px", height:"28px", padding:"0"}}>
                    <i  className="fa fa-square-o fa-lg"></i>
                </button>
            </div>
        );
    }

    //Not Clearing --> Render Checking Box
    return (
        <div>
            <button onClick={applyToggle} style={{borderRadius:"4px", width:"28px", height:"28px", padding:"0"}}>
                <i style={{color:"#2e7ce2"}} className="fa fa-check-square fa-lg"></i>
            </button>
        </div>
    );

}


const EmailSettingsTableManager = ({ fleetUsers }) => {

    //No users in the fleet, return empty
    if (fleetUsers.length == 0) {
        return ( <div></div> );
    }

    //Update user email preferences
    function updateUserEmailPreferences(fleetUser, updatedSettings) {

        let updatedEmailTypeSettingsTarget = updatedSettings.find(setting => setting.userId === fleetUser.userId).emailTypesUser;

        var submissionData = {
            handleUpdateType : "HANDLE_UPDATE_MANAGER",
            fleetUserID : fleetUser.userId,
            fleetID : fleetUser.fleetID,
            ...updatedEmailTypeSettingsTarget
        }

        $.ajax({
            type: 'POST',
            url: '/protected/update_email_preferences',
            data: submissionData,
            dataType: 'json',
            async: false,

            success: function(response) {
                // console.log('Preferences updated successfully!', response);
                console.log('Preferences updated successfully!');
            },
            error: function(jqXHR, textStatus, errorThrown) {
                console.log('Error updating preferences:', errorThrown);
            }

        });
    
    }

    //Fetch user email preferences
    function getUserEmailPreferences(userTarget) {

        let resultsOut = "No results found.";

        let submissionData = {
            handleFetchType : "HANDLE_FETCH_MANAGER",
            fleetUserID : userTarget.id
        };

        $.ajax({
            type: 'GET',
            url: '/protected/email_preferences',
            data: submissionData,
            dataType : 'json',
            async: false,

            success : function(response) {
                console.log("got user pref response");
                // console.log(response);
                resultsOut = response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log("Error getting upset data:");
                console.log(errorThrown);
            },

        });
            
        return resultsOut;

    }

    //For each user in the fleet, get their email preferences
    let fleetUsersEmailSettings = [];
    for (let i = 0; i < fleetUsers.length; i++) {

        let userCurrent = fleetUsers[i];

        console.log("User Current: ", userCurrent);

        let userFleetID = userCurrent.fleetAccess.fleetId;
        let userSettings = getUserEmailPreferences(userCurrent);
        let userEmail = userCurrent.email;

        console.log("User Settings: ", userSettings);

        userSettings.email = userEmail;
        userSettings.fleetID = userFleetID;

        //Filter out email types marked as HIDDEN or FORCED
        let emailTypesKeys = Object.keys(userSettings.emailTypesUser).filter(
            type => (
                (type.includes("HIDDEN") !== true)
                && (type.includes("FORCED") !== true)
            )
        );

        //For admins...
        if (userCurrent.isAdmin) {

            /*

            ----------------------------------------------------------
            Management for Admin email types will be disabled for now.
            ----------------------------------------------------------

            //...sort the ADMIN email types to the end of the list
            let emailTypesKeysAdmin = emailTypesKeys.filter(
                type => (type.includes("ADMIN") === true)
            );

            emailTypesKeys = emailTypesKeys.filter(
                type => (type.includes("ADMIN") !== true)
            );

            emailTypesKeys = emailTypesKeys.concat(emailTypesKeysAdmin);

            */

        }

        //For non-admins
        else {

            //...filter out the ADMIN email types
            emailTypesKeys = emailTypesKeys.filter(
                type => (type.includes("ADMIN") !== true)
            );

        }

        userSettings.emailTypesKeys = emailTypesKeys;
        
        fleetUsersEmailSettings.push(userSettings);

    }

    //Get the email type keys
    var emailTypesSet = {};
    for(let i = 0 ; i < fleetUsersEmailSettings.length ; i++) {

        let userCurrent = fleetUsersEmailSettings[i];
        let emailTypesKeys = userCurrent.emailTypesKeys;

        for(let j = 0 ; j < emailTypesKeys.length ; j++) {
            let emailType = emailTypesKeys[j];
            emailTypesSet[emailType] = true;
        }

    }
    const emailTypes = Object.keys(emailTypesSet);


    const [usersList, setUserSettings] = useState(fleetUsersEmailSettings);

    
    const handleCheckboxChange = (userTarget, type) => {

        setUserSettings(prevSettings => {

            const updatedSettings = prevSettings.map(
                setting =>
                (setting.userId === userTarget.userId)
                ? {
                    ...setting,
                    emailTypesUser: {
                        ...setting.emailTypesUser,
                        [type]: !setting.emailTypesUser[type]
                    }
                }
                : setting
            );

            let userSettingsTarget = updatedSettings.find(setting => setting.userId === userTarget.userId).emailTypesUser;

            updateUserEmailPreferences(userTarget, updatedSettings);

            return updatedSettings;

        });

    };

    return (
        <table style={{
        width:"100%",
        borderCollapse: "collapse",
        }}>

            <thead>
                <tr>
                    <th style={{padding:"0 12px"}}>Email</th>
                    {
                        emailTypes.map((type, index) => (
                            <th key={index}>
                                <div style={{display: 'flex', alignItems: 'center', padding:"20px 0px", margin:"-7px", gap:"8px", width:"95%"}}>
                                    <ToggleButtonColumnManager updateUserEmailPreferences={updateUserEmailPreferences} setSettings={setUserSettings} usersList={usersList} emailTypes={emailTypes} emailTypeIndex={index}></ToggleButtonColumnManager>
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
            <tr style={{border:"solid", borderColor:"#00004411", borderWidth: "2px 0px"}}>
            </tr>
            {
                usersList.map((userCurrent, settingIndex) => (
                    <tr key={settingIndex} style={{border:"solid", borderColor:"#00004411", borderWidth: "1px 0px", backgroundColor: (settingIndex%2 === 0) ? '#FFFFFF00' : '#00000009'}}>
                        <td style={{padding:"16px 12px"}}>{userCurrent.email}</td>
                        {
                            emailTypes.map((type, typeIndex) => (
                                <td key={typeIndex}>
                                    <input
                                        type="checkbox"
                                        checked={userCurrent.emailTypesUser[type] || false}
                                        onChange={() => handleCheckboxChange(userCurrent, type)}
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

};