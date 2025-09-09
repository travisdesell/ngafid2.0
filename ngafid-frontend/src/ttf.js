import 'bootstrap';
import React from "react";
import { createRoot } from 'react-dom/client';
import Popover from 'react-bootstrap/Popover';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import Button from 'react-bootstrap/Button';
import ButtonGroup from 'react-bootstrap/ButtonGroup';

import {closer, container, initializeMap, layers, map, overlay, styles} from "./map.js";
import {TimeHeader, TurnToFinalHeaderComponents} from "./time_header.js";
import SignedInNavbar from "./signed_in_navbar.js";
import {paletteGenerator} from "./map_utils.js";
import {fromLonLat} from 'ol/proj.js';
import {Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Circle, Stroke, Style} from 'ol/style.js';

import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';

import Plotly from 'plotly.js';

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
        const style = {
            top: this.props.pixel[1] + this.props.map_container_rect.y - 10,
            left: this.props.pixel[0] + this.props.map_container_rect.x + 15
        };

        if (!this.state.visible) {
            return null;
        }

        return (
            <div style={{height: 120}}>
                <Popover
                    style={style}
                >
                    <Popover.Title as="h2" style={{display: "flex"}}><Row>
                        <Col>Flight {this.props.flight_id} Approach {this.props.approach_n}</Col>
                        <Col sm="auto"></Col>
                        <Col>
                            <ButtonGroup>
                                <Button variant="outline-info"
                                        href={`/protected/flights?flight_id=${  this.props.flight_id}`} target="_blank">
                                    <i className="fa fa-plane p-1"></i>
                                </Button>
                                <Button onClick={() => this.close()} data-bs-toggle="button" variant="outline-danger">
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
                {min}&deg;
                <input
                    id="rollSlider" type="range" min={min} max={max} value={getValue()} className="slider"
                    onChange={(val) => onChange(val)}
                    style={{margin: 'auto', verticalAlign: 'middle'}}
                /> {max}&deg;
                <br/>
                <div style={{
                    margin: "auto",
                    width: "83%",
                    backgroundImage: "linear-gradient(90deg, rgb(0, 255, 0), 55%, rgb(255, 255, 0), 66%, rgb(255, 0, 0))",
                    height: "4px"
                }}></div>
            </div>
        );
    }

    render() {
        return this.makeRollSlider(this.props.rollSliderMin, this.props.rollSliderMax, this.props.rollSliderChanged, this.props.rollSliderValue);
    }
}

const rollPalette = paletteGenerator([[0, 255, 0], [255, 255, 0], [255, 0, 0]], [0, 26, 30]);


class TTFCard extends React.Component {

    constructor(props) {
        super(props);
        const date = new Date();
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

            startYear: date.getFullYear(),
            startMonth: 1,
            endYear: date.getFullYear(),
            endMonth: date.getMonth() + 1,
            startDate: "2000-01-01",
            startDateObject: this.parseDate("2000-01-01"),
            endDate: `${new String(date.getFullYear())  }-${  date.getMonth() + 1  }-01`,
            endDateObject: this.parseDate(`${new String(date.getFullYear())  }-${  date.getMonth() + 1  }-01`),

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

        const navbarContainer = document.querySelector('#navbar');
        const navbarRoot = createRoot(navbarContainer);
        navbarRoot.render(
            <SignedInNavbar
                filterVisible={false}
                showPlotButton={false}
                disableMapButton={true}
                mapVisible={this.state.mapVisible}
                activePage="ttf"
                filterSelected={false}
                mapStyle={this.state.mapStyle}
                togglePlot={() => this.togglePlot()}
                toggleFilter={() => this.toggleFilter()}
                toggleMap={() => this.toggleMap()}
                mapSelectChanged={(style) => this.mapSelectChanged(style)}
                waitingUserCount={waitingUserCount}
                fleetManager={fleetManager}
                unconfirmedTailsCount={unconfirmedTailsCount}
                modifyTailsAccess={modifyTailsAccess}
                darkModeOnClickAlt={() => {
                    this.displayPlots();
                }}
            />
        );

        // https://embed.plnkr.co/plunk/hhEAWk
        map.on('click', (event => this.openMapPopup(event)));
        map.on('moveend', (() => this.zoomChanged()));

    }

