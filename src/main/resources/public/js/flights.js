class Itinerary extends React.Component {
    constructor(props) {
        super(props);
    }

    itineraryClicked(index) {
        mainContent.showMap();

        var stop = this.props.itinerary[index];
        let modifiedIndex = parseInt(stop.minAltitudeIndex) - this.props.nanOffset;
        //console.log("index: " + stop.minAltitudeIndex + ", nanOffset: " + this.props.nanOffset + ", modifeid_index: " + modifiedIndex);

        let latlon = this.props.coordinates[modifiedIndex];
        //console.log(latlon);

        const coords = ol.proj.fromLonLat(latlon);
        map.getView().animate({center: coords, zoom: 13});
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        /*
        let cellClasses = "p-1 card mr-1 flex-fill"
        let itinerary = this.props.itinerary;

        let result = "";
        for (let i = 0; i < itinerary.length; i++) {
            result += itinerary[i].airport + " (" + itinerary[i].runway + ")";
            if (i != (itinerary.length - 1)) result += " => ";
        }
        */

        return (
            <div className={cellClasses} style={cellStyle}>
                {
                    this.props.itinerary.map((stop, index) => {
                        let identifier = stop.airport;
                        if (stop.runway != null) identifier += " (" + stop.runway + ")";

                        return (
                            <button className={buttonClasses} key={index} style={styleButton} onClick={() => this.itineraryClicked(index)}>
                                { identifier }
                            </button>
                        );
                    })
                }
            </div>
        );

    }
}

class TraceButtons extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            parentFlight : this.props.parentFlight
        };
    }

    traceClicked(seriesName) {
        mainContent.showPlot();

        let parentFlight = this.state.parentFlight;

        //check to see if we've already loaded this time series
        if (!(seriesName in parentFlight.state.traceIndex)) {
            var thisTrace = this;

            console.log(seriesName);
            console.log("seriesName: " + seriesName + ", flightId: " + this.props.flightId);

            var submissionData = {
                request : "GET_DOUBLE_SERIES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flightId : this.props.flightId,
                seriesName : seriesName
            };   

            $.ajax({
                type: 'POST',
                url: './protected/double_series',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    var trace = {
                        x : response.x,
                        y : response.y,
                        mode : "lines",
                        //marker : { size: 1},
                        name : thisTrace.props.flightId + " - " + seriesName
                    }

                    //set the trace number for this series
                    parentFlight.state.traceIndex[seriesName] = $("#plot")[0].data.length;
                    parentFlight.state.traceVisibility[seriesName] = true;
                    parentFlight.setState(parentFlight.state);

                    Plotly.addTraces('plot', [trace]);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    display_error_modal("Error Loading Flight Coordinates", errorThrown);
                },   
                async: true 
            });  
        } else {
            //toggle visibility for this series
            let visibility = !parentFlight.state.traceVisibility[seriesName];
            parentFlight.state.traceVisibility[seriesName] = visibility;
            parentFlight.setState(parentFlight.state);

            console.log("toggled visibility to: " + visibility);

            Plotly.restyle('plot', { visible: visibility }, [ parentFlight.state.traceIndex[seriesName] ])
        }
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        let parentFlight = this.state.parentFlight;


        return (
            <div className={cellClasses} style={cellStyle}>
                {
                    parentFlight.state.traceNames.map((traceName, index) => {
                        let ariaPressed = parentFlight.state.traceVisibility[traceName];
                        let active = "";
                        if (ariaPressed) active = " active";

                        return (
                            <button className={buttonClasses + active} key={traceName} style={styleButton} data-toggle="button" aria-pressed={ariaPressed} onClick={() => this.traceClicked(traceName)}>
                                {traceName}
                            </button>
                        );
                    })
                }
            </div>
        );
    }
}

