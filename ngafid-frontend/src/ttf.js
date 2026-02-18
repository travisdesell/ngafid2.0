import 'bootstrap';
import React from "react";
import Button from 'react-bootstrap/Button';
import ButtonGroup from 'react-bootstrap/ButtonGroup';
import Col from 'react-bootstrap/Col';
import Popover from 'react-bootstrap/Popover';
import Row from 'react-bootstrap/Row';
import { createRoot } from 'react-dom/client';

import { Vector as VectorLayer } from 'ol/layer.js';
import { fromLonLat } from 'ol/proj.js';
import { Vector as VectorSource } from 'ol/source.js';
import { Circle, Stroke, Style } from 'ol/style.js';
import { closer, container, initializeMap, layers, map, overlay, styles } from "./map.js";
import { paletteGenerator } from "./map_utils.js";
import SignedInNavbar from "./signed_in_navbar.js";
import { TimeHeader, TurnToFinalHeaderComponents } from "./time_header.js";

import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';

import Plotly from 'plotly.js';

const ROLL_THRESHOLDS = {
    Min: 0,
    Default: 0,
    Dangerous: 26,
    MaxSoft: 30,
    MaxHard: 45
};

class TTFMapPopup extends React.Component {

    constructor(props) {
        super(props);
        console.log("Creating TTFMapPopup");
        this.state = {
            visible: true
        };
    }

    show() {
        this.setState({ visible: true });
    }

