import "bootstrap";

import {errorModal} from "./error_modal.js";
import {confirmModal} from "./confirm_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import React from 'react'
import ReactDOM from 'react-dom/client'


import './index.css' //<-- include Tailwind
import {NGAFIDUser} from "./types.js";


interface BugReportPageProps {
    user: NGAFIDUser;
}


export default class BugReportPage extends React.Component<BugReportPageProps> {

    constructor(props: any) {

        super(props);

    }

    async componentDidMount(): Promise<void> {

        console.log("Bug Report Page mounted...");

    }

    async submitBugReport(): Promise<void> {

        $('#loading').show();
        console.log("Showing loading spinner!");

        const submit = async () => {

            //Get current time
            const sendStart = new Date();
            console.log("Submitting bug report... (" + sendStart.toLocaleString() + ")");

            //Fetch the bug report data
            const title = (document.getElementById("bug-title") as HTMLInputElement)
                .value
                .trim();
            const description = (document.getElementById("bug-description") as HTMLTextAreaElement)
                .value
                .trim();
            const includeEmail = (document.getElementById("bug-email") as HTMLInputElement)
                .checked;

            //No title / description provided, exit
            if (!title || !description) {

                errorModal.show(
                    "Error Submitting Bug Report",
                    "Please ensure the title and description fields are filled out.",
                );

                return;

            }

            const BUG_REPORT_EMAIL_UNKNOWN = "(Unknown Email!)";
            const body = description;
            const senderEmail = user.email ?? BUG_REPORT_EMAIL_UNKNOWN;

            const SUBMIT_BUG_REPORT_URL = `/api/bug`;
            const submissionData = {
                title,
                body,
                senderEmail,
                includeEmail,
            };

            $.ajax({
                type: "POST",
                url: SUBMIT_BUG_REPORT_URL,
                data: JSON.stringify(submissionData),
                dataType: "json",
                contentType: "application/json",
                processData: false,
                async: true,
                success: (response) => {

                    const sendEnd = new Date();
                    console.log("Bug report submitted successfully! (" + sendEnd.toLocaleString() + ")");

                    confirmModal.show(
                        "Bug Report Submitted",
                        "Your bug report has been submitted successfully. Thank you for your feedback!",
                    );

                },
                error: function (jqXHR, textStatus, errorThrown) {

                    const sendEnd = new Date();
                    console.warn("Error submitting bug report. (" + sendEnd.toLocaleString() + ")");

                    console.error(`jqXHR: ${jqXHR}`);
                    console.error(`textStatus: ${textStatus}`);
                    console.error(`errorThrown: ${errorThrown}`);

                    errorModal.show(
                        "Error Submitting Bug Report",
                        errorThrown,
                    );

                },

            });


        }

        //Submit the bug report
        await submit();

        //Sleep for X seconds
        const BUG_REPORT_SUBMIT_DELAY_MS = 2_000;
        await new Promise(resolve => setTimeout(resolve, BUG_REPORT_SUBMIT_DELAY_MS));

        //Hide the loading spinner
        $('#loading').hide();

    }

    render() {

        const jsxOut = <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

            {/* Navbar */}
            <div style={{flex: "0 0 auto"}}>

                <SignedInNavbar
                    activePage="bug-report"
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
                        Bug Report Page
                    </div>

                    {/* Display Status Entries */}
                    <div className="card-body text-center text-sm">

                        {/* Text Input Area -- Bug Title */}
                        <div className="form-group">
                            <input type="text" className="form-control" id="bug-title"
                                   placeholder="Bug Title (Required)"/>
                        </div>

                        {/* Text Input Area -- Bug Description */}
                        <div className="form-group">
                            <textarea className="form-control" id="bug-description" rows={5}
                                      placeholder="Bug Description (Required)"/>
                        </div>

                        {/* Bottom Row */}
                        <div className="flex flex-row items-center gap-4 justify-between">

                            {/* Bottom Row -- Left Elements */}
                            <div className="flex flex-row gap-8">

                                {/* Checkbox -- Sign Report with Email */}
                                <div className="flex flex-row bg-[var(--c_tag_badge)] rounded-lg items-center p-2 pr-4">

                                    <div className="mr-3 ml-2">
                                        <div className="flex flex-row gap-2 font-bold items-center">
                                            <i className="fa fa-envelope"/>
                                            User
                                        </div>
                                        Send BCC With Report
                                    </div>

                                    <input
                                        defaultChecked
                                        id="bug-email"
                                        type="checkbox"
                                        className="ml-2 !scale-200"
                                    />

                                </div>

                                {/* Descriptive List */}
                                <ul className="list-disc p-0 m-0 text-left italic my-auto">
                                    <li>Try to provide a brief title and detailed description</li>
                                    <li>Use the checkbox to BCC your email in the report</li>
                                    <li>Wait up to 1 minute to confirm the report was submitted</li>
                                </ul>

                            </div>


                            {/* Submit Button */}
                            <div className="form-group my-auto">

                                <button
                                    type="button"
                                    className="btn btn-primary"
                                    onClick={() => this.submitBugReport()}
                                >
                                    Submit Bug Report
                                </button>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

        </div>

        return jsxOut;

    }

}


declare const user: NGAFIDUser; /* SERVER-INJECTED ⚠ */

const root = ReactDOM.createRoot(
    document.getElementById("bug-report-page") as HTMLElement
);
root.render(<BugReportPage user={user}/>);  