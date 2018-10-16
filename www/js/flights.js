class Itinerary extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let cellClasses = "p-1 card mr-1 flex-fill"
        let itinerary = this.props.itinerary;

        let result = "";
        for (let i = 0; i < itinerary.length; i++) {
            result += itinerary[i].airport + " (" + itinerary[i].runway + ")";
            if (i != (itinerary.length - 1)) result += " => ";
        }

        return (
            <div className={cellClasses}>
                {result}
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
            layer : null
        }
    }

    globeClicked() {
        if (this.props.flightInfo.has_coords === "0") return;

        main_content.showMap();

        if (!this.state.mapLoaded) {
            this.state.mapLoaded = true;

            var thisFlight = this;

            var submission_data = {
                request : "GET_COORDINATES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flight_id : this.props.flightInfo.id
            };   

            $.ajax({
                type: 'POST',
                url: './request.php',
                data : submission_data,
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
            this.state.layer.setVisible(this.state.pathVisible);
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

        let startTime = moment(flightInfo.start_time);
        let endTime = moment(flightInfo.end_time);

        let globeClasses = "";
        let globeTooltip = "";

        //console.log(flightInfo);
        if (flightInfo.has_coords === "0") {
            console.log("flight " + flightInfo.id + " doesn't have coords!");
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


        return (
            <div className="card mb-1">
                <div className="card-body m-0 p-0">
                    <div className="d-flex flex-row p-1">
                        <div className={firstCellClasses}>
                            <i className="fa fa-plane p-1">{flightInfo.id}</i>
                        </div>

                        <div className={cellClasses}>
                            {flightInfo.tail_number}
                        </div>

                        <div className={cellClasses}>
                            {flightInfo.airframe_type}
                        </div>

                        <div className={cellClasses}>
                            {flightInfo.start_time}
                        </div>

                        <div className={cellClasses}>
                            {flightInfo.end_time}
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

                            <button className={buttonClasses} style={styleButton} onClick={() => this.chartClicked()}>
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

                    <div className="d-flex flex-row p-1">
                        <Itinerary itinerary={flightInfo.itinerary}/>
                    </div>
                </div>
            </div>
        );
    }
}


class FlightsCard extends React.Component {
    constructor(props) {
        super(props);

        let flights = props.flights;
        if (flights == undefined) flights = [];

        this.state = {
            flights : flights
        };
    }

    render() {
        let hidden = this.props.hidden;

        return (
            <div className="card-body" hidden={hidden}>
                {
                    this.state.flights.map((flightInfo, index) => {
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
