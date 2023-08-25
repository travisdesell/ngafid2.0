import ReactDOM from "react-dom";
import React from "react";
import {
    Cartesian3,
    Ion, Math, IonResource,
     JulianDate,
    SampledPositionProperty,
    TimeIntervalCollection,
    TimeInterval,
    PathGraphics, Color,
    PolylineOutlineMaterialProperty, PolylineGraphics, CornerType
} from "cesium";
import {Viewer, Entity, Scene, Globe, Clock, SkyAtmosphere, ModelGraphics, Model} from "resium";
import * as Cesium from "cesium";


class CesiumPage extends React.Component {


    constructor(props) {
        super(props);
        Ion.defaultAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJiNzg1ZDIwNy0wNmRlLTQ0OWUtOTUwZS0zZTI4OGM0NTFlODIiLCJpZCI6MTYyNDM4LCJpYXQiOjE2OTI5MDc0MzF9.ZtqAnFch5mkZWLZdmNY2Zh-pNH_-XhUPhMrBZSsxyjw";
        Math.setRandomNumberSeed(9);
        console.log("in constructor");
        const urlParams = new URLSearchParams(window.location.search);
        let flightId = urlParams.get("flight_id");
        var cesiumData = this.getCesiumData(flightId);
    //    var url = this.loadModel(cesiumData[flightId]["airframeType"])
    //    console.log(url);
        this.state = {
            cesiumData: cesiumData,
            modelURL: null,
            positionProperty: null,
            flightId : flightId,
            airFrameModels : {},
            modelLoaded : null,
            phaseChecked : {}
        };
         
        console.log(this.state.modelURL);


        // this.setState(this.state);

    }

    async loadModel(airplaneType) {
        var url = null;
        if (airplaneType == "Fixed Wing" || airplaneType == "UAS Fixed Wing") {
            await IonResource.fromAssetId(1084423).then().then(function (value) {
                console.log(value._ionEndpointResource);
                url = value._ionEndpointResource;
            });
        }
        else if (airplaneType == "UAS Rotorcraft") {
            await IonResource.fromAssetId(1117220).then().then(function (value) {
                console.log(value._ionEndpointResource);
                url = value._ionEndpointResource;
            });
        }

        return url;
    }
    getPositionProperty(flightId) {
        var positionProperty = new Cesium.SampledPositionProperty();
        var infoAglDemo = this.state.cesiumData[flightId]["flightGeoInfoAgl"];
        var infAglDemoTimes = this.state.cesiumData[flightId]["flightAglTimes"];
        var positions = Cartesian3.fromDegreesArrayHeights(infoAglDemo);
        for (let i = 0; i < positions.length; i++ ) {
            positionProperty.addSample(Cesium.JulianDate.fromIso8601(infAglDemoTimes[i]), positions[i]);
        }
        return positionProperty;
    }

    componentDidMount() {
        console.log("in will mount");
        var flightId = this.state.flightId;
        // this.state.modelURL = this.loadModel(this.state.cesiumData[flightId]["airframeType"]);
        // this.setState(this.state);
        this.state.positionProperty = this.getPositionProperty(flightId);
        console.log(this.state.modelURL);

        this.setState(this.state);
    }


    getCesiumData(flightId) {
        var cesiumData = null;
        var submissionData = {
            "flight_id" : flightId
        };
        $.ajax({
            type : 'POST',
            url : '/protected/cesium_data',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log(response)
                cesiumData = response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log(errorThrown);
            },
            async: false
        });

