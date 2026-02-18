import 'bootstrap';
import React from "react";
import { createRoot } from 'react-dom/client';

import { showLoginModal } from "./login.js";

import {DarkModeToggle} from "./dark_mode_toggle.js";


import './index.css'; //<-- include Tailwind


class NavLink extends React.Component {
    render() {
        const name = this.props.name;
        const hidden = this.props.hidden;
        const active = this.props.active;
        let onClick = this.props.onClick;
        let href = this.props.href;

        if (typeof href == 'undefined')
            href = "#!";

        //onClick not defined, make it an empty function
        if (typeof onClick == 'undefined') onClick = function () { /* ... */ };

        const classNames = active ? "nav-item active" : "nav-item";
        const isCurrent = active ? (<span className="sr-only">(current)</span>) : "";

        return (
            <span className={classNames}>
                <a className="nav-link" href={href} hidden={hidden} onClick={() => onClick()}>
                    {name} {isCurrent}
                </a>
            </span>
        );
    }
}


export default class HomeNavbar extends React.Component {

    attemptLogIn() {
        console.log("Showing login modal: ...");
        showLoginModal();
    }

    render() {

        return (
            <nav
                id='navbar'
                className="navbar navbar-expand-lg navbar-light flex! flex-row! items-center justify-between!"
                style={{zIndex: "999", opacity: "1.0", backgroundColor: "var(--c_navbar_bg)"}}
            >

                {/* Left Elements */}
                <div>

                    {/* Navbar Brand & Home Link */}
                    <a className="navbar-brand" style={{color: "var(--c_text)"}} href="/">
                        NGAFID
                    </a>
                </div>

                {/* Right Elements */}
                <div className="flex flex-row items-center justify-end">

                    {/* Navlink Buttons */}
                    {
                        (this.props.displayNavlinkButtons)
                        &&
                        <>
                            {/* Login Button */}
                            <NavLink name={"Login"} onClick={() => this.attemptLogIn()}/>

                            {/* Create Account Button */}
                            <NavLink name={"Create Account"} href="/create_account"/>
                        </>
                    }

                    {/* Dark Mode Toggle Button */}
                    <div className="ml-2">
                        <DarkModeToggle onClickAlt={this.darkModeOnClickAlt}/>
                    </div>
                </div>

            </nav>
        );
    }
}

HomeNavbar.defaultProps = {
    displayNavlinkButtons: true,
};


//Conditionally render and export a homeNavbar to keep the Login modal working
const container = document.querySelector("#navbar");
let navbar = null;
if (container) {
    navbar = createRoot(container);
    navbar.render(<HomeNavbar/>);
}

export {navbar as homeNavbar};