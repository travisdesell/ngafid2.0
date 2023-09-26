import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Form from "react-bootstrap/Form";
import Popover from 'react-bootstrap/Popover';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import Button from 'react-bootstrap/Button';
import ButtonGroup from 'react-bootstrap/ButtonGroup';

import { map, styles, layers, Colors, overlay, initializeMap, container, content, closer } from "./map.js";
import { TimeHeader, TurnToFinalHeaderComponents } from "./time_header.js";
import SignedInNavbar from "./signed_in_navbar.js";
import { paletteAt, paletteGenerator } from "./map_utils.js";

import View from 'ol/View';
import Overlay from 'ol/Overlay';
import Coordinate from 'ol/coordinate';
import {fromLonLat, toLonLat} from 'ol/proj.js';
import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import Draw from 'ol/interaction/Draw.js';

import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';
import { Filter } from './filter.js';

import Plotly from 'plotly.js';

var moment = require('moment');

initializeMap();

class TTFMapPopup extends React.Component {
    constructor(props) {
        super(props);
        console.log("Creating TTFMapPopup");
        this.state = {
            visible: true
        };
    }

    show() {
        this.setState({visible: true});
    }

    close() {
        this.setState({visible: false});
    }

    render() {
        var style = {
            top: this.props.pixel[1] + this.props.map_container_rect.y - 10,
            left: this.props.pixel[0] + this.props.map_container_rect.x + 15
        };

        if (!this.state.visible) {
            return null;
        }

        return (
            <div style={{ height: 120 }}>
                <Popover
                    style={style}
                >
                    <Popover.Title as="h2" style={{ display: "flex" }}><Row>
                            <Col >Flight {this.props.flight_id} Approach {this.props.approach_n}</Col>
                            <Col sm="auto"></Col>
                            <Col>
                            <ButtonGroup>
                                <Button variant="outline-info" href={"/protected/flight?flight_id=" + this.props.flight_id} target="_blank">
                                    <i className="fa fa-plane p-1"></i>
                                </Button>
                                <Button onClick={ () => this.close() } data-toggle="button" variant="outline-danger">
                                    <i className="fa fa-times p-1"></i>
                                </Button>
                            </ButtonGroup>
                            </Col>
                    </Row></Popover.Title>
                    <Popover.Content></Popover.Content>
                </Popover>
            </div>
        );
    }
}

class RollSlider extends React.Component {
    constructor(props) {
        super(props);
    }

    makeRollSlider(min, max, onChange, getValue) {
        return (
            <div className="col-auto" style={{textAlign: 'center', margin: 'auto'}}
                  >
                Minimum Roll Value = {getValue()}&deg;  <br/>
                {min}&deg; <input  id="rollSlider" type="range" min={min} max={max} value={getValue()} className="slider" id="rollSlider" onChange={(val) => onChange(val)}
                        style={{margin: 'auto', verticalAlign: 'middle'}}/> {max}&deg;
                <br/>
                <div style={{ margin: "auto", width: "83%", backgroundImage: "linear-gradient(90deg, rgb(0, 255, 0), 55%, rgb(255, 255, 0), 66%, rgb(255, 0, 0))", height: "4px" }}></div>
            </div>
        );
    }

    render() {
        return this.makeRollSlider(this.props.rollSliderMin, this.props.rollSliderMax, this.props.rollSliderChanged, this.props.rollSliderValue)
    }
}

let rollPalette = paletteGenerator([[0, 255, 0], [255, 255, 0], [255, 0, 0]], [0, 26, 30]);