    componentDidMount() {

        //Display plots
        this.displayPlots();

        //Update map layers to use default style
        this.mapSelectChanged(this.state.mapStyle);

    }

    openMapPopup(event) {
        closer.onclick = function () {
            overlay.setPosition(undefined);
            closer.blur();
            return false;
        };

        const f = map.forEachFeatureAtPixel(event.pixel, function (feature) {
            return feature;
        });

        console.log(f);
        if (f && f.get('type') == 'ttf') {

            console.log(`selected feature ${  f.get('name')}`);
            // window.open("/protected/flight?flight_id=" + f.get('name'), '_blank').focus();
            const ttfObject = f.get('ttf');
            const rect = document.getElementById('map-container').getBoundingClientRect();
            const popupProps = {
                pixel: event.pixel,
                flight_id: f.get('name'),
                map_container_rect: rect,
                approach_n: ttfObject.approachn
            };

            const outerHTML = document.createElement('div');
            createRoot(outerHTML).render(React.createElement(TTFMapPopup, popupProps));
            document.body.appendChild(outerHTML);
            this.state.popups.push(outerHTML);

            outerHTML.setAttribute("id", `popover${  this.state.popups.length}`);

        } else {

            container.style.display = 'none';

        }
    }

    zoomChanged() {

        for (const i in this.state.popups) {
            this.state.popups[i].close();
        }

        this.setState({ popups: [] });
    }

    mapSelectChanged(style) {
        for (let i = 0, ii = layers.length; i < ii; ++i) {
            layers[i].setVisible(styles[i] === style);
        }

        this.setState({mapStyle: style});
    }

    showMap() {
        if (this.state.mapVisible) return;

        if (!$("#map-toggle-button").hasClass("active")) {
            $("#map-toggle-button").addClass("active");
            $("#map-toggle-button").attr("aria-pressed", true);
        }

        this.setState({ mapVisible: true });

        $("#map-div").css("height", "50%");
        $("#map").show();

        $("#map").css("width", "100%");
    }

    hideMap() {
        if (!this.state.mapVisible) return;

        if ($("#map-toggle-button").hasClass("active")) {
            $("#map-toggle-button").removeClass("active");
            $("#map-toggle-button").attr("aria-pressed", false);
        }

        this.setState({ mapVisible: false });

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

        const points = [];
        ttf.points = points;

        for (let i = 0; i < ttf.lon.length; i++) {
            const point = fromLonLat([ttf.lon[i], ttf.lat[i]]);
            points.push(point);
        }

        return points;
    }