        return cesiumData;

    }
    getFlightKeepGroundEntity(type, data) {
        let positionsArr = Cartesian3.fromDegreesArrayHeights(data);
        let colorMap = {
            "default": {
                color: Color.LEMONCHIFFON,
                outlineColor: Color.BLACK
            },
            "Taxiing": {
                color: Color.BLUE,
                outlineColor: Color.BLACK
            },
            "Takeoff": {
                color: Color.PURPLE,
                outlineColor: Color.BLACK
            },
            "Climb": {
                color: Color.SADDLEBROWN,
                outlineColor: Color.BLACK
            }
        }

        let entity = (
            <Entity
                polyline={ new PolylineGraphics({
                    positions: positionsArr,
                    width: 3,
                    material: new PolylineOutlineMaterialProperty({
                        color: colorMap[type]["color"],
                        outlineColor: colorMap[type]["outlineColor"],
                        outlineWidth: 2
                    }),
                    clampToGround: true
                }
               )}
            >
            </Entity>
        );

        return entity;
    }

    getLoadAGLEntity(type, data) {

        let detailsMap = {
            "default" : {
                positions : Cartesian3.fromDegreesArrayHeights(data),
                material: Color.WHITESMOKE.withAlpha(0),
                cornerType: CornerType.BEVELED,
            },
            "Taxiing" : {
                positions: Cartesian3.fromDegreesArrayHeights(data),
                material: Color.BLUE.withAlpha(0.8),
                cornerType: CornerType.BEVELED,
            },
            "Takeoff" : {
                positions: Cartesian3.fromDegreesArrayHeights(data),
                material: Color.BLUE.withAlpha(0.8),
                cornerType: CornerType.BEVELED,
            },
            "Climb" : {
                positions: Cartesian3.fromDegreesArrayHeights(data),
                material: Color.SADDLEBROWN.withAlpha(0.8),
                cornerType: CornerType.BEVELED,
            }
        }
        let today = new Date();
        let entity = (
            <Entity
                name={"NGAFID CESIUM : " + type + " " + this.state.flightId}
                description={"<a><h3> NGAFID Flight ID: " + this.state.flightId + "</h3> </a>" + " <hr> <p> Reanimation of Flight - " + type +
                    "</p>" + "<hr> <p> Displaying Time: " + today + "</p>"}
                wall={ detailsMap[type]}
            >
            </Entity>
        );

        return entity;
    }

    getFlightLineEntity(type, data) {

        let positionsArr = Cartesian3.fromDegreesArrayHeights(data);
        let colorMap = {
            default : {
                color: Color.BLUE,
                outlineColor: Color.BLACK
            },
            Taxiing : {
                color: Color.BLUE,
                outlineColor: Color.BLACK
            },
            Takeoff : {
                color: Color.PURPLE,
                outlineColor: Color.BLACK
            },
            Climb: {
                color: Color.SADDLEBROWN,
                outlineColor: Color.BLACK
            }
        };
        console.log(colorMap[type].color)
        var today = new Date();

        let entity = (
            <Entity
                name={"NGAFID CESIUM FLIGHT  " + type}
                description={"<a><h3> NGAFID Flight ID: " + this.state.flightId + "</h3> </a>" + " <hr> <p> Reanimation of Flight -" + type  + "</p>" + "<hr> <p> Displaying Time: " + today + "</p>"}
                polyline={new PolylineGraphics({
                    positions: positionsArr,
                    width: 3,
                    material: new PolylineOutlineMaterialProperty({
                        color: colorMap[type].color,
                        outlineColor: colorMap[type].outlineColor,
                        outlineWidth:2
                    })
                })}
            >
            </Entity>
        );

        return entity;
    }
    // componentDidMount() {
    //     this.getModel()
    //     console.log("In component did mount");
    //     console.log(this.state.airFrameModels["Airplane"])
    //     if (this.viewer && this.scene) {
    //         console.log("got viewer object");
    //         viewer.zoomTo(viewer.trackedEntity);
    //     }
    // }

    render() {

        // var cesiumData = this.getCesiumData();
        // console.log(cesiumData);
        console.log("in render");
        console.log("model");
        console.log(this.state.model);
        console.log("Position Property");
        console.log(this.state.positionProperty);


        var flightPhases = ["Select Flight Phase", "Show Taxiing", "Show Takeoff", "Show Climb", "Show Cruise to Final", "Show Full Flight"]
        var togglePathColors = ["Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet", "Black", "White"]

        var cesiumData = this.state.cesiumData;
        var startTime = cesiumData[this.state.flightId].startTime;
        var endTime = cesiumData[this.state.flightId].endTime;

        var infoAglDemo = cesiumData[this.state.flightId].flightGeoInfoAgl;
        var pathColor = Color.fromRandom();

        let model = this.state.airFrameModels["Airplane"];
        this.state.modelLoaded = model;
        console.log("model");
        console.log(this.state.modelURL);
        let taxiing = cesiumData[this.state.flightId].flightGeoAglTaxiing;
        let takeOff = cesiumData[this.state.flightId].flightGeoAglTakeOff;
        let climb = cesiumData[this.state.flightId].flightGeoAglClimb;

        //default entities
        let geoFlightLoadAGLEntireDemoEntity = this.getLoadAGLEntity("default", infoAglDemo);
        let geoFlightKeepGroundEntireEntity = this.getFlightKeepGroundEntity("default", infoAglDemo)

        //taxiing entitites
        let geoFlightLoadAGLTaxiEntity = this.getLoadAGLEntity("Taxiing", taxiing);
        let geoFlightlineTaxiEntity = this.getFlightLineEntity("Taxiing", taxiing);
        let geoFlightKeepGroundTaxiEntity = this.getFlightKeepGroundEntity("Taxiing", taxiing);

        //takeoff entities
        let geoFlightLoadAGLTakeOffEntity = this.getLoadAGLEntity("Takeoff", takeOff);
        let geoFlightlineTakeOffEntity = this.getFlightLineEntity("Takeoff", takeOff);
        let geoFlightKeepGroundTakeOffEntity = this.getFlightKeepGroundEntity("Takeoff", takeOff);

        // climb entities
        let geoFlightLoadAGLClimbfEntity = this.getLoadAGLEntity("Climb", climb);
        let geoFlightlineClimbEntity = this.getFlightLineEntity("Climb", climb);
        let geoFlightKeepGroundClimbEntity = this.getFlightKeepGroundEntity("Climb", climb);

            return (
                <div>
                    <div id="cesiumContainer">

                        <Viewer full
                                ref={e => {
                                    this.viewer = e ? e.cesiumElement : undefined;
                                }}
                                timeline={true}
                                scene3DOnly={true}
                                selectionIndicator={true}
                                baseLayerPicker={false}

                        >
                            <Clock
                                startTime={JulianDate.fromIso8601(startTime)}
                                stopTime={JulianDate.fromIso8601(endTime)}
                                currentTime={JulianDate.fromIso8601(startTime)}
                                shouldAnimate={true}
                                multiplier={25}
                            >
                            </Clock>
                            <Scene
                                ref={e => {
                                    this.scene = e ? e.cesiumElement : undefined;
                                }}
                            >
                            </Scene>
                            <SkyAtmosphere
                                hueShift={0.0}
                                saturationShift={0.0}
                                brightnessShift={0.0}
                            ></SkyAtmosphere>
                            <Globe
                                depthTestAgainstTerrain = {true}
                                atmosphereHueShift={0.0}
                                atmosphereSaturationShift={0.0}
                                atmosphereBrightnessShift={0.0}
                            >
                            </Globe>

                            {
                                <Entity
                                    availability={new TimeIntervalCollection([new TimeInterval({start: JulianDate.fromIso8601(startTime), stop: JulianDate.fromIso8601(endTime)})])}
                                    position={this.state.positionProperty}
                                    orientation={new Cesium.VelocityOrientationProperty(this.state.positionProperty)}
                                    path={new PathGraphics({
                                            width : 5,
                                            material : new PolylineOutlineMaterialProperty({
                                                color : pathColor,
                                                outlineColor: pathColor,
                                                outlineWidth: 5
                                            })
                                        }
                                    )}
                                    point={{ pixelSize: 10 , color: Color.RED}}
                                    tracked={true}
                                >
                                    <ModelGraphics
                                    uri={"./images/model.glb"}
                                    scale={1}
                                    minimumPixelSize={128}
                                    maximumScale={2000}
                                    >
                                    </ModelGraphics>
                                </Entity>
                            }
                            {geoFlightLoadAGLEntireDemoEntity}
                            {geoFlightKeepGroundEntireEntity}
                            {geoFlightKeepGroundTaxiEntity}
                            {geoFlightLoadAGLTaxiEntity}
                            {geoFlightlineTaxiEntity}
                            {geoFlightLoadAGLClimbfEntity}
                            {geoFlightKeepGroundClimbEntity}
                            {geoFlightlineClimbEntity}
                        </Viewer>
                    </div>
                    <div className="dropdown">
                        <button className="dropdown-toggle" type="button" data-toggle="dropdown">Select Flight Phase</button>
                        <div className="dropdown-menu">
                            {flightPhases.map((phase, index) => (
                                <div
                                    key={index}
                                    // className={`dropdown-item ${selectedOptions.includes(option.value) ? 'active' : ''}`}
                                    // onClick={() => handleOptionClick(option.value)}
                                >
                                    {phase}
                                </div>
                            ))}
                        </div>
                    </div>
                    {/*    <div id="toolbar">*/}
                    {/*        <select className="cesium-button" id="dropdown">*/}
                    {/*            {*/}
                    {/*                flightPhases.map((phase, index) => {*/}
                    {/*                    return (*/}
                    {/*                        <div key={index} className="form-check">*/}
                    {/*                            <input className="form-check-input" type="checkbox" value="" id={"phase-check-" + index} checked={this.state.phaseChecked[phase]}></input>*/}
                    {/*                            <label className="form-check-label">{phase}</label>*/}
                    {/*                        </div>*/}
                    {/*                    )*/}
                    {/*                    }*/}
                    {/*                )*/}
                    {/*            }*/}
                    {/*        </select>*/}
                    {/*</div>*/}
                    <div id="toggle-path-color">
                        <select className="cesium-button" id="color-options">
                            <option value="0">Select Current Flight Path Color</option>
                            {
                                togglePathColors.map((color, index) => (<option key={index}>{color}</option>))
                            }
                        </select>
                    </div>
                </div>
            )



    }
}

var cesiumPage = ReactDOM.render(
    <CesiumPage></CesiumPage>,
    document.querySelector("#cesium_page")
)

export default CesiumPage