// Default chart layout, used for empty charts
const plotlyDefaultLayout = {
    title: 'Chart',
    showlegend: true,
    autosize: true,
    margin: {
        pad: 2
    },
    xaxis: {
        title: {
            text: 'Time (Seconds)',
            font: {
                family: 'sans serif',
                size: 18,
                color: '#000000'
            }
        },
    },
    yaxis: {
        title: {
            text: 'Self Defined Glide Angle',
            font: {
                family: 'sans serif',
                size: 18,
                color: '#000000'
            }
        }
    }
};
const glideAngleHistLayout = {
    title: 'Histogram of Glide Path Angles',
    bargap: 0.05,
    showlegend: true,
    autosize: true,
    margin: {
        pad: 10
    },
    xaxis: {title: 'Frequency'},
    yaxis: {title: 'Glide Path Angle'}
};
const deviationsPlotlyLayout = {
    title: 'Glide Path Deviations',
    showlegend: true,
    autosize: true,
    margin: {
        pad: 2
    },
    xaxis: {
        title: {
            text: 'Distance from Runway (ft.)',
            font: {
                family: 'sans serif',
                size: 18,
                color: '#000000'
            }
        },
        autorange: "reversed"
    },
    yaxis: {
        title: {
            text: 'Distance above Glidepath (ft.)',
            font: {
                family: 'sans serif',
                size: 18,
                color: '#000000'
            },
            range: [-100, 100]
        }
    }
};
const altitudePlotlyLayout = {
    title: 'Altitude vs. Distance to Runway',
    showlegend: true,
    autosize: true,
    margin: {
        pad: 2
    },
    xaxis: {
        title: {
            text: 'Distance from Runway (ft.)',
            font: {
                family: 'sans serif',
                size: 18,
                color: '#000000'
            }
        },
        autorange: "reversed"
    },
    yaxis: {
        title: {
            text: 'Alitude (AGL) (ft.)',
            font: {
                family: 'sans serif',
                size: 18,
                color: '#000000'
            }
        }
    }
};
const plotlyConfig = {responsive: true};

class TTFCard extends React.Component {
    constructor(props) {
        super(props);
        var date = new Date();
        this.state = {
            // The start of the date range that this.state.data corresponds to.
            // This will be null if and only if this.state.data is null
            dataStartDate: null,

            // The end of the date range that this.state.data corresponds to.
            // This will be null if and only if this.state.data is null
            dataEndDate: null,

            // Data is an object containing the following information:
            //     data = {
            //         airports: [ <list of airports that appear in the ttf list> ],
            //         ttfs: [ <list of turn to final objects> ],
            //     }
            // The turn to final objects are defined in the org.ngafid.flights.TurnToFinal::jsonify method.
            data: null,

            datesChanged: true,

            minRoll: 25.0,

            startYear: 2000,
            startMonth: 1,
            endYear: date.getFullYear(),
            endMonth: date.getMonth() + 1,
            startDate: "2000-01-01",
            startDateObject: this.parseDate("2000-01-01"),
            endDate: new String(date.getFullYear()) + "-" + (date.getMonth() + 1) + "-01",
            endDateObject: this.parseDate(new String(date.getFullYear()) + "-" + (date.getMonth() + 1) + "-01"),

            selectedAirport: airports[0],
            selectedRunway: "Any Runway",
            mapVisible: true,
            plotVisible: true,
            mapStyle: "Road",
            disableFetching: false,
            popups: [],
            // Style object for ttf lines. This is just a thin green line style.
            ttfStyle:
                new Style({
                    stroke: new Stroke({
                        color: "#00ff00",
                        width: 2.5
                    }),
                    image: new Circle({
                        radius: 5,
                        //fill: new Fill({color: [0, 0, 0, 255]}),
                        stroke: new Stroke({
                            color: [0, 0, 0, 0],
                            width: 2
                        })
                    })
                }),
        };

        var navbar = ReactDOM.render(
            <SignedInNavbar
                                 filterVisible={false}
                                 plotVisible={this.state.plotVisible}
                                 mapVisible={this.state.mapVisible}
                                 activePage="ttf"
                                 filterSelected={false}
                                 plotSelected={false}
                                 mapSelected={false}
                                 mapStyle={this.state.mapStyle}
                                 togglePlot={() => this.togglePlot()}
                                 toggleFilter={() => this.toggleFilter()}
                                 toggleMap={() => this.toggleMap()}
                                 mapSelectChanged={(style) => this.mapSelectChanged(style)}
                                 waitingUserCount={waitingUserCount}
                                 fleetManager={fleetManager}
                                 unconfirmedTailsCount={unconfirmedTailsCount}
                                 modifyTailsAccess={modifyTailsAccess}/>,
            document.querySelector('#navbar')
        );
        
        var thisTTF = this;
        // https://embed.plnkr.co/plunk/hhEAWk
        map.on('click', (event => this.openMapPopup(event)));
        map.on('moveend', (() => this.zoomChanged()));

        Plotly.newPlot('deviations-plot', [], deviationsPlotlyLayout, plotlyConfig);
        Plotly.newPlot('alt-plot', [], altitudePlotlyLayout, plotlyConfig);
        Plotly.newPlot('glide-angle-hist', [], glideAngleHistLayout, plotlyConfig);
    }