    close() {
        this.setState({ visible: false });
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
            <div style={{ height: 120 }}>
                <Popover
                    style={style}
                >
                    <Popover.Title as="h2" style={{ display: "flex" }}><Row>
                        <Col>Flight {this.props.flight_id} Approach {this.props.approach_n}</Col>
                        <Col sm="auto"></Col>
                        <Col>
                            <ButtonGroup>
                                <Button variant="outline-info"
                                    href={`/protected/flights?flight_id=${this.props.flight_id}`} target="_blank">
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
            <div className="col-auto min-w-72" style={{ textAlign: 'center', margin: 'auto' }}
            >

                {/* Minimum Roll Value */}
                <span>
                    Minimum Roll Value = {getValue()}&deg;
                </span>

                {/* Slider Range Values */}
                <div className="w-full flex-row flex gap-2 h-6">
                    <span>{min}&deg;</span>
                    <hr className="w-full bg-(--c_border_alt)" />
                    <span>{max}&deg;</span>
                </div>

                {/* Slider Input */}
                <input
                    id="rollSlider" type="range" min={min} max={max} value={getValue()} className="slider w-full"
                    onChange={(val) => onChange(val)}
                    style={{ margin: 'auto', verticalAlign: 'middle' }}
                />

                {/* Gradient Bar */}
                <div className="w-full" style={{
                    margin: "auto",
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

const rollPalette = paletteGenerator([[0, 255, 0], [255, 255, 0], [255, 0, 0]], [ROLL_THRESHOLDS.Min, ROLL_THRESHOLDS.Dangerous, ROLL_THRESHOLDS.MaxSoft]);


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

            minRoll: ROLL_THRESHOLDS.Default,

            startYear: date.getFullYear(),
            startMonth: 1,
            endYear: date.getFullYear(),
            endMonth: date.getMonth() + 1,
            startDate: "2000-01-01",
            startDateObject: this.parseDate("2000-01-01"),
            endDate: `${new String(date.getFullYear())}-${date.getMonth() + 1}-01`,
            endDateObject: this.parseDate(`${new String(date.getFullYear())}-${date.getMonth() + 1}-01`),

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

    }

    componentDidMount() {

        initializeMap();
        if (map && !map.get('ttfHandlersAttached')) {
            // https://embed.plnkr.co/plunk/hhEAWk
            map.on('click', (event => this.openMapPopup(event)));
            map.on('moveend', (() => this.zoomChanged()));
            map.set('ttfHandlersAttached', true);
        }

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


        console.log("Handling map click event with feature: ", f ?? "(No feature)");
        if (f && f.get('type') == 'ttf') {

            console.log(`selected feature ${f.get('name')}`);
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

            outerHTML.setAttribute("id", `popover${this.state.popups.length}`);

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

        this.setState({ mapStyle: style });
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

        const lats = ttf.lat ?? ttf.latitude;
        const lons = ttf.lon ?? ttf.longitude;
        if (!Array.isArray(lats) || !Array.isArray(lons)) {
            console.warn("TTF missing lat/lon arrays", ttf);
            return [];
        }

        const points = [];
        ttf.points = points;

        for (let i = 0; i < lons.length; i++) {
            const lon = Number(lons[i]);
            const lat = Number(lats[i]);
            if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
                continue;
            }
            const point = fromLonLat([lon, lat]);
            points.push(point);
        }

        if (points.length === 0) {
            console.warn("TTF has no valid points", {
                flightId: ttf.flightId,
                latsLength: lats.length,
                lonsLength: lons.length
            });
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
            for (let j = i + 1; j < len && list[j] == list[j - 1] + 1; j++) { /* ... */ }
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
        if (points.length === 0) {
            return null;
        }

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
                // Assuming anything past 45 is really bad, so color will get increasingly red as roll approaches 45
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
        const layer = this.makeTTFLayers(ttf);
        if (!layer) {
            return;
        }
        layer.setVisible(true);
    }

    hideTTF(ttf) {
        const layer = this.makeTTFLayers(ttf);
        if (!layer) {
            return;
        }
        layer.setVisible(false);
    }

    // setMaximumRoll will move the roll slider to the maximum roll found in the set of ttfs so that
    // all flights will be displayed
    plotCharts(ttfs, setMaximumRoll = false) {

        console.log("Plotting charts");

        if (setMaximumRoll) {

            let minRoll = Math.min(...ttfs.map(ttf => ttf.maxRoll));
            minRoll = Math.max(minRoll, ROLL_THRESHOLDS.Min);
            if (!isFinite(minRoll)) {
                console.warn("No valid roll values found in ttfs, defaulting to 0");
                minRoll = 0;
            }

            this.onRollSliderChanged(minRoll, true);
        }

        let ttfIndex = -1;
        const curves = ttfs
            .map(ttf => {
                ttfIndex += 1;
                const glideAngle = ttf.selfDefinedGlideAngle;
                const alt = ttf.AltAGL;
                const traceName = ttf.flightId ?? ttf.flight_id ?? "(unknown)";

                // This is what applies the roll filter
                if (this.shouldDisplay(ttf)) {
                    return {
                        deviations: {
                            name: traceName,
                            x: ttf.distanceFromRunway,
                            y: ttf.selfDefinedGlidePathDeviations,
                            type: 'scatter',
                            mode: 'lines'
                        },
                        alt: { name: traceName, x: ttf.distanceFromRunway, y: alt, type: 'scatter', mode: 'lines' },
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
        console.log("Deviations Plot: ", devPlot);

        const maxGlideAngles = curves.map(x => x.maxGlideAngle);
        console.log("Max Glide Angles: ", maxGlideAngles);
        const glideAngleTrace = {
            type: 'histogram',
            y: maxGlideAngles,
            ybins: {
                end: ROLL_THRESHOLDS.MaxSoft,
                size: 1.0,
                start: 0,
            }
        };

        Plotly.newPlot('glide-angle-hist', [glideAngleTrace], this.glideAngleHistLayout);

        const altCurves = curves.map(x => x.alt);
        Plotly.newPlot('alt-plot', altCurves, this.altitudePlotlyLayout, this.state.plotlyConfig);
    }

    updateMapViewForTTFs(ttfs, airports) {
        const view = map.getView();
        let extent = null;
        for (const ttf of ttfs) {
            const points = this.ttfToPoints(ttf);
            for (const point of points) {
                if (!extent) {
                    extent = [point[0], point[1], point[0], point[1]];
                } else {
                    extent[0] = Math.min(extent[0], point[0]);
                    extent[1] = Math.min(extent[1], point[1]);
                    extent[2] = Math.max(extent[2], point[0]);
                    extent[3] = Math.max(extent[3], point[1]);
                }
            }
        }

        if (extent) {
            view.fit(extent, { padding: [40, 40, 40, 40], maxZoom: 15 });
            return;
        }

        if (airports) {
            const airport = airports[this.state.selectedAirport];
            if (airport && Number.isFinite(airport.longitude) && Number.isFinite(airport.latitude)) {
                view.setCenter(fromLonLat([airport.longitude, airport.latitude]));
                view.setZoom(12);
            }
        }
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
        const flightDate = this.parseDate(ttf.flightStartDate);
        const withinDateRange = flightDate
            ? this.dateWithinRange(flightDate, this.state.startDateObject, this.state.endDateObject)
            : true;
        const should = ttf.enabled && (this.state.selectedRunway == null || ['Any Runway', ttf.runway.name].includes(runway))
            && withinDateRange
            && ttf.maxRoll >= this.state.minRoll;
        return should;
    }

    // For parsing dates in the format "yyyy-mm-dd hh:mm:ss" where the hh:mm:ss is optional
    parseDate(dateString) {

        if (dateString == null)
            return null;
        const parsed = new Date(dateString);

        // Date string already in a format that Date.parse can handle, use it
        if (!Number.isNaN(parsed.getTime()))
            return parsed;

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
        const responseFunction = (response) => {

            const ttfs = [];
            const approachCounts = {};
            for (let i = 0; i < response.ttfs.length; i++) {

                const ttf = response.ttfs[i];
                // Normalize field names from backend JSON to frontend expectations.
                if (ttf.flightId === undefined && ttf.flight_id !== undefined)
                    ttf.flightId = ttf.flight_id;

                if (ttf.lat === undefined && Array.isArray(ttf.latitude))
                    ttf.lat = ttf.latitude;
                
                if (ttf.lon === undefined && Array.isArray(ttf.longitude))
                    ttf.lon = ttf.longitude;
                
                if (!(ttf.flightId in approachCounts))
                    approachCounts[ttf.flightId] = 0;

                ttf.approachn = ++approachCounts[ttf.flightId];
                if (!ttf.flightId)
                    ttf.flightId = `Approach ${ttf.approachn}`;
                
                ttfs.push(ttf);
                this.plotTTF(ttf);
            }

            this.setState({ data: response });
            this.plotCharts(ttfs, true);
            this.updateMapViewForTTFs(ttfs, response.airports);

        };

        this.setState({ datesChanged: false });

        /*
            If we already have some data, and the date range of that data contains the new date range,
            then we don't need to fetch any more data - just turn off layers for data that not within the new range.
            
            We should keep the old dataStartDate and dataEndDate though to preserve the entire range.
        */
        if (
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
            this.setState({ disableFetching: true });

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
                    console.error("Error fetching TTF data: ", jqXHR.responseText);
                    this.setState({ disableFetching: false, datesChanged: false, });
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
        this.setState({ selectedAirport: iataCode, selectedRunway: "Any Runway", datesChanged: true, });
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
            startDate = `${this.state.startYear}-0${this.state.startMonth}-01`;
        else
            startDate = `${this.state.startYear}-${this.state.startMonth}-01`;

        const startDateObject = this.parseDate(startDate);
        const finalDayOfMonth = getDaysInMonth(this.state.endYear, this.state.endMonth);

        if (this.state.endMonth < 10)
            endDate = `${this.state.endYear}-0${this.state.endMonth}-${finalDayOfMonth}`;
        else
            endDate = `${this.state.endYear}-${this.state.endMonth}-${finalDayOfMonth}`;

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

            this.setState({ minRoll: slider.value });
            if (this.state.data != null)
                this.updateDisplay();

        } else {

            this.setState({ minRoll: value });

        }

    }

    displayPlots() {

        const docStyles = getComputedStyle(document.documentElement);
        const plotBgColor = docStyles.getPropertyValue("--c_plotly_bg").trim() || "transparent";
        const plotTextColor = docStyles.getPropertyValue("--c_plotly_text").trim() || "#e6e6e6";
        const plotGridColor = docStyles.getPropertyValue("--c_plotly_grid").trim() || "rgba(255,255,255,0.15)";
        const titleFont = { color: plotTextColor, size: 16 };
        const legendFont = { color: plotTextColor, size: 11 };

        this.glideAngleHistLayout = {
            title: { text: 'Histogram of Glide Path Angles', font: titleFont },
            bargap: 0.05,
            showlegend: false,
            autosize: true,
            margin: {
                t: 40,
                l: 60,
                r: 20,
                b: 40,
                pad: 10
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            legend: {
                font: legendFont
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
            title: { text: 'Glide Path Deviations', font: titleFont },
            showlegend: false,
            autosize: true,
            margin: {
                t: 40,
                l: 60,
                r: 20,
                b: 40,
                pad: 2
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            legend: {
                font: legendFont
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
            title: { text: 'Altitude vs. Distance to Runway', font: titleFont },
            showlegend: false,
            autosize: true,
            margin: {
                t: 40,
                l: 60,
                r: 20,
                b: 40,
                pad: 2
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            legend: {
                font: legendFont
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
        const plotlyConfig = { responsive: true };


        Plotly.newPlot('deviations-plot', [], this.deviationsPlotlyLayout, plotlyConfig);
        Plotly.newPlot('alt-plot', [], this.altitudePlotlyLayout, plotlyConfig);
        Plotly.newPlot('glide-angle-hist', [], this.glideAngleHistLayout, plotlyConfig);

    }

    render() {

        const runwayList = runways[this.state.selectedAirport].map(runway => runway.name);
        const rollSlider =
            <RollSlider
                rollSliderMin={0}
                rollSliderMax={ROLL_THRESHOLDS.MaxHard}
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
                runwayChange={(runway) => this.onRunwayFilterChanged(runway)} />;

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
root.render(<TTFCard />);

console.log("Rendered ttfCard!");

export { TTFCard };
