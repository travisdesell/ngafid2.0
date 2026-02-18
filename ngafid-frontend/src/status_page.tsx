import "bootstrap";

import { showErrorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import React from 'react';
import { createRoot } from 'react-dom/client';


import './index.css'; //<-- include Tailwind


/*
    Define Status types & enums
*/
enum StatusIcon {
    UNKNOWN = "fa-question",
    OK = "fa-check",
    WARNING = "fa-warning",
    ERROR = "fa-exclamation-circle",
    UNCHECKED = "fa-forward",
}

enum StatusName {
    UNKNOWN = "UNKNOWN",
    OK = "OK",
    WARNING = "WARNING",
    ERROR = "ERROR",
    UNCHECKED = "UNCHECKED",
}

const STATUS_TUPLE_INDEX_NAME = 0;
const STATUS_TUPLE_INDEX_ICON = 1;
const STATUS_TUPLES = [
    [StatusName.UNKNOWN, StatusIcon.UNKNOWN],
    [StatusName.OK, StatusIcon.OK],
    [StatusName.WARNING, StatusIcon.WARNING],
    [StatusName.ERROR, StatusIcon.ERROR],
    [StatusName.UNCHECKED, StatusIcon.UNCHECKED],
] as const;

type Status = {
    name: StatusName;
    icon: StatusIcon;
}

const STATUS_DEFAULT = StatusName.UNKNOWN;

type StatusEntry = {
    name: string;
    nameDisplay: string;
    status: Status;
    message: string;
}
const STATUS_DEFAULT_MESSAGE = "No message available...";

const DOCKER_SEPARATOR_ENTRY = "DOCKER_SEPARATOR_ENTRY";
const STATUS_NAMES_LIST = [
    "flight-processing",
    "event-processing",
    "kafka",
    "chart-service",
    "database",

    DOCKER_SEPARATOR_ENTRY,
    "ngafid-email-consumer",
    "ngafid-event-consumer",
    "ngafid-event-observer",
    "ngafid-upload-consumer",
];


const STATUS_ENTRIES = STATUS_NAMES_LIST.map((name) => {

    const formatName = (name: string) => {

        const nameParts = name.split("-");
        const formattedName = nameParts.map((part) => {
            return part.charAt(0).toUpperCase() + part.slice(1);
        }).join(" ");

        return formattedName;

    };

    return {
        name: name,
        nameDisplay: formatName(name),
        status: {name: STATUS_DEFAULT, icon: StatusIcon.UNKNOWN},
        message: STATUS_DEFAULT_MESSAGE
    } as StatusEntry;

});


export default class StatusPage extends React.Component {

    constructor(props: object) {
        super(props);
    }


    //Fetch status entries from the server
    async componentDidMount(): Promise<void> {

        console.log("Status Page mounted, fetching statuses...");

        const STATUS_FAILURE_NAMES: string[] = [];

        const statusEntryRequests = STATUS_ENTRIES.map((entry) => {

            //Skip the DOCKER_SEPARATOR_ENTRY
            if (entry.name === DOCKER_SEPARATOR_ENTRY)
                return Promise.resolve();

            const stautsURL = `/api/status/${encodeURIComponent(entry.name)}`;

            return new Promise<void>((resolve, reject) => {

                $.ajax({
                    type: "GET",
                    url: stautsURL,
                    dataType: "json",
                    contentType: "application/json",
                    async: true,
                    success: (response) => {

                        //Update Status Name
                        entry.status.name = (response.status ?? StatusName.UNKNOWN);

                        //Update Status Icon
                        const statusTuple = STATUS_TUPLES.find((tuple) => (tuple[STATUS_TUPLE_INDEX_NAME] === entry.status.name));
                        if (statusTuple) {
                            entry.status.icon = statusTuple[STATUS_TUPLE_INDEX_ICON];
                        } else {
                            console.warn(`Status ${entry.status.name} not found in STATUS_TUPLES`);
                            entry.status.icon = StatusIcon.UNKNOWN;
                        }

                        //Update Status Message
                        entry.message = (response.message ?? STATUS_DEFAULT_MESSAGE);

                        console.log(`Status(${entry.name}) updated: ${entry.status.name} - ${entry.message}`);

                        resolve();

                    },
                    error: function (jqXHR, textStatus, errorThrown) {

                        STATUS_FAILURE_NAMES.push(entry.name);

                        console.error(`jqXHR: ${jqXHR}`);
                        console.error(`textStatus: ${textStatus}`);
                        console.error(`errorThrown: ${errorThrown}`);

                        console.warn("...End of preceding error log");

                        reject(errorThrown);

                    },

                });

            });

        });

        console.log("Status Page mounted, all status requests sent");

        //Wait for every status entry request to finish
        await Promise.allSettled(statusEntryRequests);

        console.log("Status Page mounted, all status requests finished");

        //Got status failures, show error modal with names
        if (STATUS_FAILURE_NAMES.length > 0) {

            const errorMessage = `Failed to fetch ${(STATUS_FAILURE_NAMES.length > 1) ? "statuses" : "status"} for: ${STATUS_FAILURE_NAMES.join(", ")}`;

            const errorModalTitle = (STATUS_FAILURE_NAMES.length > 1) ? "Error Fetching Statuses" : "Error Fetching Status";
            showErrorModal(errorModalTitle, errorMessage);
        }

        //Trigger a re-render
        this.forceUpdate();

    }


    render() {

        const jsxOut = <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

            {/* Navbar */}
            <div style={{flex: "0 0 auto"}}>

                <SignedInNavbar
                    activePage="status"
                    waitingUserCount={waitingUserCount}
                    fleetManager={fleetManager}
                    unconfirmedTailsCount={unconfirmedTailsCount}
                    modifyTailsAccess={modifyTailsAccess}
                    plotMapHidden={plotMapHidden}
                />

            </div>

            {/* Main Content */}
            <div style={{overflowY: "auto", flex: "1 1 auto"}}>

                <div className="card flex flex-col m-16 my-4">

                    {/* Header */}
                    <div className="text-2xl card-header">
                        Status Page
                    </div>

                    {/* Display Status Entries */}
                    <div className="card-body text-center text-sm">
                        <table className="table-hover table-fixed rounded-lg w-full">

                            <colgroup>
                                <col style={{width: "20%"}}/>
                                <col style={{width: "20%"}}/>
                                <col style={{width: "60%"}}/>
                            </colgroup>

                            <thead className="leading-16 text-[var(--c_text)] border-b-1">
                            <tr>
                                <th>Name</th>
                                <th>Status</th>
                                <th>Message</th>
                            </tr>
                            </thead>


                            <tbody className="text-[var(--c_text)] leading-8 before:content-['\A']">

                                {/* Empty spacer row */}
                                <tr className="pointer-none bg-transparent">
                                    <td colSpan={3} className="h-6"/>
                                </tr>

                                {STATUS_ENTRIES.map((entry, index) => {
                                    return (

                                        //Got DOCKER_SEPARATOR_ENTRY, render a separator row
                                        (entry.name === DOCKER_SEPARATOR_ENTRY)
                                        ? (
                                            <tr key={entry.name} className="bg-[var(--c_row_bg)] text-[var(--c_text_alt)] pointer-events-none underline">
                                                <td className="pt-2 font-bold mr-auto">
                                                    <i className="fa fa-archive mr-2"/>
                                                    Docker Services
                                                </td>
                                                {/* Empty Cells */}
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                            </tr>
                                        )

                                        //Otherwise, render a status entry row
                                        : (
                                            <tr key={entry.name}
                                                className={`${index % 2 ? "bg-[var(--c_row_bg)]" : "bg-[var(--c_row_bg_alt)]"} text-[var(--c_text_alt)]`}
                                            >

                                                <td className="truncate whitespace-nowrap overflow-hidden">

                                                    {/* Status Entry Name (Strip leading 'ngafid') */}
                                                    {entry.nameDisplay.replace(/^ngafid/i, "")}
                                                </td>

                                                <td className="font-mono truncate whitespace-nowrap overflow-hidden">

                                                    {/* Status Icon */}
                                                    <i className={`mr-2 scale-100 fa ${entry.status.icon}`}/>

                                                    {/* Status Name */}
                                                    {entry.status.name}

                                                </td>

                                                <td className={`${entry.message == STATUS_DEFAULT_MESSAGE ? "italic opacity-50" : ""}`}>
                                                    {entry.message}
                                                </td>

                                            </tr>
                                        )
                                    );
                                })}

                            </tbody>
                        </table>
                    </div>

                </div>

            </div>

        </div>;

        return jsxOut;

    }

}


const root = createRoot(
    document.getElementById("status-page") as HTMLElement
);
root.render(<StatusPage/>);