    openMapPopup(event) {
        closer.onclick = function() {
            overlay.setPosition(undefined);
            closer.blur();
            return false;
        };

        var coordinate = event.coordinate;
        var f = map.forEachFeatureAtPixel(event.pixel, function(feature, layer) { return feature; });
        console.log(f);
        if (f && f.get('type') == 'ttf') {
            var geometry = f.getGeometry();
            var coord = geometry.getCoordinates();
            console.log("selected feature " + f.get('name'));
            // window.open("/protected/flight?flight_id=" + f.get('name'), '_blank').focus();
            var ttfObject = f.get('ttf');
            var rect = document.getElementById('map-container').getBoundingClientRect();
            var popupProps = {
                pixel: event.pixel,
                flight_id: f.get('name'),
                map_container_rect: rect,
                approach_n: ttfObject.approachn
            };
            
            var outerHTML = document.createElement('div');
            var popup = ReactDOM.render(React.createElement(TTFMapPopup, popupProps), outerHTML);
            document.body.appendChild(outerHTML);
            this.state.popups.push(popup);
            outerHTML.setAttribute("id", "popover" + this.state.popups.length);
        } else {
            container.style.display = 'none';
        }     
    }

    zoomChanged() {
        for (var i in this.state.popups)
            this.state.popups[i].close();
        this.state.popups = [];
    }

    mapSelectChanged(style) {
        for (var i = 0, ii = layers.length; i < ii; ++i) {
            layers[i].setVisible(styles[i] === style);
        }

        this.setState({mapStyle: style});
    }

    showMap() {
        if (this.state.mapVisible) return;

        if ( !$("#map-toggle-button").hasClass("active") ) {
            $("#map-toggle-button").addClass("active");
            $("#map-toggle-button").attr("aria-pressed", true);
        }

        this.state.mapVisible = true;
        this.setState(this.state);

        $("#map-div").css("height", "50%");
        $("#map").show();

        $("#map").css("width", "100%");
    }

    hideMap() {
        if (!this.state.mapVisible) return;

        if ( $("#map-toggle-button").hasClass("active") ) {
            $("#map-toggle-button").removeClass("active");
            $("#map-toggle-button").attr("aria-pressed", false);
        }

        this.state.mapVisible = false;
        this.setState(this.state);

        $("#map").hide();

        $("#map").css("height", "0%");
    }

    toggleMap() {
        if (this.state.mapVisible) {
            this.hideMap();
        } else {
            this.showMap();
        }
    }

    toggleFilter() {
        if (this.state.filterVisible) {
            this.hideFilter();
        } else {
            this.showFilter();
        }
    }

    ttfToPoints(ttf) {
        // This will generate a list of points in the appropriate format and save it, so it is only computed once.
        // This should only ever get called once though so I guess this is unnecessary.
        if (typeof ttf.points !== 'undefined')
            return ttf.points;

        var points = [];
        ttf.points = points;

        for (var i = 0; i < ttf.lon.length; i++) {
            var point = fromLonLat([ttf.lon[i], ttf.lat[i]]);
            points.push(point);
        }

        return points;
    }

    rangeExtraction(list) {
        var len = list.length;
        if (len == 0) return [];

        var out = [];
        var i, j;

        for (i = 0; i < len; i = j + 1) {
            // find end of range
            for (var j = i + 1; j < len && list[j] == list[j-1] + 1; j++);
            j--;

            if (i == j) {
                // single number
                out.push([list[i]]);
            } else {
                out.push([list[i], list[j]]);
            }
        }

        return out;
    }

