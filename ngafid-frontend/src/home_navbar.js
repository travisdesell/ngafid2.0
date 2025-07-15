import 'bootstrap';
import React from "react";
import { createRoot } from 'react-dom/client';

// import {loginModal} from "./login.js";
import { showLoginModal } from "./login.js";

import {DarkModeToggle} from "./dark_mode_toggle.js";

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
            <li className={classNames}>
                <a className="nav-link" href={href} hidden={hidden} onClick={() => onClick()}>
                    {name} {isCurrent}
                </a>
            </li>
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
            <nav id='navbar' className="navbar navbar-expand-lg navbar-light"
                 style={{zIndex: "999", opacity: "1.0", backgroundColor: "var(--c_navbar_bg)"}}>

                <a className="navbar-brand" style={{color: "var(--c_text)"}} href="/">NGAFID</a>
                <button className="navbar-toggler" type="button" data-bs-toggle="collapse"
                        data-bs-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false"
                        aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="collapse navbar-collapse" id="navbarNavDropdown">
                    <ul className="navbar-nav mr-auto">
                    </ul>

                    <ul className="navbar-nav">
                        <NavLink name={"Login"} onClick={() => this.attemptLogIn()}/>
                        <NavLink name={"Create Account"} href="/create_account"/>
                    </ul>
                </div>

                <div>
                    &nbsp;<DarkModeToggle/>
                </div>

            </nav>
        );
    }
}

const container = document.querySelector("#navbar");
const navbar = createRoot(container);
navbar.render(<HomeNavbar/>);

export {navbar as homeNavbar};