'use strict';



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
                        <TabHeader name={"Imports"} activeName={activeName} onClick={() => this.changeCard("Imports")} />
                        <TabHeader name={"Uploads"} activeName={activeName} onClick={() => this.changeCard("Uploads")} />
                    </ul>
                </div>
                <FlightsCard name={"Flights"} hidden={activeName != "Flights"} />
                <ImportsCard name={"Imports"} hidden={activeName != "Imports"} imports={this.props.imports} />
                <UploadsCard name={"Uploads"} hidden={activeName != "Uploads"} uploads={this.props.uploads} />
            </div>
        );
    }
}


$(document).ready(function() {
    console.log("document ready!");

    var submission_data = {
        request : "GET_MAIN_CONTENT",
        id_token : "TEST_ID_TOKEN",
        //id_token : id_token,
        user_id : 1
        //user_id : user_id
    };   

    $.ajax({
        type: 'POST',
        url: './request.php',
        data : submission_data,
        dataType : 'json',
        success : function(response) {
            console.log("received response: ");
            console.log(response);

            for (var i = 0; i < response.uploads.length; i++) {
                if (response.uploads[i].status == "UPLOADING") {
                    response.uploads[i].status = "UPLOAD INCOMPLETE";
                }
            }

            var mainContent = ReactDOM.render(
                <MainContent activeName="Uploads" uploads={response.uploads} imports={response.imports} />,
                document.querySelector('#main')
            );
        },   
        error : function(jqXHR, textStatus, errorThrown) {
            display_error_modal("Error Loading Uploads", errorThrown);
        },   
        async: true 
    });  

});