    rangesToFeatures(points, ranges) {
        // Ranges should be a list of ranges generated by rangeExtraction
        // the ranges generated by list extraction are either ranges (an array, [min, max]) or a single number in an array
        // [like_this]. We ignore the numbers that are by themselves since they cannot be connected to anything. In the
        // future perhaps it would be good to display them as dots.
        var features = [];
        for (var i = 0; i < ranges.length; i++) {
            let r = ranges[i];
            if (r.length == 1) continue; // There is no range, just a single number

            let min = r[0];
            let max = r[1];
            let rangePoints = [];

            // This is an inclusive range.
            for (var j = min; j <= max; j++)
                rangePoints.push(points[j]);

            features.push(new Feature({
                geometry: new LineString(rangePoints)
            }));
        }
        return features;
    }

    makeTTFLayers(ttf) {
        // This will generate a layer for this ttf if once hasn't been generated before, and add it to the map as a
        // hidden layer.
        if (typeof ttf.layer !== 'undefined')
            return ttf.layer;

        var points = this.ttfToPoints(ttf);

        // Create simple layer of the path.
        let trackingPoint =
            new Feature({
                geometry : new Point(points[0]),
                name: 'TrackingPoint'
            });
        var features = [
            new Feature({
                geometry: new LineString(points),
                name: ttf.flightId,
                type: 'ttf',
                ttf: ttf
            }),
            trackingPoint
        ];

        let layer = new VectorLayer({
            style: this.getStyle(ttf),
            source : new VectorSource({
                features: features,
            }),
        });

        ttf.layer = layer;
        map.addLayer(layer);
        layer.setVisible(false);
        ttf.enabled = true;
        return ttf.layer;
    }

    getStyle(ttf) {
        return new Style({
            stroke: new Stroke({
                // Assuming anything past 45 is really bad, so color will get increasingly
                // red as roll approaches 45
                color: rollPalette(ttf.maxRoll),
                width: 2.5
            }),
            image: new Circle({
                radius: 5,
                //fill: new Fill({color: [0, 0, 0, 255]}),
                stroke: new Stroke({
                    color: [0, 0, 0, 0],
                    width: 2
                })
            })
        })
    }

    updateDisplay() {
        if (this.state.data != null) {
            this.plotTTFs();
            this.plotCharts(this.state.data.ttfs) 
        }
    }

    plotTTFs() {
        for (const ttf of this.state.data.ttfs) {
            if (this.shouldDisplay(ttf))
                this.plotTTF(ttf);
            else
                this.hideTTF(ttf);
        }
    }
    plotTTF(ttf) {
        this.makeTTFLayers(ttf);

        let layer = ttf.layer;
        layer.setVisible(true);
    }

    hideTTF(ttf) {
        this.makeTTFLayers(ttf);
        let layer = ttf.layer;
        layer.setVisible(false);
    }

