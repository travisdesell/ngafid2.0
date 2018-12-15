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

var mainContent = null;

class MainContent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            activeName : props.activeName,
            mapVisible : false,
            plotVisible : false 
        };

        mainContent = this;
    }

    showMap() {
        if (this.state.mapVisible) return;

        if ( !$("#map-toggle-button").hasClass("active") ) { 
            $("#map-toggle-button").addClass("active");
            $("#map-toggle-button").attr("aria-pressed", true);
        }

        this.state = {
            activeName : this.state.activeName,
            plotVisible : this.state.plotVisible,
            mapVisible : true
        };
        this.setState(this.state);

        $("#plot-map-div").css("height", "50%");
        $("#map").show();

        if (this.state.plotVisible) {
            $("#map").css("width", "50%");
            map.updateSize();
            $("#plot").css("width", "50%");
            Plotly.Plots.resize("plot");
        } else {
            $("#map").css("width", "100%");
            map.updateSize();
        }

    }

    hideMap() {
        if (!this.state.mapVisible) return;

        if ( $("#map-toggle-button").hasClass("active") ) { 
            $("#map-toggle-button").removeClass("active");
            $("#map-toggle-button").attr("aria-pressed", false);
        }   

        this.state = {
            activeName : this.state.activeName,
            plotVisible : this.state.plotVisible,
            mapVisible : false
        };
        this.setState(this.state);

        $("#map").hide();

        if (this.state.plotVisible) {
            $("#plot").css("width", "100%");
            var update = { width : "100%" };
            Plotly.Plots.resize("plot");
        } else {
            $("#plot-map-div").css("height", "0%");
        }
    }

    toggleMap() {
        if (this.state.mapVisible) {
            this.hideMap();
        } else {
            this.showMap();
        }
    }

    showPlot() {
        if (this.state.plotVisible) return;

        if ( !$("#plot-toggle-button").hasClass("active") ) { 
            $("#plot-toggle-button").addClass("active");
            $("#plot-toggle-button").attr("aria-pressed", true);
        }

        this.state = {
            activeName : this.state.activeName,
            plotVisible : true,
            mapVisible : this.state.mapVisible
        };
        this.setState(this.state);

        $("#plot").show();
        $("#plot-map-div").css("height", "50%");

        if (this.state.mapVisible) {
            $("#map").css("width", "50%");
            map.updateSize();
            $("#plot").css("width", "50%");
            Plotly.Plots.resize("plot");
        } else {
            $("#plot").css("width", "100%");
            Plotly.Plots.resize("plot");
        }
    }

    hidePlot() {
        if (!this.state.plotVisible) return;

        if ( $("#plot-toggle-button").hasClass("active") ) { 
            $("#plot-toggle-button").removeClass("active");
            $("#plot-toggle-button").attr("aria-pressed", false);
        }   

        this.state = {
            activeName : this.state.activeName,
            plotVisible : false,
            mapVisible : this.state.mapVisible
        };
        this.setState(this.state);

        $("#plot").hide();

        if (this.state.mapVisible) {
            $("#map").css("width", "100%");
            map.updateSize();
        } else {
            $("#plot-map-div").css("height", "0%");
        }
    }

    togglePlot() {
        if (this.state.plotVisible) {
            this.hidePlot();
        } else {
            this.showPlot();
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
        if (this.state.mapVisible || this.state.plotVisible) {
            console.log("rendering half");
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
    /*
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

    var layout2 = { 
            yaxis: {rangemode: 'tozero',
                        zeroline: true}
    };

    Plotly.newPlot('plot', data, layout1);
    */

    var layout1 = { 
        yaxis: {
            rangemode: 'tozero',
            showline: true,
            zeroline: true
        }
    };
    layout1 = {};

    Plotly.newPlot('plot', [], layout1);

    var submission_data = {
        request : "GET_MAIN_CONTENT",
        id_token : "TEST_ID_TOKEN",
        //id_token : id_token,
        user_id : 1
        //user_id : user_id
    };   

    $.ajax({
        type: 'POST',
        url: './protected/main_content',
        data : submission_data,
        dataType : 'json',
        success : function(response) {
            console.log("received response: ");
            console.log(response);

            if (response.err_msg) {
                display_error_modal(response.err_title, response.err_msg);
                return;
            }

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
