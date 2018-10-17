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


var navbar = null;
class Navbar extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loggedIn : this.props.loggedIn,
            flightsActive : true,
            importsActive : false,
            uploadsActive : false
        };

        navbar = this;
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

    isMapVisible() {
        return main_content.state.mapVisible;
    }

    toggleMap() {
        main_content.toggleMap();
    }

    togglePlot() {
        main_content.togglePlot();
    }

    show(newCard) {
        main_content.changeCard(newCard);

        let flightsActive = false;
        let importsActive = false;
        let uploadsActive = false;

        if (newCard === "Flights") flightsActive = true;
        else if (newCard === "Imports") importsActive = true;
        else if (newCard === "Uploads") uploadsActive = true;

        this.state = {
            loggedIn : this.props.loggedIn,
            flightsActive : flightsActive,
            importsActive : importsActive,
            uploadsActive : uploadsActive 
        }

        this.setState(this.state);
     }

    renderLoggedIn() {

        let flightsActive = this.state.flightsActive;
        let importsActive = this.state.importsActive;
        let uploadsActive = this.state.uploadsActive;

        let buttonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        const buttonStyle = { };

        return (
            <div className="collapse navbar-collapse" id="navbarNavDropdown">
                <ul className="navbar-nav mr-auto">

                    <button id="map-toggle-button" className={buttonClasses} data-toggle="button" title="Toggle the map." aria-pressed="false" style={buttonStyle} onClick={() => this.toggleMap()}>
                        <i className="fa fa-globe p-1"></i>
                    </button>

                    <button id="plot-toggle-button" className={buttonClasses} data-toggle="button" title="Toggle the plot." aria-pressed="false" style={buttonStyle} onClick={() => this.togglePlot()}>
                        <i className="fa fa-area-chart p-1"></i>
                    </button>


                </ul>

                <ul className="navbar-nav">
                    <NavLink name={"Flights"} onClick={() => this.show("Flights")} active={flightsActive}/>
                    <NavLink name={"Imports"} onClick={() => this.show("Imports")} active={importsActive}/>
                    <NavLink name={"Uploads"} onClick={() => this.show("Uploads")} active={uploadsActive}/>

                    <li className="nav-item dropdown">
                        <a className="nav-link dropdown-toggle" href="javascript:void(0)" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            Approach Analysis
                        </a>
                        <div className="dropdown-menu" aria-labelledby="navbarDropdownMenuLink">
                            <a className="dropdown-item" href="javascript:void(0)">Stabilized Approach</a>
                            <a className="dropdown-item" href="javascript:void(0)">Self Defined Glide Path</a>
                        </div>
                    </li>

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
    <Navbar loggedIn={true} />,
    document.querySelector('#navbar')
);

