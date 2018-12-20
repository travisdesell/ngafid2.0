class ProfileCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {};

        mainCards['profile'] = this;
        console.log("constructed ProfileCard, set mainCards");
    }

    setUser(user) {
        this.state.user = user;
        this.setState(this.state);
    }

    render() {
        const hidden = this.props.hidden;
        const bgStyle = {opacity : 0.8};
        const fgStyle = {opacity : 1.0};

        return (
            <div className="card-body" hidden={hidden}>
                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header" style={fgStyle}>
                        Profile
                    </h5>

                    <div className="card-body" style={fgStyle}>
                        <p className="card-text">
                            User Profile Here.
                        </p>
                    </div>

                </div>
            </div>
        );
    }
}