class Flight extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            pathVisible : false,
            mapLoaded : false,
            traceNames : null,
            traceIndex : [],
            traceVisibility : [],
            traceNamesVisible : false,
            itineraryVisible : false,
            layer : null
        }
    }

    plotClicked() {
        if (this.state.traceNames == null) {
            var thisFlight = this;

            var submissionData = {
                request : "GET_DOUBLE_SERIES_NAMES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flightId : this.props.flightInfo.id
            };   

            $.ajax({
                type: 'POST',
                url: './protected/double_series_names',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    var names = response.names;

                    //set the trace number for this series
                    thisFlight.state.traceNames = response.names;
                    thisFlight.state.traceNamesVisible = true;
                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    this.state.traceNames = null;
                    display_error_modal("Error Getting Potentail Plot Parameters", errorThrown);
                },   
                async: true 
            });  
        } else {
            let visible = !this.state.traceNamesVisible;

            for (let i = 0; i < this.state.traceNames.length; i++) {
                let seriesName = this.state.traceNames[i];
                //check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }

            this.state.traceNamesVisible = !this.state.traceNamesVisible;
            this.setState(this.state);
        }

        /*
        var trace3 = {
            x: [1, 2, 3, 4, 5],
            y: [11, 8, 1, 7, 13],
            type: 'scatter',
            yaxis: 'y2'
        };
        var data_update = [trace3];
        var layout_update = {
            title : "new title!"
        }

        console.log($("#plot"));
        console.log($("#plot")[0].layout);
        var layout = $("#plot")[0].layout;
        layout.yaxis2 = {
            title: 'yaxis2 title',
            titlefont: {color: '#ff7f0e'},
            tickfont: {color: '#ff7f0e'},
            anchor: 'free',
            overlaying: 'y',
            side: 'left',
            position: 0.15
        };

        Plotly.update('plot', $("#plot").data, layout);

        Plotly.addTraces('plot', data_update);
        */
    }

    globeClicked() {
        if (this.props.flightInfo.has_coords === "0") return;


        if (!this.state.mapLoaded) {
            mainContent.showMap();
            this.state.mapLoaded = true;

            var thisFlight = this;

            var submissionData = {
                request : "GET_COORDINATES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flightId : this.props.flightInfo.id
            };   

            $.ajax({
                type: 'POST',
                url: './protected/coordinates',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    //console.log("received response: ");
                    //console.log(response);

                    var coordinates = response.coordinates;

                    var points = [];
                    for (var i = 0; i < coordinates.length; i++) {
                        var point = ol.proj.fromLonLat(coordinates[i]);
                        points.push(point);
                    }

                    var color = Colors.random();
                    console.log(color);

                    thisFlight.state.layer = new ol.layer.Vector({
                        style: new ol.style.Style({
                            stroke: new ol.style.Stroke({
                                color: color,
                                width: 1.5
                            }),
                        }),

                        source : new ol.source.Vector({
                            features: [new ol.Feature({
                                geometry: new ol.geom.LineString(points),
                                name: 'Line'
                            })]
                        }),
                    });
                    thisFlight.state.layer.setVisible(true);
                    thisFlight.state.pathVisible = true;
                    thisFlight.state.itineraryVisible = true;
                    thisFlight.state.nanOffset = response.nanOffset;
                    thisFlight.state.coordinates = response.coordinates;

                    map.addLayer(thisFlight.state.layer);

                    let extent = thisFlight.state.layer.getSource().getExtent();
                    console.log(extent);
                    map.getView().fit(extent, map.getSize());

                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    thisFlight.state.mapLoaded = false;
                    thisFlight.setState(thisFlight.state);

                    display_error_modal("Error Loading Flight Coordinates", errorThrown);
                },   
                async: true 
            });  
        } else {
            //toggle visibility if already loaded
            this.state.pathVisible = !this.state.pathVisible;
            this.state.itineraryVisible = !this.state.itineraryVisible;
            this.state.layer.setVisible(this.state.pathVisible);

            if (this.state.pathVisibile) {
                mainContent.showMap();
            }

            this.setState(this.state);

            if (this.state.pathVisible) {
                let extent = this.state.layer.getSource().getExtent();
                console.log(extent);
                map.getView().fit(extent, map.getSize());
            }
        }
    }

    render() {
        let buttonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        let lastButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";
        const styleButton = { };

        let firstCellClasses = "p-1 card mr-1"
        let cellClasses = "p-1 card flex-fill mr-1"

        let flightInfo = this.props.flightInfo;

        let startTime = moment(flightInfo.startDateTime);
        let endTime = moment(flightInfo.endDateTime);

        let globeClasses = "";
        let globeTooltip = "";

        //console.log(flightInfo);
        if (!flightInfo.hasCoords) {
            //console.log("flight " + flightInfo.id + " doesn't have coords!");
            globeClasses += " disabled";
            globeTooltip = "Cannot display flight on the map because the flight data did not have latitude/longitude.";
        } else {
            globeTooltip = "Click the globe to display the flight on the map.";
        }

        let visitedAirports = [];
        for (let i = 0; i < flightInfo.itinerary.length; i++) {
            if ($.inArray(flightInfo.itinerary[i].airport, visitedAirports) < 0) {
                visitedAirports.push(flightInfo.itinerary[i].airport);
            }
        }

        let itineraryRow = "";
        if (this.state.itineraryVisible) {
            itineraryRow = (
                <Itinerary itinerary={flightInfo.itinerary} coordinates={this.state.coordinates} nanOffset={this.state.nanOffset}/>
            );
        }

        let tracesRow = "";
        if (this.state.traceNamesVisible) {
            tracesRow = 
                (
                    <TraceButtons parentFlight={this} flightId={flightInfo.id}/>
                );
        }

        return (
            <div className="card mb-1">
                <div className="card-body m-0 p-0">
                    <div className="d-flex flex-row p-1">
                        <div className={firstCellClasses}>
                            <i className="fa fa-plane p-1">{flightInfo.id}</i>
                        </div>

                        <div className={cellClasses}>
                            {flightInfo.tailNumber}
                        </div>

                        <div className={cellClasses}>
                            {flightInfo.airframeType}
                        </div>

                        <div className={cellClasses}>
                            {flightInfo.startDateTime}
                        </div>

                        <div className={cellClasses}>
                            {flightInfo.endDateTime}
                        </div>

                        <div className={cellClasses}>
                            {moment.utc(endTime.diff(startTime)).format("HH:mm:ss")}
                        </div>

                        <div className={cellClasses}>
                            {visitedAirports.join(", ")}
                        </div>

                        <div className="p-0">
                            <button className={buttonClasses} style={styleButton} onClick={() => this.editClicked()}>
                                <i className="fa fa-pencil p-1"></i>
                            </button>

                            <button className={buttonClasses + globeClasses} data-toggle="button" title={globeTooltip} aria-pressed="false" style={styleButton} onClick={() => this.globeClicked()}>
                                <i className="fa fa-globe p-1"></i>
                            </button>

                            <button className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.plotClicked()}>
                                <i className="fa fa-area-chart p-1"></i>
                            </button>

                            <button className={buttonClasses} style={styleButton} onClick={() => this.replayClicked()}>
                                <i className="fa fa-video-camera p-1"></i>
                            </button>

                            <button className={lastButtonClasses} style={styleButton} onClick={() => this.downloadClicked()}>
                                <i className="fa fa-download p-1"></i>
                            </button>
                        </div>
                    </div>

                    {itineraryRow}

                    {tracesRow}
                </div>
            </div>
        );
    }
}

class FlightsCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            hidden : this.props.hidden
        }

        mainCards['flights'] = this;
        console.log("constructed FlightsCard, set mainCards");
    }

    setContent(flights) {
        this.state.flights = flights;
        this.setState(this.state);
    }

    render() {
        console.log("rendering flights!");
        let hidden = this.props.hidden;

        let flights = [];
        if (typeof this.state.flights != 'undefined') {
            flights = this.state.flights;
        }

        return (
            <div className="card-body" hidden={hidden}>
                {
                    flights.map((flightInfo, index) => {
                        return (
                            <Flight flightInfo={flightInfo} key={flightInfo.id} />
                        );
                    })
                }

                <div id="load-more"></div>
            </div>
        );
    }
}
