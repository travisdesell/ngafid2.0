class AwaitingAccessCard extends React.Component {
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
                        Account Awaiting Fleet Access
                    </h5>

                    <div className="card-body" style={fgStyle}>
                        <p className="card-text">
                            Welcome to the NGAFID! Your account has created however the your fleet manager(s) have not yet granted you access to the fleet data. Please contact your fleet manager(s) so they can log in and provide access.
                        </p>
                    </div>

                </div>
            </div>
        );
    }
}
