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
                        Welcome to the NGAFID
                    </h5>

                    <div className="card-body" style={fgStyle}>
                        <p className="card-text">
                            Fleet overview and welcome message goes here.
                        </p>
                    </div>

                </div>
            </div>
        );
    }
}

