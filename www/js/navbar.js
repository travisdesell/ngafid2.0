'use strict';

var loggedIn = false;

class NavLink extends React.Component {
    render() {
        const name = this.props.name;
        const active = this.props.active;
        const onClick = this.props.onClick;

        const classNames = active ? "nav-item active" : "nav-item";
        const isCurrent = active ? (<span className="sr-only">(current)</span>) : "";

        return (
            <li className={classNames}>
                <a className="nav-link" href="javascript:void(0)" onClick={() => onClick()}>{name} {isCurrent}</a>
            </li>
        );
    }
}

class Navbar extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loggedIn : this.props.loggedIn
        };
    }

    logOut() {
        console.log("logging out!");

        this.setState({
            loggedIn : false
        });
    }

    logIn() {
        console.log("logging in!");

        this.setState({
            loggedIn : true
        });
    }

    renderLoggedIn() {
        return (
            <div className="collapse navbar-collapse" id="navbarNavDropdown">
                <ul className="navbar-nav mr-auto">
                    <NavLink name={"Home"} active={true} />
                    <NavLink name={"Trends"} />
                    <NavLink name={"Import"} />

                    <li className="nav-item dropdown">
                        <a className="nav-link dropdown-toggle" href="javascript:void(0)" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            Approach Analysis
                        </a>
                        <div className="dropdown-menu" aria-labelledby="navbarDropdownMenuLink">
                            <a className="dropdown-item" href="javascript:void(0)">Stabilized Approach</a>
                            <a className="dropdown-item" href="javascript:void(0)">Self Defined Glide Path</a>
                        </div>
                    </li>
                </ul>

                <ul className="navbar-nav">
                    <NavLink name={"Logout"} onClick={() => this.logOut()}/>
                </ul>
            </div>
        );
    }

    renderLoggedOut() {
        return (
            <div className="collapse navbar-collapse" id="navbarNavDropdown">
                <ul className="navbar-nav mr-auto">
                </ul>

                <ul className="navbar-nav">
                    <NavLink name={"Login"} onClick={() => this.logIn()} />
                </ul>
            </div>
        );
    }

    render() {
        const isLoggedIn = this.state.loggedIn;
        let navbar_content;

        if (isLoggedIn) {
            navbar_content = this.renderLoggedIn();
        } else {
            navbar_content = this.renderLoggedOut();
        }

        return (
            <nav className="navbar navbar-expand-lg navbar-light bg-light">
                <a className="navbar-brand" href="javascript:void(0)">NGAFID</a>
                <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"></span>
                </button>
                {navbar_content}
            </nav>
        );
    }
}

var navbar = ReactDOM.render(
    <Navbar loggedIn={false} />,
    document.querySelector('#navbar')
);

