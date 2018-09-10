'use strict';



class ProcessingQueueCard extends React.Component {
    render() {
        let hidden = this.props.hidden;

        return (
            <div className="card-body" hidden={hidden}>
                <h5 className="card-title">Processing Queue Goes Here!</h5>
                <p className="card-text">With supporting text below as a natural lead-in to additional content.</p>
                <a href="#" className="btn btn-primary">Go somewhere</a>
            </div>
        );
    }
}


class FlightsCard extends React.Component {
    render() {
        let hidden = this.props.hidden;

        return (
            <div className="card-body" hidden={hidden}>
                <h5 className="card-title">Flights Go Here!</h5>
                <p className="card-text">With supporting text below as a natural lead-in to additional content.</p>
                <a href="#" className="btn btn-primary">Go somewhere</a>
            </div>
        );
    }
}


class TabHeader extends React.Component {
    render() {
        const classNames = (this.props.activeName == this.props.name) ? "nav-link active" : "nav-link";
        const name = this.props.name;
        const onClick = this.props.onClick;

        return (
            <li className="nav-item">
                <a className={classNames} href="javascript:void(0)" onClick={() => onClick()}>{name}</a>
            </li>
        );
    }
}

class MainContent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            activeName : props.activeName
        };
    }

    changeCard(newName) {
        console.log("changing active card to: '" + newName + "'");

        this.setState({
            activeName : newName
        });
    }

    render() {
        let activeName = this.state.activeName;

        return (
            <div id="MainCards">
                <div className="card-header">
                    <ul className="nav nav-tabs card-header-tabs">
                        <TabHeader name={"Flights"} activeName={activeName} onClick={() => this.changeCard("Flights")} />
                        <TabHeader name={"Processing Queue"} activeName={activeName} onClick={() => this.changeCard("Processing Queue")} />
                        <TabHeader name={"Uploads"} activeName={activeName} onClick={() => this.changeCard("Uploads")} />
                    </ul>
                </div>
                <FlightsCard name={"Flights"} hidden={activeName != "Flights"} />
                <ProcessingQueueCard name={"Processing Queue"} hidden={activeName != "Processing Queue"} />
                <UploadsCard name={"Uploads"} hidden={activeName != "Uploads"} />
            </div>
        );
    }
}

var mainContent = ReactDOM.render(
    <MainContent activeName="Uploads"/>,
    document.querySelector('#main')
);