    // setMaximumRoll will move the roll slider to the maximum roll found in the set of ttfs so that
    // all flights will be displayed
    plotCharts(ttfs, setMaximumRoll = false) {
        console.log("Plotting charts");
        if (setMaximumRoll) {
            let minRoll = Math.min(...ttfs.map(ttf => ttf.maxRoll));
            this.onRollSliderChanged(minRoll, true);
        }

        let max = Math.max(...ttfs.map(ttf => ttf.AltMSL.length));
        let ttfIndex = -1;
        let curves = ttfs
            .map(ttf => {
                ttfIndex += 1;
                let glideAngle = ttf.selfDefinedGlideAngle;
                let alt = ttf.AltAGL;

                // This is what applies the roll filter
                if (this.shouldDisplay(ttf)) {
                    return { deviations: { name: ttf.flightId, x: ttf.distanceFromRunway, y: ttf.selfDefinedGlidePathDeviations, type: 'scatter', mode: 'lines' },
                             alt: { name: ttf.flightId, x: ttf.distanceFromRunway, y: alt, type: 'scatter', mode: 'lines' },
                             maxGlideAngle: glideAngle, _ttfIndex: ttfIndex };
                } else
                    return null;
            })
            .filter(curve => curve != null);
        let curveMap = {};
        for (var i = 0; i < curves.length; i++) {
            curveMap[i] = curves[i]._ttfIndex;
        }
        let deviationsCurves = curves.map(x => x.deviations);
        
        let devPlot = document.getElementById('deviations-plot');
        Plotly.newPlot('deviations-plot', deviationsCurves, deviationsPlotlyLayout, this.state.plotlyConfig);
        console.log(devPlot);

        let maxGlideAngles = curves.map(x => x.maxGlideAngle);
        console.log(maxGlideAngles);
        var glideAngleTrace = {
            type: 'histogram',
            y: maxGlideAngles,
            ybins: {
                end: 30, // Math.ceil(Math.max(...maxGlideAngles)),
                size: 1.0,
                start: 0, // Math.floor(Math.min(...maxGlideAngles)),
            }
        };

        Plotly.newPlot('glide-angle-hist', [glideAngleTrace], glideAngleHistLayout);
        let this_ = this;
        function onLegendClick(data) {
            // Disable this single item and re-draw map and charts.
            ttfs[curveMap[data.curveNumber]].enabled = !ttfs[curveMap[data.curveNumber]].enabled;
            Plotly.restyle('myDiv', update,[data.curveNumber]);
            this_.updateDisplay();
            return true;
        }

        function onLegendDoubleClick(data) {
            for (const ttf of ttfs) {
                ttf.enabled = false;
            }
            ttfs[curveMap[data.curveNumber]].enabled = true;
            this_.updateDisplay();
            return true;
        }

        // devPlot.on('plotly_legendclick', onLegendClick);

        let altCurves = curves.map(x => x.alt);
        Plotly.newPlot('alt-plot', altCurves, altitudePlotlyLayout, this.state.plotlyConfig);

        let airport = this.state.selectedAirport;
        let lat = runways[airport][0]['lat1'];
        let lon = runways[airport][0]['lon1'];
        let view = map.getView();
        view.setCenter(fromLonLat([lon, lat]));
        view.setZoom(12);
        map.setView(view);
    }

    getRunwayValue() {
        let runwayElement = this.state.selectedRunway;
        if (runwayElement == null)
            return null;
        else if (this.state.dataAirport != this.getAirportValue())
            return null;
        else
            return runwayElement.value;
    }

    getAirportValue() {
        return this.state.selectedAirport;
    }

    shouldDisplay(ttf) {
        let runway = this.state.selectedRunway;
        let should = ttf.enabled && (this.state.selectedRunway == null || ['Any Runway', ttf.runway.name].includes(runway))
                && this.dateWithinRange(this.parseDate(ttf.flightStartDate), this.state.startDateObject, this.state.endDateObject)
                && ttf.maxRoll >= this.state.minRoll;
        return should;
    }

    // For parsing dates in the format "yyyy-mm-dd hh:mm:ss" where the hh:mm:ss is optional
    parseDate(dateString) {
        if (dateString == null) return null;
        var pieces = dateString.split(" ");
        var yyyymmdd = pieces[0].split("-");
        var year = yyyymmdd[0];
        // Minus 1 because dates are zero indexed in javascript
        var month = parseInt(yyyymmdd[1]) - 1;
        var day = yyyymmdd[2];

        if (pieces.length > 1) {
            var hhmmss = pieces[1].split(":");
            var hour = hhmmss[0];
            var minutes = hhmmss[1];
            var seconds = hhmmss[2];

            return new Date(year, month, day, hour, minutes, seconds);
        } else
            return new Date(year, month, day);

        return new Date(year, month, day);
    }
    // These should all be date objects
    dateWithinRange(date, startDate, endDate) {
        return startDate <= date && date <= endDate;
    }

