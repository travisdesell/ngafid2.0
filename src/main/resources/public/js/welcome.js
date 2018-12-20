class WelcomeCard extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const hidden = this.props.hidden;
        const bgStyle = {opacity : 0.8};
        const fgStyle = {opacity : 1.0};

        return (
            <div className="card-body" hidden={hidden}>
                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header" style={fgStyle}>
                        Welcome to the National General Aviation Flight
                        Information Database (NGAFID)
                    </h5>

                    <div className="card-body" style={fgStyle}>
                        <p className="card-text">
                            The NGAFID is part of Aviation Safety Information
                            Analysis and Sharing (ASIAS), a Federal Aviation
                            Administration (FAA) funded, joint
                            government-industry, collaborative information
                            sharing program to proactively analyze broad and
                            extensive data sources towards the advancement of
                            safety initiatives and the discovery of
                            vulnerabilities in the National Airspace System
                            (NAS). The primary objective of ASIAS is to provide a
                            national resource for use in discovering common,
                            systemic safety problems that span multiple
                            operators, fleets, and regions of the airspace.
                            Safety information discovered through ASIAS
                            activities is used across the aviation industry to
                            drive improvements and support a variety of safety
                            initiatives. The NGAFID was originally conceived to
                            bring voluntary Flight Data Monitoring (FDM)
                            capabilities to General Aviation, but has now
                            expanded to include the broader aviation community.
                        </p>
                        <p className="card-text">
                            While sharing flight data is voluntary, there are
                            many reasons pilots and operators should consider
                            participating.
                        </p>

                        <span className="text-info"><b>What is digital Flight Data Monitoring?</b></span>
                        <p className="card-text">
                            FDM is the recording of flight‐related information.
                            Analysis of FDM data can help pilots, instructors,
                            or operator groups improve performance and safety.
                        </p>

                        <span className="text-info"><b>Why should I participate with ASIAS and NGAFID?</b></span>
                        <ul>
                            <li>
                                You can replay your own flights and view your
                                data to identify potential safety risks.
                            </li>
                            <li>
                                Pilots in safety programs are less likely to be
                                involved in an accident (GAO 13‐36, pg. 13).
                            </li>
                            <li>
                                Attitude data you collect will provide you
                                enhanced feedback to improve your skills.
                            </li>
                            <li>
                                Your data will improve safety for the entire
                                aviation community.
                            </li>
                            <li>
                                <strong>
                                    <em>
                                        Your data cannot be used for any
                                        enforcement purposes. The FAA cannot see
                                        your data.
                                    </em>
                                </strong>
                            </li>
                        </ul>

                        <span className="text-info"><b>How will this project benefit the aviation community?</b></span>
                        <ul>
                            <li>
                                By working together, the community will identify
                                risks and safety hazards specific to the general
                                aviation and other communities.
                            </li>
                            <li>
                                The communities can develop and implement
                                solutions to recognized problems.
                            </li>
                        </ul>

                        <span className="text-info"><b>How can I participate?</b></span>
                        <p>
                            You can participate in two ways. (1) Data can come
                            from either your on-board avionics (for example, a
                            G1000 or data recorder) or (2) using a newly
                            developed mobile app — on your smart phone or
                            tablet.
                        </p>

                        <span className="text-info"><b>To sign up for an NGAFID account <a className="text-warning" href="{{ url('/auth/register') }}">click here</a> or download the GAARD (GA Recording Device) App for IOS and Android below:</b></span>

                        <div className="d-flex">
                            <div className="p-2 flex-fill">
                                <div className="d-flex">
                                    <div className="p-2 flex-fill">
                                        For iOS devices, search for GAARD at the App Store or click here:
                                    </div>

                                    <div className="p-2">
                                        <a href="https://geo.itunes.apple.com/us/app/gaard-general-aviation-airborne/id929718718?mt=8" target="_blank" style={{display:'inline-block', overflow:'hidden', background:'url(http://linkmaker.itunes.apple.com/images/badges/en-us/badge_appstore-lrg.svg)', backgroundRepeat:'no-repeat', width:'165px', height:'40px'}}></a>
                                    </div>
                                </div>
                            </div>

                            <div className="p-2 flex-fill">
                                <div className="d-flex">
                                    <div className="p-2 flex-fill">
                                        For Android devices, search for GAARD at Google Play or click here:
                                    </div>

                                    <div className="p-2">
                                        <a href='https://play.google.com/store/apps/details?id=org.mitre.asgaard.beta&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' style={{marginTop:'-10px', height:'60px'}}/></a>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <span className="text-info"><b>GAARD App Screenshots</b></span>

                        <div className="d-flex">
                            <div className="p-2 flex-fill">
                                <img className="img-responsive" src="./images/gaardImg1.png" style={{width:'100%'}}/>
                            </div>
                            <div className="p-2 flex-fill">
                                <img className="img-responsive" src="./images/gaardImg2.png" style={{width:'100%'}}/>
                            </div>
                            <div className="p-2 flex-fill">
                                <img className="img-responsive" src="./images/gaardImg3.png" style={{width:'100%'}}/>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        );
    }
}