    rangeExtraction(list) {

        const len = list.length;
        if (len == 0)
            return [];

        const out = [];
        let i, j;

        for (i = 0; i < len; i = j + 1) {
            
            //Find end of range
            for (let j = i + 1; j < len && list[j] == list[j - 1] + 1; j++) { /* ... */}
            j--;

            //Single number
            if (i == j) {
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
        const features = [];
        for (let i = 0; i < ranges.length; i++) {
            const r = ranges[i];
            if (r.length == 1) continue; // There is no range, just a single number

            const min = r[0];
            const max = r[1];
            const rangePoints = [];

            // This is an inclusive range.
            for (let j = min; j <= max; j++)
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

        const points = this.ttfToPoints(ttf);

        // Create simple layer of the path.
        const trackingPoint =
            new Feature({
                geometry: new Point(points[0]),
                name: 'TrackingPoint'
            });
        const features = [
            new Feature({
                geometry: new LineString(points),
                name: ttf.flightId,
                type: 'ttf',
                ttf: ttf
            }),
            trackingPoint
        ];

        const layer = new VectorLayer({
            style: this.getStyle(ttf),
            source: new VectorSource({
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
        });
    }

    updateDisplay() {
        if (this.state.data != null) {
            this.plotTTFs();
            this.plotCharts(this.state.data.ttfs);
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

        const layer = ttf.layer;
        layer.setVisible(true);
    }

    hideTTF(ttf) {
        this.makeTTFLayers(ttf);
        const layer = ttf.layer;
        layer.setVisible(false);
    }

    // setMaximumRoll will move the roll slider to the maximum roll found in the set of ttfs so that
    // all flights will be displayed
    plotCharts(ttfs, setMaximumRoll = false) {
        console.log("Plotting charts");
        if (setMaximumRoll) {
            const minRoll = Math.min(...ttfs.map(ttf => ttf.maxRoll));
            this.onRollSliderChanged(minRoll, true);
        }

        let ttfIndex = -1;
        const curves = ttfs
            .map(ttf => {
                ttfIndex += 1;
                const glideAngle = ttf.selfDefinedGlideAngle;
                const alt = ttf.AltAGL;

                // This is what applies the roll filter
                if (this.shouldDisplay(ttf)) {
                    return {
                        deviations: {
                            name: ttf.flightId,
                            x: ttf.distanceFromRunway,
                            y: ttf.selfDefinedGlidePathDeviations,
                            type: 'scatter',
                            mode: 'lines'
                        },
                        alt: {name: ttf.flightId, x: ttf.distanceFromRunway, y: alt, type: 'scatter', mode: 'lines'},
                        maxGlideAngle: glideAngle, _ttfIndex: ttfIndex
                    };
                } else
                    return null;
            })
            .filter(curve => curve != null);
        const curveMap = {};
        for (let i = 0; i < curves.length; i++) {
            curveMap[i] = curves[i]._ttfIndex;
        }
        const deviationsCurves = curves.map(x => x.deviations);

        const devPlot = document.getElementById('deviations-plot');
        Plotly.newPlot('deviations-plot', deviationsCurves, this.deviationsPlotlyLayout, this.state.plotlyConfig);
        console.log(devPlot);

        const maxGlideAngles = curves.map(x => x.maxGlideAngle);
        console.log(maxGlideAngles);
        const glideAngleTrace = {
            type: 'histogram',
            y: maxGlideAngles,
            ybins: {
                end: 30, // Math.ceil(Math.max(...maxGlideAngles)),
                size: 1.0,
                start: 0, // Math.floor(Math.min(...maxGlideAngles)),
            }
        };

        Plotly.newPlot('glide-angle-hist', [glideAngleTrace], this.glideAngleHistLayout);

        const altCurves = curves.map(x => x.alt);
        Plotly.newPlot('alt-plot', altCurves, this.altitudePlotlyLayout, this.state.plotlyConfig);

        const airport = this.state.selectedAirport;
        const lat = runways[airport][0]['lat1'];
        const lon = runways[airport][0]['lon1'];
        const view = map.getView();
        view.setCenter(fromLonLat([lon, lat]));
        view.setZoom(12);
        map.setView(view);
    }

    getRunwayValue() {
        const runwayElement = this.state.selectedRunway;
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
        const runway = this.state.selectedRunway;
        const should = ttf.enabled && (this.state.selectedRunway == null || ['Any Runway', ttf.runway.name].includes(runway))
            && this.dateWithinRange(this.parseDate(ttf.flightStartDate), this.state.startDateObject, this.state.endDateObject)
            && ttf.maxRoll >= this.state.minRoll;
        return should;
    }

    // For parsing dates in the format "yyyy-mm-dd hh:mm:ss" where the hh:mm:ss is optional
    parseDate(dateString) {
        
        if (dateString == null)
            return null;
        const pieces = dateString.split(" ");
        const yyyymmdd = pieces[0].split("-");
        const year = yyyymmdd[0];
        // Minus 1 because dates are zero indexed in javascript
        const month = parseInt(yyyymmdd[1]) - 1;
        const day = yyyymmdd[2];

        if (pieces.length > 1) {

            const hhmmss = pieces[1].split(":");
            const hour = hhmmss[0];
            const minutes = hhmmss[1];
            const seconds = hhmmss[2];

            return new Date(year, month, day, hour, minutes, seconds);

        }

        return new Date(year, month, day);
    }

    // These should all be date objects
    dateWithinRange(date, startDate, endDate) {
        return startDate <= date && date <= endDate;
    }

    onFetchClicked() {

        this.setDates();
        const startDateString = this.state.startDate;
        const endDateString = this.state.endDate;
        const airport = this.state.selectedAirport;

        const submissionData = {
            startDate: startDateString,
            endDate: endDateString,
            airport: airport,
        };

        const startDate = this.parseDate(startDateString);
        const endDate = this.parseDate(endDateString);

        /*
            Start and end dates for the data we already have.

            This won't encounter an error even if either of dataStartDate or dataEndDate is null.
            If they're null then they won't get used because this.state.data is also null.
            
            This feels like bad practice though.
        */
        const dataStartDate = this.parseDate(this.state.dataStartDate);
        const dataEndDate = this.parseDate(this.state.dataEndDate);

        /*
            This will show TTFs in the specified date range and hide every other TTF.
            If the TTFs have already been plotted it will use the previous layer.
        */
        function responseFunction(response) {

            const ttfs = [];
            const approachCounts = {};
            for (let i = 0; i < response.ttfs.length; i++) {

                const ttf = response.ttfs[i];
                if (!(ttf.flightId in approachCounts))
                    approachCounts[ttf.flightId] = 0;
                
                ttf.approachn = ++approachCounts[ttf.flightId];
                ttfs.push(ttf);
                this.plotTTF(ttf);
            }

            this.setState({data: response});
            this.plotCharts(ttfs, true);

        }

        this.setState({datesChanged: false});

        /*
            If we already have some data, and the date range of that data contains the new date range,
            then we don't need to fetch any more data - just turn off layers for data that not within the new range.
            
            We should keep the old dataStartDate and dataEndDate though to preserve the entire range.
        */
        if  (
            this.state.data != null
            && this.dateWithinRange(startDate, dataStartDate, dataEndDate)
            && this.dateWithinRange(endDate, dataStartDate, dataEndDate)
            && airport == this.state.dataAirport) {
                
            console.log("Used cached data");
            for (const ttf of this.state.data.ttfs) {
                ttf.enabled = true;
            }

            this.updateDisplay();

        } else {

            //Remove all old layers
            if (this.state.data != null) {

                for (const ttf of this.state.data.ttfs) {
                    map.removeLayer(ttf.layer);
                }

            }
            this.setState({disableFetching: true});

            // Fetch the data.
            $.ajax({
                type: 'GET',
                url: '/api/flight/turn-to-final',
                data: submissionData,
                async: true,
                success: (response) => {
                    console.log("Fetched response: ", response);

                    // store the dates we have fetched data for
                    this.setState({
                        disableFetching: false,
                        dataAirport: submissionData.airport,
                        dataStartDate: submissionData.startDate,
                        dataEndDate: submissionData.endDate
                    }, () => {
                        responseFunction(response);
                    });
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    console.log(textStatus);
                    console.log(errorThrown);
                    this.setState({disableFetching: false, datesChanged: false,});
                },
            });
        }
    }

    getAirports() {

        if (this.state.data == null) {
            console.log("This shouldn't get called when this.state.data == null");
            return null;
        }

        //list of airport IATA codes
        const airports = [];

        for (const [ap] of Object.entries(this.state.data.airports)) {
            airports.push(ap.iataCode);
        }

        return airports;

    }

    onAirportFilterChanged(airport) {
        const iataCode = airport;
        this.setState({selectedAirport: iataCode, selectedRunway: "Any Runway", datesChanged: true,});
    }

    onUpdateStartYear(year) {

        this.setState({ startYear: year }, () => {
            this.setDates();
            this.forceUpdate();
        });

    }

    onUpdateStartMonth(month) {

        this.setState({ startMonth: month }, () => {
            this.setDates();
            this.forceUpdate();
        });

    }

    onUpdateEndYear(year) {
    
        this.setState({ endYear: year }, () => {
            this.setDates();
            this.forceUpdate();
        });

    }

    onUpdateEndMonth(month) {

        this.setState({ endMonth: month }, () => {
            this.setDates();
            this.forceUpdate();
        });

    }

    setDates() {
        function getDaysInMonth(year, month) {
            return new Date(year, month, 0).getDate();
        }

        let endDate;
        let startDate;

        this.setState({ datesChanged: true });

        if (this.state.startMonth < 10)
            startDate = `${  this.state.startYear  }-0${  this.state.startMonth  }-01`;
        else
            startDate = `${  this.state.startYear  }-${  this.state.startMonth  }-01`;
        
        const startDateObject = this.parseDate(startDate);
        const finalDayOfMonth = getDaysInMonth(this.state.endYear, this.state.endMonth);

        if (this.state.endMonth < 10)
            endDate = `${  this.state.endYear  }-0${  this.state.endMonth  }-${  finalDayOfMonth}`;
        else
            endDate = `${  this.state.endYear  }-${  this.state.endMonth  }-${  finalDayOfMonth}`;

        const endDateObject = this.parseDate(endDate);

        this.setState({
            dateChanged: true,
            endDate: endDate,
            startDate: startDate,
            endDateObject: endDateObject,
            startDateObject: startDateObject
        });

    }

    onRunwayFilterChanged(runway) {

        if (runway == null)
            throw "getRunwayValue returned null even though the onRunwayFilterChanged event handler was called.";

        this.setState({ selectedRunway: runway }, () => {
            this.forceUpdate();
        });

        if (this.state.data != null)
            for (const ttf of this.state.data.ttfs) {
                if (this.shouldDisplay(ttf))
                    this.plotTTF(ttf);
                else
                    this.hideTTF(ttf);
            }

    }

    onRollSliderChanged(value, override = false) {

        const slider = document.getElementById('rollSlider');
        if (!override) {

            this.setState({minRoll: slider.value});
            if (this.state.data != null)
                this.updateDisplay();
            
        } else {

            this.setState({minRoll: value});

        }

    }

    displayPlots() {

        const docStyles = getComputedStyle(document.documentElement);
        const plotBgColor = docStyles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = docStyles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = docStyles.getPropertyValue("--c_plotly_grid").trim();

        this.glideAngleHistLayout = {
            title: 'Histogram of Glide Path Angles',
            bargap: 0.05,
            showlegend: true,
            autosize: true,
            margin: {
                pad: 10
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            xaxis: {
                title: 'Frequency',
                gridcolor: plotGridColor
            },
            yaxis: {
                title: 'Glide Path Angle',
                gridcolor: plotGridColor
            }
        };
        this.deviationsPlotlyLayout = {
            title: 'Glide Path Deviations',
            showlegend: true,
            autosize: true,
            margin: {
                pad: 2
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            xaxis: {
                title: 'Distance from Runway (ft.)',
                gridcolor: plotGridColor,
                autorange: "reversed"
            },
            yaxis: {
                title: 'Distance above Glidepath (ft.)',
                gridcolor: plotGridColor,
                range: [-100, 100]
            }
        };
        this.altitudePlotlyLayout = {
            title: 'Altitude vs. Distance to Runway',
            showlegend: true,
            autosize: true,
            margin: {
                pad: 2
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            xaxis: {
                title: 'Distance from Runway (ft.)',
                gridcolor: plotGridColor,
                autorange: "reversed"
            },
            yaxis: {
                title: 'Alitude (AGL) (ft.)',
                gridcolor: plotGridColor,
            }
        };
        const plotlyConfig = {responsive: true};


        Plotly.newPlot('deviations-plot', [], this.deviationsPlotlyLayout, plotlyConfig);
        Plotly.newPlot('alt-plot', [], this.altitudePlotlyLayout, plotlyConfig);
        Plotly.newPlot('glide-angle-hist', [], this.glideAngleHistLayout, plotlyConfig);

    }

    render() {

        const runwayList = runways[this.state.selectedAirport].map(runway => runway.name);
        const rollSlider =
            <RollSlider
                rollSliderMin={0}
                rollSliderMax={45}
                rollSliderChanged={(val) => this.onRollSliderChanged(val)}
                rollSliderValue={() => this.state.minRoll}
            />;
        const turnToFinalHeaderComponents =
            <TurnToFinalHeaderComponents
                airframes={[]}
                airport={this.state.selectedAirport}
                airports={airports}
                airportChange={(airport) => this.onAirportFilterChanged(airport)}
                runway={this.state.selectedRunway}
                runways={runwayList}
                runwayChange={(runway) => this.onRunwayFilterChanged(runway)}/>;

        const form = (
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
                    updateEndMonth={(newEndMonth) => this.onUpdateEndMonth(newEndMonth)}
                />
            </div>
        );

        return form;
    }
}

const ttfContainer = document.querySelector('#ttf-card');
const root = createRoot(ttfContainer);
root.render(<TTFCard/>);

console.log("Rendered ttfCard!");

export {TTFCard};