    onFetchClicked() {
        this.setDates();
        var startDateString = this.state.startDate;
        var endDateString = this.state.endDate;
        var airport = this.state.selectedAirport;
        var runway = this.state.selectedRunway;

        var submissionData = {
            startDate: startDateString,
            endDate: endDateString,
            airport: airport,
        };
        var thisTTF = this;

        var startDate = this.parseDate(startDateString);
        var endDate = this.parseDate(endDateString);

        // start and end dates for the data we already have
        // This won't encounter an error even if either of dataStartDate or dataEndDate is null.
        // If they're null then they won't get used because this.state.data is also null.
        // This feels like bad practice though.
        var dataStartDate = this.parseDate(this.state.dataStartDate);
        var dataEndDate = this.parseDate(this.state.dataEndDate);

        // This will show TTFs in the specified date range and hide every other TTF.
        // If the TTFs have already been plotted it will use the previous layer.
        function responseFunction(response) {
            var ttfs = [];
            var approachCounts = {};
            for (var i = 0; i < response.ttfs.length; i++) {
                let ttf = response.ttfs[i];
                if (!(ttf.flightId in approachCounts)) {
                    approachCounts[ttf.flightId] = 0;
                }
                ttf.approachn = ++approachCounts[ttf.flightId];
                ttfs.push(ttf);
                thisTTF.plotTTF(ttf);
            }

            thisTTF.setState({data: response})
            thisTTF.plotCharts(ttfs, true);
        }

        this.setState({ datesChanged: false })

        // If we already have some data, and the date range of that data contains the new date range,
        // then we don't need to fetch any more data - just turn off layers for data that not within the new range.
        // We should keep the old dataStartDate and dataEndDate though to preserve the entire range.
        if (this.state.data != null &&
            this.dateWithinRange(startDate, dataStartDate, dataEndDate) &&
            this.dateWithinRange(endDate,   dataStartDate, dataEndDate) &&
            airport == this.state.dataAirport) {
            console.log("Used cached data");
            for (const ttf of this.state.data.ttfs) {
                ttf.enabled = true;
            }
            this.updateDisplay();
        } else {
            // Remove all old layers
            if (this.state.data != null) {
                for (var ttf of this.state.data.ttfs) {
                    map.removeLayer(ttf.layer);
                }
            }
            this.setState({disableFetching: true})

            // Fetch the data.
            $.ajax({
                type: 'POST',
                url: '/protected/ttf',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("Fetched response " + response);

                    // store the dates we have fetched data for
                    thisTTF.state.dataAirport = submissionData.airport;
                    thisTTF.state.dataStartDate = submissionData.startDate;
                    thisTTF.state.dataEndDate = submissionData.endDate;

                    thisTTF.setState({disableFetching: false, dataEndDate: submissionData.endDate, dataStartDate: submissionData.startDate})
                    responseFunction(response);
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    console.log(textStatus);
                    console.log(errorThrown);
                    thisTTF.setState({disableFetching: false, datesChanged: false,})
                },
                async: true
            });
        }
    }

    getAirports() {
        if (this.state.data == null) {
            console.log("This shouldn't get called when this.state.data == null");
            return null;
        }

        // list of airport IATA codes
        var airports = [];

        for (let [name, ap] of Object.entries(this.state.data.airports)) {
            airports.push(ap.iataCode);
        }

        return airports;
    }

    onAirportFilterChanged(airport) {
        let iataCode = airport;
        this.setState({selectedAirport: iataCode, selectedRunway: "Any Runway", datesChanged: true,});
    }

    onUpdateStartYear(year) {
        this.state.startYear = year;
        this.setDates();
        this.forceUpdate();
    }

    onUpdateStartMonth(month) {
        this.state.startMonth = month;
        this.setDates();
        this.forceUpdate();
    }

    onUpdateEndYear(year) {
        this.state.endYear = year;
        this.setDates();
        this.forceUpdate();
    }

    onUpdateEndMonth(month) {
        this.state.endMonth = month;
        this.setDates();
        this.forceUpdate();
    }

    setDates() {
        function getDaysInMonth(year, month) {
            return new Date(year, month, 0).getDate()
        }
        let endDate;
        let startDate;

        this.state.datesChanged = true;
        if (this.state.startMonth < 10) {
            startDate = "" + this.state.startYear + "-0" + this.state.startMonth + "-01";
        } else {
            startDate = "" + this.state.startYear + "-" + this.state.startMonth + "-01";
        }
        let startDateObject = this.parseDate(startDate)
        let finalDayOfMonth = getDaysInMonth(this.state.endYear, this.state.endMonth)
        if (this.state.endMonth < 10) {
            endDate = "" + this.state.endYear + "-0" + this.state.endMonth + "-" + finalDayOfMonth;
        } else {
            endDate = "" + this.state.endYear + "-" + this.state.endMonth + "-" + finalDayOfMonth;
        }
        let endDateObject = this.parseDate(endDate);

        this.setState({
            dateChanged: true,
            endDate: endDate,
            startDate: startDate,
            endDateObject: endDateObject,
            startDateObject: startDateObject
        });

    }

    onRunwayFilterChanged(runway) {
        if (runway == null) {
            throw "getRunwayValue returned null even though the onRunwayFilterChanged event handler was called."
        }

        this.state.selectedRunway = runway;
        this.forceUpdate();

        if (this.state.data != null)
            for (const ttf of this.state.data.ttfs) {
                if (this.shouldDisplay(ttf))
                    this.plotTTF(ttf);
                else
                    this.hideTTF(ttf);
        }

    }

    onRollSliderChanged(value, override=false) {
        let slider = document.getElementById('rollSlider')
        if (!override) {
            this.setState({minRoll: slider.value});
            if (this.state.data != null) {
                this.updateDisplay();
            }
        } else {
            this.setState({minRoll: value});
        }
    }

    

    render() {
        let runwaySelect;

        if (this.state.data == null) {
            runwaySelect = ( <div> </div> );
        } else {
            let airport = this.state.dataAirport;
            runwaySelect = (
                <div>
                    <label htmlFor="runway">Runway: </label>
                    <Form.Control as="select" id="runway" selected="Any Runway" onChange={()=>this.onRunwayFilterChanged()}>
                        <option key="Any Runway"> Any Runway </option>
                        {
                            runways[airport].map(runway => ( <option key={runway.name}>{runway.name}</option> ))
                        }
                    </Form.Control>
                </div>
            );
        }
        let runwayList = runways[this.state.selectedAirport].map(runway => runway.name);
        let rollSlider =
            <RollSlider
                 rollSliderMin={0}
                 rollSliderMax={45}
                 rollSliderChanged={(val) => this.onRollSliderChanged(val)}
                 rollSliderValue={() => this.state.minRoll}
                />
        let turnToFinalHeaderComponents =
            <TurnToFinalHeaderComponents
                 airframes={[]}
                 airport={this.state.selectedAirport}
                 airports={airports}
                 airportChange={(airport) => this.onAirportFilterChanged(airport)}
                 runway={this.state.selectedRunway}
                 runways={runwayList}
                 runwayChange={(runway) => this.onRunwayFilterChanged(runway)}/>

        let form = (
            <div>
                <TimeHeader
                    name="Flight Filters"
                    disabled={this.state.disableFetching}
                    buttonContent={'Fetch'}
                    extraHeaderComponents={turnToFinalHeaderComponents}
                    extraRowComponents={rollSlider}
                    startYear={this.state.startYear}
                    startMonth={this.state.startMonth}
                    endYear={this.state.endYear}
                    endMonth={this.state.endMonth}
                    datesChanged={this.state.datesChanged}
                    dateChange={() => this.onFetchClicked()}
                    updateStartYear={(newStartYear) => this.onUpdateStartYear(newStartYear)}
                    updateStartMonth={(newStartMonth) => this.onUpdateStartMonth(newStartMonth)}
                    updateEndYear={(newEndYear) => this.onUpdateEndYear(newEndYear)}
                    updateEndMonth={(newEndMonth) => this.onUpdateEndMonth(newEndMonth)}/>
                <br/>
            </div>
        );

        return form;
    }
}

let ttfCard = null;
//check to see if flights has been defined already. unfortunately
//the navbar includes flights.js (bad design) for the navbar buttons
//to toggle flights, etc. So this is a bit of a hack.
ttfCard = ReactDOM.render(
    <TTFCard />,
    document.querySelector('#ttf-card')
);

console.log("rendered ttfCard!");

export { ttfCard };
