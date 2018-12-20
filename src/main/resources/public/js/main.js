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
var mainCards = {}

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

        let ucNewName = newName.toLowerCase();

        //don't request data from server if we're in on of the non-logged navbar cards
        //use reflection to get the correct props field (flights, imports or uploads)
        //the indexOf method will return -1 if the array doesn't contain newName
        let loggedOutCards = ["Home", "Create Account", "Awaiting Access", "Welcome"];

        if (newName == "Create Account" && typeof mainCards['create_account'].state.fleets == 'undefined') {
            var submission_data = {};   

            $('#loading').show();

            $.ajax({
                type: 'POST',
                url: './get_fleet_names',
                data : submission_data,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    $('#loading').hide();
                    //TODO: processResponse instead


                    if (response.err_msg) {
                        display_error_modal(response.err_title, response.err_msg);
                        return;
                    }

                    console.log("setting mainCards['create_account'] content");
                    mainCards['create_account'].setFleets( response );

                    mainContent.state.activeName = newName;
                    mainContent.setState(mainContent.state);

                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    display_error_modal("Error Loading Uploads", errorThrown);
                },   
                async: true 
            });  


        } else if (loggedOutCards.indexOf(newName) < 0 && typeof mainCards[ucNewName].state[ucNewName] == 'undefined') {
            //TODO: show loading spinner until flights loaded

            var submission_data = {};   

            $('#loading').show();

            $.ajax({
                type: 'POST',
                url: './protected/get_' + ucNewName,
                data : submission_data,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    $('#loading').hide();

                    if (response.err_msg) {
                        display_error_modal(response.err_title, response.err_msg);
                        return;
                    }

                    if (ucNewName == 'uploads') {
                        for (var i = 0; i < response.length; i++) {
                            if (response[i].status == "UPLOADING") response[i].status = "UPLOAD INCOMPLETE";
                        }
                    }

                    mainContent.state.activeName = newName;
                    mainContent.setState(mainContent.state);

                    console.log("setting mainCards['" + ucNewName + "'] content");
                    mainCards[ucNewName].setContent( response );
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    display_error_modal("Error Loading Uploads", errorThrown);
                },   
                async: true 
            });  

        } else {
            //if this is a logged out card, hide the map and plot
            if (loggedOutCards.indexOf(newName) > -1) {
                this.hideMap();
                this.hidePlot();
            }

            this.setState({
                activeName : newName
            });
        }
    }

    render() {
        console.log("rendering main!");

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

        /*
        console.log('rendering with activeName: ' + activeName);
        console.log(this.state.flights);
        console.log(this.state.imports);
        console.log(this.state.uploads);

        console.log("home hidden: " + (activeName != "Home"));
        console.log("flights hidden: " + (activeName != "Flights"));
        console.log("imports hidden: " + (activeName != "Imports"));
        console.log("uploads hidden: " + (activeName != "Uplaods"));
        */

        return (
            <div id="MainCards" style={style}>
                <HomeCard name={"Home"} hidden={activeName != "Home"} />
                <CreateAccountCard name={"Create Account"} hidden={activeName != "Create Account"} />
                <AwaitingAccessCard name={"Awaiting Access"} hidden={activeName != "Awaiting Access"} />
                <WelcomeCard name={"Welcome"} hidden={activeName != "Welcome"} />
                <FlightsCard name={"Flights"} hidden={activeName != "Flights"} />
                <ImportsCard name={"Imports"} hidden={activeName != "Imports"} />
                <UploadsCard name={"Uploads"} hidden={activeName != "Uploads"} />
            </div>
        );
    }
}


$(document).ready(function() {
    var layout1 = { 
        yaxis: {
            rangemode: 'tozero',
            showline: true,
            zeroline: true
        }
    };
    layout1 = {};

    Plotly.newPlot('plot', [], layout1);

    ReactDOM.render(
        <MainContent activeName="Home"/>,
        document.querySelector('#main')
    );

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
