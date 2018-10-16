'use strict';


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

var main_content = null;

class MainContent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            activeName : props.activeName,
            mapVisible : true
        };

        main_content = this;
    }

    showMap() {
        this.state = {
            activeName : this.state.activeName,
            mapVisible : true
        };
        this.setState(this.state);
        $("#map").show();
    }

    hideMap() {
        this.state = {
            activeName : this.state.activeName,
            mapVisible : false
        };
        this.setState(this.state);
        $("#map").hide();
    }

    toggleMap() {
        if (this.state.mapVisible) {
            this.hideMap();
        } else {
            this.showMap();
        }
    }

    changeCard(newName) {
        console.log("changing active card to: '" + newName + "'");

        this.setState({
            activeName : newName
        });
    }

    render() {
        let activeName = this.state.activeName;

        let style = null;
        if (this.state.mapVisible) {
            style = { 
                overflow : "scroll",
                height : "calc(50% - 56px)"
            };
        } else {
            style = { 
                overflow : "scroll",
                height : "calc(100% - 56px)"
            };
        }

        return (
            <div id="MainCards" style={style}>
                <FlightsCard name={"Flights"} hidden={activeName != "Flights"} flights={this.props.flights} />
                <ImportsCard name={"Imports"} hidden={activeName != "Imports"} imports={this.props.imports} />
                <UploadsCard name={"Uploads"} hidden={activeName != "Uploads"} uploads={this.props.uploads} />
            </div>
        );
    }
}


$(document).ready(function() {
    var submission_data = {
        request : "GET_MAIN_CONTENT",
        id_token : "TEST_ID_TOKEN",
        //id_token : id_token,
        user_id : 1
        //user_id : user_id
    };   

    var trace1 = { 
            x: [1, 2, 3, 4], 
            y: [10, 15, 13, 17], 
            type: 'scatter'
    };
    var trace2 = { 
            x: [1, 2, 3, 4], 
            y: [16, 5, 11, 9], 
            type: 'scatter'
    };
    var data = [trace1, trace2];

    var layout1 = { 
            yaxis: {rangemode: 'tozero',
                        showline: true,
                        zeroline: true}
    };

    var layout2 = { 
            yaxis: {rangemode: 'tozero',
                        zeroline: true}
    };

    Plotly.newPlot('div1', data, layout1);


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
                <MainContent activeName="Flights" uploads={response.uploads} imports={response.imports} flights={response.flights}/>,
                document.querySelector('#main')
            );
        },   
        error : function(jqXHR, textStatus, errorThrown) {
            display_error_modal("Error Loading Uploads", errorThrown);
        },   
        async: true 
    });  

	/*
    $(window).scroll(function() {

		var end = $("#MainCards").offset().top + $("#MainCards").height();
		var viewEnd = $(window).scrollTop() + $(window).height(); 
		var distance = end - viewEnd; 

        console.log("scrolling, top: " + $(window).scrollTop() + ", document height: " + $(document).height() + ", window height: " + $(window).height());
		console.log("end: " + end + ", viewEnd: " + viewEnd);

		if (distance < 300) {
            $("#load-more").html("Load More");
            console.log("loading more!");
		}

        if ($(window).scrollTop() == $(document).height() - $(window).height()) {
            // ajax call get data from server and append to the div
            $("#load-more").html("Load More");
            console.log("loading more!");
        }
    });
	*/

});
