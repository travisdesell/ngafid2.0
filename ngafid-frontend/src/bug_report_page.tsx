import "bootstrap";

import {errorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import React from 'react'
import ReactDOM from 'react-dom/client'


import './index.css'          //<-- include Tailwind


export default class BugReportPage extends React.Component {

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
        
            console.log("Submitting bug report...");

            //Fetch the bug report data
            const title = (document.getElementById("bug-title") as HTMLInputElement)
                .value
                .trim();
            const description = (document.getElementById("bug-description") as HTMLTextAreaElement)
                .value
                .trim();
            const includeEmail = (document.getElementById("bug-email") as HTMLInputElement)
                .checked;

            //TEST: Sleep for 2 seconds [EX]
            await new Promise(resolve => setTimeout(resolve, 2000));

            //No description provided, exit
            if (!description) {
                errorModal.show(
                    "Error Submitting Bug Report",
                    "Please provide a description of the bug.",
                );
                return;
            }

            //Append the email to the description
            const EX_PLACEHOLDER_EMAIL = "placeholderemail@xyz.com";
            const body = includeEmail
                ? `${description}\n\nEmail: ${EX_PLACEHOLDER_EMAIL ?? "Unknown"}`
                : description;

            const SUBMIT_BUG_REPORT_URL = `/protected/submit_bug_report`;
            const submissionData = {
                title: title,
                body: body
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

                    //...
    
                    console.log("Bug report submitted successfully!");
    
                },
                error : function(jqXHR, textStatus, errorThrown) {

                    console.warn("Error submitting bug report.");

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
                            <input type="text" className="form-control" id="bug-title" placeholder="Bug Title (Optional)"/>
                        </div>

                        {/* Text Input Area -- Bug Description */}
                        <div className="form-group">
                            <textarea className="form-control" id="bug-description" rows={5} placeholder="Bug Description (Required)"/>
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
                                        Sign Report With Email
                                    </div>

                                    <input
                                        id="bug-email"
                                        type="checkbox"
                                        className="ml-2 !scale-200"
                                    />

                                </div>

                                {/* Descriptive List */}
                                <ul className="list-disc p-0 m-0 text-left italic my-auto">
                                    <li>Try to provide a brief title and detailed description</li>
                                    <li>Use the checkbox to include your email in the report (will be publically visible)</li>
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


const root = ReactDOM.createRoot(
    document.getElementById("bug-report-page") as HTMLElement
);
root.render(<BugReportPage/>);